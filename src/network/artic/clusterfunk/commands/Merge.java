package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds tip annotations from a metadata file. These can either be added to the tip labels or as NEXUS
 * metadata comments for the tips.
 */
public class Merge extends Command {
    public Merge(String metadataFileName1,
                 String metadataFileName2,
                 String outputFileName,
                 String indexColumn,
                 String[] addColumns,
                 boolean overwriteExisting,
                 boolean ignoreMissing,
                 boolean isVerbose) {

        super(isVerbose);

        Map<String, CSVRecord> metadata1 = readCSV(metadataFileName1, indexColumn);
        CSVRecord headerRecord1 = headerRecord;
        Map<String, CSVRecord> metadata2 = readCSV(metadataFileName2, indexColumn);
        CSVRecord headerRecord2 = headerRecord;

        if (isVerbose) {
            outStream.println("Read metadata table 1: " + metadataFileName1);
            outStream.println("                 Rows: " + metadata1.size());
            outStream.println("         Index column: " + (indexColumn == null ? headerRecord1.getParser().getHeaderNames().get(0) : indexColumn));
            outStream.println();
            outStream.println("Read metadata table 2: " + metadataFileName1);
            outStream.println("                 Rows: " + metadata1.size());
            outStream.println("         Index column: " + (indexColumn == null ? headerRecord1.getParser().getHeaderNames().get(0) : indexColumn));
            outStream.println();
        }

        List<CSVRecord> metadataRows = new ArrayList<>();
        for (String key : metadata2.keySet()) {
            CSVRecord record1 = metadata1.get(key);
            if (record1 != null) {
                CSVRecord record2 = metadata2.get(key);

//                new CSVRecord();
//                metadataRows.add(metadata.get(taxonMap.get(taxon)));
            }
        }

        if (outputFileName != null) {
            if (isVerbose) {
                outStream.println("Writing merged metadata file, " + outputFileName);
                outStream.println();
            }
            writeMetadataFile(metadataRows, outputFileName);

            try {
                PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

                List<String> headerNames = metadata1.get(0).getParser().getHeaderNames();
                writer.println(String.join(",", headerNames));

//                for (CSVRecord record : records) {
//                    CSVRecord record1 = metadata1.get(key);
//
//                    boolean first = true;
//                    for (String value : record) {
//                        if (first) {
//                            writer.print(value);
//                            first = false;
//                        } else {
//                            writer.print(",");
//                            writer.print(value);
//                        }
//                    }
//                    writer.println();
//                }

                writer.close();
            } catch (IOException e) {
                errorStream.println("Error writing metadata file: " + e.getMessage());
                System.exit(1);
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

