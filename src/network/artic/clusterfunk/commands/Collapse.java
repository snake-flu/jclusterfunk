package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.MutableRootedTree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

/**
 *
 */
public class Collapse extends Command {
    public Collapse(String treeFileName,
                    String outputPath,
                    FormatType outputFormat,
                    double branchThreshold,
                    boolean isVerbose) {

        super(isVerbose);

        if (branchThreshold <= 0.0) {
            errorStream.println("Branch length threshold value should be > 0.0");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        MutableRootedTree outTree = new MutableRootedTree(tree);

        int count = 0;
        for (Node node : outTree.getInternalNodes()) {
            if (outTree.getLength(node) < branchThreshold) {
                Node parent = outTree.getParent(node);
                if (parent != null) {
                    outTree.removeChild(node, parent);
                    for (Node child : outTree.getChildren(node)) {
                        outTree.addChild(child, parent);
                    }
                    count += 1;
                }

            }
        }

        if (isVerbose) {
            outStream.println("Branches collapsed: " + count);
            outStream.println();
            outStream.println("Writing tree file, " + outputPath + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }
        writeTreeFile(outTree, outputPath, outputFormat);
    }

}

