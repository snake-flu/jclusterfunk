package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.MutableRootedTree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.*;

/**
 *
 */
public class Insert extends Command {
    enum InsertMode {
        UNIQUE_ONLY,
        DUPLICATES
    }

    public Insert(String treeFileName,
                  String metadataFileName,
                  String outputFileName,
                  FormatType outputFormat,
                  String indexColumn,
                  int indexHeader,
                  String headerDelimiter,
                  boolean uniqueOnly,
                  boolean ignoreMissing,
                  boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<String, Node> tipMap = getTipMap(tree);


        // map with tip id and list of taxa to insert
        Map<Taxon, List<String>> insertionMap = new HashMap<>();

        Map<String, List<Taxon>> insertionLocationMap = new HashMap<>();

        for (String key : metadata.keySet()) {
            Node tip = tipMap.get(key);

            String taxa = metadata.get(key).get(1);
            String[] insertions = taxa.split("\\|");

            if (tip != null) {
                List<String> insertionList = Arrays.asList(insertions);

                Taxon locationTaxon = tree.getTaxon(tip);

                // add the list to a map keyed by taxon of insertion location
                insertionMap.put(locationTaxon, insertionList);

                // add all locations to a map keyed by insertion name
                for (String insertion : insertionList) {
                    List<Taxon> locations = insertionLocationMap.getOrDefault(insertion, new ArrayList<>());
                    locations.add(locationTaxon);
                    insertionLocationMap.put(insertion, locations);
                }

            } else {
                if (!ignoreMissing) {
                    errorStream.println("Taxon, " + key + ", not found in tree");
                    System.exit(1);
                } else if (isVerbose) {
                    outStream.println("Taxon, " + key + ", not found in tree");
                }
            }
        }
        MutableRootedTree outTree = new MutableRootedTree(tree);

        insertTips(outTree, outTree.getRootNode(), 0,
                (uniqueOnly ? InsertMode.UNIQUE_ONLY : InsertMode.DUPLICATES), insertionMap, insertionLocationMap);

        if (isVerbose) {
            int uniqueLocationCount = 0;

            for (String key : insertionLocationMap.keySet()) {
                List<Taxon> locations = insertionLocationMap.get(key);
                if (locations.size() == 1) {
                    uniqueLocationCount += 1;
                }
            }

            if (uniqueOnly) {
                outStream.println("Only adding taxa that have a single location");
                outStream.println("Number of taxa added: " + uniqueLocationCount);
                outStream.println("Final number of tips: " + outTree.getExternalNodes().size());
                outStream.println();
            } else {
                outStream.println("           Number of taxa added: " + insertionLocationMap.keySet().size());
                outStream.println("Taxa added to a single location: " + uniqueLocationCount);
                outStream.println("           Final number of tips: " + outTree.getExternalNodes().size());
                outStream.println();
            }
        }


        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(outTree, outputFileName, outputFormat);
    }

    private void insertTips(MutableRootedTree tree, Node node, int depth, InsertMode mode,
                            Map<Taxon, List<String>> insertionMap, Map<String, List<Taxon>> insertionLocationMap) {
        if (!tree.isExternal(node)) {
            for (Node child: tree.getChildren(node)) {
                insertTips(tree, child, depth + 1, mode, insertionMap, insertionLocationMap);
            }
        } else {
            Taxon locationTaxon = tree.getTaxon(node);
            List<String> insertions = insertionMap.get(locationTaxon);

            if (insertions != null) {
                List<Taxon> taxaToInsert = new ArrayList<>();

                for (String insertion : insertions) {
                    List<Taxon> insertionLocations = insertionLocationMap.get(insertion);
                    if (insertionLocations.size() > 1) {
                        if (mode != InsertMode.UNIQUE_ONLY) {
                            int index = insertionLocations.indexOf(locationTaxon);
                            assert (index >= 0);
                            taxaToInsert.add(Taxon.getTaxon(insertion + "|amb_" + (index + 1)));
                        }
                    } else {
                        taxaToInsert.add(Taxon.getTaxon(insertion));
                    }
                }

                if (taxaToInsert.size() > 0) {
                    Node parent = tree.getParent(node);

                    if (tree.getLength(node) > 0.0) {
                        Node root = tree.getRootNode();

                        tree.removeChild(node, parent);

                        // create a new internal node and add the tip as a child
                        Node newParent = tree.createInternalNode(Collections.singletonList(node));
                        tree.setLength(newParent, tree.getLength(node));
                        tree.setLength(node, 0.0);

                        tree.addChild(newParent, parent);

                        parent = newParent;

                        // createInternalNode() will have changed the root so set it back
                        tree.setRoot(root);
                    }

                    for (Taxon insertion : taxaToInsert) {
                        Node child = tree.createExternalNode(insertion);
                        tree.addChild(child, parent);
                        tree.setLength(child, 0.0);
                    }
                }
            }
        }
    }
}


