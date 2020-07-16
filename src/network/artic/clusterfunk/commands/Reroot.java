package network.artic.clusterfunk.commands;

import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.ReRootedTree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import network.artic.clusterfunk.RootType;

import java.util.Map;

/**
 *
 */
public class Reroot extends Command {
    public Reroot(String treeFileName,
           String outputPath,
           FormatType outputFormat,
           int indexHeader,
           String headerDelimiter,
           RootType rootType,
           String[] outgroups,
           boolean isVerbose) {

        super(null, null, null, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        RootedTree outTree;

        switch (rootType) {
            case OUTGROUP:
                if (isVerbose) {
                    outStream.println("Outgroup rooting");
                    outStream.println("Outgroup: " + String.join(", ", outgroups));
                    outStream.println();
                }
                outTree = new ReRootedTree(tree, ReRootedTree.RootingType.MID_POINT);
                break;
            case MIDPOINT:
                if (isVerbose) {
                    outStream.println("Midpoint rooting");
                    outStream.println();
                }
                outTree = new ReRootedTree(tree, ReRootedTree.RootingType.MID_POINT);
                break;
            default:
                throw new IllegalArgumentException("Unknown reroot type");
        }

        writeTreeFile(outTree, outputPath, outputFormat);
    }

}

