package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Context extends Command {
    public Context(String treeFileName,
                   String taxaFileName,
                   String outputPath,
                   String outputFileStem,
                   FormatType outputFormat,
                   String indexColumn,
                   int indexHeader,
                   String headerDelimiter,
                   int parentLevels,
                   boolean isVerbose) {

        super(null, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxonMap.get(taxon);
            if ((taxa.contains(index))) {
                Node node = tip;
                do {
                    node = tree.getParent(node);

                } while (!tree.isRoot(node));
            }
        }


//        if (isVerbose) {
//            outStream.println("   Number of taxa pruned: " + (tree.getExternalNodes().size() - includedTaxa.size()) );
//            outStream.println("Number of taxa remaining: " + includedTaxa.size());
//            outStream.println();
//        }
//
//        if (includedTaxa.size() < 2) {
//            errorStream.println("At least 2 taxa must remain in the tree");
//            System.exit(1);
//        }
//
//        RootedTree outTree = new RootedSubtree(tree, includedTaxa);
//
//        if (isVerbose) {
//            outStream.println("Writing tree file, " + outputPath + ", in " + outputFormat.name().toLowerCase() + " format");
//            outStream.println();
//        }
//
//        writeTreeFile(outTree, outputPath, outputFormat);
    }
}

