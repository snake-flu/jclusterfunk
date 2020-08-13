package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import static java.util.stream.Collectors.*;

import java.util.*;

/**
 *
 */
public class RaccoonDog extends Command {

    public RaccoonDog(String treeFileName,
                      String metadataFileName,
                      String outputFileName,
                      FormatType outputFormat,
                      String outputMetadataFileName,
                      String indexColumn,
                      int indexHeader,
                      String headerDelimiter,
                      String annotationName,
                      String annotationValue,
                      String clusterName,
                      String clusterPrefix,
                      final int minLineageSize,
                      boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        if (outputFormat != FormatType.NEXUS) {
            errorStream.println("Annotations are only compatible with NEXUS output format");
            System.exit(1);
        }

        RootedTree tree = readTree(treeFileName);

        String attributeName = "del_introduction";

        Map<Object, Set<Node>> attributeValues = collectTipAttributeValues(tree, attributeName);

        List<Object> keys = new ArrayList<>(attributeValues.keySet());
        keys.sort((o1, o2) -> (o1.toString().length() == o2.toString().length() ?
                o1.toString().compareTo(o2.toString()) :
                o1.toString().length() - o2.toString().length()));

        if (isVerbose) {
            outStream.println("Attribute: " + attributeName);
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

        labelLineages(tree, tree.getRootNode(), "del_introduction", null, "uk_lineage", clusterLineageMap);

        numberSubLineages(tree, tree.getRootNode(), "uk_lineage", null, "new_uk_lineage", minLineageSize);

//        clearInternalAttributes(tree);



        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(tree, outputFileName, outputFormat);

    }

    /**
     * collects all the values for a given attribute in a map with a list of tips nodes for each
     * @param tree
     * @param attributeName
     */
    private Map<Object, Set<Node>> collectTipAttributeValues(RootedTree tree, String attributeName) {
        Map<Object, Set<Node>> attributeValues = new TreeMap<>();
        for (Node tip : tree.getExternalNodes()) {
            Object value = tip.getAttribute(attributeName);
            if (value != null) {
                Set<Node> tips = attributeValues.computeIfAbsent(value, k -> new HashSet<>());
                tips.add(tip);
            }
        }
        return attributeValues;
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param clusterLineageMap
     */
    private void labelLineages(RootedTree tree, Node node, String clusterName, String parentCluster, String lineageName, Map<String, String> clusterLineageMap) {
        if (!tree.isExternal(node)) {
            String cluster = (String)node.getAttribute(clusterName);
            if (cluster != null && !cluster.equals(parentCluster)) {
                Object lineage = clusterLineageMap.get(cluster);
                node.setAttribute(lineageName, lineage.toString());
            }

            for (Node child : tree.getChildren(node)) {
                labelLineages(tree, child, clusterName, cluster, lineageName, clusterLineageMap);
            }

        }
    }

    /**
     * recursive version
     * @param tree
     * @param node
     */
    private void numberSubLineages(RootedTree tree, Node node, String lineageName, Object parentLineage, String newLineageName, int minSublineageSize) {
        if (!tree.isExternal(node)) {
            String lineage = (String)node.getAttribute(lineageName);
            if (lineage != null && !lineage.equals(parentLineage)) {
                // entered a new lineage
                if (lineage.equals("UK1286")) {
                    System.out.println("eek");
                }

                List<Pair> childSizes = new ArrayList<>();
                for (Node child : tree.getChildren(node)) {
                    String childLineage = (String)child.getAttribute(lineageName);
                    if (childLineage != null && childLineage.equals(lineage)) {
                        childSizes.add(new Pair(child, countTips(tree, child)));
                    }
                }
                childSizes.sort(Comparator.comparing(k -> -k.count));

                int sublineageNumber = 1;
                for (Pair pair : childSizes) {
                    if (pair.count >= minSublineageSize) {
                        String sublineage = lineage + "." + sublineageNumber;
                        pair.node.setAttribute(newLineageName, sublineage);
                        propagateAttribute(tree, pair.node, lineageName, lineage, newLineageName, sublineage);
                        sublineageNumber += 1;
                    } else {
                        pair.node.setAttribute(newLineageName, lineage);
                        propagateAttribute(tree, pair.node, lineageName, lineage, newLineageName, lineage);
                    }
                }
            }

            for (Node child : tree.getChildren(node)) {
                numberSubLineages(tree, child, lineageName, lineage, newLineageName, minSublineageSize);
            }

        }
    }

    private void propagateAttribute(RootedTree tree, Node node, String oldAttributeName, String oldAttributeValue, String newAttributeName, String newAttributeValue) {
        if (!tree.isExternal(node)) {
            for (Node child : tree.getChildren(node)) {
                propagateAttribute(tree, child, oldAttributeName, oldAttributeValue, newAttributeName, newAttributeValue);
            }
        }
        Object value = node.getAttribute(oldAttributeName);
        if (value != null && value.equals(oldAttributeValue)) {
            node.setAttribute(newAttributeName, newAttributeValue);
        }

    }

    private int countTips(RootedTree tree, Node node) {
        if (tree.isExternal(node)) {
            return 1;
        }

        int count = 0;
        for (Node child : tree.getChildren(node)) {
            count += countTips(tree, child);
        }
        return count;
    }

    private Set<Node> collectTips(RootedTree tree, Node node) {
        if (tree.isExternal(node)) {
            return Collections.singleton(node);
        }

        Set<Node> tips = new HashSet<>();
        for (Node child : tree.getChildren(node)) {
            tips.addAll(collectTips(tree, child));
        }
        return tips;
    }

    private String getMostCommonAttribute(RootedTree tree, Node node, String attributeName) {
        Set<Node> tips = collectTips(tree, node);
        Map<String, Integer> lineageCounts = new HashMap<>();
        for (Node tip: tips) {
            String lineage = (String)tip.getAttribute(attributeName);
            if (lineage != null) {
                int count = lineageCounts.computeIfAbsent(lineage, k -> 0);
                lineageCounts.put(lineage, count + 1);
            }
        }
        Map<String, Integer> sortedCounts = lineageCounts
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        return sortedCounts.keySet().iterator().next();
    }

    public class Pair {
        public Pair(Node node, int count) {
            this.node = node;
            this.count = count;
        }

        Node node;
        int count;
    }

}

