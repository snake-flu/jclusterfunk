package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.*;

/**
 * @deprecated
 */
public class Subcluster extends Command {

    public Subcluster(String treeFileName,
                      String outputFileName,
                      FormatType outputFormat,
                      String outputMetadataFileName,
                      String clusterPrefix,
                      String annotationName,
                      final int minLineageSize,
                      boolean isVerbose) {

        super(isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        numberSubLineages(tree, tree.getRootNode(), clusterPrefix, annotationName, minLineageSize);

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputFileName, outputFormat);

    }

    /**
     * recursive version
     * @param tree
     * @param node
     */
    private void numberSubLineages(RootedTree tree, Node node, String lineage, String newLineageName, int minSublineageSize) {
        if (!tree.isExternal(node)) {
            List<Pair> childSizes = new ArrayList<>();
            for (Node child : tree.getChildren(node)) {
                childSizes.add(new Pair(child, countTips(tree, child)));
            }
            childSizes.sort(Comparator.comparing(k -> -k.count));

            int sublineageNumber = 1;
            for (Pair pair : childSizes) {
                if (pair.count >= minSublineageSize) {
                    String sublineage = lineage + "." + sublineageNumber;
                    pair.node.setAttribute(newLineageName, sublineage);
                    propagateAttribute(tree, pair.node, newLineageName, sublineage);
                    sublineageNumber += 1;
                } else {
                    pair.node.setAttribute(newLineageName, lineage);
                    propagateAttribute(tree, pair.node, newLineageName, lineage);
                }
            }

        }
    }



}

