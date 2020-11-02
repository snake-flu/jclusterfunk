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
public class GrapevineAssignHaplotypes extends Command {
    private final static double GENOME_LENGTH = 29903;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.01; // 1% of a 1 SNP branch length

    public GrapevineAssignHaplotypes(String treeFileName,
                                     String metadataFileName,
                                     String outputFileName,
                                     FormatType outputFormat,
                                     String indexColumn,
                                     int indexHeader,
                                     String headerDelimiter,
                                     String annotationName,
                                     boolean ignoreMissing,
                                     boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }                                                           

        RootedTree tree = readTree(treeFileName);

        if (annotationName == null) {
            annotationName = "sequence_hash";
        }
        
        annotateTips(tree, getTaxonMap(tree), annotationName, ignoreMissing);
        annotateTips(tree, getTaxonMap(tree), "ambiguity_count", ignoreMissing);

        if (isVerbose) {
            outStream.println("Attribute: " + annotationName);
            outStream.println();
        }

        int labelledCount = labelInternalNodes(tree, annotationName);

        if (isVerbose) {
            outStream.println("Internal nodes: " + tree.getInternalNodes().size());
            outStream.println("Labelled with haplotype: " + labelledCount);
            outStream.println();

            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputFileName, outputFormat);

    }



    /**
     * recursive version
     * @param tree
     * @param attributeName
     */
    private int labelInternalNodes(RootedTree tree, String attributeName) {
        int count = 0;
        for (Node node : tree.getInternalNodes()) {
            Map<String, Integer> haplotypeCounts = new HashMap<>();
            Map<String, Integer> ambiguityCounts = new HashMap<>();
            for (Node child : tree.getChildren(node)) {
                if (tree.isExternal(child)) {
                    if (tree.getLength(child) < ZERO_BRANCH_THRESHOLD) {
                        String hap = (String)child.getAttribute(attributeName);
                        haplotypeCounts.put(hap, haplotypeCounts.getOrDefault(hap, 0) + 1);
                        int amb = Integer.parseInt((String)child.getAttribute("ambiguity_count"));
                        ambiguityCounts.put(hap, amb);
                    }
                }
            }

            // order by frequency
            List<String> haplotypeList = new ArrayList<>();
            haplotypeCounts.entrySet()
                    .stream()
                    // sort by frequencies and break ties with fewest ambiguities
                    .sorted((e1, e2) -> e1.getValue().equals(e2.getValue()) ?
                            ambiguityCounts.get(e1.getKey()) - ambiguityCounts.get(e2.getKey()) :
                            e2.getValue() - e1.getValue())
                    .forEachOrdered(x -> {
                        haplotypeList.add(x.getKey());
                    });

            if (haplotypeCounts.size() > 0) {
                // get the most frequent
                String haplotypeHash = haplotypeList.get(0);
                int ambiguityCount = ambiguityCounts.get(haplotypeHash);

//                if (haplotypeCounts.size() > 1) {
//                    errorStream.println("multiple haplotypes on internal node");
//                }
                node.setAttribute(attributeName, haplotypeHash);
                node.setAttribute("ambiguity_count", ambiguityCount);
                count += 1;
            }
        }
        return count;
    }

}

