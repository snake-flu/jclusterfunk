package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.NexusExporter;
import jebl.evolution.trees.RootedTree;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Base class for clusterfunk commands. Provides some static utility functions.
 */
class Command {
    final boolean isVerbose;

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
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (CSVRecord record : records) {
                csv.put(record.get(0), record);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return csv;
    }

    /**
     * Writes a tree
     * @param tree
     * @param fileName
     */
    static void writeTree(RootedTree tree, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            NexusExporter exporter = new NexusExporter(writer);
            exporter.exportTree(tree);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
