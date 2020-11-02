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
    private final static double GENOME_LENGTH = 29903;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.01; // 1% of a 1 SNP branch length

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

        List<Cluster> clusterList = new ArrayList<>();
        nodeClusterMap.entrySet()
                .stream()
                // sort by frequencies and break ties with fewest ambiguities
                .sorted((e1, e2) ->
                        e2.getValue().ukTipCount - e1.getValue().ukTipCount)
                .forEachOrdered(x -> {
                    clusterList.add(x.getValue());
                });

        Map<String, List<Cluster>> lineageNodeMap = new HashMap<>();

        clusterLineages(tree, tree.getRootNode(), "new_uk_lineage", minSublineageSize, nodeClusterMap, lineageNodeMap);


        cleanLineages(tree, lineageNodeMap, "new_uk_lineage" );

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
        Map<String, Integer> lineageCountMap = new HashMap<>();

        for (Cluster cluster : clusterList) {
            lineageCountMap.put(cluster.ukLineage, lineageCountMap.getOrDefault(cluster.ukLineage, 0) + 1);
        }

        try {
            String outputFileName = path + outputPrefix + "_clusters.csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

            Map<String, Integer> lineageSublineageMap = new HashMap<>();

            writer.println("uk_lineage,sequence_hash,depth,del_trans,uk_tip_count,tip_count");
            for (Cluster cluster : clusterList) {
                String lineageName = cluster.ukLineage;
                if (lineageCountMap.get(lineageName) > 1) {
                    int sublineage = lineageSublineageMap.getOrDefault(lineageName, 0) + 1;
                    lineageSublineageMap.put(lineageName, sublineage);
                    lineageName += "." + sublineage;
                }

                writer.println(lineageName + "," + cluster.haplotype + "," + cluster.depth + "," + cluster.delTrans + "," + cluster.ukTipCount + "," + cluster.tipCount);
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

    private void cleanLineages(RootedTree tree, Map<String, List<Cluster>> lineageNodeMap, String newLineageName) {
        List<Cluster> unlabelled = new ArrayList<>();

        for (String lineage : lineageNodeMap.keySet()) {
            List<Cluster> clusterList = lineageNodeMap.get(lineage);
            if (clusterList.size() > 1) {
                clusterList.sort(Comparator.comparing(Cluster::getUkTipCount).reversed());
                for (int i = 1; i < clusterList.size(); i++) {
                    unlabelled.add(clusterList.get(i));
                    clusterList.get(i).ukLineage = null;
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
//            if (nodeLineageMap.containsValue(ukLineage)) {
//                throw new RuntimeException("multiple roots of a ukLineage present");
//            }
//            if (ukLineage.equals("UK2461")) {
//                System.err.println("UK2461");
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
                findClusterRoots(tree, child, isUK, nodeClusterMap);
            }
        }
    }

    private Cluster createCluster(RootedTree tree, Node node, String ukLineage) {
        String delLineage = (String)node.getAttribute("del_lineage");
        int tipCount = countTips(tree, node);
        int ukTipCount = countTips(tree, node, "country_uk_deltran", true);

        if (ukLineage.equals("UK78")) {
            errorStream.println("node");
        }

        Node[] haplotypeChild = new Node[1];
        int depth = findHaplotype(tree, node, 0, haplotypeChild);
        if (haplotypeChild[0] == null) {
            errorStream.println("node");
        }
        String haplotype = (String)haplotypeChild[0].getAttribute("sequence_hash");

        if (haplotype == null) {
            errorStream.println("no haplotype");
        }
        return new Cluster(node, delLineage, haplotype, depth, ukLineage, tipCount, ukTipCount);
    }

    /**
     * Finds a haplotype deeper in the tree and returns how many nodes it is deep
     * @param tree
     * @param node
     * @return
     */
    private int findHaplotype(RootedTree tree, Node node, int depth, Node[] haplotypeNode) {
        String haplotype = (String)node.getAttribute("sequence_hash");
        if (haplotype != null) {
            haplotypeNode[0] = node;
            return depth;
        }

        if (!tree.isExternal(node)) {
            int minDepth = Integer.MAX_VALUE;
            List<Node> bestChildren = new ArrayList<>();
            for (Node child : tree.getChildren(node)) {
                boolean isUK = (Boolean)child.getAttribute("country_uk_deltran");
                if (isUK) {
                    int d = findHaplotype(tree, child, depth + 1, haplotypeNode);
                    if (d < minDepth) {
                        bestChildren.clear();
                        minDepth = d;
                    }

                    if (d == minDepth) {
                        bestChildren.add(haplotypeNode[0]);
                    }
                }
            }

            if (bestChildren.size() > 0) {
                bestChildren.sort((node1, node2) -> {
                    int amb1 = Integer.parseInt(node1.getAttribute("ambiguity_count").toString());
                    int amb2 = Integer.parseInt(node2.getAttribute("ambiguity_count").toString());
                    return amb1 - amb2;
                } );
                haplotypeNode[0] = bestChildren.get(0);
                return minDepth;
            }
        }

        // no haplotype found
        return Integer.MAX_VALUE;
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
        if (!tree.isExternal(node)) {
            Cluster cluster = nodeClusterMap.get(node);
            if (cluster == null) {
                // no cluster allocation for this node yet - determine if clusters of children
                // should be merged (all UK nodes that are direct children of this node).
                Map<String, Integer> childLineages = new HashMap<>();
                for (Node child : tree.getChildren(node)) {
                    Cluster childCluster = nodeClusterMap.get(child);
                    if (childCluster != null) {
                        childLineages.put(childCluster.ukLineage,
                                childLineages.getOrDefault(childCluster.ukLineage, 0) + 1);
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
                        if (childCluster != null && childLineage.equals(childCluster.ukLineage)) {
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

                        List<Cluster> clusterList = lineageClusterMap.getOrDefault(nodeCluster.ukLineage, new ArrayList<>());
                        clusterList.add(nodeCluster);
                        lineageClusterMap.put(nodeCluster.ukLineage, clusterList);

                        lineageNode.setAttribute(newLineageName, nodeCluster.ukLineage);
                        propagateAttribute(tree, lineageNode, "country_uk_deltran", true, newLineageName, nodeCluster.ukLineage);

                        // now label large sublineages if there are two or more
                        int sublineageSize = 0;
                        if (bigSublineageCount > 1) {
                            int sublineageNumber = 1;
                            for (Pair pair : childSizes) {
                                // then give children larger than minSublineageSize a sublineage designation
                                if (pair.count >= minSublineageSize) {
                                    String sublineage = nodeCluster.ukLineage + "." + sublineageNumber;
                                    Cluster childCluster = nodeClusterMap.get(pair.node);
                                    childCluster.ukLineage = sublineage;
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
//                lineageNodeMap.put(cluster.ukLineage, node);
                node.setAttribute(newLineageName, cluster.ukLineage);
                propagateAttribute(tree, node, "country_uk_deltran", true, newLineageName, cluster.ukLineage);
            }

            // finally recurse down
            for (Node child : tree.getChildren(node)) {
                clusterLineages(tree, child, newLineageName, minSublineageSize, nodeClusterMap, lineageClusterMap);
            }
        } else {
//            if (nodeLineage != null) {
//                lineageNodeMap.put(nodeLineage, node);
//                node.setAttribute(newLineageName, nodeLineage);
//                if (isVerbose) {
//                    outStream.println("Creating lineage: " + nodeLineage + " [1 taxon]");
//                }
//            }
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

    class Cluster {
        public Cluster(Node node, String delTrans, String haplotype, int depth, String ukLineage, int tipCount, int ukTipCount) {
            this.node = node;
            this.delTrans = delTrans;
            this.haplotype = haplotype;
            this.depth = depth;
            this.ukLineage = ukLineage;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
        }

        public int getUkTipCount() {
            return ukTipCount;
        }

        final Node node;
        final String delTrans;
        final String haplotype;
        final int depth;
        String ukLineage;
        final int tipCount;
        final int ukTipCount;

    }

}

