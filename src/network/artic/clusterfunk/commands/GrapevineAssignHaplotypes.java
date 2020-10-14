package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.*;

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

        annotateTips(tree, getTaxonMap(tree), annotationName,ignoreMissing);

        if (isVerbose) {
            outStream.println("Attribute: " + annotationName);
            outStream.println();
        }

        labelInternalNodes(tree, annotationName);

        if (isVerbose) {
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
    private void labelInternalNodes(RootedTree tree, String attributeName) {
        for (Node node : tree.getInternalNodes()) {
            Set<String> haplotypeSet = new HashSet<>();
            for (Node child : tree.getChildren(node)) {
                if (tree.isExternal(child)) {
                    if (tree.getLength(child) < ZERO_BRANCH_THRESHOLD) {
                        haplotypeSet.add( (String)node.getAttribute(attributeName));
                    }
                }
            }

            if (haplotypeSet.size() > 0) {
                String haplotypeHash = haplotypeSet.iterator().next();
                if (haplotypeSet.size() > 1) {
                    errorStream.println("multiple haplotypes on internal node");
                }
                node.setAttribute(attributeName, haplotypeHash);
            }
        }
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param clusterLineageMap
     */
    private void clusterLineages(RootedTree tree, Node node, String lineageName, String parentCluster, String newLineageName, Map<String, String> clusterLineageMap) {
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                clusterLineages(tree, child, lineageName, null, newLineageName, clusterLineageMap);
            }

            Map<String, Integer> lineages = new HashMap<>();
            for (Node child : tree.getChildren(node)) {
                String lineage = (String)child.getAttribute(lineageName);
                if (lineage != null) {
                    int count = lineages.computeIfAbsent(lineage, k -> 0);
                    lineages.put(lineage, count + 1);
                }
            }

            String lineage = null;

            if (lineages.size() > 0) {
                if (lineages.size() > 1) {
                    throw new RuntimeException("more than one child lineage present");
                }
                lineage = lineages.keySet().iterator().next();

                List<Pair> childSizes = new ArrayList<>();
                for (Node child : tree.getChildren(node)) {
                    if (lineage.equals((String)child.getAttribute(lineageName))) {
                        childSizes.add(new Pair(child, countTips(tree, child)));
                    }
                }
                childSizes.sort(Comparator.comparing(k -> -k.count));

                int minSublineageSize = 50;
                int bigSublineageCount = 0;

                int totalSize = 0;
                for (Pair pair : childSizes) {
                    // first give everyone the base lineage designation
                    if (pair.count >= minSublineageSize) {
                        bigSublineageCount += 1;
                    }
                    totalSize += pair.count;

                    pair.node.setAttribute(newLineageName, lineage);
                    propagateAttribute(tree, pair.node, "country_uk_deltran", true, newLineageName, lineage);
                }

                int sublineageSize = 0;
                if (bigSublineageCount > 1) {
                    int sublineageNumber = 1;
                    for (Pair pair : childSizes) {
                        // then give children larger than minSublineageSize a sublineage designation
                        if (pair.count >= minSublineageSize) {
                            String sublineage = lineage + "." + sublineageNumber;
                            pair.node.setAttribute(newLineageName, sublineage);
                            propagateAttribute(tree, pair.node, "country_uk_deltran", true, newLineageName, sublineage);
                            sublineageNumber += 1;
                            sublineageSize += pair.count;

                            if (isVerbose) {
                                outStream.println("Creating sublineage: " + sublineage + " [" + pair.count + " taxa]");
                            }
                        }
                    }
                }
                if (isVerbose) {
                    outStream.println("Creating lineage: " + lineage + " [" + (totalSize - sublineageSize) + " taxa]");
                }
            }


        }
    }


}

