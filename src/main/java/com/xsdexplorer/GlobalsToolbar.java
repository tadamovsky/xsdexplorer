package com.xsdexplorer;

import static org.apache.xerces.impl.xs.SchemaGrammar.isAnyType;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.impl.dv.DatatypeException;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.SchemaSymbols;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSWildcardDecl;
import org.apache.xerces.xs.*;
import org.apache.xerces.xs.datatypes.ObjectList;

import com.xsdexplorer.uihelpers.Utils;
import com.xsdexplorer.uihelpers.DragResizer;
import com.xsdexplorer.uihelpers.FilterableTreeItem;
import com.xsdexplorer.uihelpers.TreeItemPredicate;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class GlobalsToolbar {
    
    private XSModel model;
    private TreeView<TreeViewRecord> treeView;
    private Consumer<XSObject> selectedCallback;
    
    private TableView<Prop> treeTableView = createPropertiesPanel();
    
    private TextField searchField = new TextField();

    public GlobalsToolbar(XSModel model, Consumer<XSObject> selectedCallback) {
        this.model = model;
        this.selectedCallback = selectedCallback;
        globalModelGroups = fillGlobalModelGroups();
    }

    public Parent createToolBar() {
        VBox vbox = new VBox();
        Node toolBar = createGlobalsTree();
        
        vbox.getChildren().addAll(createTextFilter(), toolBar, treeTableView);
        VBox.setVgrow(treeTableView, Priority.NEVER);
        VBox.setVgrow(toolBar, Priority.ALWAYS);
        vbox.setFillWidth(true);
        
        DragResizer.makeResizable(vbox);
        return vbox;
    }

    private Node createTextFilter() {
        Label label = new Label("Search: ");
        HBox hbox = new HBox(5, label, searchField);
        hbox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(searchField, new Insets(2, 0, 2, 0));
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
            }
        });
        return hbox;
    }
    
    private void initCellFactloty() {
        final ContextMenu contextMenu = createContextMenu();
        treeView.setCellFactory(tv ->  {
            
            TreeCell<TreeViewRecord> cell = new TreeCell<>() {

                @Override
                public void updateItem(TreeViewRecord item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setOnContextMenuRequested(null);
                        setTooltip(null);
                    } else {
                        setText(item.name);
                        setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
                        if (item.xsObject != null) {
                            Tooltip tooltip = new OnDemandTooltip(item.xsObject);
                            tooltip.setShowDelay(Duration.millis(500));
                            tooltip.setShowDuration(Duration.millis(10000));
                            //tooltip.setWrapText(true);
                            //tooltip.setPrefWidth(200);
                            setTooltip(tooltip);
                        }
                    }
                }
            };
            
            
            cell.setOnMouseClicked(e -> {
                if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2 && !cell.isEmpty()) {
                    TreeItem<TreeViewRecord> sel = cell.getTreeItem();
                    XSObject ret = sel.getValue().xsObject;
                    if (ret != null) {
                        selectedCallback.accept(ret);
                    }
                }
            });
            return cell ;
        });
    }
    
    private static ContextMenu createContextMenu() {
        MenuItem editItem = new MenuItem("Copy");
        ContextMenu contextMenu = new ContextMenu(editItem);
        editItem.setOnAction(e -> {
            
            Object ownerNode = contextMenu.getOwnerNode();
            if (ownerNode instanceof TreeCell<?> cell) {
                //System.out.println(cell.getText());
                Utils.clipboardCopy(cell.getText());
            }
        });

        return contextMenu;
    }
    
    
    private Node createGlobalsTree() {
        TreeView<TreeViewRecord> treeView = this.treeView = new TreeView<>();
        initCellFactloty();
        TreeItem<TreeViewRecord> rootItem = new TreeItem<>(new TreeViewRecord("Globals"));
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.setPrefHeight(200);

        //treeView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked); 
        StringList namespaces = model.getNamespaces();
        //boolean includeSchemaNs = namespaces.size() == 1; //show xsd namespace for schema of schema only
        for (int i = 0; i < namespaces.size(); ++i) {
            String ns = (String) namespaces.get(i);
            //if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(ns) && !includeSchemaNs) {
            //    continue;
            //}            
            List<XSObject> elements = getComponentsByNamespace(ELEMENT_DECLARATION, ns).stream()
                    .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).toList();
            Map<Boolean, List<XSObject>> coll = getComponentsByNamespace(TYPE_DEFINITION, ns).stream()//.filter(t -> t instanceof XSComplexTypeDecl)
                    .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).collect(Collectors.partitioningBy(t -> t instanceof XSComplexTypeDecl));
            
            List<XSObject> complexTypes = coll.get(true);
            List<XSObject> simpleTypes = coll.get(false);
            
            if (elements.isEmpty() && complexTypes.isEmpty() && simpleTypes.isEmpty()) {
                continue;
            }            
            TreeItem<TreeViewRecord> nsItem = new TreeItem<>(new TreeViewRecord(ns == null || ns.isEmpty() ? "<no namespace>" : ns));
            rootItem.getChildren().add(nsItem);
            nsItem.setExpanded(true);
            
            if (!elements.isEmpty()) {
                FilterableTreeItem<TreeViewRecord> elItem = new FilterableTreeItem<>(new TreeViewRecord("element"));
                nsItem.getChildren().add(elItem);
                for (XSObject o : elements) {
                    elItem.getInternalChildren().add(new TreeItem<>(new TreeViewRecord(o.getName(), o)));
                }
                bindSearchProperty(elItem);
                elItem.getBackingList().addListener(new SmallListExpander(elItem));
            }
            
            if (!complexTypes.isEmpty()) {
                FilterableTreeItem<TreeViewRecord> typesItem = new FilterableTreeItem<>(new TreeViewRecord("complexType"));
                nsItem.getChildren().add(typesItem);
                for (XSObject o : complexTypes) {
                    typesItem. getInternalChildren().add(new TreeItem<>(new TreeViewRecord(o.getName(), o)));
                }
                bindSearchProperty(typesItem);
                typesItem.getBackingList().addListener(new SmallListExpander(typesItem));
            }
            
            if (!simpleTypes.isEmpty()) {
                FilterableTreeItem<TreeViewRecord> typesItem = new FilterableTreeItem<>(new TreeViewRecord("simpleType"));
                nsItem.getChildren().add(typesItem);
                for (XSObject o : simpleTypes) {
                    typesItem.getInternalChildren().add(new TreeItem<>(new TreeViewRecord(o.getName(), o)));
                }
                bindSearchProperty(typesItem);
                typesItem.getBackingList().addListener(new SmallListExpander(typesItem));
            }
            
        }
        
        return treeView;
    }
    
    private TreeItemPredicate<TreeViewRecord> searchTreeItemPredicate() {
        if ((searchField.getText() == null) || (searchField.getText().isEmpty())) {
            return null;
        }
         return this::shouldFilter;
    }    
    private boolean shouldFilter(TreeItem<TreeViewRecord> parent, TreeViewRecord value) {
        String search = searchField.getText();
        return StringUtils.containsIgnoreCase(value.name, search);
    }
    
    private void bindSearchProperty(FilterableTreeItem<TreeViewRecord> treeItem) {
        treeItem.predicateProperty().bind(
                Bindings.createObjectBinding(this::searchTreeItemPredicate, searchField.textProperty()));
    }
    
    @SuppressWarnings("unchecked")
    private Collection<XSObject> getComponentsByNamespace(short ctype, String ns) {
        return model.getComponentsByNamespace(ctype, ns).values();
    }

    @SuppressWarnings("unchecked")
    private Collection<XSModelGroupDefinition> getModelGroups() {
        return model.getComponents(XSConstants.MODEL_GROUP_DEFINITION).values();
    }
    
    private Map<XSModelGroup, String> globalModelGroups;
    
    private Map<XSModelGroup, String> fillGlobalModelGroups() {
        Map<XSModelGroup, String> ret = new HashMap<>();
        for (XSModelGroupDefinition mgd : getModelGroups()) {
            ret.put(mgd.getModelGroup(), mgd.getName());
        }
        return ret;
    }
    
    private TableView<Prop> createPropertiesPanel() {
        TableView<Prop> treeTableView = new TableView<>();
        treeTableView.setPrefHeight(220);
        treeTableView.setMaxHeight(Region.USE_PREF_SIZE);

        TableColumn<Prop, String> treeTableColumn1 = new TableColumn<>("Key");
        TableColumn<Prop, Object> treeTableColumn2 = new TableColumn<>("Value");

        treeTableColumn1.setCellValueFactory(new PropertyValueFactory<>("key"));
        treeTableColumn2.setCellValueFactory(new PropertyValueFactory<>("value"));

        treeTableColumn2.setCellFactory((tableColumn) -> {
            TableCell<Prop, Object> tableCell = new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    }
                    else {
                        if (item instanceof XSTypeDefinition td) {
                            setText(null);
                            Hyperlink h = new Hyperlink(td.getName());
                            setGraphic(h);
                            h.setOnAction(e -> {
                                selectedCallback.accept(td);
                            });                            
                        }
                        else {
                            setText(item == null ? null : item.toString());
                            setGraphic(null);
                        }
                    }
                }
            };

            return tableCell;
        });        
        
        treeTableView.getColumns().add(treeTableColumn1);
        treeTableView.getColumns().add(treeTableColumn2);
        
        //ctrl-c copy value to clipboard
        treeTableView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.C && e.isControlDown()) {
                var selectedItem = treeTableView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String value = selectedItem.toString();
                    Utils.clipboardCopy(value);
                }
            }
        });
        return treeTableView;
    }
    
    private static Prop prop(String key, Object value) {
        return new Prop(key, value);
    }
    
    public static class Prop {
        String key;
        Object value;
        
        Prop(String key, Object value) {
            this.key = key;
            this.value = value;
        }
        
        public String getKey() {
            return key;
        }
        public Object getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            if (value instanceof XSTypeDefinition td) {
                return td.getName();
            }
            return value == null ? "" : value.toString();
        }
    }
    
    private List<Prop> getElementProps(XsdTreeNode node) {
        XSElementDeclaration el = (XSElementDeclaration) node.getTerm();
        XSObject parent = node.getParentTreeNode() != null ? node.getParentTreeNode().getTerm() : null;
        List<Prop> m = new ArrayList<>();
        m.add(prop("name", el.getName()));
        if (parent != null) {
            if (parent.getType() == XSConstants.MODEL_GROUP) {
                m.add(prop("isRef", Boolean.valueOf(el.getScope() == XSElementDecl.SCOPE_GLOBAL).toString()));
            }
            m.add(prop("minOccurs", String.valueOf(node.getMinOccurs())));
            m.add(prop("maxOccurs", node.getMaxOccurs() == -1 ? "unbounded" : String.valueOf(node.getMaxOccurs())));
        }
        XSTypeDefinition type = el.getTypeDefinition();
        if (type instanceof XSComplexTypeDecl td) {
            addComplexTypeNameAndDerivedBy(td, m);
            m.add(prop("content", "complex"));
            m.add(prop("mixed", Boolean.valueOf(td.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED).toString()));
            //m.add(Pair.of("substGroup", parent != null && parent.getType() == XSConstants.ELEMENT_DECLARATION ? parent.getName() : ""));
            String substGr = "";
            if (el.getSubstitutionGroupAffiliation() != null) {
                substGr = el.getSubstitutionGroupAffiliation().getName();
            }
            m.add(prop("substGroup", substGr));
            m.add(prop("abstract", Boolean.valueOf(el.getAbstract()).toString()));
        }
        else {//simple
            m.add(prop("type", type));
            m.add(prop("content", "simple"));
            m.add(prop("default", el.getConstraintType() ==  XSConstants.VC_DEFAULT ? el.getValueConstraintValue().getNormalizedValue() : ""));
            m.add(prop("fixed", el.getConstraintType() ==  XSConstants.VC_FIXED ? el.getValueConstraintValue().getNormalizedValue() : ""));
        }
        m.add(prop("nillable", Boolean.valueOf(el.getNillable()).toString()));
        return m;
    }

    private void addComplexTypeNameAndDerivedBy(XSComplexTypeDecl td, List<Prop> m) {
        Object type = null;
        String derived = null;
        if (!td.getAnonymous()) {
            type = td;
        }
        else if (!isAnyType(td.getBaseType()) || td.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION) {
            type = td.getBaseType();
            derived = td.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION ? "extension" : "restriction";
        }
        m.add(prop("type", type));
        m.add(prop("derived by", derived));
    }

    private List<Prop> getModelGroupProps(XsdTreeNode node) {
        XSModelGroup g = node.group();//, XSObject parent
        List<Prop> m = new ArrayList<>();
        m.add(prop("name", globalModelGroups.get(g)));
        m.add(prop("minOccurs", String.valueOf(node.getMinOccurs())));
        m.add(prop("maxOccurs", node.getMaxOccurs() == -1 ? "unbounded" : String.valueOf(node.getMaxOccurs())));
        return m;
    }
    
    private List<Prop> getTypeProps(XsdTreeNode node) {
        XSTypeDefinition td = (XSTypeDefinition) node.getTerm();
        List<Prop> m = new ArrayList<>();
        m.add(prop("name", td.getName()));
        if (td instanceof XSComplexTypeDecl ct) {
            XSTypeDefinition baseT = !isAnyType(td.getBaseType()) || ct.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION 
                    || SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(ct.getTargetNamespace()) ? td.getBaseType() : null;
            m.add(prop("base", baseT));
            m.add(prop("derived by", baseT == null ? "" : ct.getDerivationMethod() == XSConstants.DERIVATION_EXTENSION ? "extension" : "restriction"));

            m.add(prop("mixed", Boolean.valueOf(ct.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED).toString()));
            m.add(prop("abstract", Boolean.valueOf(ct.getAbstract()).toString()));
        }
        else {
            //XSSimpleTypeDefinition t1 = (XSSimpleTypeDefinition) td;
            XSSimpleTypeDecl t2 = (XSSimpleTypeDecl) td;
            XSTypeDefinition baseType = t2.getBaseType();
            if (baseType != null && !"".equals(baseType.getName()))
                m.add(prop("base", baseType));
            ObjectList enumVals = t2.getActualEnumeration();
            if (!enumVals.isEmpty())
                m.add(prop("enum", enumVals.toString()));
            if (t2.getVariety() != XSSimpleTypeDefinition.VARIETY_UNION) {
                try {
                    final String[] whitespace = { "preserve", "replace", "collapse" };
                    String val = whitespace[t2.getWhitespace()]; //WS_PRESERVE, WS_REPLACE, WS_COLLAPSE
                    m.add(prop("whitespace", val));
                } catch (DatatypeException e) {
                }
            }
            else {
                XSObjectList memberTypes = t2.getMemberTypes();
                m.add(prop("union", memberTypes.toString()));
            }
            StringList lexicalPattern = t2.getLexicalPattern();
            if (!lexicalPattern.isEmpty())
                m.add(prop("pattern", lexicalPattern.toString()));
            {
                XSFacet facet = (XSFacet) t2.getFacet(XSSimpleTypeDefinition.FACET_LENGTH);
                m.add(prop("length", facet != null ? facet.getLexicalFacetValue(): null));
                facet = (XSFacet) t2.getFacet(XSSimpleTypeDefinition.FACET_MINLENGTH);
                m.add(prop("minLength", facet != null ? facet.getLexicalFacetValue(): null));
                facet = (XSFacet) t2.getFacet(XSSimpleTypeDefinition.FACET_MAXLENGTH);
                m.add(prop("maxLength", facet != null ? facet.getLexicalFacetValue(): null));
            }
        }

        return m;
    }
    
    private List<Prop> getWildcardProps(XsdTreeNode node) {
        XSWildcardDecl w = (XSWildcardDecl) node.getTerm();
        List<Prop> m = new ArrayList<>();
        m.add(prop("name", w.getName()));
        //w.getConstraintType()
        //StringList nsConstraintList = w.getNsConstraintList();
        m.add(prop("description", w.toString()));
        return m;
    }

    private List<Prop> getAttributeProps(XSAttributeUse attr) {
        XSAttributeDeclaration decl = attr.getAttrDeclaration();
        List<Prop> m = new ArrayList<>();
        m.add(prop("name", decl.getName()));
        m.add(prop("type", decl.getTypeDefinition()));
        m.add(prop("required", String.valueOf(attr.getRequired())));
        m.add(prop("default", attr.getConstraintType() ==  XSConstants.VC_DEFAULT ? attr.getValueConstraintValue().getNormalizedValue() : ""));
        m.add(prop("fixed", attr.getConstraintType() ==  XSConstants.VC_FIXED ? attr.getValueConstraintValue().getNormalizedValue() : ""));
        
        return m;
    }

    
    public void populatePropsFor(Object treeNode) {
        ObservableList<Prop> children = treeTableView.getItems();
        children.clear();
        List<Prop> props = Collections.emptyList();
        if (treeNode instanceof XsdTreeNode node) {
            XSObject term = node.getTerm();
            switch (term.getType()) {
            case XSConstants.ELEMENT_DECLARATION:
                props = getElementProps(node);
                break;
            case XSConstants.MODEL_GROUP:
                props = getModelGroupProps(node);
                break;
            case XSConstants.TYPE_DEFINITION:
                props = getTypeProps(node);
                break;
            case XSConstants.WILDCARD:
                props = getWildcardProps(node);
                break;
                
            }
        }
        else if (treeNode instanceof XSAttributeUse node) { 
            props = getAttributeProps(node);
        }
        children.addAll(props);
    }

    private static class TreeViewRecord {
        String name;
        XSObject xsObject;
        
        public TreeViewRecord(String name) {
            this.name = name;
        }
        public TreeViewRecord(String name, XSObject xsObject) {
            this.name = name;
            this.xsObject = xsObject;
        }
        @Override
        public String toString() {
            return name;
        }
    }
    
    private static class OnDemandTooltip extends Tooltip {
        private XSObject o;
        public OnDemandTooltip(XSObject o) {
            super();
            this.o = o;
        }

        @Override
        protected void show() {
            if (getGraphic() == null) {
                String tip;
                if (o instanceof XSElementDeclaration xsEl) {
                    tip = xsEl.getName() +": "+ xsEl.getTypeDefinition().toString();
                }
                else
                    tip = o.toString();
                tip = tip.replace(", ", "\n");
                tip = fixParticle(tip);
                
                Label label = new Label(tip);
                label.setWrapText(true);
                label.setMinHeight(Region.USE_PREF_SIZE);
                //label.setPrefWidth(300);
                label.setMinWidth(Region.USE_PREF_SIZE);
                setGraphic(label);
                
            }
            super.show();
        }

        //particle='("http://www.springframework.org/schema/beans":description{0-1},("http://www.springframework.org/schema/beans":bean|"http://www.springframework.org/schema/beans":ref|"http://www.springframework.org/schema/beans":idref|"http://www.springframework.org/schema/beans":value|"http://www.springframework.org/schema/beans":null|"http://www.springframework.org/schema/beans":array|"http://www.springframework.org/schema/beans":list|"http://www.springframework.org/schema/beans":set|"http://www.springframework.org/schema/beans":map|"http://www.springframework.org/schema/beans":props|(WC[##other:"http://www.springframework.org/schema/beans",""]){0-UNBOUNDED}){0-UNBOUNDED})'
        //remove ns and limit length
        private String fixParticle(String tip) {
            int i1 = tip.indexOf("particle='");
            if (i1 == -1) return tip;
            int i2 = tip.indexOf('\n', i1);
            if (i2 == -1) i2 = tip.length();
            String part = tip.substring(i1, i2);
            String p = part.replaceAll("\"[^\"]+\":", "");
            if (p.length() > 100)
                p = p.substring(0, 97) + "...";
            return tip.replace(part, p);
        }
    }    
    
    private static class SmallListExpander implements ListChangeListener<TreeItem<TreeViewRecord>> {
        private FilterableTreeItem<TreeViewRecord> elItem;

        SmallListExpander(FilterableTreeItem<TreeViewRecord> elItem) {
            this.elItem = elItem;
        }

        @Override
        public void onChanged(Change<? extends TreeItem<TreeViewRecord>> c) {
            while (c.next()) {
                if (c.wasRemoved() && c.getList().size() < 6) {
                    elItem.setExpanded(true);
                    return;
                }
            }
        }
        
    }
}
