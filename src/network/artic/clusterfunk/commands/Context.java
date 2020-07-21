package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 *
 */
public class Context extends Command {
    public Context(String treeFileName,
                   String taxaFileName,
                   String[] targetTaxa,
                   String metadataFileName,
                   String outputPath,
                   String outputFileStem,
                   FormatType outputFormat,
                   String outputMetadataFileName,
                   String indexColumn,
                   int indexHeader,
                   String headerDelimiter,
                   int maxParentLevel,
                   boolean ignoreMissing,
                   boolean isVerbose) {

        super(metadataFileName, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        List<String> targetTaxaList = (targetTaxa != null ? Arrays.asList(targetTaxa) : Collections.emptyList());

        if (taxa == null && targetTaxaList.size() == 0) {
            throw new IllegalArgumentException("context command requires a taxon list and/or additional target taxa");
        }

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        String path = checkOutputPath(outputPath);

        if (!ignoreMissing && taxa != null) {
            if (taxa != null) {
                for (String key : taxa) {
                    if (!taxonMap.containsValue(key)) {
                        errorStream.println("Taxon, " + key + ", not found in tree");
                        System.exit(1);
                    }
                }
            }

            for (String key : targetTaxaList) {
                if (!taxonMap.containsValue(key)) {
                    errorStream.println("Taxon, " + key + ", not found in tree");
                    System.exit(1);
                }
            }
        }

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxonMap.get(taxon);
            if ((taxa != null && taxa.contains(index)) || targetTaxaList.contains(index)) {
                Node node = tip;
                int parentLevel = 0;
                do {
                    node = tree.getParent(node);
                    parentLevel += 1;
                    node.setAttribute("include", true);
                } while (parentLevel < maxParentLevel && !tree.isRoot(node));
            }
        }

        splitSubtrees(tree, "include", true, path, outputFileStem, false, outputFormat);
    }

}

