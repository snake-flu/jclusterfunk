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
public class GrapevineSublineages extends GrapevineAssignLineages {
    private final static double GENOME_LENGTH = 29903;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.01; // 1% of a 1 SNP branch length

    public GrapevineSublineages(String treeFileName,
                                String outputPath,
                                String outputPrefix,
                                FormatType outputFormat,
                                String haplotypeName1,
                                final int minSublineageSize,
                                boolean isVerbose) {

        super(isVerbose);

        String path = checkOutputPath(outputPath);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Map<Node, Cluster> nodeClusterMap = new HashMap<>();

//        String clusterAttribute = "country_uk_acctran";
        String clusterAttribute = "country_uk_deltran";

        findClusterRoots(tree, tree.getRootNode(), false, nodeClusterMap);

        int bigClusters = 0;
        int smallClusters = 0;
        int singletons = 0;
        for (Node node : nodeClusterMap.keySet()) {
            Cluster cluster = nodeClusterMap.get(node);
            if (cluster.ukTipCount >= 50) {
                bigClusters += 1;
            } else if (cluster.ukTipCount > 1) {
                smallClusters += 1;
            } else {
                singletons += 1;
            }
        }



        if (isVerbose) {
            outStream.println("Found " + nodeClusterMap.size() + " clusters with " + clusterAttribute);
            outStream.println("Large (>=50): " + bigClusters);
            outStream.println(" Small (<50): " + smallClusters);
            outStream.println("  Singletons: " + singletons);
            outStream.println();
        }

        Set<Integer> ukLineageSet = new TreeSet<>();
        List<Cluster> clusterList = new ArrayList<>();
        nodeClusterMap.entrySet()
                .stream()
                // sort by frequencies and break ties with fewest ambiguities
                .sorted((e1, e2) ->
                        e2.getValue().ukTipCount - e1.getValue().ukTipCount)
                .forEachOrdered(x -> {
                    clusterList.add(x.getValue());
                    int lineageNumber = Integer.parseInt(x.getValue().lineage.substring(2));
                    ukLineageSet.add(lineageNumber);
                });


        Map<String, List<Cluster>> lineageClusterListMap = new HashMap<>();

        clusterLineages(tree, tree.getRootNode(), "new_uk_lineage", minSublineageSize, nodeClusterMap, lineageClusterListMap);

        List<Integer> ukLineageNumbers = new ArrayList<>(ukLineageSet);
        int nextUKLineageNumber = ukLineageNumbers.get(ukLineageNumbers.size() - 1) + 1;

        cleanLineages(tree, lineageClusterListMap, "new_uk_lineage", nextUKLineageNumber);

        String outputTreeFileName = path + outputPrefix + ".nexus";
        if (isVerbose) {
            outStream.println("Writing tree file, " + outputTreeFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputTreeFileName, outputFormat);

//        Map<String, String> lineageHaplotypeMap = new HashMap<>();
//
//        extractLineageHaplotypes(tree, tree.getRootNode(), "new_uk_lineage", null, lineageHaplotypeMap);

        // count the number of each uk_lineage to divide into sublineages
//        Map<String, Integer> lineageCountMap = new HashMap<>();
//        for (Cluster cluster : clusterList) {
//            lineageCountMap.put(cluster.ukLineage, lineageCountMap.getOrDefault(cluster.ukLineage, 0) + 1);
//        }

        try {
            String outputFileName = path + outputPrefix + "_clusters.csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

//            Map<String, Integer> lineageSublineageMap = new HashMap<>();

            writer.println("uk_lineage,representative,depth,del_trans,uk_tip_count,tip_count");
            for (Cluster cluster : clusterList) {
                writer.println(cluster.lineage +
                        "," + cluster.representatives +
                        "," + cluster.depth +
                        "," + cluster.delTrans +
                        "," + cluster.ukTipCount +
                        "," + cluster.tipCount);
            }

            writer.close();

//            outputFileName = path + outputPrefix + ".csv";
//            writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));
//
//            writer.println("sequence_name,uk_lineage");
//            for (Node tip : tree.getExternalNodes()) {
//                String lineage = (String)tip.getAttribute("uk_lineage");
//                if (lineage != null) {
//                    writer.println(tree.getTaxon(tip).getName() + "," + lineage);
//                }
//            }
//
//            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }
    }

    private void cleanLineages(RootedTree tree, Map<String, List<Cluster>> lineageClusterListMap, String newLineageName, int nextLineageNumber) {
        List<Cluster> unlabelled = new ArrayList<>();

        for (String lineage : lineageClusterListMap.keySet()) {
            List<Cluster> clusterList = lineageClusterListMap.get(lineage);
            if (clusterList.size() > 1) {
                clusterList.sort(Comparator.comparing(Cluster::getUkTipCount).reversed());
                for (int i = 1; i < clusterList.size(); i++) {
                    unlabelled.add(clusterList.get(i));
                    clusterList.get(i).lineage = "UK" + nextLineageNumber;
                    nextLineageNumber += 1;
                }
            }
        }


    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param nodeClusterMap
     */
    private void findClusterRoots(RootedTree tree, Node node, boolean parentIsUK, Map<Node, Cluster> nodeClusterMap) {
        boolean isUK = (Boolean)node.getAttribute("country_uk_deltran");
        if (Boolean.TRUE.equals(isUK) && !parentIsUK) {
            // start of a new lineage
            Map<Object, Integer> ukLineages = getTipAttributes(tree, node, "uk_lineage");
            String ukLineage = (String)ukLineages.keySet().iterator().next();
//            if (ukLineage.equals("UK8686")) {
//                errorStream.println("UK8686");
//            }
//            if (nodeLineageMap.containsValue(ukLineage)) {
//                throw new RuntimeException("multiple roots of a ukLineage present");
//            }
            if (ukLineages.size() > 1) {
                throw new RuntimeException("ambiguous lineage");
            }
            Cluster cluster = createCluster(tree, node, ukLineage);
            nodeClusterMap.put(node, cluster);
        }
        if (!tree.isExternal(node)) {
            // finally recurse down
            for (Node child : tree.getChildren(node)) {
                if (!tree.isExternal(child)) {
                    // don't bother with tips (singletons)
                    findClusterRoots(tree, child, isUK, nodeClusterMap);
                }
            }
        }
    }


    /**
     * recursive version
     * @param tree
     * @param node
     * @param lineageClusterMap
     */
    private void clusterLineages(RootedTree tree, Node node,
                                 String newLineageName, int minSublineageSize,
                                 Map<Node, Cluster> nodeClusterMap,
                                 Map<String, List<Cluster>> lineageClusterMap) {
        Cluster cluster = nodeClusterMap.get(node);
        if (!tree.isExternal(node)) {
            if (cluster == null) {
                // no cluster allocation for this node yet - determine if clusters of children
                // should be merged (all UK nodes that are direct children of this node).
                Map<String, Integer> childLineages = new HashMap<>();
                for (Node child : tree.getChildren(node)) {
                    Cluster childCluster = nodeClusterMap.get(child);
                    if (childCluster != null) {
                        childLineages.put(childCluster.lineage,
                                childLineages.getOrDefault(childCluster.lineage, 0) + 1);
                    }
//                    if ("del_trans_182".equals(child.getAttribute("del_lineage"))) {
//                        System.out.println("hi");
//                    }
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
                        Cluster childCluster = nodeClusterMap.get(child);
                        if (childCluster != null && childLineage.equals(childCluster.lineage)) {
                            childSizes.add(new Pair(child, childCluster.ukTipCount));
                            if (childCluster.ukTipCount >= minSublineageSize) {
                                bigSublineageCount += 1;
                            }

                            totalSize += childCluster.ukTipCount;
                        }
                    }

                    childSizes.sort(Comparator.comparing(k -> -k.count));

                    Cluster nodeCluster;
                    Node lineageNode;
                    if (childSizes.size() > 1) {
                        // there are multiple children of this lineage - merge them at this node
                        lineageNode = node;
                        nodeCluster = createCluster(tree, node, childLineage);

                        List<Cluster> clusterList = lineageClusterMap.getOrDefault(nodeCluster.lineage, new ArrayList<>());
                        clusterList.add(nodeCluster);
                        lineageClusterMap.put(nodeCluster.lineage, clusterList);

                        lineageNode.setAttribute(newLineageName, nodeCluster.lineage);
                        propagateAttribute(tree, lineageNode, "country_uk_deltran", true, newLineageName, nodeCluster.lineage);

                        // now label large sublineages if there are two or more
                        int sublineageSize = 0;
                        if (bigSublineageCount > 1) {
                            int sublineageNumber = 1;
                            for (Pair pair : childSizes) {
                                // then give children larger than minSublineageSize a sublineage designation
                                if (pair.count >= minSublineageSize) {
                                    String sublineage = nodeCluster.lineage + "." + sublineageNumber;
                                    Cluster childCluster = nodeClusterMap.get(pair.node);
                                    childCluster.lineage = sublineage;
                                    pair.node.setAttribute(newLineageName, sublineage);
                                    propagateAttribute(tree, pair.node, "country_uk_deltran", true, newLineageName, sublineage);
                                    sublineageNumber += 1;
                                    sublineageSize += pair.count;

                                    if (lineageClusterMap.containsKey(sublineage)) {
                                        throw new RuntimeException("multiple roots of a sublineage present");
                                    }
//                                    lineageClusterMap.put(sublineage, pair.node);

                                    if (isVerbose) {
                                        outStream.println("Creating sublineage: " + sublineage + " [" + pair.count + " taxa]");
                                    }
                                }
                            }
                        }
                        if (isVerbose) {
//                            outStream.println("Creating lineage: " + nodeLineage + " [" + (totalSize - sublineageSize) + " taxa]");
                        }
                    }
                }
            } else {
                List<Cluster> clusterList = lineageClusterMap.getOrDefault(cluster.lineage, new ArrayList<>());
                clusterList.add(cluster);
                lineageClusterMap.put(cluster.lineage, clusterList);

                node.setAttribute(newLineageName, cluster.lineage);
                propagateAttribute(tree, node, "country_uk_deltran", true, newLineageName, cluster.lineage);
            }

            // finally recurse down
            for (Node child : tree.getChildren(node)) {
                clusterLineages(tree, child, newLineageName, minSublineageSize, nodeClusterMap, lineageClusterMap);
            }
        } else {
            if (cluster != null) {
                List<Cluster> clusterList = lineageClusterMap.getOrDefault(cluster.lineage, new ArrayList<>());
                clusterList.add(cluster);
                lineageClusterMap.put(cluster.lineage, clusterList);

                node.setAttribute(newLineageName, cluster.lineage);
            } else {
                // do nothing - probably non UK
            }
        }
    }
//
//    /**
//     * recursive version
//     * @param tree
//     * @param node
//     * @param lineageHaplotypeMap
//     */
//    private void extractLineageHaplotypes(RootedTree tree, Node node, String lineageName, String parentLineage, Map<String, String> lineageHaplotypeMap) {
//        if (!tree.isExternal(node)) {
//            String lineage = (String)node.getAttribute(lineageName);
//            if (lineage != null && !lineage.equals(parentLineage)) {
//                String haplotype = (String)node.getAttribute("sequence_hash");
//                if (haplotype != null) {
//                    lineageHaplotypeMap.put(haplotype, lineage);
//                }
//            }
//            for (Node child : tree.getChildren(node)) {
//                extractLineageHaplotypes(tree, child, lineageName, lineage, lineageHaplotypeMap);
//            }
//        }
//    }

}

