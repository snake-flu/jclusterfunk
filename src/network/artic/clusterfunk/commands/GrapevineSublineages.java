package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;

import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * Bespoke function to split Grapevine UK_lineages into sublineages
 */
public class GrapevineSublineages extends Command {

    public GrapevineSublineages(String treeFileName,
                                String outputFileName,
                                FormatType outputFormat,
                                final int minSublineageSize,
                                boolean isVerbose) {

        super(isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        String clusterName = "del_lineage";

        Map<Object, Set<Node>> attributeValues = collectTipAttributeValues(tree, clusterName);

        List<Object> keys = new ArrayList<>(attributeValues.keySet());
        keys.sort((o1, o2) -> (o1.toString().length() == o2.toString().length() ?
                o1.toString().compareTo(o2.toString()) :
                o1.toString().length() - o2.toString().length()));

        if (isVerbose) {
            outStream.println("Attribute: " + clusterName);
            outStream.println("Values (" + keys.size() + "): " + String.join(", ", toString(keys)));
            outStream.println();
        }

        Map<String, String> clusterLineageMap = new HashMap<>();

        for (Object key : keys) {
            Set<Node> tips = attributeValues.get(key);
            Map<String, Integer> counts = new HashMap<>();
            for (Node tip: tips) {
                String ukLineage = (String)tip.getAttribute("uk_lineage");
                int count = counts.computeIfAbsent(ukLineage, k -> 0);
                counts.put(ukLineage, count + 1);
            }
            Map<String, Integer> sortedCounts = counts
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(
                            toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

            if (counts.size() > 1) {
                errorStream.println("There is more than one uk_lineage associated with " + key);
                System.exit(1);
            }

            clusterLineageMap.put(key.toString(), counts.keySet().iterator().next());
        }

        labelLineages(tree, clusterName, "uk_lineage", clusterLineageMap);
        clusterLineages(tree, tree.getRootNode(), "uk_lineage", null, "new_uk_lineage", minSublineageSize, clusterLineageMap);

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputFileName, outputFormat);

    }

    /**
     * recursive version
     * @param tree
     * @param clusterLineageMap
     */
    private void labelLineages(RootedTree tree, String clusterName, String lineageName, Map<String, String> clusterLineageMap) {
        for (Node node : tree.getInternalNodes()) {
            String cluster = (String)node.getAttribute(clusterName);
            if (cluster != null) {
                String lineage = clusterLineageMap.get(cluster);
                if (isVerbose) {
                    outStream.println("Found cluster, " + cluster + " - assigning to " + lineage);
                }
                node.setAttribute(lineageName, lineage);
            }
        }
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param clusterLineageMap
     */
    private void clusterLineages(RootedTree tree, Node node, String lineageName, String parentCluster, String newLineageName, int minSublineageSize, Map<String, String> clusterLineageMap) {
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                clusterLineages(tree, child, lineageName, null, newLineageName, minSublineageSize, clusterLineageMap);
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

