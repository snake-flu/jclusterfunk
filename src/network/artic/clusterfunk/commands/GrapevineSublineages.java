package network.artic.clusterfunk.commands;

import com.sun.org.apache.xpath.internal.operations.Bool;
import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * Bespoke function to split Grapevine UK_lineages into sublineages
 */
public class GrapevineSublineages extends Command {

    public GrapevineSublineages(String treeFileName,
                                String outputPath,
                                String outputPrefix,
                                FormatType outputFormat,
                                final int minSublineageSize,
                                boolean isVerbose) {

        super(isVerbose);

        String path = checkOutputPath(outputPath);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Map<Node, String> nodeLineageMap = new HashMap<>();

        findClusterRoots(tree, tree.getRootNode(), false, nodeLineageMap);

        // find cases where more than one node has the same lineage designation
        Set<String> lineageSet = new HashSet<>();
        Set<String> ambiguousLineageSet = new HashSet<>();
        for (Node node : nodeLineageMap.keySet()) {
            String lineage = nodeLineageMap.get(node);
            if (lineageSet.contains(lineage)) {
                ambiguousLineageSet.add(lineage);
//                errorStream.println("Lineage " + lineage + " exists in more than one place");
            }
            lineageSet.add(lineage);
        }

        // get the set of nodes for each ambiguous lineage
        Map<String, Set<Node>> ambiguousLineageMap = new HashMap<>();
        for (Node node : nodeLineageMap.keySet()) {
            String lineage = nodeLineageMap.get(node);
            if (ambiguousLineageSet.contains(lineage)) {
                Set<Node> nodeSet = ambiguousLineageMap.getOrDefault(lineage, new HashSet<>());
                nodeSet.add(node);
                ambiguousLineageMap.put(lineage, nodeSet);
             }
        }

        // for each ambiguous lineage, find the largest node and remove the rest
        for (String lineage : ambiguousLineageMap.keySet()) {
            Set<Node> nodeSet = ambiguousLineageMap.get(lineage);
            int maxSize = 0;
            Node maxNode = null;
            for (Node node : nodeSet) {
                int size = countTips(tree, node);
                if (size > maxSize) {
                    maxSize = size;
                    maxNode = node;
                }
            }

            for (Node node : nodeSet) {
                if (node != maxNode) {
                    nodeLineageMap.put(node, "unassigned");
                }
            }

        }

        Map<String, Node> lineageNodeMap = new HashMap<>();

        clusterLineages(tree, tree.getRootNode(), "uk_lineage", null,
                "new_uk_lineage", minSublineageSize, nodeLineageMap, lineageNodeMap);

        String outputTreeFileName = path + outputPrefix + ".nexus";
        if (isVerbose) {
            outStream.println("Writing tree file, " + treeFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputTreeFileName, outputFormat);

        Map<String, String> lineageHaplotypeMap = new HashMap<>();

        extractLineageHaplotypes(tree, tree.getRootNode(), "new_uk_lineage", null, lineageHaplotypeMap);

        try {
            String outputFileName = path + outputPrefix + "_lineages.csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

            writer.println("new_uk_lineage,sequence_hash");
            for (String haplotype : lineageHaplotypeMap.keySet()) {
                String lineage = lineageHaplotypeMap.get(haplotype);
                writer.println(lineage + "," + haplotype);
            }

            writer.close();

            outputFileName = path + outputPrefix + ".csv";
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

            writer.println("sequence_name,new_uk_lineage");
            for (Node tip : tree.getExternalNodes()) {
                String lineage = (String)tip.getAttribute("new_uk_lineage");
                if (lineage != null) {
                    writer.println(tree.getTaxon(tip).getName() + "," + lineage);
                }
            }

            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param nodeLineageMap
     */
    private void findClusterRoots(RootedTree tree, Node node, boolean parentIsUK, Map<Node, String> nodeLineageMap) {
        String delLineage = (String)node.getAttribute("del_lineage");
        String delIntroduction = (String)node.getAttribute("del_introduction");
        boolean isUK = (Boolean)node.getAttribute("country_uk_deltran");
        if (Boolean.TRUE.equals(isUK) && !parentIsUK) {
            // start of a new lineage
            Map<Object, Integer> ukLineages = getTipAttributes(tree, node, "uk_lineage");
            String ukLineage = (String)ukLineages.keySet().iterator().next();
//            if (nodeLineageMap.containsValue(ukLineage)) {
//                throw new RuntimeException("multiple roots of a ukLineage present");
//            }
            if (ukLineages.size() > 1) {
                throw new RuntimeException("ambiguous lineage");
            }
            nodeLineageMap.put(node, ukLineage);
        }
        if (!tree.isExternal(node)) {
            // finally recurse down
            for (Node child : tree.getChildren(node)) {
                findClusterRoots(tree, child, isUK, nodeLineageMap);
            }
        }
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param lineageNodeMap
     */
    private void clusterLineages(RootedTree tree, Node node, String lineageName, String parentLineage,
                                 String newLineageName, int minSublineageSize,
                                 Map<Node, String> nodeLineageMap,
                                 Map<String, Node> lineageNodeMap) {
        String nodeLineage = nodeLineageMap.get(node);
        if (!tree.isExternal(node)) {
            if (nodeLineage == null) {

                Map<String, Integer> childLineages = new HashMap<>();
                for (Node child : tree.getChildren(node)) {
                    String childLineage = nodeLineageMap.get(child);
                    if (childLineage != null && !childLineage.equals("unassigned")) {
                        int count = childLineages.getOrDefault(childLineage, 0);
                        childLineages.put(childLineage, count + 1);
                    }
                    if ("del_trans_182".equals(child.getAttribute("del_lineage"))) {
                        System.out.println("hi");
                    }
                }

                if (childLineages.size() > 0) {
                    if (childLineages.size() > 1) {
                        throw new RuntimeException("more than one child lineage present");
                    }
                    String childLineage = childLineages.keySet().iterator().next();

                    int bigSublineageCount = 0;
                    int totalSize = 0;
                    List<Pair> childSizes = new ArrayList<>();
                    // first count the size of children lineages and give everyone the base lineage designation
                    for (Node child : tree.getChildren(node)) {
                        if (childLineage.equals(nodeLineageMap.get(child))) {
                            int count = countTips(tree, child);
                            childSizes.add(new Pair(child, count));
                            if (count >= minSublineageSize) {
                                bigSublineageCount += 1;
                            }

                            totalSize += count;
                        }
                    }

                    childSizes.sort(Comparator.comparing(k -> -k.count));

                    Node lineageNode;
                    if (childSizes.size() > 1) {
                        // there are multiple children of this lineage - merge them at this node
                        lineageNode = node;
                        nodeLineage = childLineage;
//                    } else {
//                        lineageNode = childSizes.iterator().next().node;
//                    }
                        if (lineageNodeMap.containsKey(nodeLineage)) {
                            Node otherNode = lineageNodeMap.get(nodeLineage);
                            throw new RuntimeException("multiple roots of a lineage present");
                        }
                        lineageNodeMap.put(nodeLineage, lineageNode);
                        lineageNode.setAttribute(newLineageName, nodeLineage);
                        propagateAttribute(tree, lineageNode, "country_uk_deltran", true, newLineageName, nodeLineage);

                        // now label large sublineages if there are two or more
                        int sublineageSize = 0;
                        if (bigSublineageCount > 1) {
                            int sublineageNumber = 1;
                            for (Pair pair : childSizes) {
                                // then give children larger than minSublineageSize a sublineage designation
                                if (pair.count >= minSublineageSize) {
                                    String sublineage = nodeLineage + "." + sublineageNumber;
                                    pair.node.setAttribute(newLineageName, sublineage);
                                    propagateAttribute(tree, pair.node, "country_uk_deltran", true, newLineageName, sublineage);
                                    sublineageNumber += 1;
                                    sublineageSize += pair.count;

                                    if (lineageNodeMap.containsKey(sublineage)) {
                                        throw new RuntimeException("multiple roots of a sublineage present");
                                    }
                                    lineageNodeMap.put(sublineage, pair.node);

                                    if (isVerbose) {
                                        outStream.println("Creating sublineage: " + sublineage + " [" + pair.count + " taxa]");
                                    }
                                }
                            }
                        }
                        if (isVerbose) {
                            outStream.println("Creating lineage: " + nodeLineage + " [" + (totalSize - sublineageSize) + " taxa]");
                        }
                    }
                }
            } else {
                lineageNodeMap.put(nodeLineage, node);
                node.setAttribute(newLineageName, nodeLineage);
                propagateAttribute(tree, node, "country_uk_deltran", true, newLineageName, nodeLineage);
            }

            // finally recurse down
            for (Node child : tree.getChildren(node)) {
                clusterLineages(tree, child, lineageName, nodeLineage, newLineageName, minSublineageSize, nodeLineageMap, lineageNodeMap);
            }
        } else {
            if (nodeLineage != null) {
                lineageNodeMap.put(nodeLineage, node);
                node.setAttribute(newLineageName, nodeLineage);
                if (isVerbose) {
                    outStream.println("Creating lineage: " + nodeLineage + " [1 taxon]");
                }
            }
        }
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param lineageHaplotypeMap
     */
    private void extractLineageHaplotypes(RootedTree tree, Node node, String lineageName, String parentLineage, Map<String, String> lineageHaplotypeMap) {
        if (!tree.isExternal(node)) {
            String lineage = (String)node.getAttribute(lineageName);
            if (lineage != null && !lineage.equals(parentLineage)) {
                String haplotype = (String)node.getAttribute("sequence_hash");
                if (haplotype != null) {
                    lineageHaplotypeMap.put(haplotype, lineage);
                }
            }
            for (Node child : tree.getChildren(node)) {
                extractLineageHaplotypes(tree, child, lineageName, lineage, lineageHaplotypeMap);
            }
        }
    }

    class Cluster {
        Node node;
        String delTrans;
        String haplotype;
        String ukLineage;
        int tipCount;
        int ukTipCount;
    }
}

