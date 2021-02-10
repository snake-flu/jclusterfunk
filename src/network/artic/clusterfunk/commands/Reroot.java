package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Graph;
import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.ReRootedTree;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.RootedTreeUtils;
import network.artic.clusterfunk.FormatType;
import network.artic.clusterfunk.RootType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Reroot extends Command {
    public Reroot(String treeFileName,
           String outputPath,
           FormatType outputFormat,
           int indexHeader,
           String headerDelimiter,
           RootType rootType,
           double rootLocation,
           String[] outgroups,
           boolean isVerbose) {

        super(null, null, null, indexHeader, headerDelimiter, isVerbose);

        if (rootLocation < 0.0 || rootLocation > 1.0) {
            errorStream.println("root-location option should be between 0.0 and 1.0 ");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        RootedTree outTree;

        switch (rootType) {
            case OUTGROUP:
                if (isVerbose) {
                    outStream.println("Outgroup rooting");
                    outStream.println("Outgroup: " + String.join(", ", outgroups));
                    outStream.println();
                }
//                throw new UnsupportedOperationException("Outgroup rooting not implemented yet");

                Set<Node> outgroupTips = new HashSet<>();
                for (String outgroup : outgroups) {
                    Node tip = tree.getNode(Taxon.getTaxon(outgroup));
                    if (tip == null) {
                        errorStream.println("Outgroup, " + outgroup + ", not found in the tree");
                        System.exit(1);
                    }
                    outgroupTips.add(tip);
                }

                Node outgroupNode = RootedTreeUtils.getCommonAncestorNode(tree, outgroupTips);
                Node ingroupNode = tree.getParent(outgroupNode);
                double ingroupBranchLength = tree.getLength(outgroupNode) * rootLocation;
                try {
                    outTree = new ReRootedTree(tree, ingroupNode, outgroupNode, ingroupBranchLength);
                } catch (Graph.NoEdgeException nee) {
                    throw new RuntimeException(nee);
                }
                break;
            case MIDPOINT:
                if (isVerbose) {
                    outStream.println("Midpoint rooting");
                    outStream.println();
                }
//                throw new UnsupportedOperationException("Midpoint rooting not implemented yet");
                outTree =  ReRootedTree.rootTreeAtCenter(tree);
                break;
            default:
                throw new IllegalArgumentException("Unknown reroot type");
        }

        writeTreeFile(outTree, outputPath, outputFormat);
    }

}

