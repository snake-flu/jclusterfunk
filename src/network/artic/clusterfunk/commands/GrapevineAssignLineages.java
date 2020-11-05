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

/**
 *
 */
public class GrapevineAssignLineages extends Command {

    public GrapevineAssignLineages(String treeFileName,
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

        Map<String, CSVRecord> lineages = readCSV(clusterFileName, clusterName);

        Set<Integer> ukLineageSet = new TreeSet<>();
        Map<String, Cluster> haplotypeClusterMap = new HashMap<>();
        for (String lineageName : lineages.keySet()) {
            CSVRecord record = lineages.get(lineageName);

            //uk_lineage,sequence_hash,depth,del_trans,uk_tip_count
            Cluster cluster = new Cluster(lineageName,
                    record.get("representative"),
                    record.get("sequence_hash"),
                    Integer.parseInt(record.get("depth")),
                    Integer.parseInt(record.get("uk_tip_count")),
                    Integer.parseInt(record.get("tip_count")));

            String lineage = record.get("uk_lineage").substring(2);
            int dotIndex = lineage.indexOf('.');
            if (dotIndex > 0) {
                lineage = lineage.substring(0, dotIndex);
            }
            ukLineageSet.add(Integer.parseInt(lineage));
            haplotypeClusterMap.put(record.get("sequence_hash"), cluster);
        }

        if (isVerbose) {
            outStream.println("Lineage name: " + clusterName);
            outStream.println("    Lineages: " + haplotypeClusterMap.size());
            outStream.println();
        }

        Map<Node, Cluster> nodeClusterMap = new HashMap<>();

        List<Integer> ukLineageNumbers = new ArrayList<>(ukLineageSet);
        int nextUKLineageNumber = ukLineageNumbers.get(ukLineageNumbers.size() - 1) + 1;

        if (isVerbose) {
            outStream.println("Finding lineage roots");
            outStream.println();
        }

        findClusterRoots(tree, haplotypeClusterMap, nextUKLineageNumber, nodeClusterMap);

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

            int newLineageCount = 0;

            writer.println("uk_lineage,representative,sequence_hash,depth,del_trans,uk_tip_count,tip_count");
            for (Cluster cluster : nodeClusterMap.values()) {
                if (cluster.newLineage) {
                    newLineageCount += 1;
                }
                writer.println(cluster.lineage +
                        "," + cluster.representative +
                        "," + cluster.haplotype +
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
     * @param haplotypeClusterMap
     * @param nodeClusterMap
     */
    private void findClusterRoots(RootedTree tree, Map<String, Cluster> haplotypeClusterMap, int nextUKLineageNumber, Map<Node, Cluster> nodeClusterMap) {
        for (Node node : tree.getInternalNodes()) {
            String haplotype = (String)node.getAttribute("sequence_hash");
            Cluster cluster = haplotypeClusterMap.get(haplotype);
            if (cluster != null) {

                Node clusterNode = node;
                int depth = 0;
                while (tree.getParent(clusterNode) != null && depth < cluster.depth) {
                    clusterNode = tree.getParent(clusterNode);
                    depth += 1;
                }

                if (tree.isRoot(clusterNode)) {
                    errorStream.println("Root of lineage, " + cluster.lineage + ", is the root of the tree");
                    System.exit(1);
                }

                cluster.delTrans = (String)node.getAttribute("del_lineage");

//                boolean isUK = (Boolean)clusterNode.getAttribute("country_uk_deltran");
//
//                Node parent = tree.getParent(clusterNode);
//                boolean isParentUK = (Boolean)parent.getAttribute("country_uk_deltran");
//                if (isParentUK) {
//                    // if the parent is also UK, then crawl this lineage up...
//                    while (isParentUK) {
//                        clusterNode = tree.getParent(clusterNode);
//                        depth += 1;
//                        isUK = (Boolean) clusterNode.getAttribute("country_uk_deltran");
//                    }
//                }
//                if (!isUK) {
//                    // does this matter?
////                    errorStream.println("Cluster, " + cluster.lineage + ", haplotype defined node is not UK");
//                }

                cluster.node = clusterNode;
                nodeClusterMap.put(node, cluster);
            } else {
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

                        if (isVerbose) {
                            outStream.println("Creating new lineage: " + newCluster.lineage + " [" + newCluster.ukTipCount + " uk tips]");
                        }
                    }
                }
            }
        }
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

    private Cluster createCluster(RootedTree tree, Node node, String ukLineage) {
        String delLineage = (String)node.getAttribute("del_lineage");
        int tipCount = countTips(tree, node);
        int ukTipCount = countTips(tree, node, "country_uk_deltran", true);

        Node[] haplotypeChild = new Node[1];
        int depth = findHaplotype(tree, node, 0, haplotypeChild);

        String haplotype = (String)haplotypeChild[0].getAttribute("sequence_hash");
        if (haplotype == null) {
            errorStream.println("no haplotype");
        }

        String representative = (tree.isExternal(haplotypeChild[0]) ?
                tree.getTaxon(haplotypeChild[0]).getName() :
                (String)haplotypeChild[0].getAttribute("representative"));
        if (representative == null) {
            errorStream.println("no haplotype");
        }

        return new Cluster(node, delLineage, ukLineage, representative, haplotype, depth, tipCount, ukTipCount);
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

    class Cluster {
        public Cluster(String lineage, String representative, String haplotype, int depth, int tipCount, int ukTipCount) {
            this.lineage = lineage;
            this.representative = representative;
            this.haplotype = haplotype;
            this.depth = depth;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
            this.newLineage = false;
        }

        public Cluster(Node node, String delTrans, String lineage, String representative, String haplotype, int depth, int tipCount, int ukTipCount) {
            this.node = node;
            this.delTrans = delTrans;
            this.lineage = lineage;
            this.representative = representative;
            this.haplotype = haplotype;
            this.depth = depth;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
            this.newLineage = true;
        }

        final String lineage;
        final String representative;
        final String haplotype;
        final int depth;
        final int tipCount;
        final int ukTipCount;
        final boolean newLineage;
        Node node;
        String delTrans;
    }

}

