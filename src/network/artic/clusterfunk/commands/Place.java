package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.sequences.NucleotideState;
import jebl.evolution.sequences.Nucleotides;
import jebl.evolution.sequences.State;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.MutableRootedTree;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.util.*;

/**
 *
 */
public class Place extends Command {

    private final static boolean DEBUG = false;

    public Place(String treeFileName,
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

        List<Branch> branches = new ArrayList<>();

        for (String name : metadata.keySet()) {
            CSVRecord record = metadata.get(name);
            if (!tipMap.containsKey(name)) {
                branches.add(new Branch(name, record.get("differences"), record.get("ambiguities")));
            } else {
                if (isVerbose) {
                    outStream.println("Tip, " + name + ", is already in the tree, skipping");
                }
            }
        }

        if (isVerbose) {
            outStream.println("New tips to place: " + branches.size());
            outStream.println();
        }

        MutableRootedTree outTree = new MutableRootedTree(tree);

        if (isVerbose) {
            outStream.println("Writing tree file, " + outputFileName + ", in " + outputFormat.name().toLowerCase() + " format");
            outStream.println();
        }

        writeTreeFile(outTree, outputFileName, outputFormat);
    }

    /**
     * Take a list of substitutions for each taxon and reconstruct them onto the tree, annotating the
     * nodes.
     * @param tree
     * @param node
     * @param branchMap
     * @return
     */
    private Set<Integer> annotateSubstitutions(RootedTree tree, Node node, Map<String, Branch> branchMap) {
        Set<Integer> substitutions;

        if (!tree.isExternal(node)) {
            substitutions = new HashSet<>();
            Set<Integer> intersection = null;

            for (Node child: tree.getChildren(node)) {
                Set<Integer> childSubstitutions = annotateSubstitutions(tree, child, branchMap);
                if (intersection == null) {
                    intersection = new HashSet<>(childSubstitutions);
                } else {
                    intersection.retainAll(childSubstitutions);
                }
            }

            for (Node child: tree.getChildren(node)) {
                Set<Integer> childSubstitutions = (Set<Integer>)child.getAttribute("substitutions");
                if (intersection == null) {
                    intersection = new HashSet<>(childSubstitutions);
                } else {
                    intersection.retainAll(childSubstitutions);
                }
            }

        } else {
            String tipName = tree.getTaxon(node).getName();
            Branch branch = branchMap.get(tipName);

            if (branch == null) {
                errorStream.println("Tip, " + tipName + ", not found in substitutions list");
                System.exit(1);
            }

            substitutions = branch.substitutions;
        }

        node.setAttribute("path", substitutions);

        return substitutions;
    }

    private void findPlacements(MutableRootedTree tree, Node node, List<Integer> substitutionList) {
        List<Integer> newSubstitutions = new ArrayList<>(substitutionList);

        String substString = (String)node.getAttribute("substitutions");
        if (substString != null) {
            newSubstitutions.addAll(getSubstitutions(substString));
        }

        if (!tree.isExternal(node)) {
            for (Node child: tree.getChildren(node)) {
                findPlacements(tree, child, newSubstitutions);
            }
        } else {
            Taxon locationTaxon = tree.getTaxon(node);


        }
    }

    private class Branch {

        public Branch(String name, String mutationString, String ambiguityString) {
            substitutions = new HashSet<>(getSubstitutions(mutationString));
            ambiguities = new HashSet<>();

            String[] ambs = ambiguityString.split("\\|");
            for (String ambiguity : ambs) {
                String[] locs = ambiguity.split("-");
                if (locs.length > 1) {
                    for (int location = Integer.parseInt(locs[0]); location <= Integer.parseInt(locs[1]); location++) {
                        ambiguities.add(location);
                    }
                } else {
                    int location = Integer.parseInt(locs[0]);
                    ambiguities.add(location);
                }
            }

            this.name = name;
        }

        final Set<Integer> substitutions;
        final Set<Integer> ambiguities;
        final String name;
    }

    private static List<Integer> getSubstitutions(String mutationString) {
        final List<Integer> substitutions = new ArrayList<>();

        String[] muts = mutationString.split("\\|");
        for (String mutation : muts) {
            if (mutation.length() > 0) {
                State state = Nucleotides.getState(mutation.charAt(0));
                int location = Integer.parseInt(mutation.substring(1));
                substitutions.add(getSubstitution(location, state));
            }
        }
        return substitutions;
    }

    private static int getSubstitution(int location, State state) {
        return (location << 4) + ((NucleotideState)state).bitCode;
    }

    private static int getLocation(int substitution) {
        return substitution >> 4;
    }

    private static int getState(int substitution) {
        throw new UnsupportedOperationException("not implemented");
    }

}


