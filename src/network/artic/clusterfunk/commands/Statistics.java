package network.artic.clusterfunk.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 */
public class Statistics extends Command {

    enum StatisticType {
        JT
    }

    public Statistics(final String treeFileName,
                      final String outputFileName,
                      final String[] statistics,
                      final boolean isVerbose) {

        super(null, null, null, 0, null, isVerbose);

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

            writer.print("tree");
            writer.print("\t");
            writer.print("tip");
            writer.print("\t");
            writer.print("cluster");
            writer.print("\t");
            writer.print("tmrca");
            writer.println();
        } catch (IOException ioe) {
            errorStream.println("Error opening output file: " + ioe.getMessage());
            System.exit(1);
        }

        final PrintWriter finalWriter = writer;

        processTrees(treeFileName, (tree) -> {
            return tree;
        });

        finalWriter.close();
    }


}

