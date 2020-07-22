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


        if (isVerbose) {
            outStream.println("Annotating clusters in tree where " + annotationName + " = " + annotationValue + " as " + clusterName );
            outStream.println();
        }

        processTrees(treeFileName, new TreeFunction() {
            @Override
            public RootedTree processTree(RootedTree tree) {
                annotateClusters(tree, annotationName, annotationValue, clusterName, clusterPrefix);
                return tree;
            }
        });

    }



}

