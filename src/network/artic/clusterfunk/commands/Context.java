package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.RootedTreeUtils;
import jebl.evolution.trees.SimpleRootedTree;
import network.artic.clusterfunk.FormatType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Extracts subtrees that are close ancestors, siblings or children of a set of tip.
 */
public class Context extends Command {
    private final static double GENOME_LENGTH = 29903;
    private final static double EVOLUTIONARY_RATE = 0.001;
    private final static double ZERO_BRANCH_THRESHOLD = (1.0 / GENOME_LENGTH) * 0.01; // 1% of a 1 SNP branch length

    public Context(String treeFileName,
                   String taxaFileName,
                   String[] targetTaxa,
                   String metadataFileName,
                   String outputPath,
                   String outputFileStem,
                   FormatType outputFormat,
                   boolean outputTaxa,
                   String indexColumn,
                   int indexHeader,
                   String headerDelimiter,
                   boolean mrca,
                   int maxParentLevel,
                   int maxChildLevel,
                   int maxSiblingCount,
                   String collapseBy,
                   int tipBudget,
                   boolean ignoreMissing,
                   boolean isVerbose) {

        super(metadataFileName, taxaFileName, indexColumn, indexHeader, headerDelimiter, isVerbose);

        List<String> targetTaxaList = (targetTaxa != null ? Arrays.asList(targetTaxa) : Collections.emptyList());

        if (taxa == null && targetTaxaList.size() == 0) {
            throw new IllegalArgumentException("context command requires a taxon list and/or additional target taxa");
        }

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        if (collapseBy != null) {
            annotateTips(tree, taxonMap, collapseBy, ignoreMissing);
        }

        String path = checkOutputPath(outputPath);

        if (!ignoreMissing && taxa != null) {
                for (String key : taxa) {
                    if (!taxonMap.containsValue(key)) {
                        errorStream.println("Taxon, " + key + ", not found in tree");
                        System.exit(1);
                    }
                }

            for (String key : targetTaxaList) {
                if (!taxonMap.containsValue(key)) {
                    errorStream.println("Taxon, " + key + ", not found in tree");
                    System.exit(1);
                }
            }
        }

        Set<Node> targetTips = new HashSet<>();

        for (Node tip : tree.getExternalNodes()) {
            Taxon taxon = tree.getTaxon(tip);
            String index = taxonMap.get(taxon);
            if ((taxa != null && taxa.contains(index)) || targetTaxaList.contains(index)) {
                targetTips.add(tip);
            }
        }

        Map<Node, Subtree> subtreeMap = new HashMap<>();

        if (!mrca) {
            annotateContext(tree, targetTips, maxParentLevel);
        } else {
            Node node = RootedTreeUtils.getCommonAncestorNode(tree, targetTips);
            node.setAttribute("include", true);
        }

        collectSubtrees(tree, tree.getRootNode(), false, subtreeMap);

        for (Node node : subtreeMap.keySet()) {
            collapseSubtrees(tree, node, 0, maxChildLevel, tipBudget);
        }

        Map<String, Set<String>> collapsedNodeMap = new HashMap<>();

        createSubtrees(tree, subtreeMap, maxSiblingCount, collapseBy, collapsedNodeMap);

        writeSubtrees(subtreeMap, path, outputFileStem, outputFormat, outputTaxa);

        writeCollapsedNodes(collapsedNodeMap, path, outputFileStem);
    }

    private void annotateContext(RootedTree tree, Set<Node> targetTips, int maxParentLevel) {
        for (Node tip : tree.getExternalNodes()) {
            if (targetTips.contains(tip)) {
                Node node = tip;
                int parentLevel = 0;
                do {
                    node = tree.getParent(node);
                    parentLevel += 1;
                    node.setAttribute("include", true);
                } while (maxParentLevel > 0 && parentLevel < maxParentLevel && !tree.isRoot(node));
            }
        }
    }

