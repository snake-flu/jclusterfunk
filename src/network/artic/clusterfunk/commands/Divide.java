package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import network.artic.clusterfunk.FormatType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Divides up a tree into roughly equal sized subtrees. This can be used for dynamic tree building approaches.
 * One of either maxSubtreeCount or maxSubtreeSize can be specified (the other should be 0).
 */
public class Divide extends Command {
    public Divide(String treeFileName,
                  String outputPath,
                  String outputFileStem,
                  FormatType outputFormat,
                  int maxSubtreeCount,
                  int minSubtreeSize,
                  boolean isVerbose) {

        super(isVerbose);


        String path = checkOutputPath(outputPath);

        RootedTree tree = readTree(treeFileName);

        Map<Node, Subtree> subtreeMap = new HashMap<>();

        if (maxSubtreeCount > 1) {

            int minSize = 2;
            collectSubtrees(tree, tree.getRootNode(), minSize, subtreeMap);

            while (subtreeMap.keySet().size() > maxSubtreeCount) {
                minSize *= 2;
                subtreeMap.clear();
                clearInternalAttributes(tree);
                collectSubtrees(tree, tree.getRootNode(), minSize, subtreeMap);

                if (isVerbose) {
                    outStream.println("Min subtree size: " + minSize + " - " + subtreeMap.keySet().size() + " subtrees");
                }
            }

        } else if (minSubtreeSize > 1) {
            collectSubtrees(tree, tree.getRootNode(), minSubtreeSize, subtreeMap);
        } else {
            errorStream.println("Specify one or other of max-size and max-count");
            System.exit(1);
        }

        createSubtrees(tree, subtreeMap);

        writeSubtrees(subtreeMap, path, outputFileStem, outputFormat);

        writeSubtreeRoots(subtreeMap, path, outputFileStem);
    }

    /**
     * Finds the nodes which transition from not included to included as the root of a subtree.
     * @param tree
     * @param node
     * @param subtreeMap
     */
    private int collectSubtrees(RootedTree tree, Node node, int maxSubtreeSize, Map<Node, Subtree> subtreeMap) {
        if (!tree.isExternal(node)) {
            int count = 0;

            for (Node child : tree.getChildren(node)) {
                count += collectSubtrees(tree, child, maxSubtreeSize, subtreeMap);
            }

            if (count > maxSubtreeSize) {
                String name = "subtree_" + (subtreeMap.size() + 1);
                node.setAttribute("subtree", name);

                subtreeMap.put(node, new Subtree(node, name, count));
                return 1;
            }

            return count;
        }

        return 1;
    }


    void createSubtrees(RootedTree tree, Map<Node, Subtree> subtreeMap) {

        for (Node key : subtreeMap.keySet()) {
            Subtree subtree = subtreeMap.get(key);

            SimpleRootedTree newTree = new SimpleRootedTree();
            createNodes(tree, subtree.root, newTree);
            subtree.tree = newTree;

            Map<Taxon, Integer> tips = findRootRepresentative(tree, tree.getRootNode(), 0);


        }
    }

    /**
     * Returns a map of all taxa and their distance from a node.
     * @param tree
     * @param node
     * @return
     */
    private Map<Taxon, Integer> findRootRepresentative(RootedTree tree, Node node, int distance) {
        Map<Taxon, Integer> tips  = new HashMap<>();
        
        for (Node child: tree.getChildren(node)) {
            if (tree.isExternal(child)) {
                tips.put(tree.getTaxon(child), distance);
            } else {
                findRootRepresentative(tree, node, distance + 1);
            }
        }

        return tips;
    }

    /**
     * Clones the entire tree structure from the given RootedTree.
     * @param tree
     * @param node
     * @return
     */
    private Node createNodes(RootedTree tree, Node node, SimpleRootedTree newTree) {

        Node newNode;
        if (tree.isExternal(node)) {
            newNode = newTree.createExternalNode(tree.getTaxon(node));
        } else {
            List<Node> children = new ArrayList<Node>();

            for (Node child : tree.getChildren(node)) {
                String subtree = (String)child.getAttribute("subtree");

                if (subtree != null) {
                    // is the root of a subtree - replace with a tip labelled as the subtree
                    Node newChild = newTree.createExternalNode(Taxon.getTaxon(subtree));
                    children.add(newChild);
                    newTree.setHeight(newChild, tree.getHeight(child));
                } else {
                    children.add(createNodes(tree, child, newTree));
                }
            }

            newNode = newTree.createInternalNode(children);
        }

        for( Map.Entry<String, Object> e : node.getAttributeMap().entrySet() ) {
            newNode.setAttribute(e.getKey(), e.getValue());
        }

        newTree.setHeight(newNode, tree.getHeight(node));

        return newNode;
    }

    /**
     * Write all the subtrees...
     */
    void writeSubtrees(Map<Node, Subtree> subtreeMap, String outputPath, String outputFileStem, FormatType outputFormat) {


        for (Node key : subtreeMap.keySet()) {
            Subtree subtree = subtreeMap.get(key);

            String fileName = outputPath + outputFileStem + subtree.name + "." + outputFormat.name().toLowerCase();
            if (isVerbose) {
                outStream.println("Writing subtree file: " + fileName + ", " + subtree.count + " tips");
            }
            writeTreeFile(subtree.tree, fileName, outputFormat);
        }
    }

    /**
     */
    void writeSubtreeRoots(Map<Node, Subtree> subtreeMap,  String outputPath, String outputFileStem) {


        String fileName = outputPath + outputFileStem + "subtrees.csv";
        try {
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(fileName)));

            writer.println("name,count,root_representitive,root_length");


            for (Node key : subtreeMap.keySet()) {
                Subtree subtree = subtreeMap.get(key);
                writer.print(subtree.name);
                writer.print(",");
                writer.print(subtree.count);
                writer.print(",");
                writer.print(subtree.rootRepresentitive);
                writer.print(",");
                writer.print(subtree.rootLength);
            }

            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }

    }
    private static class Subtree {
        public Subtree(Node root, String name, int count) {
            this.root = root;
            this.name = name;
            this.count = count;
        }

        Node root;
        int count;
        String name;
        SimpleRootedTree tree;
        String rootRepresentitive;
        double rootLength;
    }
}

