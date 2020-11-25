package network.artic.clusterfunk.commands;

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
                                final int minSublineageSize,
                                boolean isVerbose) {

        super(isVerbose);

        final String lineageName = "uk_lineage";
        final String stateName = "country_uk_deltran";

        String path = checkOutputPath(outputPath);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Map<Node, Lineage> nodeClusterMap = new HashMap<>();

//        String clusterAttribute = "country_uk_acctran";
        String clusterAttribute = "country_uk_deltran";

        findClusterRoots(tree, tree.getRootNode(), lineageName, stateName, false, nodeClusterMap);

        int bigClusters = 0;
        int smallClusters = 0;
        int singletons = 0;
        for (Node node : nodeClusterMap.keySet()) {
            Lineage cluster = nodeClusterMap.get(node);
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
        List<Lineage> clusterList = new ArrayList<>();
        nodeClusterMap.entrySet()
                .stream()
                // sort by frequencies and break ties with fewest ambiguities
                .sorted((e1, e2) ->
                        e2.getValue().ukTipCount - e1.getValue().ukTipCount)
                .forEachOrdered(x -> {
                    clusterList.add(x.getValue());
                    int lineageNumber = Integer.parseInt(x.getValue().name.substring(2));
                    ukLineageSet.add(lineageNumber);
                });


        Map<String, List<Lineage>> lineageClusterListMap = new HashMap<>();

        clusterLineages(tree, tree.getRootNode(), lineageName, minSublineageSize, stateName, nodeClusterMap, lineageClusterListMap);

        List<Integer> ukLineageNumbers = new ArrayList<>(ukLineageSet);
        int nextUKLineageNumber = ukLineageNumbers.get(ukLineageNumbers.size() - 1) + 1;

        cleanLineages(tree, lineageClusterListMap, "new_uk_lineage", nextUKLineageNumber);

        String outputTreeFileName = path + outputPrefix + "_tree.nexus";
        if (isVerbose) {
            outStream.println("Writing tree file, " + outputTreeFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputTreeFileName, outputFormat);

//         count the number of each uk_lineage to divide into sublineages
//        Map<String, Integer> lineageCountMap = new HashMap<>();
//        for (Lineage cluster : clusterList) {
//            lineageCountMap.put(cluster.ukLineage, lineageCountMap.getOrDefault(cluster.ukLineage, 0) + 1);
//        }

        try {
            String outputFileName = path + outputPrefix + ".csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

//            Map<String, Integer> lineageSublineageMap = new HashMap<>();

            writer.println("uk_lineage,cluster_id,del_trans,uk_tip_count,tip_count");
            for (Lineage cluster : clusterList) {
                writer.println(cluster.name +
                        "," + cluster.clusterId +
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

    private void cleanLineages(RootedTree tree, Map<String, List<Lineage>> lineageClusterListMap, String newLineageName, int nextLineageNumber) {
        List<Lineage> unlabelled = new ArrayList<>();

        for (String lineage : lineageClusterListMap.keySet()) {
            List<Lineage> clusterList = lineageClusterListMap.get(lineage);
            if (clusterList.size() > 1) {
                clusterList.sort(Comparator.comparing(Lineage::getUkTipCount).reversed());
                for (int i = 1; i < clusterList.size(); i++) {
                    unlabelled.add(clusterList.get(i));
                    clusterList.get(i).name = "UK" + nextLineageNumber;
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
    private void findClusterRoots(RootedTree tree, Node node, String lineageName, String stateName, boolean parentIsUK, Map<Node, Lineage> nodeClusterMap) {
        boolean isUK = (Boolean)node.getAttribute(stateName);
        if (Boolean.TRUE.equals(isUK) && !parentIsUK) {
            // start of a new lineage
            Map<Object, Integer> ukLineages = getTipAttributes(tree, node, lineageName);
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
            Lineage cluster = createLineage(tree, node, stateName, ukLineage);
            nodeClusterMap.put(node, cluster);
        }
        if (!tree.isExternal(node)) {
            // finally recurse down
            for (Node child : tree.getChildren(node)) {
                if (!tree.isExternal(child)) {
                    // don't bother with tips (singletons)
                    findClusterRoots(tree, child, lineageName, stateName, isUK, nodeClusterMap);
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
                                 String stateName,
                                 Map<Node, Lineage> nodeClusterMap,
                                 Map<String, List<Lineage>> lineageClusterMap) {
        Lineage cluster = nodeClusterMap.get(node);
        if (!tree.isExternal(node)) {
            if (cluster == null) {
                // no cluster allocation for this node yet - determine if clusters of children
                // should be merged (all UK nodes that are direct children of this node).
                Map<String, Integer> childLineages = new HashMap<>();
                for (Node child : tree.getChildren(node)) {
                    Lineage childCluster = nodeClusterMap.get(child);
                    if (childCluster != null) {
                        childLineages.put(childCluster.name,
                                childLineages.getOrDefault(childCluster.name, 0) + 1);
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
                        Lineage childCluster = nodeClusterMap.get(child);
                        if (childCluster != null && childLineage.equals(childCluster.name)) {
                            childSizes.add(new Pair(child, childCluster.ukTipCount));
                            if (childCluster.ukTipCount >= minSublineageSize) {
                                bigSublineageCount += 1;
                            }

                            totalSize += childCluster.ukTipCount;
                        }
                    }

                    childSizes.sort(Comparator.comparing(k -> -k.count));

                    Lineage nodeCluster;
                    Node lineageNode;
                    if (childSizes.size() > 1) {
                        // there are multiple children of this lineage - merge them at this node
                        lineageNode = node;
                        nodeCluster = createLineage(tree, node, stateName, childLineage);

                        List<Lineage> clusterList = lineageClusterMap.getOrDefault(nodeCluster.name, new ArrayList<>());
                        clusterList.add(nodeCluster);
                        lineageClusterMap.put(nodeCluster.name, clusterList);

                        lineageNode.setAttribute(newLineageName, nodeCluster.name);
                        propagateAttribute(tree, lineageNode, "country_uk_deltran", true, newLineageName, nodeCluster.name);

                        // now label large sublineages if there are two or more
                        int sublineageSize = 0;
                        if (bigSublineageCount > 1) {
                            int sublineageNumber = 1;
                            for (Pair pair : childSizes) {
                                // then give children larger than minSublineageSize a sublineage designation
                                if (pair.count >= minSublineageSize) {
                                    String sublineage = nodeCluster.name + "." + sublineageNumber;
                                    Lineage childCluster = nodeClusterMap.get(pair.node);
                                    childCluster.name = sublineage;
                                    pair.node.setAttribute(newLineageName, sublineage);
                                    propagateAttribute(tree, pair.node, stateName, true, newLineageName, sublineage);
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
                List<Lineage> clusterList = lineageClusterMap.getOrDefault(cluster.name, new ArrayList<>());
                clusterList.add(cluster);
                lineageClusterMap.put(cluster.name, clusterList);

                node.setAttribute(newLineageName, cluster.name);
                propagateAttribute(tree, node, stateName, true, newLineageName, cluster.name);
            }

            // finally recurse down
            for (Node child : tree.getChildren(node)) {
                clusterLineages(tree, child, newLineageName, minSublineageSize, stateName, nodeClusterMap, lineageClusterMap);
            }
        } else {
            if (cluster != null) {
                List<Lineage> clusterList = lineageClusterMap.getOrDefault(cluster.name, new ArrayList<>());
                clusterList.add(cluster);
                lineageClusterMap.put(cluster.name, clusterList);

                node.setAttribute(newLineageName, cluster.name);
            } else {
                // do nothing - probably non UK
            }
        }
    }

}

