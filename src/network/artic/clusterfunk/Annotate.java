package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 *
 */
class Annotate extends Command {
    Annotate(String treeFileName,
             String metadataFileName,
             String outputPath,
             Format outputFormat,
             String indexColumn,
             int indexHeader,
             String headerDelimiter,
             String[] headerColumns,
             String[] annotationColumns,
             boolean replace,
             boolean isVerbose) {

        super(isVerbose);

        RootedTree tree = null;

        try {
            NexusImporter importer = new NexusImporter(new FileReader(treeFileName));
            tree = (RootedTree)importer.importNextTree();
        } catch (ImportException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Map<String, CSVRecord> metadata = readCSV(metadataFileName, indexColumn);

        CSVRecord firstRecord =  metadata.get(metadata.keySet().iterator().next());

        if (isVerbose) {
            System.out.println("          Read tree: " + treeFileName);
            System.out.println("               Taxa: " + tree.getTaxa().size());
            System.out.println("Read metadata table: " + metadataFileName);
            System.out.println("               Rows: " + metadata.size());
            System.out.println("       Index column: " + (indexColumn == null ? firstRecord.getParser().getHeaderNames().get(0) : indexColumn));
            System.out.println();
        }

        if (annotationColumns != null && annotationColumns.length > 0) {
            if (isVerbose) {
                System.out.println((replace ? "Replacing" : "Appending") + " tip annotations with columns: " + String.join(", ", annotationColumns));
            }
            annotateTips(tree, metadata, annotationColumns, replace);
        }

        if (headerColumns != null && headerColumns.length > 0) {
            if (isVerbose) {
                System.out.println((replace ? "Replacing" : "Appending") + " tip labels with columns: " + String.join(", ", headerColumns));
            }
            relabelTips(tree, metadata, headerColumns, replace, headerDelimiter);
        }

        if (isVerbose) {
            System.out.println("Writing tree...");
        }
        writeTreeFile(tree, outputPath, outputFormat);

        if (isVerbose) {
            System.out.println("Done.");
        }

    }

    /**
     * Annotates the tips of a tree with a set of columns from the metadata table
     * @param tree
     * @param metadata
     * @param columnNames
     * @param replace
     */
    private void annotateTips(RootedTree tree, Map<String, CSVRecord> metadata, String[] columnNames, boolean replace) {
        if (replace) {
            clearExternalAttributes(tree);
        }

        for (Node tip : tree.getExternalNodes()) {
            String key = tree.getTaxon(tip).getName();
            CSVRecord record = metadata.get(key);
            if (record == null) {
                errorStream.println("Tip index, " + key + ", not found in metadata table");
                System.exit(1);
            }
            for (String name : columnNames) {
                tip.setAttribute(name, record.get(name));
            }
        }
    }

    /**
     * Annotates the tips of a tree with a set of columns from the metadata table
     * @param tree
     * @param metadata
     * @param columnNames
     * @param replace
     */
    private void relabelTips(RootedTree tree, Map<String, CSVRecord> metadata, String[] columnNames, boolean replace, String headerDelimiter) {
        for (Node tip : tree.getExternalNodes()) {
            String key = tree.getTaxon(tip).getName();
            CSVRecord record = metadata.get(key);
            if (record == null) {
                errorStream.println("Tip index, " + key + ", not found in metadata table");
                System.exit(1);
            }
            StringBuilder tipLabel = new StringBuilder();
            boolean first = true;
            if (!replace) {
                tipLabel.append(tree.getTaxon(tip).getName());
                first = false;
            }

            for (String name : columnNames) {
                if (!first) {
                    tipLabel.append(headerDelimiter);
                    first = false;
                }
                tipLabel.append(record.get(name));
            }
            tree.renameTaxa(tree.getTaxon(tip), Taxon.getTaxon(tipLabel.toString()));
        }
    }


}

