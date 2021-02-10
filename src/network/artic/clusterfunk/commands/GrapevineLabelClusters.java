package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class GrapevineLabelClusters extends Command {

    private final static double GENOME_LENGTH = 29903;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.01; // 1% of a 1 SNP branch length

    private static final int MAX_DEPTH = 5;
    private static final int MAX_RESERVE = 5;

    public GrapevineLabelClusters(boolean isVerbose) {
        super(isVerbose);
    }

    public GrapevineLabelClusters(String treeFileName,
                                  String clusterFileName,
                                  String lineageFileName,
                                  String outputPath,
                                  String outputPrefix,
                                  FormatType outputFormat,
                                  boolean isVerbose) {

        super(isVerbose);

        String path = checkOutputPath(outputPath);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Map<String, Cluster> clusterMap = new HashMap<>();

        if (clusterFileName != null) {
            if (isVerbose) {
                outStream.println("Reading cluster file: " + clusterFileName);
                outStream.println();
            }
            // read the CSV of currently defined clusters
            Map<String, CSVRecord> clusters = readCSV(clusterFileName, "cluster_id");

            for (String clusterId : clusters.keySet()) {
                if (clusterMap.containsKey(clusterId)) {
                    errorStream.println("Duplicate cluster ID, " + clusterId + ", in cluster file");
                    System.exit(1);
                }

                CSVRecord record = clusters.get(clusterId);

                final String[] reserves = new String[MAX_DEPTH];
                final int[] reserveDepths = new int[MAX_DEPTH];
                for (int i = 0; i < MAX_DEPTH; i++) {
                    if (!record.get("reserve" + i).isEmpty()) {
                        reserves[i] = record.get("reserve" + i);
                        reserveDepths[i] = Integer.parseInt(record.get("reserve_depth" + i));
                    }
                }
                Cluster cluster = new Cluster(clusterId,
                        record.get("representative"),
                        Integer.parseInt(record.get("depth")),
                        "",
//                        record.get("sequence_hash"),
                        reserves,
                        reserveDepths,
                        Integer.parseInt(record.get("tip_count")),
                        record.get("status")
                );

//                if (cluster.clusterId.equals("a70720")) {
//                    errorStream.println("a70720");
//                }

                clusterMap.put(clusterId, cluster);
            }

            if (isVerbose) {
                outStream.println("       Clusters read: " + clusterMap.size());
                outStream.println();
            }
        }

        Set<String> priorityClusterIds = new HashSet<>();
        if (lineageFileName != null) {
            if (isVerbose) {
                outStream.println("Reading lineage file: " + lineageFileName);
                outStream.println();
            }
            // read the CSV of currently defined clusters
            Map<String, CSVRecord> lineages = readCSV(lineageFileName, null);

            for (String id : lineages.keySet()) {
                CSVRecord record = lineages.get(id);
                priorityClusterIds.add(record.get("cluster_id"));
            }
        }

        Map<Node, Cluster> nodeClusterMap = new HashMap<>();

        if (isVerbose) {
            outStream.println("Finding existing cluster roots");
            outStream.println();
        }

        Set<Cluster> lostClusters = new HashSet<>();
        Set<Cluster> dupeClusters = new HashSet<>();
        Set<Cluster> foundClusters = new HashSet<>();

        // start by assigning clusters that have a representative right on the node
        int maxDepth = 0;
        int foundClusterCount = findClusterRoots(tree, clusterMap, maxDepth, nodeClusterMap, priorityClusterIds, foundClusters, dupeClusters, lostClusters);
        if (isVerbose) {
            outStream.println(foundClusterCount + " with representative depth " + maxDepth);
            outStream.println();
        }

        // then ones further away
        while (foundClusterCount > 0 && maxDepth <= MAX_DEPTH) {
            maxDepth += 1;
            foundClusterCount = findClusterRoots(tree, clusterMap, maxDepth, nodeClusterMap, priorityClusterIds, foundClusters, dupeClusters, lostClusters);
            if (isVerbose) {
                outStream.println(foundClusterCount + " with representative depth " + maxDepth);
                outStream.println();
            }
        }

        if (isVerbose) {
            String lost = String.join(",", lostClusters.stream().map(c -> c.clusterId).collect(Collectors.toList()));
            String dupes = String.join(",", dupeClusters.stream().map(c -> c.clusterId).collect(Collectors.toList()));
            String found = String.join(",", foundClusters.stream().map(c -> c.clusterId).collect(Collectors.toList()));
            outStream.println("Found " + nodeClusterMap.size() + " existing clusters");
            outStream.println("  Lost clusters: (" + lostClusters.size() + ") " + lost );
            outStream.println("  Duplicate clusters: (" + dupeClusters.size() + ") " + dupes );
            outStream.println("  Previously lost clusters now found: (" + foundClusters.size() + ") " + found );
            outStream.println();
        }

        if (isVerbose) {
            outStream.println("Finding new clusters");
            outStream.println();
        }

        Set<String> clusterLabels = new HashSet<>(clusterMap.keySet());

        int newClusterCount = findNewClusters(tree, nodeClusterMap, clusterLabels);

        if (isVerbose) {
            outStream.println("Found " + newClusterCount + " new clusters");
            outStream.println();
        }

        String outputTreeFileName = path + outputPrefix + "_tree.nexus";
        if (isVerbose) {
            outStream.println("Writing tree file, " + outputTreeFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        // annotate tree with cluster names
        for (Node node : nodeClusterMap.keySet()) {
            Cluster cluster = nodeClusterMap.get(node);
            node.setAttribute("cluster_id", cluster.clusterId);
        }

        writeTreeFile(tree, outputTreeFileName, outputFormat);

        try {
            String outputClusterFileName = path + outputPrefix + "_clusters.csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputClusterFileName)));

            writer.print("cluster_id,representative,depth");
            for (int i = 0; i < MAX_RESERVE; i++) {
                writer.print(",reserve" + i);
                writer.print(",reserve_depth" + i );
            }
            writer.println(",tip_count,status");

            for (Cluster cluster : nodeClusterMap.values()) {
                writer.print(cluster.clusterId +
                        "," + cluster.representative +
//                        "," + cluster.sequenceHash +
                        "," + cluster.depth);
                for (int i = 0; i < MAX_RESERVE; i++) {
                    if (cluster.reserve[i] != null) {
                        writer.print("," + cluster.reserve[i]);
                        writer.print("," + cluster.reserveDepth[i]);
                    } else {
                        writer.print(",,");
                    }
                }
                writer.print("," + cluster.tipCount);
                writer.println("," + cluster.status);
            }

            // write the lost ones in case they come back
            for (Cluster cluster : lostClusters) {
                writer.print(cluster.clusterId +
                        "," + cluster.representative +
                        "," + cluster.depth);
                for (int i = 0; i < MAX_RESERVE; i++) {
                    if (cluster.reserve[i] != null) {
                        writer.print("," + cluster.reserve[i]);
                        writer.print("," + cluster.reserveDepth[i]);
                    } else {
                        writer.print(",,");
                    }
                }
                writer.print("," + cluster.tipCount);
                writer.println("," + cluster.status);
            }

            writer.close();

            if (isVerbose) {
                outStream.println("Written cluster file: " + outputClusterFileName);
                outStream.println("            Clusters: " + nodeClusterMap.values().size());
                outStream.println("        New clusters: " + newClusterCount);
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
    private int findClusterRoots(RootedTree tree, Map<String, Cluster> clusterMap, int maxDepth,
                                 Map<Node, Cluster> nodeClusterMap, Set<String> priorityClusterIds,
                                 Set<Cluster> foundClusters, Set<Cluster> dupeClusters, Set<Cluster> lostClusters) {

        // create a map of all the tip names
        Map<String, Node> nameTipMap = new HashMap<>();
        for (Node tip : tree.getExternalNodes()) {
            nameTipMap.put(tree.getTaxon(tip).getName(), tip);
        }

        int foundClusterCount = 0;

        int i = 0;

        // go through all the clusters
        for (String clusterId : clusterMap.keySet()) {
            Cluster cluster = clusterMap.get(clusterId);
//            if (cluster.clusterId.equals("10cb9f")) {
//                errorStream.println("10cb9f");
//            }
//            if (cluster.clusterId.equals("b48e7e")) {
//                errorStream.println("b48e7e");
//            }
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
            i++;
            if (repDepth == maxDepth) {
                if (clusterNode != null) {

                    clusterNode = tree.getParent(clusterNode);

                    if (maxDepth > 1) {
                        // if the representative tip is not on the node, walk up to find it
                        // technically depth of 0 means a child with a zero branch length
                        int depth = 1;
                        while (tree.getParent(clusterNode) != null && depth < repDepth) {
                            clusterNode = tree.getParent(clusterNode);
                            depth += 1;
                        }
                    }

                    cluster.node = clusterNode;

                    if (nodeClusterMap.containsKey(clusterNode)) {
                        Cluster c = nodeClusterMap.get(clusterNode);
                        if (priorityClusterIds.contains(c.clusterId) || !priorityClusterIds.contains(cluster.clusterId)) {
                            // if the existing cluster id is already on the priority list or the new one isn't then just flag it as a dupe...
                            if (isVerbose) {
                                outStream.println("WARNING: Node already has a cluster_id, " + c.clusterId + ", cannot relabel with " + cluster.clusterId + " - adding to duplicate list");
                            }
                            cluster.status = "dupe";
                            dupeClusters.add(cluster);
                            continue;
                        }
                        // otherwise go on and replace it
                    }

                    if (!cluster.status.equals("")) {
                        cluster.status = "";
                        foundClusters.add(cluster);
                    }
                    foundClusterCount += 1;
                    nodeClusterMap.put(clusterNode, cluster);

                } else {
                    cluster.status = "lost";
                    lostClusters.add(cluster);
                }
            }
        }

        return foundClusterCount;
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
                String clusterLabel = getUniqueHexCode(clusterLabels);
//                clusterLabels.add(clusterLabel);
                Cluster newCluster = createCluster(tree, node, clusterLabel);
                nodeClusterMap.put(node, newCluster);

                newLineageCount += 1;

//                if (isVerbose) {
//                    outStream.println("Creating new cluster with label: " + newCluster.clusterId + " [" + newCluster.tipCount + " tips]");
//                }
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
                        .thenComparing(Representative::isUK)
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
            String label = tree.getTaxon(node).getName();
            boolean isUK = label.startsWith("England") |
                    label.startsWith("Scotland") |
                    label.startsWith("Wales") |
                    label.startsWith("Northern_Ireland");
//            String sequenceHash = (String)node.getAttribute("sequence_hash");
//            sequenceHash = (sequenceHash == null ? "" : sequenceHash);
//            String deltrans = node.getAttribute("country_uk_deltran").toString();
//            boolean isUK = Boolean.parseBoolean(deltrans);
//            return Collections.singletonList(new Representative(tree, node, sequenceHash, isUK, depth));
            return Collections.singletonList(new Representative(tree, node, null, isUK, depth));
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
        public Cluster(String clusterId, String representative, int depth,
                       String sequenceHash,
                       String[] reserves, int[] reserveDepths,
                       int tipCount, String status) {
            this.clusterId = clusterId;
            this.representative = representative;
            this.depth = depth;
            this.sequenceHash = sequenceHash;
            for (int i = 0; i < MAX_DEPTH; i++) {
                this.reserve[i] = reserves[i];
                this.reserveDepth[i] = reserveDepths[i];
            }
            this.tipCount = tipCount;
            this.newCluster = false;
            this.status = status;
        }

        public Cluster(Node node, String clusterId, Representative representative,
                       List<Representative> reserves,
                       int tipCount) {
            this.node = node;
            this.clusterId = clusterId;
            this.representative = representative.name;
            this.depth = representative.depth;
            this.sequenceHash = representative.sequenceHash;
            for (int i = 0; i < Math.min(reserves.size(), MAX_RESERVE); i++) {
                this.reserve[i] = reserves.get(i).name;
                this.reserveDepth[i] = reserves.get(i).depth;
            }
            this.tipCount = tipCount;
            this.newCluster = true;
            this.status = "";
        }

        Node node;
        final String clusterId;
        final String representative;
        final String sequenceHash;
        final int depth;
        final String[] reserve = new String[MAX_DEPTH];
        final int[] reserveDepth = new int[MAX_DEPTH];
        final int tipCount;
        final boolean newCluster;
        String status;
    }

    protected class Representative {
        public Representative(RootedTree tree, Node node, String sequenceHash, boolean isUK, int depth) {
            this.name = tree.getTaxon(node).getName();
            this.sequenceHash = sequenceHash;
            this.isUK = isUK;
            this.length = tree.getLength(node);
            if (depth == 1 && length < ZERO_BRANCH_THRESHOLD) {
                this.depth = 0;
            } else {
                this.depth = depth;
            }
            this.ambiguityCount =
                    node.getAttribute("ambiguity_count") != null ?
                            Integer.parseInt(node.getAttribute("ambiguity_count").toString()) : Integer.MAX_VALUE;
        }

        public String getName() {
            return name;
        }

        public boolean isUK() {
            return isUK;
        }

        public double getLength() {
            return length;
        }

        public int getDepth() {
            return depth;
        }

        public int getAmbiguityCount() {
            return ambiguityCount;
        }

        final String name;
        final String sequenceHash;
        final boolean isUK;
        final int depth;
        final double length;
        final int ambiguityCount;
    }

}

