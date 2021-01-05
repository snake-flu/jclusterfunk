package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 */
public class Statistics extends Command {
    private final static double GENOME_LENGTH = 29903;

    enum StatisticType {
        INTERNAL_BRANCH_LENGTHS
    }

    public Statistics(final String treeFileName,
                      final String outputFileName,
                      final String[] statistics,
                      final boolean isVerbose) {

        super(null, null, null, 0, null, isVerbose);

        RootedTree tree = readTree(treeFileName);

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

            writer.print("tip_count");
            writer.print("\t");
            writer.print("length");
            writer.print("\t");
            writer.print("changes");
            writer.println();

            for (Node node : tree.getInternalNodes()) {
                if (!tree.isRoot(node)) {
                    writer.print(tree.getExternalNodeCount(node));
                    writer.print("\t");
                    writer.print(tree.getLength(node));
                    writer.print("\t");
                    writer.print((int)(tree.getLength(node) * GENOME_LENGTH));
                    writer.println();
                }
            }
//            writer.print("tree");
//            writer.print("\t");
//            writer.print("tip");
//            writer.print("\t");
//            writer.print("cluster");
//            writer.print("\t");
//            writer.print("tmrca");
//            writer.println();
        } catch (IOException ioe) {
            errorStream.println("Error opening output file: " + ioe.getMessage());
            System.exit(1);
        }

//        final PrintWriter finalWriter = writer;
//
//        processTrees(treeFileName, (tree) -> {
//            return tree;
//        });
//
//        finalWriter.close();
    }


}

