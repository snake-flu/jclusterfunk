package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.*;
import jebl.evolution.sequences.SequenceStateException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * Base class for clusterfunk commands. Provides some static utility functions.
 */
abstract class Command {
    final boolean isVerbose;
    final static PrintStream errorStream = System.err;
    final static PrintStream outStream = System.out;

    final String indexColumn;
    final int indexHeader;
    final String headerDelimiter;

    Map<String, CSVRecord> metadata = null;
    CSVRecord headerRecord = null;
    Set<String> taxa = null;

    /**
     * Simple constructor
     * @param isVerbose
     */
    public Command(boolean isVerbose) {
        this(null, null, null, 0, null, isVerbose);
    }

    /**
     * Constructor
     * @param metadataFileName
     * @param taxaFileName
     * @param indexColumn
     * @param indexHeader
     * @param headerDelimiter
     * @param isVerbose
     */
    Command(String metadataFileName, String taxaFileName, String indexColumn, int indexHeader, String headerDelimiter, boolean isVerbose) {
        this.indexColumn = indexColumn;
        this.indexHeader = indexHeader;
        this.headerDelimiter = headerDelimiter;
        this.isVerbose = isVerbose;

        if (metadataFileName != null) {
            readMetadataTable(metadataFileName, indexColumn);
        }

        if (taxaFileName != null) {
            readTaxa(taxaFileName, indexColumn);
        }
    }

    private void readMetadataTable(String metadataFileName, String indexColumn) {
        metadata = readCSV(metadataFileName, indexColumn);
        taxa = metadata.keySet();

        if (isVerbose) {
            outStream.println("Read metadata table: " + metadataFileName);
            outStream.println("               Rows: " + metadata.size());
            outStream.println("       Index column: " + (indexColumn == null ? headerRecord.getParser().getHeaderNames().get(0) : indexColumn));
            outStream.println();
        }
    }

    private void readTaxa(String taxaFileName, String indexColumn) {

        try {
            TreeImporter importer = null;

            FormatType format = getTreeFileType(new FileReader(taxaFileName));

            if (format == FormatType.NEXUS) {
                importer = new NexusImporter(new FileReader(taxaFileName));
            } else if (format == FormatType.NEWICK) {
                importer = new NewickImporter(new FileReader(taxaFileName), false);
            } else {
                // not a tree file - do nothing...
            }

            if (importer != null) {
                RootedTree tree = (RootedTree) importer.importNextTree();
                taxa = new HashSet<>(getTaxonMap(tree).values());
            } else {
                taxa = readCSV(taxaFileName, indexColumn).keySet();
            }

        } catch (IOException ioe) {
            errorStream.println("Error reading taxon file, " + taxaFileName + ": " + ioe.getMessage());
            System.exit(1);
        } catch (ImportException ie) {
            errorStream.println("Error parsing taxon file, " + taxaFileName + ": " + ie.getMessage());
            System.exit(1);
        }


        if (isVerbose) {
            outStream.println("Read taxa list: " + taxaFileName);
            outStream.println("        Number: " + taxa.size());
            outStream.println();
        }
    }

    final RootedTree readTree(String treeFileName) {
        return readTrees(treeFileName).get(0);
    }

    final List<RootedTree> readTrees(String treeFileName) {
        List<RootedTree> trees = new ArrayList<>();

        if (isVerbose) {
            outStream.println("Reading treefile: " + treeFileName);
        }

        try {
            TreeImporter importer = null;

            FormatType format = getTreeFileType(new FileReader(treeFileName));

            if (format == FormatType.NEXUS) {
                importer = new NexusImporter(new FileReader(treeFileName));
            } else if (format == FormatType.NEWICK) {
                importer = new NewickImporter(new FileReader(treeFileName), false);
            } else {
                errorStream.println("Unrecognised tree format in file, " + treeFileName);
                System.exit(1);
            }

            while (importer.hasTree()) {
                trees.add((RootedTree) importer.importNextTree());
            }

        } catch (IOException ioe) {
            errorStream.println("Error reading tree file, " + treeFileName + ": " + ioe.getMessage());
            System.exit(1);
        } catch (ImportException ie) {
            errorStream.println("Error parsing tree file, " + treeFileName + ": " + ie.getMessage());
            System.exit(1);
        }

        if (isVerbose) {
            if (trees.size() > 1) {
                outStream.println(" Number of trees: " + trees.size());
            }
            outStream.println("  Number of tips: " + trees.get(0).getExternalNodes().size());
            outStream.println();
        }

        return trees;
    }

