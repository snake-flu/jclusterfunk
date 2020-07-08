package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusExporter;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.*;
import org.apache.commons.cli.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 */
class TreePruner {
    private TreePruner(String treeFileName, String attributeName, String outputPath, String outputFileStem, boolean isVerbose) {

        this.isVerbose = isVerbose;

        RootedTree tree = null;

        try {
            NexusImporter importer = new NexusImporter(new FileReader(treeFileName));
            tree = (RootedTree)importer.importNextTree();
        } catch (ImportException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Map<Object, Set<Node>> attributeValues = new TreeMap<>();
        collectAttributeValues(tree, attributeName, attributeValues);

        if (isVerbose) {
            System.out.println("Read tree: " + treeFileName);
            System.out.println("Taxa: " + tree.getTaxa().size());
            System.out.println("Attribute: " + attributeName);
            System.out.println("Values: " + String.join(", ", toString(attributeValues.keySet())));
        }

        List<Object> keys = new ArrayList<>(attributeValues.keySet());
        keys.sort((Comparator<Object>) (o1, o2) -> {
            return (o1.toString().length() == o2.toString().length() ?
                    o1.toString().compareTo(o2.toString()) :
                    o1.toString().length() - o2.toString().length());
        });

        for (Object value: keys) {
            annotateMonophyleticNodes(tree, "lineage", value, true, "new_lineage");
        }

        writeTree(tree, outputPath + "all_lineages.tree");

        for (Object value: keys) {
            pruneSubtrees(tree, "new_lineage", value, outputPath, outputFileStem);
        }

    }

    /**
     * collects all the values for a given attribute in a map with a list of tips nodes for each
     * @param tree
     * @param attributeName
     * @param attributeValues
     */
    private void collectAttributeValues(RootedTree tree, String attributeName,  Map<Object, Set<Node>> attributeValues) {
        collectAttributeValues(tree, tree.getRootNode(), attributeName, attributeValues);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param attributeValues
     */
    private void collectAttributeValues(RootedTree tree, Node node, String attributeName, Map<Object, Set<Node>> attributeValues) {
        if (tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            Set<Node> tips = attributeValues.computeIfAbsent(value, k -> new HashSet<>());
            tips.add(node);
        }

        for (Node child : tree.getChildren(node)) {
            collectAttributeValues(tree, child, attributeName, attributeValues);
        }
    }

    /**
     * Finds the MRCA for a set of tip nodes and then recursively annotates the subtree
     * @param tree
     * @param attributeName
     * @param attributeValue
     */
    private void annotateMonophyleticNodes(RootedTree tree, String attributeName, Object attributeValue, boolean isHierarchical, String newAttributeName) {
        annotateMonophyleticNodes(tree, tree.getRootNode(), attributeName, attributeValue, isHierarchical, newAttributeName);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param attributeValue
     */
    private boolean annotateMonophyleticNodes(RootedTree tree, Node node, String attributeName, Object attributeValue, boolean isHierarchical, String newAttributeName) {
        boolean isMonophyletic = true;

        if (tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            if (!(attributeValue.equals(value) || (isHierarchical && attributeValue.toString().startsWith(value.toString())))) {
                return false;
            }
        } else {

            for (Node child : tree.getChildren(node)) {
                isMonophyletic = annotateMonophyleticNodes(tree, child, attributeName, attributeValue, isHierarchical, newAttributeName) && isMonophyletic;
            }
        }

        if (isMonophyletic) {
            node.setAttribute(newAttributeName, attributeValue);
        }

        return isMonophyletic;
    }

    /**
     * Performs a parsimony reconstruction of a particular trait
     * @param tree
     * @param attributeName
     */
    private void parsimonyReconstruction(RootedTree tree, String attributeName) {
        parsimonyReconstruction(tree, tree.getRootNode(), attributeName);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @return
     */
    private Set<Object> parsimonyReconstruction(RootedTree tree, Node node, String attributeName) {
        if (tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            return Collections.singleton(value);
        }

        Set<Object> union = null;
        Set<Object> intersection = null;
        for (Node child : tree.getChildren(node)) {
            Set<Object> childSet = parsimonyReconstruction(tree, child, attributeName);
            if (union == null) {
                union = new HashSet<>(childSet);
                intersection = new HashSet<>(childSet);
            } else {
                union.addAll(childSet);
                intersection.retainAll(childSet);
            }
        }

        if (union.size() == 1) {
            node.setAttribute("union", union);
        }
        return union;
    }

    /**
     * When ever a change in the value of a given attribute occurs at a node, writes out a subtree from that node
     * @param tree
     * @param attributeName
     * @param outputFileStem
     */
    private void pruneSubtrees(RootedTree tree, String attributeName, Object attributeValue, String outputPath, String outputFileStem) {
        pruneSubtrees(tree, tree.getRootNode(), attributeName, attributeValue, null, outputPath, outputFileStem);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param parentValue
     * @param outputFileStem
     */
    private void pruneSubtrees(RootedTree tree, Node node, String attributeName, Object attributeValue, Object parentValue, String outputPath, String outputFileStem) {
        if (!tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            if (attributeValue.equals(value)) {
                if (!value.equals(parentValue)) {
                    SimpleRootedTree subtree = new SimpleRootedTree();
                    subtree.createNodes(tree, node);
                    String fileName = outputPath + outputFileStem + "_" + value.toString() + ".nexus";
                    writeTree(subtree, fileName);
                }
            }

            for (Node child : tree.getChildren(node)) {
                pruneSubtrees(tree, child, attributeName, attributeValue, value, outputPath, outputFileStem);
            }

        }
    }

    /**
     * Takes a set of objects and creates a set of strings using the toString() method
     * @param objectSet
     * @return
     */
    public Set<String> toString(Set<Object> objectSet)
    {
        Set<String> strings = new TreeSet<>();
        for (Object o : objectSet) {
            if (o != null) {
                strings.add(o.toString());
            }
        }
        return strings;
    }

    /**
     * Writes a tree
     * @param tree
     * @param fileName
     */
    private void writeTree(RootedTree tree, String fileName) {
        if (isVerbose) {
            System.err.println("Writing subtree file: " + fileName);
        }
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

    private final boolean isVerbose;

    public static void main(String[] args) {

        // create Options object
        Options options = new Options();

        options.addOption("h", "help", false, "display help");
        options.addOption("v", "verbose", false, "write analysis details to stderr");

        options.addOption( Option.builder( "i" )
                .longOpt("input")
                .argName("file")
                .hasArg()
                .required(true)
                .desc( "input tree file" )
                .type(String.class).build());

        options.addOption( Option.builder( "o" )
                .longOpt("output")
                .argName("output_path")
                .hasArg()
                .required(true)
                .desc( "output path" )
                .type(String.class).build());

        options.addOption( Option.builder( "p" )
                .longOpt("prefix")
                .argName("file_prefix")
                .hasArg()
                .required(true)
                .desc( "output tree file prefix" )
                .type(String.class).build());

        options.addOption( Option.builder( "a" )
                .longOpt("attribute")
                .argName("attribute")
                .hasArg()
                .required(true)
                .desc( "the attribute name" )
                .type(String.class).build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException pe) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "treepruner", options, true );
            return;
        }

        if(cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "treepruner", options, true );
            return;
        }

        boolean verbose = cmd.hasOption("v");

        String treeFilename = cmd.getOptionValue("i");
        String outputPath = cmd.getOptionValue("o");
        String outputPrefix = cmd.getOptionValue("p");
        String attribute = cmd.getOptionValue("a");


        if (verbose) {
            System.err.println("input tree file: " + treeFilename);
            System.err.println("    output path: " + outputPath);
            System.err.println("  output prefix: " + outputPrefix);
        }

        // todo add progress indicator callback
        long startTime = System.currentTimeMillis();

        new TreePruner(treeFilename, attribute, outputPath, outputPrefix, verbose);

        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

        System.err.println("Time taken: " + timeTaken + " secs");

    }

}

