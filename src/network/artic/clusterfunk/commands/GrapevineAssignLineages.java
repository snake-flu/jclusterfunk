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
public class GrapevineAssignLineages extends Command {

    public GrapevineAssignLineages(boolean isVerbose) {
        super(isVerbose);
    }

    public GrapevineAssignLineages(String treeFileName,
                                   String clusterFileName,
                                   String outputPath,
                                   String outputPrefix,
                                   FormatType outputFormat,
                                   String haplotypeName,
                                   String clusterName,
                                   boolean isVerbose) {

        super(isVerbose);

        if (haplotypeName == null) {
            haplotypeName = "sequence_hash";
        }

        String path = checkOutputPath(outputPath);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Map<String, CSVRecord> lineages = readCSV(clusterFileName, clusterName);

        Set<Integer> ukLineageSet = new TreeSet<>();
        Map<String, Cluster> representativeClusterMap = new HashMap<>();
        for (String lineageName : lineages.keySet()) {
            CSVRecord record = lineages.get(lineageName);

            //uk_lineage,sequence_hash,depth,del_trans,uk_tip_count
            Cluster cluster = new Cluster(lineageName,
                    record.get("representatives"),
                    Integer.parseInt(record.get("depth")),
                    Integer.parseInt(record.get("uk_tip_count")),
                    Integer.parseInt(record.get("tip_count")));

            String lineage = record.get("uk_lineage").substring(2);
            int dotIndex = lineage.indexOf('.');
            if (dotIndex > 0) {
                lineage = lineage.substring(0, dotIndex);
            }
            ukLineageSet.add(Integer.parseInt(lineage));
            representativeClusterMap.put(record.get("representative"), cluster);
        }

        if (isVerbose) {
            outStream.println("Lineage name: " + clusterName);
            outStream.println("    Lineages: " + representativeClusterMap.size());
            outStream.println();
        }

        Map<Node, Cluster> nodeClusterMap = new HashMap<>();

        List<Integer> ukLineageNumbers = new ArrayList<>(ukLineageSet);
        int nextUKLineageNumber = ukLineageNumbers.get(ukLineageNumbers.size() - 1) + 1;

        if (isVerbose) {
            outStream.println("Finding existing lineage roots");
            outStream.println();
        }

        findClusterRoots(tree, representativeClusterMap, nextUKLineageNumber, nodeClusterMap);

        if (isVerbose) {
            outStream.println("Finding new lineages");
            outStream.println();
        }

        int newLineageCount = findNewClusters(tree, nodeClusterMap, nextUKLineageNumber);

        if (isVerbose) {
            outStream.println("Found " + nodeClusterMap.size() + " lineages");
            outStream.println();
            outStream.println("Assigning lineages");
            outStream.println();
        }

        int singletonCount = assignSingletons(tree, clusterName);
        assignLineages(tree, tree.getRootNode(), clusterName, nodeClusterMap);

        int bigClusters = 0;
        int smallClusters = 0;
        for (Cluster cluster : nodeClusterMap.values()) {
            if (cluster.ukTipCount >= 50) {
                bigClusters += 1;
            } else if (cluster.ukTipCount > 1) {
                smallClusters += 1;
            }
        }

        if (isVerbose) {
            outStream.println("Assigned " + nodeClusterMap.size() + " as " + clusterName);
            outStream.println("Large (>=50): " + bigClusters);
            outStream.println(" Small (<50): " + smallClusters);
            outStream.println("  Singletons: " + singletonCount);
            outStream.println();
            outStream.println("Assigning phylotypes");
            outStream.println();
        }

        assignPhylotypes(tree, tree.getRootNode(), clusterName, nodeClusterMap);

        String outputTreeFileName = path + outputPrefix + "_tree.nexus";
        if (isVerbose) {
            outStream.println("Writing tree file, " + outputTreeFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputTreeFileName, outputFormat);

        try {

            String outputMetadataFileName = path + outputPrefix + "_metadata.csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputMetadataFileName)));

            if (isVerbose) {
                outStream.println("Writing metadata file, " + outputMetadataFileName);
                outStream.println();
            }

            writer.println("sequence_name," + clusterName + ",phylotype");
            for (Node tip: tree.getExternalNodes()) {
                String lineage = (String)tip.getAttribute(clusterName);
                String phylotype = (String)tip.getAttribute("phylotype");
                writer.println(tree.getTaxon(tip).getName() + "," + (lineage != null ? lineage : "") + "," + (phylotype != null ? phylotype : ""));
            }

            writer.close();

        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }

        try {
            String outputLineageFileName = path + outputPrefix + "_lineages.csv";
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputLineageFileName)));

            writer.println("uk_lineage,representative,depth,del_trans,uk_tip_count,tip_count");
            for (Cluster cluster : nodeClusterMap.values()) {
                writer.println(cluster.lineage +
                        "," + cluster.representative +
                        "," + cluster.depth +
                        "," + cluster.delTrans +
                        "," + cluster.ukTipCount +
                        "," + cluster.tipCount);
            }

