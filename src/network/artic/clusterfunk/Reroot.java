package network.artic.clusterfunk;

import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.ReRootedTree;
import jebl.evolution.trees.RootedTree;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
class Reroot extends Command {
    Reroot(String treeFileName,
           String outputPath,
           Format outputFormat,
           int indexHeader,
           String headerDelimiter,
           String[] outgroups,
           boolean isVerbose) {

        super(isVerbose);

        processTreeFile(treeFileName, outputPath, outputFormat);
    }

    /**
     * Prunes the tips of a tree to a set of taxa
     * @param tree
     */
    private void rootTree(RootedTree tree, String[] outgroups) {
        ReRootedTree reRootedTree = new ReRootedTree(tree, ReRootedTree.RootingType.MID_POINT);
    }

    @Override
    RootedTree processTree(RootedTree tree) {
        return null;
    }
}

