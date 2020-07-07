package network.artic.clusterfunk;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusExporter;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
import org.apache.commons.cli.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
class TreePruner {
    private TreePruner(String treeFileName, String outputFileStem, boolean isVerbose) {

        this.isVerbose = isVerbose;

        RootedTree tree = null;

        try {
            NexusImporter importer = new NexusImporter(new FileReader(treeFileName));
            tree = (RootedTree)importer.importNextTree();
        } catch (ImportException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }


//        MutableRootedTree tree = new MutableRootedTree(referenceTree);
//        System.out.println(jebl.evolution.trees.Utils.toNewick(tree));

        pruneSubtrees(tree, "lineage", outputFileStem);
    }

    private void pruneSubtrees(RootedTree tree, String attributeName, String outputFileStem) {
        pruneSubtrees(tree, tree.getRootNode(), attributeName, null, outputFileStem);
    }

    private void pruneSubtrees(RootedTree tree, Node node, String attributeName, Object parentValue, String outputFileStem) {
        if (!tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            if (value != null) {
                if (!value.equals(parentValue)) {
                    SimpleRootedTree subtree = new SimpleRootedTree();
                    subtree.createNodes(tree, node);
                    writeTree(subtree, outputFileStem, value.toString());
                }
            }

            for (Node child : tree.getChildren(node)) {
                pruneSubtrees(tree, child, attributeName, value, outputFileStem);
            }

        }
    }

    private void annotateNodes(RootedTree tree, String attributeName) {
        annotateNodes(tree, tree.getRootNode(), attributeName);
    }

    private Set<Object> annotateNodes(RootedTree tree, Node node, String attributeName) {
        if (tree.isExternal(node)) {
            Object value = node.getAttribute(attributeName);
            return Collections.singleton(value);
        }

        Set<Object> union = null;
        Set<Object> intersection = null;
        for (Node child : tree.getChildren(node)) {
            Set<Object> childSet = annotateNodes(tree, child, attributeName);
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


    private void writeTree(RootedTree tree, String outputFileStem, String attributeValue) {
        String fileName = outputFileStem + "_" + attributeValue + ".nexus";
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
                .argName("file_prefix")
                .hasArg()
                .required(true)
                .desc( "output tree file prefix" )
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
        String outputFilename = cmd.getOptionValue("o");


        if (verbose) {
            System.err.println("input tree file: " + treeFilename);
            System.err.println("  output prefix: " + outputFilename);
        }

        // todo add progress indicator callback
        long startTime = System.currentTimeMillis();

        new TreePruner(treeFilename, outputFilename, verbose);

        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

        System.err.println("Time taken: " + timeTaken + " secs");

    }

}

