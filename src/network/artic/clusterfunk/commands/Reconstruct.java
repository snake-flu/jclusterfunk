package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.*;

/**
 *
 */
public class Reconstruct extends Command {

    public Reconstruct(String treeFileName,
                       String outputFileName,
                       FormatType outputFormat,
                       String tipStateAttibuteName,
                       String reconstructedStateAttributeName,
                       boolean isVerbose) {

        super(isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Set<Object> attributeValues = collectTipAttributeValues(tree, tipStateAttibuteName).keySet();

    //        List<Object> keys = new ArrayList<>(attributeValues.keySet());
    //        keys.sort((o1, o2) -> (o1.toString().length() == o2.toString().length() ?
    //                o1.toString().compareTo(o2.toString()) :
    //                o1.toString().length() - o2.toString().length()));

        if (isVerbose) {
            outStream.println("Tip State Attribute: " + tipStateAttibuteName);
            outStream.println("Values: " + String.join(", ", toString(attributeValues)));
            outStream.println();
        }

        parsimonyReconstruction(tree, tipStateAttibuteName, reconstructedStateAttributeName);

//        clearInternalAttributes(tree);

//        for (Object value : keys) {
//            annotateMonophyleticNodes(tree, attributeName, value, false, attributeName);
//        }

//        for (Object value: keys) {
//            collapseSubtrees(tree, attributeName, value);
//        }

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputFileName, outputFormat);

    }

    /**
     * Performs a parsimony reconstruction of a particular trait
     * @param tree
     * @param tipAttributeName
     */
    private void parsimonyReconstruction(RootedTree tree, String tipAttributeName, String nodeAttributeName) {
        parsimonyReconstruction(tree, tree.getRootNode(), tipAttributeName, nodeAttributeName);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param tipAttributeName
     * @return
     */
    private Set<Object> parsimonyReconstruction(RootedTree tree, Node node, String tipAttributeName, String nodeAttributeName) {
        if (tree.isExternal(node)) {
            Object value = node.getAttribute(tipAttributeName);
            return Collections.singleton(value);
        }

        Set<Object> union = null;
        Set<Object> intersection = null;
        for (Node child : tree.getChildren(node)) {
            Set<Object> childSet = parsimonyReconstruction(tree, child, tipAttributeName, nodeAttributeName);
            if (union == null) {
                union = new HashSet<>(childSet);
                intersection = new HashSet<>(childSet);
            } else {
                union.addAll(childSet);
                intersection.retainAll(childSet);
            }
        }

        if (union.size() == 2) {
            node.setAttribute("union", union);
        }

        return union;
    }

    /**
     * Finds the MRCA for a set of tip nodes and then recursively annotates the subtree
     * @param tree
     * @param attributeName
     * @param attributeValue
     */
    private void annotateMonophyleticNodes(RootedTree tree, String attributeName, Object attributeValue, boolean isHierarchical, String newAttributeName) {
        annotateMonophyleticNodes(tree, tree.getRootNode(), attributeName, attributeValue, isHierarchical, newAttributeName);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param attributeValue
     */
    private boolean annotateMonophyleticNodes(RootedTree tree, Node node, String attributeName, Object attributeValue,
                                              boolean isHierarchical, String newAttributeName) {
        boolean isMonophyletic = true;

        if (tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            if (value == null || !(attributeValue.equals(value) || (isHierarchical && attributeValue.toString().startsWith(value.toString())))) {
                return false;
            }
        } else {

            for (Node child : tree.getChildren(node)) {
                isMonophyletic = annotateMonophyleticNodes(tree, child, attributeName, attributeValue, isHierarchical, newAttributeName) && isMonophyletic;
            }
        }

        if (isMonophyletic) {
            node.setAttribute(newAttributeName, attributeValue);
        }

        return isMonophyletic;
    }

}