    final void processTrees(String treeFileName, TreeFunction function) {
        processTrees(treeFileName, null, null, function);
    }

    final void processTrees(String treeFileName, String outputFileName, FormatType outputFormat, TreeFunction function) {

        if (isVerbose) {
            outStream.println("  Reading treefile: " + treeFileName);
        }

        TreeImporter importer = null;
        TreeExporter exporter = null;

        try {

            FormatType format = getTreeFileType(new FileReader(treeFileName));

            if (format == FormatType.NEXUS) {
                importer = new NexusImporter(new FileReader(treeFileName));
            } else if (format == FormatType.NEWICK) {
                importer = new NewickImporter(new FileReader(treeFileName), false);
            } else {
                errorStream.println("Unrecognised tree format in file, " + treeFileName);
                System.exit(1);
            }
        } catch (IOException ioe) {
            errorStream.println("Error reading tree file: " + ioe.getMessage());
            System.exit(1);
        }

        FileWriter writer = null;

        if (outputFileName != null) {
            try {
                if (isVerbose) {
                    outStream.println("  Writing treefile: " + outputFileName);
                }
                writer = new FileWriter(outputFileName);

                switch (outputFormat) {
                    case NEXUS:
                        exporter = new NexusExporter(writer);
                        break;
                    case NEWICK:
                        exporter = new NewickExporter(writer);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown format: " + outputFormat);
                }

            } catch (IOException ioe) {
                errorStream.println("Error writing tree file: " + ioe.getMessage());
                System.exit(1);
            }
        }

        try {
            int count = 0;
            while (importer.hasTree()) {
                RootedTree tree = function.processTree((RootedTree) importer.importNextTree());

                if (exporter != null) {
                    exporter.exportTree(tree);
                }
                count++;
                if (isVerbose && count % 100 == 0) {
                    outStream.println("Number of trees processed: " + count);
                }
            }

            if (exporter != null) {
                exporter.close();
            }

            if (isVerbose) {
                outStream.println("Total trees processed: " + count);
                outStream.println();
            }

        } catch (ImportException ie) {
            errorStream.println("Error parsing tree file, " + treeFileName + ": " + ie.getMessage());
            System.exit(1);
        } catch (IOException ioe) {
            errorStream.println("Error processing tree file: " + ioe.getMessage());
            System.exit(1);
        }

    }



    private FormatType getTreeFileType(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = bufferedReader.readLine();
        while (line != null && line.length() == 0) {
            line = bufferedReader.readLine();
        }

        if (line != null && line.trim().toUpperCase().startsWith("#NEXUS")) {
            return FormatType.NEXUS;
        }
        if (line != null && line.trim().toUpperCase().startsWith("(")) {
            return FormatType.NEWICK;
        }

        return null;
    }

    final Map<Taxon, String> getTaxonMap(RootedTree tree) {
        Map<Taxon, String> taxonMap = new HashMap<>();

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxon.getName();
            if (indexHeader > 0) { // index header indexed from 1
                // if an index header field has been specified then split it out (otherwise use the entire name)
                String[] headers = taxon.getName().split(headerDelimiter);
                if (indexHeader > headers.length) {
                    errorStream.println("Tip name, " + taxon.getName() + ", doesn't have enough fields (index-header = " + indexHeader + ")");
                    System.exit(1);
                }
                index = headers[indexHeader - 1];
            }
            taxonMap.put(taxon, index);
        }

