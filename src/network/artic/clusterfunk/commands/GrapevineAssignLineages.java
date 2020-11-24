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

    private static final int MAX_RESERVE = 5;

    public GrapevineAssignLineages(boolean isVerbose) {
        super(isVerbose);
    }

    public GrapevineAssignLineages(String treeFileName,
                                   String clusterFileName,
                                   String outputPath,
                                   String outputPrefix,
                                   FormatType outputFormat,
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

        // find all the cluster labels in the tree
        Map<String, Node> clusterNodeMap = new HashMap<>();
        for (Node node : tree.getInternalNodes()) {
            String cluster = (String)node.getAttribute("cluster_id");
            if (cluster != null) {
                clusterNodeMap.put(cluster, node);
            }
        }

        Map<String, CSVRecord> lineages = readCSV(clusterFileName, lineageName);

        Set<Integer> ukLineageSet = new TreeSet<>();
        List<Lineage> lineageList = new ArrayList<>();

        for (String lineageId : lineages.keySet()) {
            CSVRecord record = lineages.get(lineageId);

            Lineage lineage = new Lineage(lineageId,
                    record.get("cluster_id"),
                    Integer.parseInt(record.get("tip_count")),
                    Integer.parseInt(record.get("uk_tip_count")));

            String l = lineageId.substring(2);
            int dotIndex = l.indexOf('.');
            if (dotIndex > 0) {
                l = l.substring(0, dotIndex);
            }
            ukLineageSet.add(Integer.parseInt(l));

            lineageList.add(lineage);
        }

        if (isVerbose) {
            outStream.println("Lineage name: " + lineageName);
            outStream.println("    Lineages: " + lineageList.size());
            outStream.println();
        }

        List<Integer> ukLineageNumbers = new ArrayList<>(ukLineageSet);
        int nextUKLineageNumber = ukLineageNumbers.get(ukLineageNumbers.size() - 1) + 1;

        if (isVerbose) {
            outStream.println("Finding existing lineage roots");
            outStream.println();
        }

        Map<Node, Lineage> nodeLineageMap = new HashMap<>();
        findLineageRoots(tree, lineageName, clusterNodeMap, lineageList, nextUKLineageNumber, nodeLineageMap);

        if (isVerbose) {
            outStream.println("Existing lineages found: " + nodeLineageMap.size());
            outStream.println();
        }

        if (isVerbose) {
            outStream.println("Finding new lineages");
            outStream.println();
        }

        int count = nodeLineageMap.size();

        int newLineageCount = findNewClusters(tree, lineageName, stateName, nodeLineageMap, nextUKLineageNumber);

        if (isVerbose) {
            outStream.println("Found " + (nodeLineageMap.size() - count) + " new lineages");
            outStream.println();
            outStream.println("Assigning lineages");
            outStream.println();
        }

        int singletonCount = assignSingletons(tree, lineageName, stateName);
        assignLineages(tree, tree.getRootNode(), lineageName, stateName, nodeLineageMap);

        int bigClusters = 0;
        int smallClusters = 0;
        for (Lineage cluster : nodeLineageMap.values()) {
            if (cluster.ukTipCount >= 50) {
                bigClusters += 1;
            } else if (cluster.ukTipCount > 1) {
                smallClusters += 1;
            }
        }

        if (isVerbose) {
            outStream.println("Assigned " + nodeLineageMap.size() + " as " + lineageName);
            outStream.println("Large (>=50): " + bigClusters);
            outStream.println(" Small (<50): " + smallClusters);
            outStream.println("  Singletons: " + singletonCount);
            outStream.println();
            outStream.println("Assigning phylotypes");
            outStream.println();
        }

        assignPhylotypes(tree, tree.getRootNode(), lineageName, nodeLineageMap);

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

            writer.println("sequence_name," + lineageName + ",phylotype");
            for (Node tip: tree.getExternalNodes()) {
                String lineage = (String)tip.getAttribute(lineageName);
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

            writer.println("uk_lineage,cluster_id,del_trans,uk_tip_count,tip_count");
            for (Lineage lineage : nodeLineageMap.values()) {
                writer.println(lineage.name +
                        "," + lineage.clusterId +
                        "," + lineage.delTrans +
                        "," + lineage.ukTipCount +
                        "," + lineage.tipCount);
            }

            writer.close();

            if (isVerbose) {
                outStream.println("Written lineage file: " + outputLineageFileName);
                outStream.println("            Lineages: " + nodeLineageMap.values().size());
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
     * @param lineageList
     * @param nextUKLineageNumber
     * @param nodeLineageMap
     */
    private void findLineageRoots(RootedTree tree, String lineageName, Map<String, Node> clusterNodeMap, List<Lineage> lineageList, int nextUKLineageNumber, Map<Node, Lineage> nodeLineageMap) {

        // go through all the lineages
        for (Lineage lineage : lineageList) {
            Node node = clusterNodeMap.get(lineage.clusterId);
            if (node != null) {
                lineage.node = node;
                lineage.node.setAttribute(lineageName, lineage.name);
                nodeLineageMap.put(node, lineage);
            } else {
                errorStream.println("Cluster, " + lineage.clusterId + ", for lineage, " + lineage.name + ", not found in tree");
            }
        }
    }

    /**
     * recursive version
     * @param tree
     * @param nodeClusterMap
     */
    private int findNewClusters(RootedTree tree, String lineageName, String stateName, Map<Node, Lineage> nodeClusterMap, int nextUKLineageNumber) {
        int newLineageCount = 0;
        for (Node node : tree.getInternalNodes()) {
            Lineage cluster = nodeClusterMap.get(node);
            if (cluster == null) {
                // new UK lineage?
                boolean isUK = (Boolean)node.getAttribute(stateName);
                boolean isParentUK = !tree.isRoot(node) && (Boolean)tree.getParent(node).getAttribute(stateName);
                if (isUK && !isParentUK) {
                    // start of a new lineage
                    Map<Object, Integer> ukSubLineages = getTipAttributes(tree, node, lineageName);
                    String ukLineage = (String)ukSubLineages.keySet().iterator().next();
                    if (ukSubLineages.size() == 0) {

                        Lineage newCluster = createCluster(tree, node, stateName, "UK" + nextUKLineageNumber);
                        nextUKLineageNumber += 1;
                        nodeClusterMap.put(node, newCluster);

                        newLineageCount += 1;

                        if (isVerbose) {
                            outStream.println("Creating new lineage: " + newCluster.name + " [" + newCluster.ukTipCount + " uk tips]");
                        }
                    }
                }
            }
        }
        return newLineageCount;
    }

    private int assignSingletons(RootedTree tree, String lineageName, String stateName) {
        int count = 0;
        for (Node node : tree.getExternalNodes()) {
            boolean isUK = (Boolean)node.getAttribute(stateName);
            boolean isParentUK = !tree.isRoot(node) && (Boolean)tree.getParent(node).getAttribute(stateName);
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
     * @param nodeLineageMap
     */
    private void assignLineages(RootedTree tree, Node node, String lineageName, String stateName, Map<Node, Lineage> nodeLineageMap) {
        Lineage lineage = nodeLineageMap.get(node);
        if (lineage != null) {
            assignLineage(tree, node, stateName, true, lineageName, lineage.name);
        }
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                assignLineages(tree, child, lineageName, stateName, nodeLineageMap);
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

    protected Lineage createCluster(RootedTree tree, Node node, String stateName, String ukLineage) {
        String delLineage = (String)node.getAttribute("del_lineage");
        int tipCount = countTips(tree, node);
        int ukTipCount = countTips(tree, node, stateName, true);
        String clusterId = (String)node.getAttribute("cluster_id");
        if (clusterId == null) {
            errorStream.println("No cluster id available for lineage: " + ukLineage);
        }

        return new Lineage(node, delLineage, ukLineage, clusterId, tipCount, ukTipCount);
    }

    /**
     * recursive version
     * @param tree
     * @param nodeClusterMap
     */
    private void assignPhylotypes(RootedTree tree, Node node, String lineageName, Map<Node, Lineage> nodeClusterMap) {
        Lineage cluster = nodeClusterMap.get(node);
        if (cluster != null) {
            assignPhylotype(tree, node, cluster.name, 1, lineageName, cluster.name);
        }
        for (Node child : tree.getChildren(node)) {
            assignPhylotypes(tree, child, lineageName, nodeClusterMap);
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

    protected class Lineage {
        public Lineage(String name, String clusterId, int tipCount, int ukTipCount) {
            this.name = name;
            this.clusterId = clusterId;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
            this.newLineage = false;
        }

        public Lineage(Node node, String delTrans, String name, String clusterId, int tipCount, int ukTipCount) {
            this.node = node;
            this.delTrans = delTrans;
            this.name = name;
            this.clusterId = clusterId;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
            this.newLineage = true;
        }

        public int getUkTipCount() {
            return ukTipCount;
        }

        String name;
        final String clusterId;
        final int tipCount;
        final int ukTipCount;
        final boolean newLineage;
        Node node;
        String delTrans;
    }

}

