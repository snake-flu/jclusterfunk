package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

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
                                   String outputMetadata,
                                   String indexColumn,
                                   int indexHeader,
                                   String headerDelimiter,
                                   String annotationName,
                                   String annotationValue,
                                   String clusterName,
                                   String clusterPrefix,
                                   boolean isVerbose) {

        super(isVerbose);

        String path = checkOutputPath(outputPath);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        clusterName = "uk_lineage";

        Map<String, CSVRecord> lineages = readCSV(clusterFileName, clusterName);

        Map<String, Cluster> haplotypeClusterMap = new HashMap<>();
        for (String lineageName : lineages.keySet()) {
            CSVRecord record = lineages.get(lineageName);

            //uk_lineage,sequence_hash,depth,del_trans,uk_tip_count
            Cluster cluster = new Cluster(lineageName,
                    record.get("sequence_hash"),
                    Integer.parseInt(record.get("depth")),
                    Integer.parseInt(record.get("uk_tip_count")),
                    Integer.parseInt(record.get("tip_count")));

            haplotypeClusterMap.put(record.get("sequence_hash"), cluster);
        }

        if (isVerbose) {
            outStream.println("Lineage name: " + clusterName);
            outStream.println("    Lineages: " + haplotypeClusterMap.size());
            outStream.println();
        }

        Map<Node, Cluster> nodeClusterMap = new HashMap<>();

        findClusterRoots(tree, haplotypeClusterMap, nodeClusterMap);

        assignLineages(tree, tree.getRootNode(), nodeClusterMap);

        String outputTreeFileName = path + outputPrefix + ".nexus";

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputTreeFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputTreeFileName, outputFormat);

    }

    /**
     * recursive version
     * @param tree
     * @param haplotypeClusterMap
     * @param nodeClusterMap
     */
    private void findClusterRoots(RootedTree tree, Map<String, Cluster> haplotypeClusterMap, Map<Node, Cluster> nodeClusterMap) {
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

                boolean isUK = (Boolean)clusterNode.getAttribute("country_uk_deltran");

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
            }
        }
    }

    /**
     * recursive version
     * @param tree
     * @param nodeClusterMap
     */
    private void assignLineages(RootedTree tree, Node node, Map<Node, Cluster> nodeClusterMap) {
        Cluster cluster = nodeClusterMap.get(node);
        if (cluster != null) {
            propagateAttribute(tree, node, "country_uk_deltran", true, "new_uk_lineage", cluster.lineage);
        }
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                assignLineages(tree, child, nodeClusterMap);
            }
        }
    }
    
    class Cluster {
        public Cluster(String lineage, String haplotype, int depth, int tipCount, int ukTipCount) {
            this.lineage = lineage;
            this.haplotype = haplotype;
            this.depth = depth;
            this.tipCount = tipCount;
            this.ukTipCount = ukTipCount;
        }

        final String lineage;
        final String haplotype;
        final int depth;
        final int tipCount;
        final int ukTipCount;
        Node node;
        String delTrans;
    }
}

