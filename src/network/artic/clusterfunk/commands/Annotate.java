package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.util.*;

/**
 * Adds tip annotations from a metadata file. These can either be added to the tip labels or as NEXUS
 * metadata comments for the tips.
 */
public class Annotate extends Command {
    public Annotate(String treeFileName,
                    String metadataFileName,
                    String outputFileName,
                    FormatType outputFormat,
                    String indexColumn,
                    int indexHeader,
                    String headerDelimiter,
                    String[] labelColumns,
                    String[] annotationColumns,
                    boolean replace,
                    boolean ignoreMissing,
                    boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        if (annotationColumns != null && annotationColumns.length > 0) {
            if (outputFormat != FormatType.NEXUS) {
                errorStream.println("Tip annotations are only compatible with NEXUS output format");
                System.exit(1);
            }

            if (isVerbose) {
                outStream.println((replace ? "Replacing" : "Appending") + " tip annotations with columns: " + String.join(", ", annotationColumns));
                outStream.println();
            }
            annotateTips(tree, taxonMap, metadata, annotationColumns, replace, ignoreMissing);
        }

        if (labelColumns != null && labelColumns.length > 0) {
            if (isVerbose) {
                outStream.println((replace ? "Replacing" : "Appending") + " tip labels with columns: " + String.join(", ", labelColumns));
                outStream.println();
            }
            relabelTips(tree, taxonMap, metadata, labelColumns, headerDelimiter, replace, ignoreMissing);
        }

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputFileName, outputFormat);

    }

    /**
     * Annotates the tips of a tree with a set of columns from the metadata table
     * @param tree
     * @param taxonMap
     * @param metadata
     * @param columnNames
     * @param replace
     */
    private void annotateTips(RootedTree tree,
                              Map<Taxon, String> taxonMap,
                              Map<String, CSVRecord> metadata,
                              String[] columnNames,
                              boolean replace,
                              boolean ignoreMissing) {
        if (replace) {
            clearExternalAttributes(tree);
        }

        for (Node tip : tree.getExternalNodes()) {
            String key = taxonMap.get(tree.getTaxon(tip));
            CSVRecord record = metadata.get(key);
            if (record == null) {
                if (!ignoreMissing) {
                    errorStream.println("Tip index, " + key + ", not found in metadata table");
                    System.exit(1);
                }
            } else {
                for (String name : columnNames) {
                    if (!record.get(name).isEmpty()) {
                        tip.setAttribute(name, record.get(name));
                    }
                }
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
    private void relabelTips(RootedTree tree,
                             Map<Taxon, String> taxonMap,
                             Map<String, CSVRecord> metadata,
                             String[] columnNames,
                             String headerDelimiter,
                             boolean replace,
                             boolean ignoreMissing) {
        for (Node tip : tree.getExternalNodes()) {
            String key = taxonMap.get(tree.getTaxon(tip));
            CSVRecord record = metadata.get(key);
            if (record == null) {
                if (!ignoreMissing) {
                    errorStream.println("Tip index, " + key + ", not found in metadata table");
                    System.exit(1);
                }
            } else {
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


}

