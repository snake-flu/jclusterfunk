package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusExporter;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.taxa.Taxon;
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
          String metadataFileName,
          String outputPath,
          Format outputFormat,
          String indexColumn,
          int indexHeader,
          String headerDelimiter,
          boolean pruneNonMatching,
          boolean isVerbose) {

        super(isVerbose);

        Map<String, CSVRecord> metadata = readCSV(metadataFileName, indexColumn);

        CSVRecord firstRecord =  metadata.get(metadata.keySet().iterator().next());

        List<RootedTree> trees = new ArrayList<>();

        try {
            NexusImporter importer = new NexusImporter(new FileReader(treeFileName));

            while (importer.hasTree()) {
                trees.add((RootedTree) importer.importNextTree());
            }

        } catch (ImportException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (isVerbose) {
            System.out.println("      Read treefile: " + treeFileName);
            if (trees.size() > 1) {
                System.out.println("    Number of trees: " + trees.size());
            }
            System.out.println("     Number of tips: " + trees.get(0).getExternalNodes().size());
            System.out.println("Read metadata table: " + metadataFileName);
            System.out.println("               Rows: " + metadata.size());
            System.out.println("       Index column: " + (indexColumn == null ? firstRecord.getParser().getHeaderNames().get(0) : indexColumn));
            System.out.println();
        }

        if (isVerbose) {
            if (trees.size() > 1) {
                System.out.println("Processing trees...");

            } else {
                System.out.println("Writing tree...");
            }
        }

        for (RootedTree tree : trees) {
            pruneTaxa(tree);
        }

        writeTreeFile(trees, outputPath, outputFormat);


        if (isVerbose) {
            System.out.println("Done.");
        }

    }

    /**
     * Prunes the tips of a tree to a set of taxa
     * @param tree
     */
    private void pruneTaxa(RootedTree tree) {
    }

}

