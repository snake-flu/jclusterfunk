package network.artic.clusterfunk.commands;

import com.sun.tools.hat.internal.model.Root;
import javafx.scene.Parent;
import jebl.evolution.graphs.Graph;
import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.*;
import network.artic.clusterfunk.FormatType;
import network.artic.clusterfunk.RootType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

