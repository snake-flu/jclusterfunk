package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Cluster extends Command {
    public Cluster(String treeFileName,
                   String outputFileName,
                   FormatType outputFormat,
                   String outputMetadataFileName,
                   String annotationName,
                   String annotationValue,
                   String clusterName,
                   String clusterPrefix,
                   final int maxChildLevel,
                   boolean isVerbose) {

        super(null, null, null, 0, null, isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Tree annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        if (isVerbose) {
            outStream.println("Annotating clusters in tree where " + annotationName + " = " + annotationValue + " as " + clusterName );
            outStream.println();
        }

        PrintWriter tmpWriter = null;
        if (outputMetadataFileName != null) {
            try {
                tmpWriter = new PrintWriter(Files.newBufferedWriter(Paths.get(outputMetadataFileName)));
            } catch (IOException ioe) {
                errorStream.println("Error opening output file: " + ioe.getMessage());
                System.exit(1);
            }
        }

        final PrintWriter outputMetadataWriter = tmpWriter;

        if (outputMetadataWriter != null) {
            outputMetadataWriter.print("tree");
            outputMetadataWriter.print("\t");
            outputMetadataWriter.print("tip");
            outputMetadataWriter.print("\t");
            outputMetadataWriter.print("cluster");
            outputMetadataWriter.print("\t");
            outputMetadataWriter.print("tmrca");
            outputMetadataWriter.println();

        }

        processTrees(treeFileName, outputFileName, outputFormat, tree -> {
            Map<Object, Double> tmrcaMap = annotateClusters(tree, annotationName, annotationValue, clusterName, clusterPrefix, maxChildLevel);

            if (outputMetadataWriter != null) {
                for (Node tip : tree.getExternalNodes()) {
                    Object value = tip.getAttribute(annotationName);
                    if (value == null) {
                        errorStream.println("Tip, " + tree.getTaxon(tip).getName() + ", missing '" + annotationName + "' attribute");
                        System.exit(1);
                    }


                    if (value.equals(annotationValue)) {
                        Object cluster = tip.getAttribute(clusterName);
                        if (cluster == null) {
                            errorStream.println("Tip, " + tree.getTaxon(tip).getName() + ", missing cluster ('" + clusterName + "') attribute");
                            System.exit(1);
                        }

                        Double tmrca = tmrcaMap.get(cluster);
                        if (tmrca == null) {
                            errorStream.println("TMRCA missing for cluster ('" + cluster + "')");
                            System.exit(1);
                        }

                        outputMetadataWriter.print(tree.getAttribute("name"));
                        outputMetadataWriter.print("\t");
                        outputMetadataWriter.print(tree.getTaxon(tip).getName());
                        outputMetadataWriter.print("\t");
                        outputMetadataWriter.print(cluster.toString());
                        outputMetadataWriter.print("\t");
                        outputMetadataWriter.print(tmrca);
                        outputMetadataWriter.println();
                    }
                }
            }
            return tree;
        });

        if (outputMetadataWriter != null) {
            outputMetadataWriter.close();
        }

    }

    /**
     * When ever a change in the value of a given attribute occurs at a node, creates a new cluster number and annotates
     * descendents with that cluster number.
     * @param tree
     * @param attributeName
     */
    Map<Object, Double> annotateClusters(RootedTree tree, String attributeName, Object attributeValue, String clusterAttributeName,
                                         String clusterPrefix, int maxChildLevel) {

        Map<Object, Double> tmrcaMap = new HashMap<Object, Double>();
        annotateClusters(tree, tree.getRootNode(), attributeName, attributeValue, null,
                clusterAttributeName, clusterPrefix, null, maxChildLevel,
                new HashMap<Object, Integer>(),
                tmrcaMap);
        return tmrcaMap;
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param parentValue
     */
    private void annotateClusters(RootedTree tree, Node node, String attributeName, Object attributeValue, Object parentValue,
                                  String clusterAttributeName, String clusterPrefix, String currentClusterName, int maxChildLevel,
                                  Map<Object, Integer> countMap, Map<Object, Double> tmrcaMap) {

        Object value = node.getAttribute(attributeName);
        if (value == null) {
            errorStream.println("Node in tree is missing '" + attributeName + "' attribute");
            System.exit(1);
        }

        if (attributeValue.equals(value)) {
            if (!value.equals(parentValue)) {

                Integer count = countMap.getOrDefault(value, 0);
                count += 1;
                if (clusterPrefix != null) {
                    currentClusterName = clusterPrefix + count;
                } else {
                    currentClusterName = "" + count;
                }

                countMap.put(value, count);
                tmrcaMap.put(currentClusterName, tree.getHeight(node));
            }

            node.setAttribute(clusterAttributeName, currentClusterName);
        }

        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                annotateClusters(tree, child, attributeName, attributeValue, value, clusterAttributeName, clusterPrefix, currentClusterName, maxChildLevel, countMap, tmrcaMap);
            }
        }
    }

}