            writer.close();

            if (isVerbose) {
                outStream.println("Written lineage file: " + outputLineageFileName);
                outStream.println("            Lineages: " + nodeClusterMap.values().size());
                outStream.println("        New lineages: " + newLineageCount);
                outStream.println();
            }

        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * recursive version
     * @param tree
     * @param representitiveClusterMap
     * @param nodeClusterMap
     */
    private void findClusterRoots(RootedTree tree, Map<String, Cluster> representitiveClusterMap, int nextUKLineageNumber, Map<Node, Cluster> nodeClusterMap) {

        // create a map of all the tip names
        Map<String, Node> nameTipMap = new HashMap<>();
        for (Node tip : tree.getExternalNodes()) {
            nameTipMap.put(tree.getTaxon(tip).getName(), tip);
        }

        // go through all the clusers
        for (String representative : representitiveClusterMap.keySet()) {
//            if (representative.contains("CAMB-7B361")) {
//                errorStream.println("CAMB-7B361");
//            }

            Cluster cluster = representitiveClusterMap.get(representative);

            // find the representative tip
            Node clusterNode = nameTipMap.get(cluster.representative);
            if (clusterNode == null) {
                errorStream.println("Can't find representative tip, " + cluster.representative + ", in tree");
                System.exit(1);
            }
            // if the representative tip is not on the node, walk up to find it
            int depth = 0;
            while (tree.getParent(clusterNode) != null && depth < cluster.depth) {
                clusterNode = tree.getParent(clusterNode);
                depth += 1;
            }

            if (tree.isRoot(clusterNode)) {
                errorStream.println("Root of lineage, " + cluster.lineage + ", is the root of the tree");
                System.exit(1);
            }

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

            cluster.delTrans = (String)clusterNode.getAttribute("del_lineage");
            cluster.node = clusterNode;
            nodeClusterMap.put(clusterNode, cluster);
        }
    }

    /**
     * recursive version
     * @param tree
     * @param nodeClusterMap
     */
    private int findNewClusters(RootedTree tree, Map<Node, Cluster> nodeClusterMap, int nextUKLineageNumber) {
        int newLineageCount = 0;
        for (Node node : tree.getInternalNodes()) {
            Cluster cluster = nodeClusterMap.get(node);
            if (cluster == null) {
                // new UK lineage?
                boolean isUK = (Boolean)node.getAttribute("country_uk_deltran");
                boolean isParentUK = !tree.isRoot(node) && (Boolean)tree.getParent(node).getAttribute("country_uk_deltran");
                if (isUK && !isParentUK) {
                    // start of a new lineage
                    Map<Object, Integer> ukSubLineages = getTipAttributes(tree, node, "uk_lineage");
                    String ukLineage = (String)ukSubLineages.keySet().iterator().next();
                    if (ukSubLineages.size() == 0) {

                        Cluster newCluster = createCluster(tree, node, "UK" + nextUKLineageNumber);
                        nextUKLineageNumber += 1;
                        nodeClusterMap.put(node, newCluster);

                        newLineageCount += 1;

                        if (isVerbose) {
                            outStream.println("Creating new lineage: " + newCluster.lineage + " [" + newCluster.ukTipCount + " uk tips]");
                        }
                    }
                }
            }
        }
        return newLineageCount;
    }

    private int assignSingletons(RootedTree tree, String lineageName) {
        int count = 0;
        for (Node node : tree.getExternalNodes()) {
            boolean isUK = (Boolean)node.getAttribute("country_uk_deltran");
            boolean isParentUK = !tree.isRoot(node) && (Boolean)tree.getParent(node).getAttribute("country_uk_deltran");
            if (isUK && !isParentUK) {
                node.setAttribute(lineageName, "singleton");
                count += 1;
            }
        }
        return count;
    }