        return taxonMap;
    }


    final Map<String, Node> getTipMap(RootedTree tree) {
        Map<String, Node> tipMap = new HashMap<>();

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxon.getName();
            if (indexHeader > 0) { // index header indexed from 1
                // if an index header field has been specified then split it out (otherwise use the entire name)
                String[] headers = taxon.getName().split(headerDelimiter);
                if (indexHeader > headers.length) {
                    errorStream.println("Tip name, " + taxon.getName() + ", doesn't have enough fields (index-header = " + indexHeader + ")");
                    System.exit(1);
                }
                index = headers[indexHeader - 1];
            }
            tipMap.put(index, tip);
        }

        return tipMap;
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

    static void propagateAttribute(RootedTree tree, Node node, String newAttributeName, String newAttributeValue) {
        propagateAttribute(tree, node, null, null, newAttributeName, newAttributeValue);
    }

    /**
     * Recursively sets an attribute. If oldAttributeName and oldAttributeValue are give then only sets the new attribute
     * if the old attribute exists and has the given value.
     * @param tree
     * @param node
     * @param oldAttributeName
     * @param oldAttributeValue
     * @param newAttributeName
     * @param newAttributeValue
     */
    static void propagateAttribute(RootedTree tree, Node node, String oldAttributeName, Object oldAttributeValue, String newAttributeName, String newAttributeValue) {
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                propagateAttribute(tree, child, oldAttributeName, oldAttributeValue, newAttributeName, newAttributeValue);
            }
        }
        if (oldAttributeName != null) {
            Object value = node.getAttribute(oldAttributeName);
            if (value != null && (oldAttributeValue == null || value.equals(oldAttributeValue))) {
                node.setAttribute(newAttributeName, newAttributeValue);
            }
        } else {
            node.setAttribute(newAttributeName, newAttributeValue);
        }
    }

    /**
     * Annotates the tips of a tree with a set of columns from the metadata table
     * @param tree
     * @param taxonMap
     * @param columnName
     * @param ignoreMissing
     */
    void annotateTips(RootedTree tree,
                      Map<Taxon, String> taxonMap,
                      String columnName,
                      boolean ignoreMissing) {

        for (Node tip : tree.getExternalNodes()) {
            String key = taxonMap.get(tree.getTaxon(tip));
            String value = getTipAnnotation(key, columnName, ignoreMissing);
            if (value != null) {
                tip.setAttribute(columnName, value);
            }
        }
    }

    String getTipAnnotation(String tipIndex, String columnName, boolean ignoreMissing) {
        CSVRecord record = metadata.get(tipIndex);
        if (record != null) {
            if (!record.get(columnName).isEmpty()) {
                return record.get(columnName);
            }
        } else if (!ignoreMissing) {
            errorStream.println("Tip index, " + tipIndex + ", not found in metadata table");
            System.exit(1);
        }

        return null;
    }

    static int countTips(RootedTree tree, Node node) {
        if (tree.isExternal(node)) {
            return 1;
        }

        int count = 0;
        for (Node child : tree.getChildren(node)) {
            count += countTips(tree, child);
        }
        return count;
    }

    static int countTips(RootedTree tree, Node node, String attributeName, Object value) {
        if (tree.isExternal(node)) {
            return value.equals(node.getAttribute(attributeName)) ? 1 : 0;
        }

        int count = 0;
        for (Node child : tree.getChildren(node)) {
            count += countTips(tree, child, attributeName, value);
        }
        return count;
    }

    static Set<Node> collectTips(RootedTree tree, Node node) {
        if (tree.isExternal(node)) {
            return Collections.singleton(node);
        }

        Set<Node> tips = new HashSet<>();
        for (Node child : tree.getChildren(node)) {
            tips.addAll(collectTips(tree, child));
        }
        return tips;
    }

    /**
     * Collects all the taxa subtended by a node into a set - if a child is a subtree then it just adds
     * that label.
     * @param tree
     * @param node
     * @return
     */
    static Set<String> collectContent(RootedTree tree, Node node) {
        if (!tree.isExternal(node)) {
            Set<String> content = new TreeSet<>();

            for (Node child : tree.getChildren(node)) {
                String subtree = (String)child.getAttribute("subtree");
                if (subtree != null) {
                    content.add(subtree);
                } else {
                    content.addAll(collectContent(tree, child));
                }
            }

            return content;
        } else {
            return Collections.singleton(tree.getTaxon(node).getName());
        }
    }

    /**
     * collects all the values for a given attribute in a map with a list of tips nodes for each
     * @param tree
     * @param attributeName
     */
    static Map<Object, Set<Node>> collectTipAttributeValues(RootedTree tree, String attributeName) {
        Map<Object, Set<Node>> attributeValues = new TreeMap<>();
        for (Node tip : tree.getExternalNodes()) {
            Object value = tip.getAttribute(attributeName);
            if (value != null) {
                Set<Node> tips = attributeValues.getOrDefault(value, new HashSet<>());
                tips.add(tip);
            }
        }
        return attributeValues;
    }

    static Map<Object, Integer> getTipAttributes(RootedTree tree, Node node, String attributeName) {
        Set<Node> tips = collectTips(tree, node);
        Map<Object, Integer> attributeCounts = new HashMap<>();
        for (Node tip: tips) {
            String lineage = (String)tip.getAttribute(attributeName);
            if (lineage != null) {
                int count = attributeCounts.getOrDefault(lineage, 0);
                attributeCounts.put(lineage, count + 1);
            }
        }
        Map<Object, Integer> sortedCounts = attributeCounts
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        return sortedCounts;
    }

    static String getMostCommonAttribute(RootedTree tree, Node node, String attributeName) {
        Set<Node> tips = collectTips(tree, node);
        Map<String, Integer> lineageCounts = new HashMap<>();
        for (Node tip: tips) {
            String lineage = (String)tip.getAttribute(attributeName);
            if (lineage != null) {
                int count = lineageCounts.computeIfAbsent(lineage, k -> 0);
                lineageCounts.put(lineage, count + 1);
            }
        }
        Map<String, Integer> sortedCounts = lineageCounts
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        return sortedCounts.keySet().iterator().next();
    }


    /**
     * Takes a set of objects and creates a set of strings using the toString() method
     * @param objectSet
     * @return
     */
    static Collection<String> toString(Collection<Object> objectSet)
    {
        Set<String> strings = new LinkedHashSet<>();
        for (Object o : objectSet) {
            if (o != null) {
                strings.add(o.toString());
            }
        }
        return strings;
    }

    protected Map<String, CSVRecord> readCSV(String fileName, String indexColumn) {
        Map<String, CSVRecord> csv = new HashMap<>();
        try {
            Reader in = new FileReader(fileName);
            CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            if (indexColumn != null) {
                // a particular column is used to index - check it is there for the first record
                // and use it to key the records

                boolean first = true;
                for (CSVRecord record : parser) {
                    if (first) {
                        headerRecord = record;
                        if (record.get(indexColumn) == null) {
                            errorStream.println("Index column, " + indexColumn + " not found in metadata table");
                            System.exit(1);
                        }
                        first = false;
                    }
                    String key = record.get(indexColumn);
                    if (csv.containsKey(key)) {
                        errorStream.println("Duplicate index value, " + key + " in metadata table");
//                        System.exit(1);
                    }
                    csv.put(key, record);
                }
            } else {
                // key the records against the first column
                boolean first = true;
                for (CSVRecord record : parser) {
                    if (first) {
                        headerRecord = record;
                        first = false;
                    }
                    String key = record.get(0);
                    if (csv.containsKey(key)) {
                        errorStream.println("Duplicate index value, " + key + " in metadata table");
//                        System.exit(1);
                    }
                    csv.put(key, record);
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
    void writeTreeFile(RootedTree tree, String fileName, FormatType format) {
        writeTreeFile(Collections.singletonList(tree), fileName, format);
    }

    /**
     * Writes a tree file with a list of trees
     * @param trees
     * @param fileName
     */
    void writeTreeFile(List<RootedTree> trees, String fileName, FormatType format) {
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
            exporter.close();
        } catch (IOException e) {
            errorStream.println("Error writing tree file: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Writes a csv file
     * @param records
     * @param fileName
     */
    void writeMetadataFile(List<CSVRecord> records, String fileName) {
        writeCSVFile(records, fileName);
    }

    /**
     * Writes a csv file
     * @param records
     * @param fileName
     */
    private static void writeCSVFile(List<CSVRecord> records, String fileName) {
        try {
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(fileName)));

            List<String> headerNames = records.get(0).getParser().getHeaderNames();
            writer.println(String.join(",", headerNames));

            for (CSVRecord record : records) {
                boolean first = true;
                for (String value : record) {
                    if (first) {
                        writer.print(value);
                        first = false;
                    } else {
                        writer.print(",");
                        writer.print(value);
                    }
                }
                writer.println();
            }

            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }

    }

    /**
     * Writes a text file
     * @param lines
     * @param fileName
     */
     static void writeTextFile(List<String> lines, String fileName) {
        try {
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(fileName)));

            for (String line : lines) {
                writer.println(line);
            }

            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing text file: " + e.getMessage());
            System.exit(1);
        }

    }

    /**
     * When ever a change in the value of a given attribute occurs at a node, writes out a subtree from that node
     * @param tree
     * @param attributeName
     * @param outputFileStem
     */
    void splitSubtrees(RootedTree tree, String attributeName, Object attributeValue, boolean includeNested,
                       String outputPath, String outputFileStem, boolean labelWithValue, FormatType outputFormat) {

        splitSubtrees(tree, tree.getRootNode(), attributeName, attributeValue,
                null, includeNested, outputPath, outputFileStem, labelWithValue, outputFormat,
                new HashMap<Object, Integer>());
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param parentValue
     * @param outputFileStem
     */
    private void splitSubtrees(RootedTree tree, Node node, String attributeName, Object attributeValue, Object parentValue, boolean includeNested,
                               String outputPath, String outputFileStem, boolean labelWithValue, FormatType outputFormat, Map<Object, Integer> prunedMap) {
        if (!tree.isExternal(node)) {
            boolean wasSplit = false;

            Object value = node.getAttribute(attributeName);
            if (attributeValue.equals(value)) {
                if (!value.equals(parentValue)) {
                    SimpleRootedTree subtree = new SimpleRootedTree();
                    subtree.createNodes(tree, node);

                    String name = (labelWithValue ? "_" + value.toString() : "");
                    Integer count = prunedMap.getOrDefault(value, 0);
                    count += 1;
                    if (count > 1 || !labelWithValue) {
                        name += "_" + count;
                    }
                    prunedMap.put(value, count);

                    String fileName = outputPath + outputFileStem + name + "." + outputFormat.name().toLowerCase();
                    if (isVerbose) {
                        outStream.println("Writing subtree file: " + fileName);
                    }
                    writeTreeFile(subtree, fileName, outputFormat);
                    wasSplit = true;
                }
            }

            if (!wasSplit || includeNested) {
                for (Node child : tree.getChildren(node)) {
                    splitSubtrees(tree, child, attributeName, attributeValue, value, includeNested, outputPath, outputFileStem, labelWithValue, outputFormat, prunedMap);
                }
            }

        }
    }

    protected String checkOutputPath(String outputPath) {
        if (outputPath == null || outputPath == "") {
            return "./";
        }

        Path file = new File(outputPath).toPath();

        if (!Files.isDirectory(file)) {
            errorStream.println("Output path is not a directory: " + outputPath);
            System.exit(1);
        }

        return outputPath.endsWith("/") ? outputPath : outputPath + "/";
    }

    protected String getFileWithExtension(String filenameStem, String[] extensions) {
        for (String extension: extensions) {
            File file = new File(filenameStem + extension);
            if (Files.exists(file.toPath())) {
                return file.toString();
            }
        }

        errorStream.println("No file found with an appropriate extension for filename stem: " + filenameStem);
        System.exit(1);

        return null;
    }





    public static class Pair {
        public Pair(Node node, int count) {
            this.node = node;
            this.count = count;
        }

        Node node;
        int count;
    }

}
