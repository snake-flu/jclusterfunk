package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Context extends Command {
    public Context(String treeFileName,
                   String taxaFileName,
                   List<String> targetTaxa,
                   String metadataFileName,
                   String outputPath,
                   String outputFileStem,
                   FormatType outputFormat,
                   String indexColumn,
                   int indexHeader,
                   String headerDelimiter,
                   int maxParentLevel,
                   boolean ignoreMissing,
                   boolean isVerbose) {

        super(metadataFileName, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        if (taxa == null && targetTaxa.size() == 0) {
            throw new IllegalArgumentException("context command requires a taxon list and/or additional target taxa");
        }

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        if (!ignoreMissing && taxa != null) {
            if (taxa != null) {
                for (String key : taxa) {
                    if (!taxonMap.containsValue(key)) {
                        errorStream.println("Taxon, " + key + ", not found in tree");
                        System.exit(1);
                    }
                }
            }

            for (String key : targetTaxa) {
                if (!taxonMap.containsValue(key)) {
                    errorStream.println("Taxon, " + key + ", not found in tree");
                    System.exit(1);
                }
            }
        }

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxonMap.get(taxon);
            if ((taxa != null && taxa.contains(index)) || targetTaxa.contains(index)) {
                Node node = tip;
                int parentLevel = 0;
                do {
                    node = tree.getParent(node);
                    parentLevel += 1;
                    node.setAttribute("include", true);
                } while (parentLevel < maxParentLevel && !tree.isRoot(node));
            }
        }

        splitSubtrees(tree, "include", true, outputPath, outputFileStem, outputFormat);

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

