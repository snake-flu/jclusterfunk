package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.MutableRootedTree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

/**
 *
 */
public class Scale extends Command {
    public Scale(String treeFileName,
                 String outputPath,
                 FormatType outputFormat,
                 double scaleFactor,
                 double branchThreshold,
                 boolean isVerbose) {

        super(isVerbose);

        if (scaleFactor <= 0.0) {
            errorStream.println("Scale factor should be > 0.0");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        MutableRootedTree outTree = new MutableRootedTree(tree);

        for (Node node : outTree.getNodes()) {
            outTree.setLength(node, outTree.getLength(node) * scaleFactor);
        }
        int scaleCount = outTree.getNodes().size();

        int collapseCount = 0;
        if (branchThreshold >= 0.0) {
            for (Node node : outTree.getInternalNodes()) {
                if (outTree.getLength(node) <= branchThreshold) {
                    Node parent = outTree.getParent(node);
                    if (parent != null) {
                        outTree.removeChild(node, parent);
                        for (Node child : outTree.getChildren(node)) {
                            outTree.addChild(child, parent);
                        }
                        collapseCount += 1;
                    }

                }
            }
        }

        if (isVerbose) {
            outStream.println("Branches scaled: " + scaleCount);
            if (branchThreshold >= 0.0) {
                outStream.println("Branches collapsed: " + collapseCount);
            }
            outStream.println();
            outStream.println("Writing tree file, " + outputPath + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }
        writeTreeFile(outTree, outputPath, outputFormat);
    }

}

