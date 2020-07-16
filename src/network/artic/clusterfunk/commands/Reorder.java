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
            String outputPath,
            FormatType outputFormat,
            OrderType orderType,
            boolean isVerbose) {

        super(null, null, null, 0, null, isVerbose);

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

