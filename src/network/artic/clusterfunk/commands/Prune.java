package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.util.*;

/**
 *
 */
public class Prune extends Command {
    public Prune(String treeFileName,
                 String taxaFileName,
                 String[] targetTaxa,
                 String metadataFileName,
                 String outputFileName,
                 FormatType outputFormat,
                 String outputMetadataFileName,
                 String indexColumn,
                 int indexHeader,
                 String headerDelimiter,
                 boolean keepTaxa,
                 boolean ignoreMissing,
                 boolean isVerbose) {

        super(metadataFileName, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);
        List<String> targetTaxaList = Arrays.asList(targetTaxa);

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

        // subtree option in JEBL requires the taxa that are to be included
        Set<Taxon> includedTaxa = new HashSet<>();

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxonMap.get(taxon);
            if ((taxa != null && taxa.contains(index) == keepTaxa) || targetTaxaList.contains(index) == keepTaxa) {
                includedTaxa.add(taxon);
            }
        }

        if (isVerbose) {
            outStream.println("   Number of taxa pruned: " + (tree.getExternalNodes().size() - includedTaxa.size()) );
            outStream.println("Number of taxa remaining: " + includedTaxa.size());
            outStream.println();
        }

        if (includedTaxa.size() < 2) {
            errorStream.println("At least 2 taxa must remain in the tree");
            System.exit(1);
        }

        RootedTree outTree = new RootedSubtree(tree, includedTaxa);

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(outTree, outputFileName, outputFormat);

        if (outputMetadataFileName != null) {
            List<CSVRecord> metadataRows = new ArrayList<>();
            for (Taxon taxon : includedTaxa) {
                metadataRows.add(metadata.get(taxonMap.get(taxon)));
            }
            if (isVerbose) {
                outStream.println("Writing metadata file, " + outputMetadataFileName);
                outStream.println();
            }
            writeMetadataFile(metadataRows, outputMetadataFileName);
        }
    }
}

