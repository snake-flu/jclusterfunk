package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.NewickExporter;
import jebl.evolution.io.NexusExporter;
import jebl.evolution.io.TreeExporter;
import jebl.evolution.trees.RootedTree;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

/**
 * Base class for clusterfunk commands. Provides some static utility functions.
 */
class Command {
    final boolean isVerbose;
    final static PrintStream errorStream = System.err;
    final static PrintStream outStream = System.out;

    Command(boolean isVerbose) {
        this.isVerbose = isVerbose;
    }

    static void clearExternalAttributes(RootedTree tree) {
        for (Node node : tree.getExternalNodes()) {
            Set<String> attributeNames = new HashSet<>(node.getAttributeNames());
            for (String attributeName : attributeNames) {
                node.removeAttribute(attributeName);
            }
        }
    }
    static void clearInternalAttributes(RootedTree tree) {
        for (Node node : tree.getInternalNodes()) {
            Set<String> attributeNames = new HashSet<>(node.getAttributeNames());
            for (String attributeName : attributeNames) {
                node.removeAttribute(attributeName);
            }
        }
    }

    /**
     * Takes a set of objects and creates a set of strings using the toString() method
     * @param objectSet
     * @return
     */
    static Set<String> toString(Set<Object> objectSet)
    {
        Set<String> strings = new TreeSet<>();
        for (Object o : objectSet) {
            if (o != null) {
                strings.add(o.toString());
            }
        }
        return strings;
    }

    static Map<String, CSVRecord> readCSV(String fileName, String indexColumn) {
        Map<String, CSVRecord> csv = new HashMap<>();
        try {
            Reader in = new FileReader(fileName);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            if (indexColumn != null) {
                // a particular column is used to index - check it is there for the first record
                // and use it to key the records

                boolean first = true;
                for (CSVRecord record : records) {
                    if (first) {
                        if (record.get(indexColumn) == null) {
                            errorStream.println("Index column, " + indexColumn + " not found in metadata table");
                            System.exit(1);
                        }
                        first = false;
                    }
                    csv.put(record.get(indexColumn), record);
                }
            } else {
                // key the records against the first column
                for (CSVRecord record : records) {
                    csv.put(record.get(0), record);
                }
            }
        } catch (IOException e) {
            errorStream.println("Error reading metadata file: " + e.getMessage());
            System.exit(1);
        }
        return csv;
    }

    /**
     * Writes a tree
     * @param tree
     * @param fileName
     */
    static void writeTreeFile(RootedTree tree, String fileName, Format format) {
        writeTreeFile(Collections.singletonList(tree), fileName, format);
    }

    /**
     * Writes a tree file with a list of trees
     * @param trees
     * @param fileName
     */
    static void writeTreeFile(List<RootedTree> trees, String fileName, Format format) {
        try {
            FileWriter writer = new FileWriter(fileName);

            TreeExporter exporter;

            switch (format) {
                case NEXUS:
                    exporter = new NexusExporter(writer);
                    break;
                case NEWICK:
                    exporter = new NewickExporter(writer);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown format: " + format);
            }

            exporter.exportTrees(trees);
            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing tree file: " + e.getMessage());
            System.exit(1);
        }
    }
}
