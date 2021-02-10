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
    private final static double GENOME_LENGTH = 29903;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.05; // 1% of a 1 SNP branch length

    public Sample(String treeFileName,
                  String metadataFileName,
                  String outputPath,
                  String outputFileStem,
                  FormatType outputFormat,
                  String indexColumn,
                  int indexHeader,
                  String headerDelimiter,
                  int maxTaxa,
                  String primaryAttribute,
                  String secondaryAttribute,
                  String collapseBy,
                  String clumpBy,
                  boolean leaveRepresentative,
                  boolean ignoreMissing,
                  boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        String path = checkOutputPath(outputPath);

        RootedTree tree = readTree(treeFileName);
        MutableRootedTree sampledTree = new MutableRootedTree(tree);

        if (isVerbose) {
            outStream.println("Collapsing branches shorter than " + ZERO_BRANCH_THRESHOLD);
            outStream.println();
        }

        collapsePolytomies(sampledTree, sampledTree.getRootNode(), ZERO_BRANCH_THRESHOLD);

        Map<Taxon, String> taxonMap = getTaxonMap(sampledTree);

//        String collapseAttributeName = "location";
//        String[] metadataFields = new String[] {"location", "adm1", "country"};
//        annotateTips(sampledTree, taxonMap, collapseAttributeName, metadataFields, ignoreMissing);

//        String[] annotationColumns = new String[] { "sample_date", "epi_week", "country", "adm1" };
//
//        for (String columnName: annotationColumns) {
//            annotateTips(sampledTree, taxonMap, columnName, ignoreMissing);
//        }

//        collapseByAttribute(sampledTree, sampledTree.getRootNode(), collapseAttributeName);

        Map<String, Subtree> subtreeMap = new HashMap<>();

        if (collapseBy != null) {
            annotateTips(sampledTree, taxonMap, collapseBy, ignoreMissing);
            pruneCollapsedSubtrees(sampledTree, sampledTree.getRootNode(), collapseBy, leaveRepresentative, subtreeMap);
//        collapseCollapsedSubtrees(sampledTree, sampledTree.getRootNode(), collapseAttributeName);
            int count = subtreeMap.values().stream().mapToInt(subtree -> subtree.tips.size()).sum();
            if (isVerbose) {
                outStream.println("Collapsed subtrees by " + collapseBy + " to " + subtreeMap.size() + " subtrees (containing " + count + " tips)");
                outStream.println();
            }
        }
        if (clumpBy != null) {
            annotateTips(sampledTree, taxonMap, clumpBy, ignoreMissing);
            clumpByAttribute(sampledTree, sampledTree.getRootNode(), clumpBy, leaveRepresentative, subtreeMap);
            if (isVerbose) {
                outStream.println("Clumped tips by " + collapseBy + "");
                outStream.println();
            }
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

    /**
     * Clumps all of the direct child tips of a node with the same attribute into a single representative tip
     * @param tree
     * @param node
     * @param attributeName
     * @return
     */
    private void clumpByAttribute(MutableRootedTree tree, Node node, String attributeName,
                                  boolean leaveRepresentative, Map<String, Subtree> subtrees) {
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                clumpByAttribute(tree, child, attributeName, leaveRepresentative, subtrees);
            }

            Map<String, List<Node>> clumps = new HashMap<>();
            for (Node child : tree.getChildren(node)) {
                if (tree.isExternal(child)) {
                    String value = (String) child.getAttribute(attributeName);
                    List<Node> externalNodes = clumps.getOrDefault(value, new ArrayList<>());
                    externalNodes.add(child);
                    clumps.put(value, externalNodes);
                }
            }
            for (String value : clumps.keySet()) {
                List<Node> externalNodes = clumps.get(value);
                if (externalNodes.size() > 1) {
                    String name = getUniqueHexCode();
                    List<String> tips = externalNodes.stream().map(node1 -> tree.getTaxon(node1).getName()).collect(Collectors.toList());

                    String representative = null;

                    double minLength = Double.MAX_VALUE;
                    for (Node externalNode : externalNodes) {
                        if (tree.getLength(externalNode) < minLength) {
                            minLength = tree.getLength(externalNode);
                            representative = tree.getTaxon(externalNode).getName();
                        }
                        tree.removeChild(externalNode, node);
                    }
                    Node tip = tree.createExternalNode(
                            Taxon.getTaxon(
                                    (leaveRepresentative ? representative + "|" : "") +
                                            name + "|" + value + "|" + tips.size()));
                    tree.addChild(tip, node);
                    tree.setLength(tip, minLength);
                    tip.setAttribute(attributeName, value);
                    tip.setAttribute("tips", tips.size());

                    subtrees.put(name, new Subtree(name, null, attributeName, value, tips));
                }
            }
        } else {
            node.setAttribute("tips", 1);
        }
    }

//    private Set<String> collapseByAttribute(RootedTree tree, Node node, String attributeName) {
//        if (!tree.isExternal(node)) {
//            Set<String> attributes = new HashSet<>();
//            for (Node child : tree.getChildren(node)) {
//                attributes.addAll(collapseByAttribute(tree, child, attributeName));
//            }
//            if (attributes.size() == 1) {
//                node.setAttribute("collapse", true);
//                node.setAttribute(attributeName, attributes.iterator().next());
//            }
//            return attributes;
//        } else {
//            return Collections.singleton((String)node.getAttribute(attributeName));
//        }
//    }


    private void pruneCollapsedSubtrees(MutableRootedTree tree, Node node, String attributeName,
                                        boolean leaveRepresentative, Map<String, Subtree> subtrees) {
        if (!tree.isExternal(node)) {
            Set<Object> attributes = getTipAttributes(tree, node, attributeName).keySet();
            if (attributes.size() == 1) {
                String value = (String)attributes.iterator().next();
                String name = getUniqueHexCode();
                List<Node> externalNodes = tree.getExternalNodes(node);
                String representative = null;
                double minDivergence = Double.MAX_VALUE;
                for (Node tip : externalNodes) {
                    double d = tree.getHeight(node) - tree.getHeight(tip);
                    if (d < minDivergence) {
                        minDivergence = d;
                        representative = tree.getTaxon(tip).getName();
                    }
                }
                List<String> tips = externalNodes.stream().map(node1 -> tree.getTaxon(node1).getName()).collect(Collectors.toList());
                Node parent = tree.getParent(node);
                tree.removeChild(node, parent);
                Node tip = tree.createExternalNode(
                        Taxon.getTaxon(
                                (leaveRepresentative ? representative + "|" : "") +
                                        name + "|" + value + "|" + tips.size()));
                tree.addChild(tip, parent);
                tree.setLength(tip, leaveRepresentative ? minDivergence : tree.getLength(node));
                tip.setAttribute(attributeName, value);
                tip.setAttribute("tips", tips.size());

                subtrees.put(name, new Subtree(name, node, attributeName, value, tips));
            } else {

                for (Node child : tree.getChildren(node)) {
                    pruneCollapsedSubtrees(tree, child, attributeName, leaveRepresentative, subtrees);
                }
            }
        }
    }

    /**
     * @param tree
     * @param node
     * @param minBranchLength
     * @return
     */
    private void collapsePolytomies(MutableRootedTree tree, Node node, double minBranchLength) {
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                collapsePolytomies(tree, child, minBranchLength);
            }

            if (!tree.isRoot(node) && tree.getLength(node) < minBranchLength) {
                Node parent = tree.getParent(node);
                tree.removeChild(node, parent);
                for (Node tip : tree.getChildren(node)) {
                    tree.addChild(tip, parent);
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

