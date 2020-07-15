package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusExporter;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 */
class Prune extends Command {
    Prune(String treeFileName,
          String taxaFileName,
          String outputPath,
          Format outputFormat,
          String indexColumn,
          int indexHeader,
          String headerDelimiter,
          boolean pruneNonMatching,
          boolean isVerbose) {

        super(isVerbose);

        Set<String> taxonSet = readTaxa(taxaFileName, indexColumn);

        processTreeFile(treeFileName, outputPath, outputFormat);

    }

    @Override
    RootedTree processTree(RootedTree tree) {
        return new RootedSubtree(tree, includedTaxa);
    }

}

