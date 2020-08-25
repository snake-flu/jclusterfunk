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
public class Graft extends Command {
    enum InsertMode {
        UNIQUE_ONLY,
        ROOTWARD,
        TIPWARD,
        DUPLICATE
    }

    public Graft(String treeFileName,
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
        Set<String> uniqueInsertions = new HashSet<>();

        int totalCount = 0;
        int uniqueCount = 0;
        for (String key : metadata.keySet()) {
            Node tip = tipMap.get(key);

            String taxa = metadata.get(key).get(1);
            String[] insertions = taxa.split(headerDelimiter);

            if (tip != null) {
                List<String> insertionList = Arrays.asList(insertions);
                totalCount += insertionList.size();
                if (insertionList.size() == 1) {
                    uniqueCount += 1;
                }
                insertionMap.put(tree.getTaxon(tip), insertionList);
                uniqueInsertions.addAll(insertionList);
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

        insertTips(outTree, outTree.getRootNode(), uniqueOnly ? InsertMode.UNIQUE_ONLY : InsertMode.DUPLICATE, insertionMap);

        if (isVerbose) {
            outStream.println("      Number of tips added: " + uniqueInsertions.size());
            outStream.println("Tips with unique locations: " + uniqueCount);
            outStream.println("      Final number of tips: " + outTree.getExternalNodes().size());
            outStream.println();
        }


        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(outTree, outputFileName, outputFormat);
    }

    private void insertTips(MutableRootedTree tree, Node node, InsertMode mode, Map<Taxon, List<String>> insertionMap) {
        if (!tree.isExternal(node)) {
            for (Node child: tree.getChildren(node)) {
                insertTips(tree, child, mode, insertionMap);
            }
        } else {
             List<String> insertions = insertionMap.get(tree.getTaxon(node));
             if (insertions != null && (mode != InsertMode.UNIQUE_ONLY || insertions.size() == 1)) {
                 Node parent = tree.getParent(node);

                 if (tree.getLength(node) > 0.0) {
                     // create a new internal node and add the tip as a child
                     List<Node> children = new ArrayList<>();
                     children.add(node);
                     parent = tree.createInternalNode(children);
                     tree.setLength(parent, tree.getLength(node));
                     tree.setLength(node, 0.0);
                 }

                 for (String insertion : insertions) {
                     Node child = tree.createExternalNode(Taxon.getTaxon(insertion));
                     tree.addChild(parent, child);
                     tree.setLength(child, 0.0);
                 }
             }
        }
    }
}