    /**
     * Finds the nodes which transition from not included to included as the root of a subtree.
     * @param tree
     * @param node
     * @param parentIncluded
     * @param subtreeMap
     */
    private void collectSubtrees(RootedTree tree, Node node, boolean parentIncluded, Map<Node, Subtree> subtreeMap) {
        if (!tree.isExternal(node)) {
            boolean included = node.getAttribute("include") == Boolean.TRUE;
            if (!parentIncluded && included) {
                String name = "subtree_" + (subtreeMap.size() + 1);
                node.setAttribute("subtree", name);
                subtreeMap.put(node, new Subtree(node, name));
            }
            for (Node child : tree.getChildren(node)) {
                collectSubtrees(tree, child, included, subtreeMap);
            }
        }
    }

    private void collapseSubtrees(RootedTree tree, Node node, int childLevel, int maxChildLevel, int maxTreeSize) {
        if (!tree.isExternal(node)) {
            if (maxChildLevel > 0 && childLevel > maxChildLevel) {
                Set<String> content = new TreeSet<>();
                for (Node child : tree.getChildren(node)) {
                    content.addAll(collectContent(tree, child));
                }
                // collapse the node
                node.setAttribute("content", content);
            } else {
                for (Node child : tree.getChildren(node)) {
                    if (child.getAttribute("include") == Boolean.TRUE) {
                        // this child has a target tip in it so should not be collapsed - reset the child level count
                        collapseSubtrees(tree, child, 0, maxChildLevel, maxTreeSize);
                    } else {
                        collapseSubtrees(tree, child, childLevel + 1, maxChildLevel, maxTreeSize);
                    }
                }
            }
        }
    }

    /**
     * When ever a change in the value of a given attribute occurs at a node, writes out a subtree from that node
     */
    void createSubtrees(RootedTree tree, Map<Node, Subtree> subtreeMap, int maxPolytomySize, String collapseBy, Map<String, Set<String>> collapsedNodeMap) {

        for (Node key : subtreeMap.keySet()) {
            Subtree subtree = subtreeMap.get(key);

            SimpleRootedTree newTree = new SimpleRootedTree();
            createNodes(tree, subtree.root, newTree, maxPolytomySize, collapseBy, collapsedNodeMap);
            subtree.tree = newTree;
        }

    }

