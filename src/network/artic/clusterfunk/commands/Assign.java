package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.util.Map;

/**
 *
 */
public class Assign extends Command {
    public Assign(String treeFileName,
                  String metadataFileName,
                  FormatType outputFormat,
                  String outputMetadataFileName,
                  String indexColumn,
                  int indexHeader,
                  String headerDelimiter,
                  String lineageName,
                  boolean cleanSublineages,
                  boolean ignoreMissing,
                  boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        annotateTips(tree, taxonMap, metadata, lineageName, ignoreMissing);

    }

    /**
     * Annotates the tips of a tree with a set of columns from the metadata table
     * @param tree
     * @param taxonMap
     * @param metadata
     * @param columnName
     * @param ignoreMissing
     */
    private void annotateTips(RootedTree tree,
                              Map<Taxon, String> taxonMap,
                              Map<String, CSVRecord> metadata,
                              String columnName,
                              boolean ignoreMissing) {

        for (Node tip : tree.getExternalNodes()) {
            String key = taxonMap.get(tree.getTaxon(tip));
            CSVRecord record = metadata.get(key);
            if (record == null) {
                if (!ignoreMissing) {
                    errorStream.println("Tip index, " + key + ", not found in metadata table");
                    System.exit(1);
                }
            } else {
                if (!record.get(columnName).isEmpty()) {
                    tip.setAttribute(columnName, record.get(columnName));
                }
            }
        }
    }


    private boolean assignLineages(RootedTree tree, Node node, String lineageName, String parentLineage,
                                   boolean isHierarchical, String newAttributeName) {
        boolean isMonophyletic = true;

//        if (tree.isExternal(node)) {
//            Object value = node.getAttribute(lineageName);
//            if (value == null || !(parentLineage.equals(value) ||
//                    (isHierarchical && isSublineage(parentLineage.toString(), value.toString())))) {
//                return false;
//            }
//        } else {
//
//            for (Node child : tree.getChildren(node)) {
//                isMonophyletic = assignLineages(tree, child, lineageName, attributeValue, isHierarchical, newAttributeName) && isMonophyletic;
//            }
//        }
//
//        if (isMonophyletic) {
//            node.setAttribute(newAttributeName, attributeValue);
//        }

        return isMonophyletic;
    }

    /**
     * Is lineage 1 a sub-lineage of lineage 2 in the dot notation
     * @param lineage1
     * @param lineage2
     * @return
     */
    private boolean isSublineage(String lineage1, String lineage2) {
        String[] split1 = lineage1.split("\\.");
        String[] split2 = lineage2.split("\\.");

        if (split1.length < split2.length) {
            // lineage1 is actually shorter than lineage2
            return false;
        }
        ;
        for (int i = 0; i < split2.length; i++) {
            if (!split1[i].equals(split2[i])) {
                return false;
            }
        }
        return true;
    }

    private String getParentLineage(String lineage) {
        int index = lineage.lastIndexOf(".");

        if (index == -1) {
            return null;
        }

        return lineage.substring(0, index);
    }

}

