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
                       String[] tipAttibutes,
                       boolean isVerbose) {

        super(isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        for (String attributeName : tipAttibutes) {
            Map<Object, Set<Node>> attributeValues = collectTipAttributeValues(tree, attributeName);

            List<Object> keys = new ArrayList<>(attributeValues.keySet());
            keys.sort((o1, o2) -> (o1.toString().length() == o2.toString().length() ?
                    o1.toString().compareTo(o2.toString()) :
                    o1.toString().length() - o2.toString().length()));

            if (isVerbose) {
                outStream.println("Attribute: " + attributeName);
                outStream.println("Values: " + String.join(", ", toString(attributeValues.keySet())));
                outStream.println();
            }


//        clearInternalAttributes(tree);

            for (Object value : keys) {
                annotateMonophyleticNodes(tree, attributeName, value, false, attributeName);
            }
        }

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

    /**
     * Performs a parsimony reconstruction of a particular trait
     * @param tree
     * @param attributeName
     */
    private void parsimonyReconstruction(RootedTree tree, String attributeName) {
        parsimonyReconstruction(tree, tree.getRootNode(), attributeName);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @return
     */
    private Set<Object> parsimonyReconstruction(RootedTree tree, Node node, String attributeName) {
        if (tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            return Collections.singleton(value);
        }

        Set<Object> union = null;
        Set<Object> intersection = null;
        for (Node child : tree.getChildren(node)) {
            Set<Object> childSet = parsimonyReconstruction(tree, child, attributeName);
            if (union == null) {
                union = new HashSet<>(childSet);
                intersection = new HashSet<>(childSet);
            } else {
                union.addAll(childSet);
                intersection.retainAll(childSet);
            }
        }

        if (union.size() == 1) {
            node.setAttribute("union", union);
        }
        return union;
    }

    private void collapseSubtrees(RootedTree tree, String attributeName, Object attributeValue) {
        collapseSubtrees(tree, tree.getRootNode(), attributeName, attributeValue, null);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param parentValue
     */
    private void collapseSubtrees(RootedTree tree, Node node, String attributeName, Object attributeValue, Object parentValue) {
        if (!tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            if (attributeValue.equals(value) && !value.equals(parentValue)) {
                node.setAttribute("!collapse", "{\"collapsed\",1.7E-4}");
            }

            for (Node child : tree.getChildren(node)) {
                collapseSubtrees(tree, child, attributeName, attributeValue, value);
            }

        }
    }


    /**
     * When ever a change in the value of a given attribute occurs at a node, writes out a subtree from that node
     * @param tree
     * @param attributeName
     * @param outputFileStem
     */
    private void splitSubtrees(RootedTree tree, String attributeName, Object attributeValue, String outputPath, String outputFileStem) {
        splitSubtrees(tree, tree.getRootNode(), attributeName, attributeValue, null, outputPath, outputFileStem, new HashMap<Object, Integer>());
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param parentValue
     * @param outputFileStem
     */
    private void splitSubtrees(RootedTree tree, Node node, String attributeName, Object attributeValue, Object parentValue,
                               String outputPath, String outputFileStem, Map<Object, Integer> prunedMap) {
        if (!tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            if (attributeValue.equals(value)) {
                if (!value.equals(parentValue)) {
                    node.setAttribute("!collapse", "{\"collapsed\",1.7E-4}");
                    SimpleRootedTree subtree = new SimpleRootedTree();
                    subtree.createNodes(tree, node);

                    String name = value.toString();
                    Integer count = prunedMap.getOrDefault(value, 0);
                    count += 1;
                    if (count > 1) {
                        name += "_" + count;
                    }
                    prunedMap.put(value, count);

                    String fileName = outputPath + outputFileStem + "_" + name + ".nexus";
                    if (isVerbose) {
                        outStream.println("Writing subtree file: " + fileName);
                    }
                    writeTreeFile(subtree, fileName, FormatType.NEXUS);
                }
            }

            for (Node child : tree.getChildren(node)) {
                splitSubtrees(tree, child, attributeName, attributeValue, value, outputPath, outputFileStem, prunedMap);
            }

        }
    }

}

