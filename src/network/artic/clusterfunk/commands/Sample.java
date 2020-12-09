package network.artic.clusterfunk.commands;

import com.sun.org.apache.xpath.internal.operations.Bool;
import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.MutableRootedTree;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.util.*;

/**
 *
 */
public class Sample extends Command {
    public Sample(String treeFileName,
                  String metadataFileName,
                  String outputFileName,
                  FormatType outputFormat,
                  String outputMetadataFileName,
                  String indexColumn,
                  int indexHeader,
                  String headerDelimiter,
                  int maxTaxa,
                  String primaryAttribute,
                  String secondaryAttribute,
                  boolean ignoreMissing,
                  boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);
        MutableRootedTree sampledTree = new MutableRootedTree(tree);

        Map<Taxon, String> taxonMap = getTaxonMap(sampledTree);

        String[] annotationColumns = new String[] { "sample_date", "epi_week", "country", "adm1", "location" };

        for (String columnName: annotationColumns) {
            annotateTips(sampledTree, taxonMap, columnName, ignoreMissing);
        }

        collapseByAttribute(sampledTree, sampledTree.getRootNode(), "country");

        pruneCollapsedSubtrees(sampledTree, sampledTree.getRootNode(), "country");

//        if (!ignoreMissing && taxa != null) {
//            if (taxa != null) {
//                for (String key : taxa) {
//                    if (!taxonMap.containsValue(key)) {
//                        errorStream.println("Taxon, " + key + ", not found in tree");
//                        System.exit(1);
//                    }
//                }
//            }
//
//            for (String key : targetTaxaList) {
//                if (!taxonMap.containsValue(key)) {
//                    errorStream.println("Taxon, " + key + ", not found in tree");
//                    System.exit(1);
//                }
//            }
//        }
//
//        // subtree option in JEBL requires the taxa that are to be included
//        Set<Taxon> includedTaxa = new HashSet<>();
//
//        for (Node tip : tree.getExternalNodes()) {
//            Taxon taxon = tree.getTaxon(tip);
//            String index = taxonMap.get(taxon);
//            if ((taxa != null && taxa.contains(index) == keepTaxa) || targetTaxaList.contains(index) == keepTaxa) {
//                includedTaxa.add(taxon);
//            }
//        }

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
        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(sampledTree, outputFileName, outputFormat);

//        if (outputMetadataFileName != null) {
//            List<CSVRecord> metadataRows = new ArrayList<>();
//            for (Taxon taxon : includedTaxa) {
//                metadataRows.add(metadata.get(taxonMap.get(taxon)));
//            }
//            if (isVerbose) {
//                outStream.println("Writing metadata file, " + outputMetadataFileName);
//                outStream.println();
//            }
//            writeMetadataFile(metadataRows, outputMetadataFileName);
//        }
    }

    private void sampleDiversity(RootedTree tree, Node node, double divergence, int depth) {
        double length = tree.getLength(node);
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                sampleDiversity(tree, child, divergence + length, depth + 1);
            }
        } else {

        }
    }

    private Set<String> collapseByAttribute(RootedTree tree, Node node, String attributeName) {
        if (!tree.isExternal(node)) {
            Set<String> attributes = new HashSet<>();
            for (Node child : tree.getChildren(node)) {
                attributes.addAll(collapseByAttribute(tree, child, attributeName));
            }
            if (attributes.size() == 1) {
                node.setAttribute("collapse", true);
                node.setAttribute(attributeName, attributes.iterator().next());
            }
            return attributes;
        } else {
            return Collections.singleton((String)node.getAttribute(attributeName));
        }
    }

    private void pruneCollapsedSubtrees(MutableRootedTree tree, Node node, String attributeName) {
        if (!tree.isExternal(node)) {
            if (Boolean.TRUE.equals(node.getAttribute("collapse"))) {
                String name = node.getAttribute(attributeName) + "[" + tree.getExternalNodeCount(node) + "]";
                Node parent = tree.getParent(node);
                tree.removeChild(node, parent);
                tree.addChild(tree.createExternalNode(Taxon.getTaxon(name)), parent);
            } else {

                for (Node child : tree.getChildren(node)) {
                    pruneCollapsedSubtrees(tree, child, attributeName);
                }
            }
        }
    }
}

