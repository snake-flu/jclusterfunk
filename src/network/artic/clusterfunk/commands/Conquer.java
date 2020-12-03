package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.MutableRootedTree;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Divides up a tree into roughly equal sized subtrees. This can be used for dynamic tree building approaches.
 * One of either maxSubtreeCount or maxSubtreeSize can be specified (the other should be 0).
 */
public class Conquer extends Command {
    private static final String[] TREE_EXTENSIONS = {".tree", ".nexus", ".nex", ".newick", ".nwk"};

    public Conquer(String inputPath,
                   String outputFileName,
                   FormatType outputFormat,
                   boolean isVerbose) {

        super(isVerbose);

        String path = checkOutputPath(inputPath);

        List<CSVRecord> csv = new ArrayList<>();
        try {
            Reader in = new FileReader(path + "subtrees.csv");
            CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : parser) {
                csv.add(record);
            }
        } catch (IOException e) {
            errorStream.println("Error reading metadata file: " + e.getMessage());
            System.exit(1);
        }

        if (isVerbose) {
            outStream.println("Read subtree table: " + path + "subtrees.csv");
            outStream.println("          Subtrees: " + csv.size());
            outStream.println();
        }

        Map<String, Subtree> subtreeMap = new HashMap<>();
        Map<String, String> rootTaxonMap = new HashMap<>();
        for (CSVRecord row : csv) {
            String subtreeName = row.get("name");
            String subtreeFilename = getFileWithExtension(path + subtreeName, TREE_EXTENSIONS);
            MutableRootedTree tree = new MutableRootedTree(readTree(subtreeFilename));
            Subtree subtree = new Subtree(subtreeName, tree, row.get("root_representitive"), Double.parseDouble(row.get("root_length")));
            rootTaxonMap.put(subtree.rootTaxon, subtreeName);
            subtreeMap.put(subtreeName, subtree);
        }

        for (String key : subtreeMap.keySet()) {
            Subtree subtree = subtreeMap.get(key);
            findSubtrees(subtree, subtree.tree, subtree.tree.getRootNode(), rootTaxonMap, subtreeMap);
        }

        String rootTreeName = null;
        for (String key : subtreeMap.keySet()) {
            Subtree subtree = subtreeMap.get(key);
            if (subtree.parentSubtree == null) {
                if (rootTreeName != null) {
                    errorStream.println("Subtrees are not fully connected: " + rootTreeName + " and " + key + " both have no location" );
                    System.exit(1);

                }
                rootTreeName = key;
            }
        }

        if (rootTreeName == null) {
            errorStream.println("Cannot find a root subtree (circularity)" );
            System.exit(1);
        }

        if (isVerbose) {
            outStream.println("Root tree: " + rootTreeName);
            outStream.println();
        }

        for (Subtree subtree : subtreeMap.values()) {
            if (subtree.parentSubtree != null) {
                MutableRootedTree parentTree = subtree.parentSubtree.tree;
                parentTree.addChild(subtree.tree.getRootNode(), parentTree.getParent(subtree.parentTip));
                parentTree.setLength(subtree.tree.getRootNode(), parentTree.getLength(subtree.parentTip));
                parentTree.removeChild(subtree.parentTip, parentTree.getParent(subtree.parentTip));
            }
        }

        RootedTree superTree = new SimpleRootedTree(subtreeMap.get(rootTreeName).tree);

        if (isVerbose) {
            outStream.println("Writing tree file: " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println("       Total tips: " + superTree.getExternalNodes().size());
            outStream.println();
        }
        writeTreeFile(superTree, outputFileName, outputFormat);
    }

    /**
     * Finds the nodes which transition from not included to included as the root of a subtree.
     * @param tree
     * @param node
     * @param subtreeMap
     */
    private void findSubtrees(Subtree parentSubtree, MutableRootedTree tree, Node node, Map<String, String> rootTaxonMap, Map<String, Subtree> subtreeMap) {
        if (tree.isExternal(node)) {
            String name = tree.getTaxon(node).getName();
            if (rootTaxonMap.containsKey(name)) {
                String subtreeName = rootTaxonMap.get(name);
                Subtree subtree = subtreeMap.get(subtreeName);
                if (subtree.tree != tree) {
                    subtree.parentSubtree = parentSubtree;
                    subtree.parentTip = node;
                }
            }
        } else {
            for (Node child : tree.getChildren(node)) {
                findSubtrees(parentSubtree, tree, child, rootTaxonMap, subtreeMap);
            }
        }
    }

    private static class Subtree {
        public Subtree(String name, MutableRootedTree tree, String rootTaxon, double rootLength) {
            this.name = name;
            this.tree = tree;
            this.rootTaxon = rootTaxon;
            this.rootLength = rootLength;
        }

        String name;
        MutableRootedTree tree;
        String rootTaxon;
        double rootLength;
        Subtree parentSubtree;
        Node parentTip;
    }
}

