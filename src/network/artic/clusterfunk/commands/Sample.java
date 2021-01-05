package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.MutableRootedTree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class Sample extends Command {
    public Sample(String treeFileName,
                  String metadataFileName,
                  String outputPath,
                  String outputFileStem,
                  FormatType outputFormat,
                  String indexColumn,
                  int indexHeader,
                  String headerDelimiter,
                  int maxTaxa,
                  String[] collapsedAttributes,
                  boolean clumpIdentical,
                  String[] enrichedAttributes,
                  String[] enrichedValues,
                  double enrichedProportion,
                  boolean ignoreMissing,
                  boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        String path = checkOutputPath(outputPath);

        RootedTree tree = readTree(treeFileName);
        MutableRootedTree sampledTree = new MutableRootedTree(tree);

        Map<Taxon, String> taxonMap = getTaxonMap(sampledTree);

        String collapseAttributeName = "country";
        annotateTips(sampledTree, taxonMap, collapseAttributeName, ignoreMissing);

//        String[] annotationColumns = new String[] { "sample_date", "epi_week", "country", "adm1" };
//
//        for (String columnName: annotationColumns) {
//            annotateTips(sampledTree, taxonMap, columnName, ignoreMissing);
//        }

        collapseByAttribute(sampledTree, sampledTree.getRootNode(), collapseAttributeName);

        Map<String, Subtree> subtreeMap = new HashMap<>();

//        pruneCollapsedSubtrees(sampledTree, sampledTree.getRootNode(), collapseAttributeName, subtreeMap);
        collapseCollapsedSubtrees(sampledTree, sampledTree.getRootNode(), collapseAttributeName);

        int count = subtreeMap.values().stream().mapToInt(subtree -> subtree.tips.size()).sum();

        if (isVerbose) {
            outStream.println("Collapsed subtrees by " + collapseAttributeName + " to " + subtreeMap.size() + " subtrees (containing " + count + " tips)");
            outStream.println();
        }

//        if (!ignoreMissing && taxa != null) {
//            if (taxa != null) {
//                for (String key : taxa) {
//                    if (!taxonMap.containsValue(key)) {
//                        errorStream.println("Taxon, " + key + ", not found in tree");
//                        System.exit(1);
//                    }
//                }
//            }
//
//            for (String key : targetTaxaList) {
//                if (!taxonMap.containsValue(key)) {
//                    errorStream.println("Taxon, " + key + ", not found in tree");
//                    System.exit(1);
//                }
//            }
//        }
//
//        // subtree option in JEBL requires the taxa that are to be included
//        Set<Taxon> includedTaxa = new HashSet<>();
//
//        for (Node tip : tree.getExternalNodes()) {
//            Taxon taxon = tree.getTaxon(tip);
//            String index = taxonMap.get(taxon);
//            if ((taxa != null && taxa.contains(index) == keepTaxa) || targetTaxaList.contains(index) == keepTaxa) {
//                includedTaxa.add(taxon);
//            }
//        }

//        if (isVerbose) {
//            outStream.println("   Number of taxa pruned: " + (tree.getExternalNodes().size() - includedTaxa.size()) );
//            outStream.println("Number of taxa remaining: " + includedTaxa.size());
//            outStream.println();
//        }
//
//        if (includedTaxa.size() < 2) {
//            errorStream.println("At least 2 taxa must remain in the tree");
//            System.exit(1);
//        }
//
//        RootedTree outTree = new RootedSubtree(tree, includedTaxa);
//
        String outputFileName = path + outputFileStem + "_tree." + outputFormat.name().toLowerCase();

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(sampledTree, outputFileName, outputFormat);

        String subtreeFileName = path + outputFileStem + "_collapsed_subtrees.csv";

        if (isVerbose) {
            outStream.println("Writing subtree description file, " + subtreeFileName);
            outStream.println();
        }

        writeSubtreeRoots(subtreeMap, subtreeFileName);

//        if (outputMetadataFileName != null) {
//            List<CSVRecord> metadataRows = new ArrayList<>();
//            for (Taxon taxon : includedTaxa) {
//                metadataRows.add(metadata.get(taxonMap.get(taxon)));
//            }
//            if (isVerbose) {
//                outStream.println("Writing metadata file, " + outputMetadataFileName);
//                outStream.println();
//            }
//            writeMetadataFile(metadataRows, outputMetadataFileName);
//        }
    }

    private void sampleDiversity(RootedTree tree, Node node, double divergence, int depth) {
        double length = tree.getLength(node);
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                sampleDiversity(tree, child, divergence + length, depth + 1);
            }
        } else {

        }
    }

    private Set<String> collapseByAttribute(RootedTree tree, Node node, String attributeName) {
        if (!tree.isExternal(node)) {
            Set<String> attributes = new HashSet<>();
            for (Node child : tree.getChildren(node)) {
                attributes.addAll(collapseByAttribute(tree, child, attributeName));
            }
            if (attributes.size() == 1) {
                node.setAttribute("collapse", true);
                node.setAttribute(attributeName, attributes.iterator().next());
            }
            return attributes;
        } else {
            return Collections.singleton((String)node.getAttribute(attributeName));
        }
    }

    private void collapseCollapsedSubtrees(MutableRootedTree tree, Node node, String attributeName) {
        if (!tree.isExternal(node)) {
            if (Boolean.TRUE.equals(node.getAttribute("collapse"))) {
                String value = (String)node.getAttribute(attributeName);
                node.setAttribute(attributeName, value);
                String name = getUniqueHexCode();
                List<Node> externalNodes = tree.getExternalNodes(node);
                List<String> tips = externalNodes.stream().map(node1 -> tree.getTaxon(node1).getName()).collect(Collectors.toList());
                node.setAttribute("Name", name);
                node.setAttribute("!cartoon", "{" + tips.size() + ",0.0}");
//                node.setAttribute("!collapsed", "{\"collapsed\",0.0}");
            } else {

                for (Node child : tree.getChildren(node)) {
                    collapseCollapsedSubtrees(tree, child, attributeName);
                }
            }
        }
    }


    private void pruneCollapsedSubtrees(MutableRootedTree tree, Node node, String attributeName, Map<String, Subtree> subtrees) {
        if (!tree.isExternal(node)) {
            if (Boolean.TRUE.equals(node.getAttribute("collapse"))) {
                String value = (String)node.getAttribute(attributeName);
                String name = getUniqueHexCode();
                List<Node> externalNodes = tree.getExternalNodes(node);
                List<String> tips = externalNodes.stream().map(node1 -> tree.getTaxon(node1).getName()).collect(Collectors.toList());
                Node parent = tree.getParent(node);
                tree.removeChild(node, parent);
                Node tip = tree.createExternalNode(Taxon.getTaxon(name));
                tree.addChild(tip, parent);
                tree.setLength(tip, tree.getLength(node));
                tip.setAttribute(attributeName, value);
                tip.setAttribute("collapsed_tips", tips.size());

                subtrees.put(name, new Subtree(name, node, attributeName, value, tips));
            } else {

                for (Node child : tree.getChildren(node)) {
                    pruneCollapsedSubtrees(tree, child, attributeName, subtrees);
                }
            }
        }
    }

    /**
     */
    void writeSubtreeRoots(Map<String, Subtree> subtreeMap, String outputFileName) {
        
        try {
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

            writer.println("name,attribute_name,attribute_value,tip_count,tips");

            for (String key : subtreeMap.keySet()) {
                Subtree subtree = subtreeMap.get(key);
                writer.print(subtree.name);
                writer.print(",");
                writer.print(subtree.attributeName);
                writer.print(",");
                writer.print(subtree.attributeValue);
                writer.print(",");
                writer.print(subtree.tips.size());
                writer.print(",");
                writer.println(String.join("|", subtree.tips));
            }

            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }

    }

    private class Subtree {
        public Subtree(String name, Node root, String attributeName, String attributeValue, List<String> tips) {
            this.name = name;
            this.root = root;
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
            this.tips = tips;
        }

        final String name;
        final Node root;
        final String attributeName;
        final String attributeValue;
        final List<String> tips;
    }
}

