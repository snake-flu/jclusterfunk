package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.*;

/**
 *
 */
public class Prune extends Command {
    public Prune(String treeFileName,
          String taxaFileName,
          String outputPath,
          FormatType outputFormat,
          String indexColumn,
          int indexHeader,
          String headerDelimiter,
          boolean pruneNonMatching,
          boolean isVerbose) {

        super(null, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        // subtree option in JEBL requires the taxa that are to be included
        Set<Taxon> includedTaxa = new HashSet<>();

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxonMap.get(taxon);
            if ((taxa.contains(index) == pruneNonMatching)) {
                includedTaxa.add(taxon);
            }

            if (isVerbose) {
                System.out.println("Number of taxa pruned: " + includedTaxa);
                System.out.println("        Number: " + taxa.size());
                System.out.println();
            }
        }

        RootedTree outTree = new RootedSubtree(tree, includedTaxa);

        writeTreeFile(outTree, outputPath, outputFormat);
    }
}