    /**
     * recursive version
     * @param tree
     * @param nodeClusterMap
     */
    private void assignLineages(RootedTree tree, Node node, String clusterName, Map<Node, Cluster> nodeClusterMap) {
        Cluster cluster = nodeClusterMap.get(node);
        if (cluster != null) {
            assignLineage(tree, node, "country_uk_deltran", true, clusterName, cluster.lineage);
        }
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                assignLineages(tree, child, clusterName, nodeClusterMap);
            }
        }
    }

    /**
     * Recursively sets an lineage.
     * @param tree
     * @param node
     */
    private void assignLineage(RootedTree tree, Node node, String attributeName, Object attributeValue, String lineageName, String lineageValue) {
        node.setAttribute(lineageName, lineageValue);

        if (!tree.isExternal(node)) {
            List<Node> children = tree.getChildren(node);
            children.sort(Comparator.comparingInt(child -> countTips(tree, child)));

            for (Node child : children) {
                Object value = node.getAttribute(attributeName);
                if ((value != null && (attributeValue == null || value.equals(attributeValue)))) {
                    assignLineage(tree, child, attributeName, attributeValue, lineageName, lineageValue);
                }
            }
        }
    }

    protected Cluster createCluster(RootedTree tree, Node node, String ukLineage) {
        String delLineage = (String)node.getAttribute("del_lineage");
        int tipCount = countTips(tree, node);
        int ukTipCount = countTips(tree, node, "country_uk_deltran", true);

        Node[] haplotypeChild = new Node[1];
        int depth = findRepresentitive(tree, node, 0, haplotypeChild);
//        if (ukLineage.equals("UK1814")) {
//            errorStream.println("UK1814");
//        }

        String r = (tree.isExternal(haplotypeChild[0]) ?
                tree.getTaxon(haplotypeChild[0]).getName() :
                (String)haplotypeChild[0].getAttribute("representatives"));
        if (r == null) {
            errorStream.println("no representative");
        }
        String[] representatives = r.split("\\|");
        List<String> representativeList = Arrays.stream(representatives).collect(Collectors.toList());
        return new Cluster(node, delLineage, ukLineage, representativeList, depth, tipCount, ukTipCount);
    }

    /**
     * Finds a haplotype deeper in the tree and returns how many nodes it is deep
     * @param tree
     * @param node
     * @return
     */
    protected int findRepresentitive(RootedTree tree, Node node, int depth, Node[] representitiveNode) {
        String representitive = (tree.isExternal(node) ?
                tree.getTaxon(node).getName() :
                (String)node.getAttribute("representitive"));
        if (representitive != null) {
            representitiveNode[0] = node;
            return depth;
        }

        if (!tree.isExternal(node)) {
            int minDepth = Integer.MAX_VALUE;
            List<Node> bestChildren = new ArrayList<>();
            for (Node child : tree.getChildren(node)) {
                boolean isUK = (Boolean)child.getAttribute("country_uk_deltran");
                if (isUK) {
                    int d = findRepresentitive(tree, child, depth + 1, representitiveNode);
                    if (d < minDepth) {
                        bestChildren.clear();
                        minDepth = d;
                    }

                    if (d == minDepth) {
                        bestChildren.add(representitiveNode[0]);
                    }
                }
            }

            if (bestChildren.size() > 0) {
                bestChildren.sort((node1, node2) -> {
                    int amb1 = Integer.parseInt(node1.getAttribute("ambiguity_count").toString());
                    int amb2 = Integer.parseInt(node2.getAttribute("ambiguity_count").toString());
                    return amb1 - amb2;
                } );
                representitiveNode[0] = bestChildren.get(0);
                return minDepth;
            }
        }

        errorStream.println("value of 'representitive' not found for tip");
        System.exit(1);

        // no haplotype found
        return Integer.MAX_VALUE;
    }

    /**
     * recursive version
     * @param tree
     * @param nodeClusterMap
     */
    private void assignPhylotypes(RootedTree tree, Node node, String clusterName, Map<Node, Cluster> nodeClusterMap) {
        Cluster cluster = nodeClusterMap.get(node);
        if (cluster != null) {
            assignPhylotype(tree, node, cluster.lineage, 1, clusterName, cluster.lineage);
        }
        for (Node child : tree.getChildren(node)) {
            assignPhylotypes(tree, child, clusterName, nodeClusterMap);
        }
    }

    /**
     * Recursively sets phylotypes from a given node. Only goes down subtrees where attributeName == attributeValue.
     * @param tree
     * @param node
     * @param attributeName
     * @param attributeValue
     */
    private void assignPhylotype(RootedTree tree, Node node, String parentPhylotype, int childNumber, String attributeName, Object attributeValue) {
        String phylotype = parentPhylotype;

        if (!tree.isExternal(node)) {
            phylotype += "." + childNumber;

            List<Node> children = tree.getChildren(node);
            children.sort(Comparator.comparingInt((child) -> countTips(tree, (Node) child)).reversed());

            int c = 1;
            for (Node child : children) {
                Object value = node.getAttribute(attributeName);
                if (attributeName == null || (value != null && (attributeValue == null || value.equals(attributeValue)))) {
                    assignPhylotype(tree, child, phylotype, c, attributeName, attributeValue);
                    c += 1;
                }
            }
        }
        node.setAttribute("phylotype", phylotype);
    }

    protected class Cluster {
        public Cluster(String lineage, List<String> representatives, int depth, int tipCount, int ukTipCount) {
            this.lineage = lineage;
            this.representatives = representatives;
            this.depth = depth;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
            this.newLineage = false;
        }

        public Cluster(Node node, String delTrans, String lineage, List<String> representatives, int depth, int tipCount, int ukTipCount) {
            this.node = node;
            this.delTrans = delTrans;
            this.lineage = lineage;
            this.representatives = representatives;
            this.depth = depth;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
            this.newLineage = true;
        }

        public int getUkTipCount() {
            return ukTipCount;
        }

        String lineage;
        final List<String> representatives;
        final int depth;
        final int tipCount;
        final int ukTipCount;
        final boolean newLineage;
        Node node;
        String delTrans;
    }

}

