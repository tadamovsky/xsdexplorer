package com.xsdexplorer.loader;

import static org.apache.xerces.impl.xs.SchemaGrammar.isAnyType;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.xs.*;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.xsdexplorer.XsdExplorer;
import com.xsdexplorer.LogView.Kind;

public class RootSelector {
	private XSModel model;
    private MutableGraph<XSObject> graph;

    private IdentityHashMap<XSObject, MutableInt> inlineElementCount = new IdentityHashMap<>();
    
    private List<XSElementDecl> globals = new ArrayList<>();

	public RootSelector(XSModel model) {
		this.model = model;

	}
	
	//select root by element containing maximum number of elements
	public XSElementDeclaration selectRoot() {
		XSNamedMap map = model.getComponents(ELEMENT_DECLARATION);
		if (map.isEmpty())
		    return null;

		if (map.size() == 1) {
			return (XSElementDeclaration) map.item(0);
		}
		
        graph = GraphBuilder.directed().allowsSelfLoops(false).build();
        
		for (int i=0; i<map.getLength(); ++i) {
			XSElementDecl el = (XSElementDecl) map.item(i);
			fillGlobalsElement(el);
		}
		
		XSNamedMap types = model.getComponents(TYPE_DEFINITION);
        for (int i=0; i<types.getLength(); ++i) {
            XSTypeDefinition td = (XSTypeDefinition) types.item(i);
            if (isAnyType(td) || !(td instanceof XSComplexTypeDecl)) 
                continue;
            fillGlobalType((XSComplexTypeDecl) td);
        }

        removeSingleNodes();
        System.out.println("Graph with "+graph.nodes().size()+" nodes, "+graph.edges().size()+" edges");
        
        ArrayList<XSObject> topLevel = new ArrayList<>();
        
        MutableGraph<XSObject> origGraph = Graphs.copyOf(graph);
        
        //works faster with transpose graph then transitiveClosuer 
        Graph<XSObject> transpose = Graphs.transpose(graph); //note it is "live" copy, deleting nodes affect it as well
        for (XSElementDeclaration node : globals) {
            if (!graph.nodes().contains(node)) 
                continue;

            boolean isTopLevel = graph.inDegree(node) == 0;
            Set<XSObject> successors = Graphs.reachableNodes(graph, node);
            if (!isTopLevel) { // check for loops
                List<XSObject> predecessors = Graphs.reachableNodes(transpose, node).stream().filter(n -> n instanceof XSElementDeclaration).toList();
                if (successors.containsAll(predecessors)) {
                    isTopLevel = true; //loop on top level (all predecessors have same node count). We take only this one and remove others
                }
            }
            if (isTopLevel) {
                topLevel.add(node);
            }
            for (XSObject p : successors) { // includes node
                graph.removeNode(p);
            }
        }
        
        
  
        System.out.println("Topl level nodes: "+topLevel);
        int maxSum = 0;
        XSElementDeclaration best = null;
        for (XSElementDeclaration global : globals) {
            if (topLevel.contains(global)) {
                Set<XSObject> reachableNodes = Graphs.reachableNodes(origGraph, global);
                int sum = reachableNodes.stream().mapToInt(this::getElementCount).sum();
                if (sum > maxSum) {
                    maxSum = sum;
                    best = global;
                }
            }
            else if (!origGraph.nodes().contains(global)) {//ignore el that contained by others
                int sum = getElementCount(global);
                if (sum > maxSum) {
                    maxSum = sum;
                    best = global;
                }
            }
        }
        
        if (best != null) {
            //System.out.println("Best element "+best.getName()+" count: "+maxSum);
            XsdExplorer.addLogViewMessage(Kind.INFO, "root detected: "+best.getName()+" (contains "+maxSum+" child elements)");
            return best;
        }
        
        //all elements have no children
        return (XSElementDeclaration) map.item(0);
	}
	

	private void removeSingleNodes() {
	    List<XSObject> all = new ArrayList<>(graph.nodes());
        for (XSObject o : all) {
            if (graph.degree(o) == 0)
                graph.removeNode(o);
        }
    }

    @SuppressWarnings("unchecked")
	private List<XSParticle> groupParticles(XSModelGroup group) {
		return group.getParticles();
	}

    @SuppressWarnings("unchecked")
    private List<XSElementDeclaration> getSubstitutionGroup(XSElementDeclaration el) {
        List<XSElementDeclaration> ret = model.getSubstitutionGroup(el);
        return ret == null ? Collections.emptyList() : ret;
    }
    
    private void fillGlobalsElement(XSElementDecl el) {
        XSTypeDefinition td = el.getTypeDefinition();
        XSParticle particle = (td instanceof XSComplexTypeDecl t ? t.getParticle() : null);
        if (particle == null)
            return; //not containing other elements
        globals.add(el);
        if (!td.getAnonymous()) {
            graph.putEdge(el, td);
            return;
        }
        processType(el, (XSComplexTypeDecl) td);
    }
	
    private void fillGlobalType(XSComplexTypeDecl td) {
        XSParticle p = td.getParticle();
        if (p == null)
            return;
        processType(td, td);
    }
	
    private void processType(XSObject parent, XSComplexTypeDecl td) {
        XSParticle p = td.getParticle();
        XSTypeDefinition baseType = td.getBaseType();
        XSParticle baseP =  (baseType instanceof XSComplexTypeDecl t ? t.getParticle() : null);
        if (!isAnyType(baseType) && baseP != null) {
            graph.putEdge(parent, baseType);
            if (td.getDerivationMethod() == XSConstants.DERIVATION_RESTRICTION || baseP == p)
                return; //all deps are in base type
        }
        XSModelGroup group = (XSModelGroup) p.getTerm();
        List<XSParticle> groupParticles = groupParticles(group);
        for (XSParticle pp : groupParticles) {
            if (pp != baseP)
                processTerm(parent, pp.getTerm());
        }
    }    
    
    private void incInlineElements(XSObject parent) {
        inlineElementCount.computeIfAbsent(parent, p -> new MutableInt()).increment();
    }
    
    private int getElementCount(XSObject parent) {
        MutableInt ret = inlineElementCount.get(parent);
        return ret == null ? 0 : ret.getValue();
    }

	private void processTerm(XSObject parent, XSTerm term) {
        if (term.getType() == XSConstants.ELEMENT_DECLARATION) {
            XSElementDecl el = (XSElementDecl) term;
            incInlineElements(parent);
            boolean isGlobalRef = (el.getScope() == XSConstants.SCOPE_GLOBAL);
            if (isGlobalRef) {
                if (parent != term)
                    graph.putEdge(parent, term);
                for (XSElementDeclaration subst : getSubstitutionGroup(el)) {
                    if (parent != subst)
                        graph.putEdge(parent, subst);
                }
                return;
            }
            XSTypeDefinition td = el.getTypeDefinition();
            XSParticle particle = (td instanceof XSComplexTypeDecl t ? t.getParticle() : null);
            if (particle == null)
                return;
            if (!td.getAnonymous()) {
                if (parent != td) {
                    graph.putEdge(parent, td);
                }
                return;
            }            
            processType(parent, (XSComplexTypeDecl) td);
        }
        else if (term.getType() == XSConstants.MODEL_GROUP) {
            XSModelGroup group = (XSModelGroup) term;
            List<XSParticle> groupParticles = groupParticles(group);
            for (XSParticle p : groupParticles) {
                processTerm(parent, p.getTerm());
            }
            
        }
    }


	
}
