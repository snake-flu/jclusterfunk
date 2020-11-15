package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 *
 */
public class GrapevineAssignRepresentatives extends Command {
    private final static double GENOME_LENGTH = 29903;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.01; // 1% of a 1 SNP branch length

    public GrapevineAssignRepresentatives(String treeFileName,
                                          String metadataFileName,
                                          String outputFileName,
                                          FormatType outputFormat,
                                          String indexColumn,
                                          int indexHeader,
                                          String headerDelimiter,
                                          boolean ignoreMissing,
                                          boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        annotateTips(tree, getTaxonMap(tree), "ambiguity_count", ignoreMissing);

        int labelledCount = labelInternalNodes(tree);

        if (isVerbose) {
            outStream.println("Internal nodes: " + tree.getInternalNodes().size());
            outStream.println("Labelled with representatives: " + labelledCount);
            outStream.println();

            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputFileName, outputFormat);

    }

    /**
     * recursive version
     * @param tree
     */
    private int labelInternalNodes(RootedTree tree) {
        int count = 0;
        for (Node node : tree.getInternalNodes()) {
            Map<String, Integer> ambiguityCounts = new HashMap<>();
            for (Node child : tree.getChildren(node)) {
                if (tree.isExternal(child)) {
                    if (tree.getLength(child) < ZERO_BRANCH_THRESHOLD) {
                        int amb = 0;
                        try {
                            amb = Integer.parseInt((String) child.getAttribute("ambiguity_count"));
                        } catch (NumberFormatException nfe) {
                            errorStream.println("unable to ambiguity parse value");
                            System.exit(1);
                        }
                        String rep = tree.getTaxon(child).getName();
                        ambiguityCounts.put(rep, amb);
                    }
                }
            }

            if (ambiguityCounts.size() > 0) {
                // order by frequency
                List<String> representitiveList = new ArrayList<>();
                List<String> ambiguityCountList = new ArrayList<>();
                ambiguityCounts.entrySet()
                        .stream()
                        // sort by frequencies and break ties with fewest ambiguities
                        .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                        .forEachOrdered(x -> {
                            representitiveList.add(x.getKey());
                            ambiguityCountList.add(x.getValue().toString());
                        });

                String representitives = String.join("|", representitiveList);
                String ambiguities = String.join("|", ambiguityCountList);

                node.setAttribute("representatives", representitives);
                node.setAttribute("ambiguities", ambiguities);
                count += 1;
            }
        }
        return count;
    }

}

