package network.artic.clusterfunk.commands;

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

        Map<String, CSVRecord> metadata1 = readCSV(metadataFileName1, null);
        List<String> headerNames1 = headerRecord.getParser().getHeaderNames();
        Map<String, CSVRecord> metadata2 = readCSV(metadataFileName2, indexColumn);
        List<String> headerNames2 = headerRecord.getParser().getHeaderNames();

        indexColumn = (indexColumn == null ? headerNames2.get(0) : indexColumn);

        if (!headerNames1.contains(indexColumn)) {
            errorStream.println("Metadata file, " + metadataFileName1 + ", does not contain index column, " + indexColumn);
            System.exit(1);
        }

        if (isVerbose) {
            outStream.println("Read metadata table 1: " + metadataFileName1);
            outStream.println("                 Rows: " + metadata1.size());
            outStream.println();
            outStream.println("Read metadata table 2: " + metadataFileName2);
            outStream.println("                 Rows: " + metadata2.size());
            outStream.println("         Index column: " + indexColumn);
            outStream.println();
        }

        List<String> headerNames = new ArrayList<>(headerNames1);
        for (String name : headerNames2) {
            if (!headerNames1.contains(name)) {
                headerNames.add(name);
            }
        }

        if (outputFileName != null) {
            if (isVerbose) {
                outStream.println("Writing merged metadata file, " + outputFileName);
                outStream.println();
            }
            int rowCount = 0;
            int valueCount = 0;

            boolean extractMatches = true;
            try {
                PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

                writer.println(String.join(",", headerNames));

                for (String key : metadata1.keySet()) {
                    CSVRecord record1 = metadata1.get(key);
                    String index = record1.get(indexColumn);
                    CSVRecord record2 = metadata2.get(index);
                    if (record2 != null) {
                        rowCount += 1;
                    }
                    if (!extractMatches || record2 != null) {
                        boolean first = true;
                        for (String name : headerNames) {
                            String value = "";
                            if (headerNames1.contains(name)) {
                                value = record1.get(name);
                            }
                            if (record2 != null && (value.isEmpty() || overwriteExisting) && !name.equals(indexColumn)) {
                                if (headerNames2.contains(name)) {
                                    value = record2.get(name);
                                    valueCount += 1;
                                }
                            }
                            writer.print(first ? "" : ",");
                            writer.print(value);
                            first = false;
                        }
                        writer.println();
                    }
                }

                writer.close();
            } catch (IOException e) {
                errorStream.println("Error writing metadata file: " + e.getMessage());
                System.exit(1);
            }

            if (isVerbose) {
                outStream.println("Columns added: " + (headerNames.size() - headerNames1.size()));
                outStream.println("  Rows merged: " + rowCount);
                outStream.println("  Values merged: " + valueCount);
                outStream.println();
            }

        }

    }

}

