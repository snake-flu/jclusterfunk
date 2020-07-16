package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.util.*;

/**
 *
 */
public class Annotate extends Command {
    public Annotate(String treeFileName,
             String metadataFileName,
             String outputPath,
             FormatType outputFormat,
             String indexColumn,
             int indexHeader,
             String headerDelimiter,
             String[] headerColumns,
             String[] annotationColumns,
             boolean replace,
             boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        if (annotationColumns != null && annotationColumns.length > 0) {
            if (isVerbose) {
                System.out.println((replace ? "Replacing" : "Appending") + " tip annotations with columns: " + String.join(", ", annotationColumns));
            }
            annotateTips(tree, taxonMap, metadata, annotationColumns, replace);
        }

        if (headerColumns != null && headerColumns.length > 0) {
            if (isVerbose) {
                System.out.println((replace ? "Replacing" : "Appending") + " tip labels with columns: " + String.join(", ", headerColumns));
            }
            relabelTips(tree, taxonMap, metadata, headerColumns, replace, headerDelimiter);
        }

    }

    /**
     * Annotates the tips of a tree with a set of columns from the metadata table
     * @param tree
     * @param taxonMap
     * @param metadata
     * @param columnNames
     * @param replace
     */
    private void annotateTips(RootedTree tree, Map<Taxon, String> taxonMap, Map<String, CSVRecord> metadata, String[] columnNames, boolean replace) {
        if (replace) {
            clearExternalAttributes(tree);
        }

        for (Node tip : tree.getExternalNodes()) {
            String key = taxonMap.get(tree.getTaxon(tip));
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
     * @param taxonMap
     * @param metadata
     * @param columnNames
     * @param replace
     */
    private void relabelTips(RootedTree tree, Map<Taxon, String> taxonMap, Map<String, CSVRecord> metadata, String[] columnNames, boolean replace, String headerDelimiter) {
        for (Node tip : tree.getExternalNodes()) {
            String key = taxonMap.get(tree.getTaxon(tip));
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

