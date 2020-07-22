package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Cluster extends Command {
    public Cluster(String treeFileName,
                   String outputPath,
                   FormatType outputFormat,
                   String annotationName,
                   String annotationValue,
                   String clusterName,
                   String clusterPrefix,
                   boolean isVerbose) {

        super(null, null, null, 0, null, isVerbose);

        RootedTree tree = readTree(treeFileName);

        if (isVerbose) {
            outStream.println("Annotating clusters in tree where " + annotationName + " = " + annotationValue + " as " + clusterName );
            outStream.println();
        }
        annotateClusters(tree, annotationName, annotationValue, clusterName, clusterPrefix);

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputPath + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputPath, outputFormat);

    }



}