    /**
     * Clones the entire tree structure from the given RootedTree.
     * @param tree
     * @param node
     * @return
     */
    private Node createNodes(RootedTree tree, Node node, SimpleRootedTree newTree, int maxPolytomySize, String collapseBy, Map<String, Set<String>> collapsedNodeMap) {

        Node newNode;
        if (tree.isExternal(node)) {
            newNode = newTree.createExternalNode(tree.getTaxon(node));
        } else {
            List<Node> children = new ArrayList<Node>();

            boolean collapsePolytomy = maxPolytomySize > 1 && tree.getChildren(node).size() > maxPolytomySize;

            Set<String> collapsedContentSet = new TreeSet<String>();

            Map<String, Set<String>> collapseByValues = new HashMap<>();
            if (collapseBy != null) {
                for (Node child : tree.getChildren(node)) {
                    if (tree.isExternal(child) && tree.getLength(child) < ZERO_BRANCH_THRESHOLD) {
                        String collapseByValue = (String)child.getAttribute(collapseBy);
                        if (collapseByValue != null) {
                            Set<String> contents = collapseByValues.getOrDefault(collapseByValue, new HashSet<>());
                            contents.add(tree.getTaxon(child).getName());
                            collapseByValues.put(collapseByValue, contents);
                        }
                    }
                }
            }

            Set<String> alreadyCollapsed = new HashSet<>();

            for (Node child : tree.getChildren(node)) {
                String subtree = (String) child.getAttribute("subtree");
                String collapsedLabel = "collapsed_" + (collapsedNodeMap.size() + 1);
                Set<String> contentSet = (Set<String>) child.getAttribute("content");
                boolean include = child.getAttribute("include") == Boolean.TRUE;
                String collapseByValue = collapseBy != null ? (String) child.getAttribute(collapseBy) : null;

                boolean hide = false;

                if (contentSet == null && collapseByValue != null) {
                    // alreadyCollapsed contains the collapseByValues that have already been added
                    if (!alreadyCollapsed.contains(collapseByValue)) {
                        Set<String> contents = collapseByValues.get(collapseByValue);
                        if (contents != null && contents.size() > 1 && contents.contains(tree.getTaxon(child).getName())) {
                            contentSet = contents;
                            collapsedLabel += "|" + collapseByValue + "-" + contentSet.size();
                            alreadyCollapsed.add(collapseByValue);
                        }
                    } else {
                        // if already added then just hide this tip
                        hide = true;
                    }
                }

                if (!hide) {
                    if (subtree != null) {
                        // is the root of a subtree - replace with a tip labelled as the subtree
                        Node newChild = newTree.createExternalNode(Taxon.getTaxon(subtree));
                        children.add(newChild);
                        newTree.setHeight(newChild, tree.getHeight(child));
                    } else if (contentSet != null) {
                        if (collapsePolytomy) {
                            collapsedContentSet.addAll(contentSet);
                        } else {
                            // this child has been collapsed so replace it with a content set
                            collapsedNodeMap.put(collapsedLabel, contentSet);
                            Node newChild = newTree.createExternalNode(Taxon.getTaxon(collapsedLabel));
                            children.add(newChild);
                            newTree.setHeight(newChild, tree.getHeight(child));
                        }
                    } else {
                        if (include || !collapsePolytomy) {
                            children.add(createNodes(tree, child, newTree, maxPolytomySize, collapseBy, collapsedNodeMap));
                        } else {
                            collapsedContentSet.addAll(collectContent(tree, child));
                        }
                    }
                }
            }
            
            if (collapsedContentSet.size() > 0) {
                String label = "collapsed_" + (collapsedNodeMap.size() + 1);
                collapsedNodeMap.put(label, collapsedContentSet);
                Node newChild = newTree.createExternalNode(Taxon.getTaxon(label));
                children.add(newChild);
                newTree.setHeight(newChild, tree.getHeight(node));
            }

            newNode = newTree.createInternalNode(children);
        }

        for( Map.Entry<String, Object> e : node.getAttributeMap().entrySet() ) {
            newNode.setAttribute(e.getKey(), e.getValue());
        }

        newTree.setHeight(newNode, tree.getHeight(node));

        return newNode;
    }

    /**
     * Write all the subtrees...
     */
    void writeSubtrees(Map<Node, Subtree> subtreeMap, String outputPath, String outputFileStem, FormatType outputFormat,
                       boolean outputTaxa) {


        for (Node key : subtreeMap.keySet()) {
            Subtree subtree = subtreeMap.get(key);

            String fileName = outputPath + outputFileStem + subtree.name + "." + outputFormat.name().toLowerCase();
            if (isVerbose) {
                outStream.println("Writing subtree file: " + fileName);
            }

            writeTreeFile(subtree.tree, fileName, outputFormat);

            if (outputTaxa) {
                List<String> taxa = new ArrayList<>();

                for (Taxon taxon : subtree.tree.getTaxa()) {
                    taxa.add(taxon.getName());
                }
                String metadataFileName = outputPath + outputFileStem + subtree.name + ".csv";
                writeTextFile(taxa, metadataFileName);
            }
        }
    }

    /**
     * Write a list of collapsed nodes and their contents
     */
    void writeCollapsedNodes(Map<String, Set<String>> collapsedNodeMap,  String outputPath, String outputFileStem) {


        String fileName = outputPath + outputFileStem + "collapsed_nodes.csv";
        try {
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(fileName)));

            writer.println("name,count,content");


            for (String collapsedNode : collapsedNodeMap.keySet()) {
                Set<String> content = collapsedNodeMap.get(collapsedNode);
                writer.print(collapsedNode);
                writer.print(",");
                writer.print(content.size());
                writer.print(",[");
                writer.print(String.join(" ", content));
                writer.println("]");
            }

            writer.close();
        } catch (IOException e) {
            errorStream.println("Error writing metadata file: " + e.getMessage());
            System.exit(1);
        }

    }

    private static class Subtree {
        public Subtree(Node root, String name) {
            this.root = root;
            this.name = name;
        }

        Node root;
        String name;
        SimpleRootedTree tree;
    }
}

