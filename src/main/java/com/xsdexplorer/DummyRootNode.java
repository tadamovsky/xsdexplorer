package com.xsdexplorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.layout.Region;

//used to wrap substition group nodes of root node
public class DummyRootNode extends TreeNodeControl {
    
    private Region control = new Region();
    private List<TreeNodeControl> layoutChildren;

    public DummyRootNode(XsdTreeNode abstractEl) {
        childNodes = Arrays.asList(abstractEl);
        List<XsdTreeNode> substGroup = abstractEl.getSubstGroup();
        layoutChildren = new ArrayList<>(substGroup.size() + 1);
        layoutChildren.add(abstractEl);
        layoutChildren.addAll(substGroup);
        control.setPrefSize(0, 0);
    }
    
    @Override
    public List<TreeNodeControl> getChildrenForLayout() {
        return layoutChildren;
    }   
    
    @Override
    Region control() {
        return control;
    }

    @Override
    boolean isOpen() {
        return true; //always open
    }

    @Override
    boolean isOptional() {
        return false;
    }

    @Override
    void expand(boolean force) {
    }
    

}
