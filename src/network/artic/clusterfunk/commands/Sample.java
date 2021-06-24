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
    enum CollapseType {
        COLLAPSED("collapsed"),
        CLUMPED("clumped");

        CollapseType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        String name;
    };

    private final static double GENOME_LENGTH = 29903;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.05; // 1% of a 1 SNP branch length

    public Sample(String treeFileName,
                  String metadataFileName,
                  String protectTaxa,
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
                  int minCollapse,
                  int minClump,
                  int maxSoft,
                  boolean ignoreMissing,
                  boolean isVerbose) {

                super(metadataFileName, protectTaxa, indexColumn, indexHeader, headerDelimiter, isVerbose);

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
            clusterByAttribute(sampledTree, sampledTree.getRootNode(), collapseBy, maxSoft, minCollapse, subtreeMap);
            int count = subtreeMap.values().stream().mapToInt(subtree -> subtree.tips.size()).sum();
            if (isVerbose) {
                outStream.println("Collapsed subtrees by " + collapseBy + " to " + subtreeMap.size() + " subtrees (containing " + count + " tips)");
                outStream.println();
            }
        }
        if (clumpBy != null) {
            annotateTips(sampledTree, taxonMap, clumpBy, ignoreMissing);
            clumpByAttribute(sampledTree, sampledTree.getRootNode(), clumpBy, maxSoft, minClump, subtreeMap);
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
            outStream.println("Writing tree file: " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println("   Number of tips: " + tree.getExternalNodes().size());
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

    /**
     * Clumps all of the direct child tips of a node with the same attribute into a single representative tip
     * @param tree
     * @param node
     * @param attributeName
     * @return
     */
    private void clumpByAttribute(MutableRootedTree tree, Node node, String attributeName,
                                  int maxSoftClumpSize, int minClumpSize, Map<String, Subtree> subtrees) {
        if (!tree.isExternal(node)) {
            // recurse down tree
            for (Node child : tree.getChildren(node)) {
                clumpByAttribute(tree, child, attributeName, maxSoftClumpSize, minClumpSize, subtrees);
            }

            // collect all tips hanging off this node and keyed by the value of the attribute
            Map<String, List<Node>> clumps = new HashMap<>();
            for (Node child : tree.getChildren(node)) {
                if (tree.isExternal(child)) {
                    String value = (String) child.getAttribute(attributeName);
                    List<Node> externalNodes = clumps.getOrDefault(value, new ArrayList<>());
                    externalNodes.add(child);
                    clumps.put(value, externalNodes);
                }
            }

            // for all the different attribute values, clump the tips
            for (String value : clumps.keySet()) {
                List<Node> externalNodes = clumps.get(value);
                if (externalNodes.size() >= minClumpSize) {
                    String name = getUniqueHexCode();
                    List<String> tips = externalNodes.stream().map(node1 -> tree.getTaxon(node1).getName()).collect(Collectors.toList());

                    double minLength = Double.MAX_VALUE;
                    double maxLength = 0.0;
                    for (Node externalNode : externalNodes) {
                        double length = tree.getLength(externalNode);
                        if (length < minLength) {
                            minLength = length;
                        }
                        if (length > maxLength) {
                            maxLength = length;
                        }
                        tree.removeChild(externalNode, node);
                    }

                    String taxonName = name + "|" + value + "|" + tips.size();

                    Node clump;
                    if (externalNodes.size() > maxSoftClumpSize) {
                        // Either replace the clumped tips with a single tip (if larger than the threshold)
                        clump = tree.createExternalNode(Taxon.getTaxon(taxonName));
                    } else {
                        // or replace with an internal node flagged as 'clumped'

                        Node root = tree.getRootNode();
                        clump = tree.createInternalNode(externalNodes);
                        
                        // creating an internal node sets that as the root so set back to the original root
                        tree.setRoot(node);
                    }

                    clump.setAttribute("!collapse", new Object[] {CollapseType.CLUMPED, tips.size(), minLength, maxLength});
                    clump.setAttribute("Name", taxonName);
                    clump.setAttribute(attributeName, value);
                    clump.setAttribute("tip_count", tips.size());

                    tree.addChild(clump, node);
                    tree.setLength(clump, 0.0);

                    subtrees.put(name, new Subtree(CollapseType.CLUMPED, name, null, attributeName, value, tips, 0.0, maxLength));
                }
            }
        } else {
            node.setAttribute("tip_count", 1);
        }
    }

    private void clusterByAttribute(MutableRootedTree tree, Node node, String attributeName,
                                        int maxSoftCollapseSize, int minCollapseSize, Map<String, Subtree> subtrees) {
        if (!tree.isExternal(node)) {
            Set<Object> attributes = getTipAttributes(tree, node, attributeName).keySet();
            if (attributes.size() == 1) {
                String value = (String)attributes.iterator().next();
                String name = getUniqueHexCode();
                List<Node> externalNodes = tree.getExternalNodes(node);

                double minDivergence = Double.MAX_VALUE;
                double maxDivergence = 0.0;
                for (Node tip : externalNodes) {
                    double d = tree.getHeight(node) - tree.getHeight(tip);
                    if (d < minDivergence) {
                        minDivergence = d;
                    }
                    if (d > maxDivergence) {
                        maxDivergence = d;
                    }
                }
                List<String> tips = externalNodes.stream().map(node1 -> tree.getTaxon(node1).getName()).collect(Collectors.toList());
                String taxonName = name + "|" + value + "|" + tips.size();

                    node.setAttribute("!collapse", new Object[] {CollapseType.COLLAPSED, maxDivergence, minDivergence, tips.size()});
                    node.setAttribute("Name", taxonName);
                    node.setAttribute(attributeName, value);
                    node.setAttribute("tip_count", tips.size());

                if (externalNodes.size() > maxSoftCollapseSize) {
                    Node parent = tree.getParent(node);
                    tree.removeChild(node, parent);
                    Node tip = tree.createExternalNode(Taxon.getTaxon(taxonName));
                    tree.addChild(tip, parent);
                    tree.setLength(tip, minDivergence);
                }
                subtrees.put(name, new Subtree(CollapseType.COLLAPSED, name, node, attributeName, value, tips, minDivergence, maxDivergence));

            } else {

                for (Node child : tree.getChildren(node)) {
                    clusterByAttribute(tree, child, attributeName, maxSoftCollapseSize, minCollapseSize, subtrees);
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

            writer.println("type,name,attribute_name,attribute_value,tip_count,min_divergence,max_divergence,tips");

            for (String key : subtreeMap.keySet()) {
                Subtree subtree = subtreeMap.get(key);
                writer.print(subtree.type);
                writer.print(",");
                writer.print(subtree.name);
                writer.print(",");
                writer.print(subtree.attributeName);
                writer.print(",");
                writer.print(subtree.attributeValue);
                writer.print(",");
                writer.print(subtree.tips.size());
                writer.print(",");
                writer.print(subtree.minDivergence);
                writer.print(",");
                writer.print(subtree.maxDivergence);
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
        public Subtree(CollapseType type, String name, Node root, String attributeName, String attributeValue, List<String> tips, double minDivergence, double maxDivergence) {
            this.type = type;
            this.name = name;
            this.root = root;
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
            this.minDivergence = minDivergence;
            this.maxDivergence = maxDivergence;
            this.tips = tips;
        }

        final CollapseType type;
        final String name;
        final Node root;
        final String attributeName;
        final String attributeValue;
        final double minDivergence;
        final double maxDivergence;
        final List<String> tips;
    }
}

