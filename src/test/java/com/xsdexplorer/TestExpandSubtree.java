package com.xsdexplorer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.testng.annotations.Test;

import com.xsdexplorer.XsdTreeNode.ExpandSubtreeResult;
import com.xsdexplorer.loader.SchemaLoader;

import javafx.application.Platform;

public class TestExpandSubtree {

    private static volatile boolean fxStarted;

    private static void ensureFx() {
        if (fxStarted) {
            return;
        }
        synchronized (TestExpandSubtree.class) {
            if (fxStarted) {
                return;
            }
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException alreadyStarted) {
            }
            fxStarted = true;
        }
    }

    private <T> T onFx(FxCall<T> call) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return call.run();
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(call.run());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(30, TimeUnit.SECONDS)) {
            fail("timed out waiting for FX thread");
        }
        if (error.get() != null) {
            if (error.get() instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(error.get());
        }
        return result.get();
    }

    @FunctionalInterface
    private interface FxCall<T> {
        T run() throws Exception;
    }

    private XsdTreeNode findElement(XsdTreeNode start, String name) {
        ArrayDeque<XsdTreeNode> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            XsdTreeNode n = queue.removeFirst();
            if (n.isElement() && name.equals(n.getTerm().getName())) {
                return n;
            }
            queue.addAll(n.getSubstGroup());
            for (TreeNodeControl child : n.childNodes()) {
                if (child instanceof XsdTreeNode xn) {
                    queue.add(xn);
                }
            }
        }
        return null;
    }

    @Test
    public void expandsClosedSubtreeAndReportsCount() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/simple/groups.xsd", "Employee");
            XsdTreeNode root = view.getRootNode();
            ExpandSubtreeResult first = root.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            assertFalse(first.limitReached);
            assertFalse(first.alreadyFullyExpanded);
            assertTrue(root.isOpen());
            assertEquals(first.nodeCount, view.getNodeCount());
            assertEquals(first.message(), "Diagram contains " + first.nodeCount + " nodes. Subtree fully expanded.");

            ExpandSubtreeResult second = root.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            assertTrue(second.alreadyFullyExpanded);
            assertFalse(second.limitReached);
            assertEquals(second.nodeCount, view.getNodeCount());
            assertEquals(second.message(), "Diagram contains " + second.nodeCount + " nodes. Subtree already fully expanded.");
            return null;
        });
    }

    @Test
    public void expandsNamedGroupsAndLeavesUnrelatedBranches() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/simple/groups.xsd", "Employee");
            XsdTreeNode root = view.getRootNode();
            root.toggleExpand();
            XsdTreeNode employeeRecord = (XsdTreeNode) root.childNodes().get(0);
            employeeRecord.toggleExpand();
            List<TreeNodeControl> children = employeeRecord.childNodes();
            XsdTreeNode employeeId = (XsdTreeNode) children.get(0);
            XsdTreeNode contactInfo = (XsdTreeNode) children.get(1);
            assertFalse(contactInfo.isOpen());
            int before = view.getNodeCount();
            assertTrue(before > 1);
            ExpandSubtreeResult blocked = contactInfo.expandSubtreeToLimit(before);
            assertTrue(blocked.limitReached);
            assertFalse(contactInfo.isOpen());
            assertEquals(blocked.nodeCount, before);

            ExpandSubtreeResult result = contactInfo.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            assertFalse(result.limitReached);
            assertEquals(result.nodeCount, view.getNodeCount());
            assertTrue(result.nodeCount > before);
            assertTrue(contactInfo.isOpen());
            assertTrue(findElement(contactInfo, "firstName") != null);
            assertFalse(employeeId.isOpen());
            assertTrue(root.isOpen());
            return null;
        });
    }

    @Test
    public void expandsSubstitutionMembers() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/abstract/pub.xsd", "Catalogue");
            XsdTreeNode root = view.getRootNode();
            ExpandSubtreeResult result = root.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            assertFalse(result.limitReached);
            assertEquals(result.nodeCount, view.getNodeCount());
            XsdTreeNode publication = findElement(root, "Publication");
            assertTrue(publication != null);
            assertFalse(publication.getSubstGroup().isEmpty());
            boolean substOpened = publication.getSubstGroup().stream().anyMatch(XsdTreeNode::isOpen);
            assertTrue(substOpened);
            return null;
        });
    }

    @Test
    public void recursionStaysTerminal() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/rec/simpleRec.xsd", "locale");
            XsdTreeNode root = view.getRootNode();
            ExpandSubtreeResult result = root.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            assertFalse(result.limitReached);
            assertEquals(result.nodeCount, view.getNodeCount());
            boolean foundRecursion = false;
            ArrayDeque<TreeNodeControl> stack = new ArrayDeque<>();
            stack.push(root);
            while (!stack.isEmpty()) {
                TreeNodeControl n = stack.pop();
                if (n instanceof RecursionNode) {
                    foundRecursion = true;
                    assertFalse(n.isOpen());
                }
                if (n instanceof XsdTreeNode xn) {
                    for (XsdTreeNode s : xn.getSubstGroup()) {
                        stack.push(s);
                    }
                }
                stack.addAll(n.childNodes());
            }
            assertTrue(foundRecursion);
            return null;
        });
    }

    @Test
    public void firstPopulateMayCrossThresholdThenStops() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/abstract/pub.xsd", "Catalogue");
            XsdTreeNode root = view.getRootNode();
            ExpandSubtreeResult result = root.expandSubtreeToLimit(3);
            assertTrue(root.isOpen());
            assertFalse(result.alreadyFullyExpanded);
            assertEquals(result.nodeCount, view.getNodeCount());
            assertTrue(view.getNodeCount() > 3);
            assertTrue(result.limitReached);
            assertTrue(result.message().contains("stopped after reaching the"));
            return null;
        });
    }

    @Test
    public void expandFullyEnabledWhenDiagramNodeFocused() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/simple/groups.xsd", "Employee");
            assertEquals(view.getFocusedTreeNode(), null);
            view.onNodeFocused(view.getRootNode());
            assertEquals(view.getFocusedTreeNode(), view.getRootNode());
            view.onNodeFocused(new Object());
            assertEquals(view.getFocusedTreeNode(), null);
            view.onNodeFocused(view.getRootNode());
            view.setRootTerm(null, view.getModel());
            assertEquals(view.getFocusedTreeNode(), null);
            return null;
        });
    }

    @Test
    public void expandFullyOnPartiallyAndFullyExpandedSubtree() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/simple/groups.xsd", "Employee");
            XsdTreeNode root = view.getRootNode();
            root.toggleExpand();
            ExpandSubtreeResult partial = root.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            assertFalse(partial.limitReached);
            assertFalse(partial.alreadyFullyExpanded);
            assertEquals(partial.nodeCount, view.getNodeCount());
            ExpandSubtreeResult again = root.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            assertTrue(again.alreadyFullyExpanded);
            assertFalse(again.limitReached);
            assertEquals(again.nodeCount, view.getNodeCount());
            assertEquals(again.message(), "Diagram contains " + again.nodeCount + " nodes. Subtree already fully expanded.");
            return null;
        });
    }

    @Test
    public void expandFullyRetryAfterLimitDoesNotCollapse() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/abstract/pub.xsd", "Catalogue");
            XsdTreeNode root = view.getRootNode();
            ExpandSubtreeResult first = root.expandSubtreeToLimit(3);
            assertTrue(first.limitReached);
            assertTrue(root.isOpen());
            int afterFirst = view.getNodeCount();
            assertEquals(first.nodeCount, afterFirst);
            ExpandSubtreeResult retry = root.expandSubtreeToLimit(3);
            assertTrue(retry.limitReached);
            assertTrue(retry.alreadyFullyExpanded);
            assertTrue(root.isOpen());
            assertEquals(view.getNodeCount(), afterFirst);
            assertEquals(retry.nodeCount, afterFirst);
            assertTrue(retry.message().contains("2,000-node threshold"));
            return null;
        });
    }

    @Test
    public void countsAlreadyCreatedDescendantsAgainstLimit() throws Exception {
        ensureFx();
        onFx(() -> {
            XsdTreeView view = loadTreeOnFx("samples/simple/groups.xsd", "Employee");
            XsdTreeNode root = view.getRootNode();
            root.expandSubtreeToLimit(XsdTreeView.MAX_DIAGRAM_NODES);
            int existing = view.getNodeCount();
            ExpandSubtreeResult again = root.expandSubtreeToLimit(existing);
            assertTrue(again.alreadyFullyExpanded);
            assertFalse(again.limitReached);
            assertEquals(again.nodeCount, existing);
            assertEquals(view.getNodeCount(), existing);
            return null;
        });
    }

    private XsdTreeView loadTreeOnFx(String file, String rootName) throws IOException {
        XSModel model = new SchemaLoader().loadSchemaFile(new File(file));
        XSElementDeclaration el = model.getElementDeclaration(rootName, model.getNamespaces().item(0));
        XsdTreeView view = new XsdTreeView();
        view.setRootTerm(el, model);
        return view;
    }
}
