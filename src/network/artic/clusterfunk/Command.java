package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.*;
import jebl.evolution.trees.RootedTree;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

/**
 * Base class for clusterfunk commands. Provides some static utility functions.
 */
abstract class Command {
    final boolean isVerbose;
    final static PrintStream errorStream = System.err;
    final static PrintStream outStream = System.out;

    /**
     * Simple constructor
     * @param isVerbose
     */
    Command(boolean isVerbose) {
        this.isVerbose = isVerbose;
    }

    final Map<String, CSVRecord> readMetadataTable(String metadataFileName, String indexColumn) {
        Map<String, CSVRecord> metadata = readCSV(metadataFileName, indexColumn);

        CSVRecord firstRecord =  metadata.get(metadata.keySet().iterator().next());

        if (isVerbose) {
            System.out.println("Read metadata table: " + metadataFileName);
            System.out.println("               Rows: " + metadata.size());
            System.out.println("       Index column: " + (indexColumn == null ? firstRecord.getParser().getHeaderNames().get(0) : indexColumn));
            System.out.println();
        }

        return metadata;
    }

    /**
     * a tree processing pipe
     * @param treeFileName
     * @param outputPath
     * @param outputFormat
     */
    final void processTreeFile(
            String treeFileName,
            String outputPath,
            Format outputFormat) {

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
            System.out.println("  Read treefile: " + treeFileName);
            if (trees.size() > 1) {
                System.out.println("Number of trees: " + trees.size());
            }
            System.out.println(" Number of tips: " + trees.get(0).getExternalNodes().size());
            System.out.println(" Number of tips: " + trees.get(0).getExternalNodes().size());
            System.out.println();
        }

        if (isVerbose) {
            if (trees.size() > 1) {
                System.out.println("Processing trees");
            } else {
                System.out.println("Writing tree...");
            }
        }

        List<RootedTree> outTrees = new ArrayList<>();

        for (RootedTree tree : trees) {
            outTrees.add(processTree(tree));
        }

        writeTreeFile(outTrees, outputPath, outputFormat);

        if (isVerbose) {
            System.out.println("Done.");
        }

    }

    abstract RootedTree processTree(RootedTree tree);

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
