package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class GrapevineLabelClusters extends Command {

    private static final int MAX_DEPTH = 5;
    private static final int MAX_RESERVE = 5;

    public GrapevineLabelClusters(boolean isVerbose) {
        super(isVerbose);
    }

    public GrapevineLabelClusters(String treeFileName,
                                  String clusterFileName,
                                  String outputPath,
                                  String outputPrefix,
                                  FormatType outputFormat,
                                  String clusterName,
                                  boolean isVerbose) {

        super(isVerbose);

        String path = checkOutputPath(outputPath);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Map<String, Cluster> clusterMap = new HashMap<>();
        Set<String> clusterLabels = new HashSet<>();

        if (clusterFileName != null) {
            if (isVerbose) {
                outStream.println("Reading cluster file: " + clusterFileName);
                outStream.println();
            }
            // read the CSV of currently defined clusters
            Map<String, CSVRecord> clusters = readCSV(clusterFileName, clusterName);

            for (String clusterLabel : clusters.keySet()) {
                if (clusterLabels.contains(clusterLabel)) {
                    errorStream.println("Duplicate cluster label, " + clusterLabel + ", in cluster file");
                    System.exit(1);
                }

                clusterLabels.add(clusterLabel);

                CSVRecord record = clusters.get(clusterLabel);

                final String[] reserves = new String[MAX_DEPTH];
                final int[] reserveDepths = new int[MAX_DEPTH];
                for (int i = 0; i < MAX_DEPTH; i++) {
                    if (!record.get("reserve" + i).isEmpty()) {
                        reserves[i] = record.get("reserve" + i);
                        reserveDepths[i] = Integer.parseInt(record.get("reserve_depth" + i));
                    }
                }
                Cluster cluster = new Cluster(clusterLabel,
                        record.get("representative"),
                        Integer.parseInt(record.get("depth")),
                        reserves,
                        reserveDepths,
                        Integer.parseInt(record.get("tip_count")));

                clusterMap.put(clusterLabel, cluster);
            }

            if (isVerbose) {
                outStream.println("        Cluster name: " + clusterName);
                outStream.println("       Clusters read: " + clusterMap.size());
                outStream.println();
            }
        }

        Map<Node, Cluster> nodeClusterMap = new HashMap<>();

        if (isVerbose) {
            outStream.println("Finding existing cluster roots");
            outStream.println();
        }

        Set<Cluster> lostClusters = new HashSet<>();

        findClusterRoots(tree, clusterMap, nodeClusterMap, lostClusters);

        if (isVerbose) {
            String lost = String.join(",", lostClusters.stream().map(c -> c.label).collect(Collectors.toList()));
            outStream.println("  Lost clusters: (" + lostClusters.size() + ") " + lost );
            outStream.println();
        }

        if (isVerbose) {
            outStream.println("Finding new clusters");
            outStream.println();
        }

        int newLineageCount = findNewClusters(tree, nodeClusterMap, clusterLabels);

        if (isVerbose) {
            outStream.println("Found " + nodeClusterMap.size() + " lineages");
            outStream.println();
        }

        String outputTreeFileName = path + outputPrefix + "_tree.nexus";
        if (isVerbose) {
            outStream.println("Writing tree file, " + outputTreeFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        // annotate tree with cluster names
        for (Node node : nodeClusterMap.keySet()) {
            node.setAttribute(clusterName, nodeClusterMap.get(node).label);
        }

        writeTreeFile(tree, outputTreeFileName, outputFormat);

        try {
            String outputClusterFileName = path + outputPrefix + "_clusters.csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputClusterFileName)));

            writer.print("cluster,representative,depth");
            for (int i = 0; i < MAX_DEPTH; i++) {
                writer.print(",reserve" + i);
                writer.print(",reserve_depth" + i );
            }
            writer.println(",tip_count");

            for (Cluster cluster : nodeClusterMap.values()) {
                writer.print(cluster.label +
                        "," + cluster.representative +
                        "," + cluster.depth);
                for (int i = 0; i < MAX_DEPTH; i++) {
                    if (cluster.reserve[i] != null) {
                        writer.print("," + cluster.reserve[i]);
                        writer.print("," + cluster.reserveDepth[i]);
                    } else {
                        writer.print(",,");
                    }
                }
                writer.println("," + cluster.tipCount);
            }

            writer.close();

            if (isVerbose) {
                outStream.println("Written cluster file: " + outputClusterFileName);
                outStream.println("            Clusters: " + nodeClusterMap.values().size());
                outStream.println("        New lineages: " + newLineageCount);
                outStream.println();
            }

        } catch (IOException e) {
            errorStream.println("Error writing cluster file: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * recursive version
     * @param tree
     * @param clusterMap
     * @param nodeClusterMap
     */
    private void findClusterRoots(RootedTree tree, Map<String, Cluster> clusterMap, Map<Node, Cluster> nodeClusterMap, Set<Cluster> lostClusters) {

        // create a map of all the tip names
        Map<String, Node> nameTipMap = new HashMap<>();
        for (Node tip : tree.getExternalNodes()) {
            nameTipMap.put(tree.getTaxon(tip).getName(), tip);
        }

        // go through all the clusters
        for (Cluster cluster : clusterMap.values()) {

            // find the representative tip
            Node clusterNode = nameTipMap.get(cluster.representative);
            int repDepth = cluster.depth;
            if (clusterNode == null) {
                int index = 0;
                do {
                    clusterNode = nameTipMap.get(cluster.reserve[index]);
                    repDepth = cluster.reserveDepth[index];
                    index ++;
                } while(index < MAX_RESERVE && clusterNode == null);

            }
            if (clusterNode != null) {
                // if the representative tip is not on the node, walk up to find it
                int depth = 0;
                while (tree.getParent(clusterNode) != null && depth < repDepth) {
                    clusterNode = tree.getParent(clusterNode);
                    depth += 1;
                }

//            if (tree.isRoot(clusterNode)) {
//                errorStream.println("Root of lineage, " + cluster.label + ", is the root of the tree");
//                System.exit(1);
//            }

//            boolean isUK = (Boolean)clusterNode.getAttribute("country_uk_deltran");
//
//            Node parent = tree.getParent(clusterNode);
//            boolean isParentUK = (Boolean)parent.getAttribute("country_uk_deltran");
//            if (isParentUK) {
//                // if the parent is also UK, then crawl this lineage up...
//                while (isParentUK) {
//                    clusterNode = tree.getParent(clusterNode);
//                    depth += 1;
//                    isUK = (Boolean) clusterNode.getAttribute("country_uk_deltran");
//                }
//            }
//            if (!isUK) {
//                // does this matter?
////                    errorStream.println("Cluster, " + cluster.lineage + ", haplotype defined node is not UK");
//            }

                cluster.node = clusterNode;
                nodeClusterMap.put(clusterNode, cluster);

            } else {
                lostClusters.add(cluster);

//                errorStream.println("Can't find any representative tip for cluster, " + cluster.label + ", in tree.");
//                errorStream.print("  " + cluster.representative);
//                for (String reserve : cluster.reserve) {
//                    if (reserve != null) {
//                        errorStream.print(", " + reserve);
//                    }
//                }
//                errorStream.println();
//                    System.exit(1);
            }


        }
    }

    /**
     * recursive version
     * @param tree
     * @param nodeClusterMap
     */
    private int findNewClusters(RootedTree tree, Map<Node, Cluster> nodeClusterMap, Set<String> clusterLabels) {
        int newLineageCount = 0;
        for (Node node : tree.getInternalNodes()) {
            Cluster cluster = nodeClusterMap.get(node);
            if (cluster == null) {
                SecureRandom random = new SecureRandom();
                String clusterLabel = String.format("%06x", random.nextInt(0xFFFFFF));
                while (clusterLabels.contains(clusterLabel)) {
                    clusterLabel = String.format("%06x", random.nextInt(0xFFFFFF));
                }
                clusterLabels.add(clusterLabel);
                Cluster newCluster = createCluster(tree, node, clusterLabel);
                nodeClusterMap.put(node, newCluster);

                newLineageCount += 1;

                if (isVerbose) {
                    outStream.println("Creating new cluster with label: " + newCluster.label + " [" + newCluster.tipCount + " tips]");
                }
            }
        }

        return newLineageCount;
    }

    protected Cluster createCluster(RootedTree tree, Node node, String label) {
        int tipCount = countTips(tree, node);

        List<Representative> representatives = findRepresentative(tree, node, 0);
        if (representatives.size() == 0) {
            errorStream.println("No representative found for cluster, " + label);
            return null;
        }
        representatives.sort(
                Comparator.comparingInt(Representative::getDepth)
                        .thenComparingInt(Representative::getAmbiguityCount)
        );

        return new Cluster(node, label, representatives.get(0), representatives.subList(1, representatives.size()), tipCount);
    }

    /**
     * Finds a representative tip deeper in the tree and returns how many nodes it is deep
     * @param tree
     * @param node
     * @return
     */
    protected List<Representative> findRepresentative(RootedTree tree, Node node, int depth) {
        if (tree.isExternal(node)) {
            return Collections.singletonList(new Representative(tree, node, depth));
        }

        List<Representative> representatives = new ArrayList<>();

        if (depth <= MAX_DEPTH) {
            for (Node child : tree.getChildren(node)) {
                representatives.addAll(findRepresentative(tree, child, depth + 1));
            }
        }
//        int minDepth = Integer.MAX_VALUE;
//        List<Node> bestChildren = new ArrayList<>();
//        for (Node child : tree.getChildren(node)) {
//            List<Representative> childRepresentatives = findRepresentative(tree, child, depth + 1);
//            if (d < minDepth) {
//                bestChildren.clear();
//                minDepth = d;
//            }
//
//            if (d == minDepth) {
//                bestChildren.addAll(representativeNodes);
//            }
//        }

//        if (bestChildren.size() > 0) {
//            bestChildren.sort((node1, node2) -> {
//                if (node1.getAttribute("ambiguity_count") == null || node2.getAttribute("ambiguity_count") == null) {
//                    return 0;
//                }
//                int amb1 = Integer.parseInt(node1.getAttribute("ambiguity_count").toString());
//                int amb2 = Integer.parseInt(node2.getAttribute("ambiguity_count").toString());
//                return amb1 - amb2;
//            } );
//            representativeNodes.addAll(bestChildren);
//            return minDepth;
//        }
        return representatives;
    }

    protected class Cluster {
        public Cluster(String label, String representative, int depth,
                       String[] reserves, int[] reserveDepths,
                       int tipCount) {
            this.label = label;
            this.representative = representative;
            this.depth = depth;
            for (int i = 0; i < MAX_DEPTH; i++) {
                this.reserve[i] = reserves[i];
                this.reserveDepth[i] = reserveDepths[i];
            }
            this.tipCount = tipCount;
            this.newCluster = false;
        }

        public Cluster(Node node, String label, Representative representative,
                       List<Representative> reserves,
                       int tipCount) {
            this.node = node;
            this.label = label;
            this.representative = representative.name;
            this.depth = representative.depth;
            for (int i = 0; i < Math.min(reserves.size(), MAX_RESERVE); i++) {
                this.reserve[i] = reserves.get(i).name;
                this.reserveDepth[i] = reserves.get(i).depth;
            }
            this.tipCount = tipCount;
            this.newCluster = true;
        }

        Node node;
        final String label;
        final String representative;
        final int depth;
        final String[] reserve = new String[MAX_DEPTH];
        final int[] reserveDepth = new int[MAX_DEPTH];
        final int tipCount;
        final boolean newCluster;
    }

    protected class Representative {
        public Representative(RootedTree tree, Node node, int depth) {
            this.name = tree.getTaxon(node).getName();
            this.depth = depth;
            this.ambiguityCount =
                    node.getAttribute("ambiguity_count") != null ?
                            Integer.parseInt(node.getAttribute("ambiguity_count").toString()) : Integer.MAX_VALUE;
        }

        public String getName() {
            return name;
        }

        public int getDepth() {
            return depth;
        }

        public int getAmbiguityCount() {
            return ambiguityCount;
        }

        final String name;
        final int depth;
        final int ambiguityCount;
    }

}

