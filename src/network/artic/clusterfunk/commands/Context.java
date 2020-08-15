package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import network.artic.clusterfunk.FormatType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 *
 */
public class Context extends Command {
    public Context(String treeFileName,
                   String taxaFileName,
                   String[] targetTaxa,
                   String metadataFileName,
                   String outputPath,
                   String outputFileStem,
                   FormatType outputFormat,
                   String outputMetadataFileName,
                   String indexColumn,
                   int indexHeader,
                   String headerDelimiter,
                   int maxParentLevel,
                   int maxChildLevel,
                   boolean ignoreMissing,
                   boolean isVerbose) {

        super(metadataFileName, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        List<String> targetTaxaList = (targetTaxa != null ? Arrays.asList(targetTaxa) : Collections.emptyList());

        if (taxa == null && targetTaxaList.size() == 0) {
            throw new IllegalArgumentException("context command requires a taxon list and/or additional target taxa");
        }

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        String path = checkOutputPath(outputPath);

        if (!ignoreMissing && taxa != null) {
            if (taxa != null) {
                for (String key : taxa) {
                    if (!taxonMap.containsValue(key)) {
                        errorStream.println("Taxon, " + key + ", not found in tree");
                        System.exit(1);
                    }
                }
            }

            for (String key : targetTaxaList) {
                if (!taxonMap.containsValue(key)) {
                    errorStream.println("Taxon, " + key + ", not found in tree");
                    System.exit(1);
                }
            }
        }

        Set<Node> targetTips = new HashSet<>();

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxonMap.get(taxon);
            if ((taxa != null && taxa.contains(index)) || targetTaxaList.contains(index)) {
                targetTips.add(tip);
            }
        }

        Map<Node, Subtree> subtreeMap = new HashMap<>();

        annotateContext(tree, targetTips, maxParentLevel);
        collectSubtrees(tree, subtreeMap);
        collapseSubtrees(tree, targetTips, maxChildLevel);

        writeSubtrees(tree, subtreeMap, path, outputFileStem, false, outputFormat);
    }

    private void annotateContext(RootedTree tree, Set<Node> targetTips, int maxParentLevel) {
        for (Node tip : tree.getExternalNodes()) {
            if (targetTips.contains(tip)) {
                Node node = tip;
                int parentLevel = 0;
                do {
                    node = tree.getParent(node);
                    parentLevel += 1;
                    node.setAttribute("include", true);
                } while (parentLevel < maxParentLevel && !tree.isRoot(node));
            }
        }
    }

    private void collectSubtrees(RootedTree tree, Map<Node, Subtree> subtreeMap) {
        collectSubtrees(tree, tree.getRootNode(), subtreeMap);
    }

    private void collectSubtrees(RootedTree tree, Node node, Map<Node, Subtree> subtreeMap) {
        if (!tree.isExternal(node)) {
            if (node.getAttribute("include") == Boolean.TRUE) {
                subtreeMap.put(node, new Subtree(node, "subtree_" + (subtreeMap.size() + 1)));
            }
            for (Node child : tree.getChildren(node)) {
                collectSubtrees(tree, child, subtreeMap);
            }
        }
    }

    private void collapseSubtrees(RootedTree tree, Set<Node> targetTips, int maxChildLevel) {
        collapseSubtrees(tree, tree.getRootNode(), targetTips, 0, maxChildLevel);
    }

    private void collapseSubtrees(RootedTree tree, Node node, Set<Node> targetTips, int childLevel, int maxChildLevel) {
        if (tree.isExternal(node)) {
            if (targetTips.contains(node)) {

            }
        } else {
            if (childLevel <= maxChildLevel) {
                for (Node child : tree.getChildren(node)) {
                    collapseSubtrees(tree, child, targetTips, childLevel + 1, maxChildLevel);
                }
            } else {
                // collapse the node
                node.setAttribute("include", false);
            }
        }
    }

    /**
     * When ever a change in the value of a given attribute occurs at a node, writes out a subtree from that node
     */
    void writeSubtrees(RootedTree tree, Map<Node, Subtree> subtreeMap,
                       String outputPath, String outputFileStem, boolean labelWithValue, FormatType outputFormat) {

        for (Node key : subtreeMap.keySet()) {
            Subtree subtree = subtreeMap.get(key);

            SimpleRootedTree newTree = new SimpleRootedTree();
            newTree.createNodes(tree, subtree.root);

            String fileName = outputPath + outputFileStem + subtree.name + "." + outputFormat.name().toLowerCase();
            if (isVerbose) {
                outStream.println("Writing subtree file: " + fileName);
            }
            writeTreeFile(newTree, fileName, outputFormat);

        }
    }

    private static class Subtree {
        public Subtree(Node root, String name) {
            this.root = root;
            this.name = name;
        }

        Node root;
        String name;
    }
}

