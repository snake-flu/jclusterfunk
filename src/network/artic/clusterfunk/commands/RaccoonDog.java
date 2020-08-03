package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.SimpleRootedTree;
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
                      final int maxChildLevel,
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

//        clearInternalAttributes(tree);
        Map<String, String> ukLineageMap = new HashMap<>();

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
            if (isVerbose) {
                StringBuilder sb = new StringBuilder();
                sb.append(key).append(": ");
                for (String ukLineage: sortedCounts.keySet()) {
                    sb.append(ukLineage).append(" (").append(sortedCounts.get(ukLineage)).append(") ");
                }
                outStream.println(sb.toString());
                outStream.println();
            }

        }

//        for (Object value: keys) {
//            collapseSubtrees(tree, attributeName, value);
//        }

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


    private void clusterLineages(RootedTree tree, String attributeName, Object attributeValue) {
        clusterLineages(tree, tree.getRootNode(), attributeName, attributeValue, null);
    }

    /**
     * recursive version
     * @param tree
     * @param node
     * @param attributeName
     * @param parentValue
     */
    private void clusterLineages(RootedTree tree, Node node, String attributeName, Object attributeValue, Object parentValue) {
        if (!tree.isExternal(node)) {
            Object value = node.getAttribute("del_introduction");
            if (attributeValue.equals(value) && !value.equals(parentValue)) {
                node.setAttribute("!collapse", "{\"collapsed\",1.7E-4}");
            }

            for (Node child : tree.getChildren(node)) {
                clusterLineages(tree, child, attributeName, attributeValue, value);
            }

        }
    }


}

