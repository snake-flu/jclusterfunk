package network.artic.clusterfunk.commands;

import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SortedRootedTree;
import network.artic.clusterfunk.FormatType;
import network.artic.clusterfunk.OrderType;

/**
 *
 */
public class Reorder extends Command {
    public Reorder(String treeFileName,
                   String metadataFileName,
                   String outputPath,
                   FormatType outputFormat,
                   String indexColumn,
                   int indexHeader,
                   String headerDelimiter,
                   OrderType orderType,
                   String[] sortColumns,
                   boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        RootedTree outTree = tree;

        if (orderType != OrderType.UNCHANGED) {
            if (isVerbose) {
                outStream.println("Reordering branches by " + orderType.name().toLowerCase() + " node density");
                outStream.println();
            }
            outTree = new SortedRootedTree(tree,
                    orderType.equals(OrderType.DECREASING) ?
                            SortedRootedTree.BranchOrdering.DECREASING_NODE_DENSITY :
                            SortedRootedTree.BranchOrdering.INCREASING_NODE_DENSITY);
        }

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputPath + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(outTree, outputPath, outputFormat);
    }

}

