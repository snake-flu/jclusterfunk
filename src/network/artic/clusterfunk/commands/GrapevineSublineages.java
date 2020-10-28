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

        findClusterRoots(tree, tree.getRootNode(), false, nodeClusterMap);

        // find cases where more than one node has the same lineage designation
        Set<String> lineageSet = new HashSet<>();
        Set<String> ambiguousLineageSet = new HashSet<>();
        for (Node node : nodeClusterMap.keySet()) {
            Cluster cluster = nodeClusterMap.get(node);
            if (lineageSet.contains(cluster.ukLineage)) {
                ambiguousLineageSet.add(cluster.ukLineage);
//                errorStream.println("Lineage " + lineage + " exists in more than one place");
            }
            lineageSet.add(cluster.ukLineage);
        }

        // get the set of nodes for each ambiguous lineage
        Map<String, Set<Node>> ambiguousLineageMap = new HashMap<>();
        for (Node node : nodeClusterMap.keySet()) {
            Cluster cluster = nodeClusterMap.get(node);
            if (ambiguousLineageSet.contains(cluster.ukLineage)) {
                Set<Node> nodeSet = ambiguousLineageMap.getOrDefault(cluster.ukLineage, new HashSet<>());
                nodeSet.add(node);
                ambiguousLineageMap.put(cluster.ukLineage, nodeSet);
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
                    nodeClusterMap.put(node, null);
                }
            }

        }

        Map<String, Node> lineageNodeMap = new HashMap<>();

//        clusterLineages(tree, tree.getRootNode(), "uk_lineage", null,
//                "new_uk_lineage", minSublineageSize, nodeClusterMap, lineageNodeMap);

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

    /**
     * recursive version
     * @param tree
     * @param node
     * @param lineageClusterMap
     */
    private void clusterLineages(RootedTree tree, Node node,
                                 String newLineageName, int minSublineageSize,
                                 Map<Node, Cluster> nodeClusterMap,
                                 Map<String, Cluster> lineageClusterMap) {
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
                        if (childLineage.equals(nodeClusterMap.get(child).ukLineage)) {
                            int count = countTips(tree, child);
                            childSizes.add(new Pair(child, count));
                            if (count >= minSublineageSize) {
                                bigSublineageCount += 1;
                            }

                            totalSize += count;
                        }
                    }

                    childSizes.sort(Comparator.comparing(k -> -k.count));

                    Cluster nodeCluster;
                    Node lineageNode;
                    if (childSizes.size() > 1) {
                        // there are multiple children of this lineage - merge them at this node
                        lineageNode = node;
                        nodeCluster = createCluster(tree, node, childLineage);

                        if (lineageClusterMap.containsKey(nodeCluster.ukLineage)) {
                            Cluster otherCluster = lineageClusterMap.get(nodeCluster.ukLineage);
                            throw new RuntimeException("multiple roots of a lineage present");
                        }
                        lineageClusterMap.put(nodeCluster.ukLineage, nodeCluster);
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
//                clusterLineages(tree, child, newLineageName, minSublineageSize, nodeClusterMap, lineageNodeMap);
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

    private Cluster createCluster(RootedTree tree, Node node, String ukLineage) {
        String delLineage = (String)node.getAttribute("del_lineage");
        int tipCount = countTips(tree, node);
        int ukTipCount = countTips(tree, node, "country_uk_deltran", true);
        String haplotype = getHaplotype(tree, node);
        return new Cluster(node, delLineage, haplotype, ukLineage, tipCount, ukTipCount);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     */
    private String getHaplotype(RootedTree tree, Node node) {
        Map<String, Integer> haplotypes = new HashMap<>();
        for (Node child : tree.getChildren(node)) {
            if (tree.isExternal(child)) {
                if (tree.getLength(child) < ZERO_BRANCH_THRESHOLD) {
                    String hap = (String)child.getAttribute("sequence_hash");
                    haplotypes.put(hap, haplotypes.getOrDefault(hap, 0) + 1);
                }
            }
        }

        // order by frequency
        LinkedHashMap<String, Integer> reversed = new LinkedHashMap<>();
        haplotypes.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reversed.put(x.getKey(), x.getValue()));


        if (haplotypes.size() > 0) {
            // get the most frequent
            String haplotypeHash = reversed.keySet().iterator().next();
            if (haplotypes.size() > 1) {
                errorStream.println("multiple haplotypes on internal node");
            }
            return haplotypeHash;
        }
        return null;
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
        public Cluster(Node node, String delTrans, String haplotype, String ukLineage, int tipCount, int ukTipCount) {
            this.node = node;
            this.delTrans = delTrans;
            this.haplotype = haplotype;
            this.ukLineage = ukLineage;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
        }

        Node node;
        String delTrans;
        String haplotype;
        String ukLineage;
        int tipCount;
        int ukTipCount;
    }
}

