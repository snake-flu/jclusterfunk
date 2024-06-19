package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 */
public class Extract extends Command {
    public Extract(String treeFileName,
                   String metadataFileName,
                   String taxaFileName,
                   String[] targetTaxa,
                   String[] attributeNames,
                   String outputFileName,
                   String indexColumn,
                   int indexHeader,
                   String headerDelimiter,
                   boolean ignoreMissing,
                   boolean isVerbose) {

        super(null, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        if (indexColumn == null) {
            indexColumn = "name";
        }
        List<String> targetTaxaList = new ArrayList<>(targetTaxa != null ? Arrays.asList(targetTaxa) : Collections.emptyList());
        if (taxa != null) {
            targetTaxaList.addAll(taxa);
        }

        if (treeFileName != null) {
            RootedTree tree = readTree(treeFileName);
            Map<Taxon, String> taxonMap = getTaxonMap(tree);

            if (!ignoreMissing && !targetTaxaList.isEmpty()) {
                for (String key : targetTaxaList) {
                    if (!taxonMap.containsValue(key)) {
                        errorStream.println("Taxon, " + key + ", not found in tree");
                        System.exit(1);
                    }
                }
            }

            PrintWriter writer = null;
            try {
                writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName)));

                writer.print(indexColumn + ",");
                writer.println(String.join(",", attributeNames));

                for (Node tip : tree.getExternalNodes()) {
                    String name = taxonMap.get(tree.getTaxon(tip));
                    if (targetTaxaList.isEmpty() || targetTaxaList.contains(name)) {
                        writer.print(name);
                        for (String attributeName : attributeNames) {
                            writer.print(",");
                            Object value = tip.getAttribute(attributeName);
                            if (value != null) {
                                writer.print(value);
                            }
                        }
                        writer.println();
                    }
                }

                writer.close();
            } catch (IOException ioe) {
                errorStream.println("Error opening output file: " + ioe.getMessage());
                System.exit(1);
            }
        }
    }
}

