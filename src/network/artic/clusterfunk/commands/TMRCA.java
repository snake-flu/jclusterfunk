package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.RootedTreeUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Finds the time of most recent common ancestor of a set of taxa
 */
public class TMRCA extends Command {
    public TMRCA(String treeFileName,
                 String taxaFileName,
                 String[] targetTaxa,
                 String outputFileName,
                 String indexColumn,
                 int indexHeader,
                 String headerDelimiter,
                 boolean isStem,
                 boolean ignoreMissing,
                 boolean isVerbose) {

        super(null, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        if (isVerbose) {
            outStream.println("Finding TMRCAs in trees for taxon set" );
            outStream.println();
        }

        final Set<Taxon> taxonSet = new HashSet<>();
        for (String taxonName : taxa) {
            taxonSet.add(Taxon.getTaxon(taxonName));
        }

        if (targetTaxa != null) {
            for (String taxonName : targetTaxa) {
                taxonSet.add(Taxon.getTaxon(taxonName));
            }
        }

        PrintWriter tmpWriter = null;
        if (outputFileName != null) {
            try {
                tmpWriter = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));
            } catch (IOException ioe) {
                errorStream.println("Error opening output file: " + ioe.getMessage());
                System.exit(1);
            }
        }

        final PrintWriter outputMetadataWriter = tmpWriter;

        if (outputMetadataWriter != null) {
            outputMetadataWriter.print("tree");
            outputMetadataWriter.print("\t");
            outputMetadataWriter.print("tmrca");
            outputMetadataWriter.println();

        }

        processTrees(treeFileName, tree -> {
            double tmrca = 0;

            try {
                Set<Node> tips = RootedTreeUtils.getTipsForTaxa(tree, taxonSet);
                tmrca = findTMRCA(tree, tips, isStem);
            } catch (MissingTaxonException mte) {
                errorStream.println("Tip missing: " + mte.getMessage());
                System.exit(1);
            }

            if (outputMetadataWriter != null) {
                        outputMetadataWriter.print(tree.getAttribute("name"));
                        outputMetadataWriter.print("\t");
                        outputMetadataWriter.print(tmrca);
                        outputMetadataWriter.println();
            }

            return null;
        });

        if (outputMetadataWriter != null) {
            outputMetadataWriter.close();
        }

    }

    /**
     * When ever a change in the value of a given attribute occurs at a node, creates a new cluster number and annotates
     * descendents with that cluster number.
     * @param tree
     */
    double findTMRCA(RootedTree tree, Set<Node> tips, boolean isStem) {
        Node mrca = RootedTreeUtils.getCommonAncestorNode(tree, tips);
        if (isStem && tree.getParent(mrca) != null) {
            return tree.getHeight(tree.getParent(mrca));
        }
        return tree.getHeight(mrca);
    }

}

