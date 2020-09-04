package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import network.artic.clusterfunk.FormatType;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class Discover extends Command {
    public Discover(String treeFileName,
                    String metadataFileName,
                    String indexColumn,
                    int indexHeader,
                    String headerDelimiter,
                    boolean ignoreMissing,
                    boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        String[] annotationColumns = new String[] { "sample_date", "country", "adm1", "adm2" };

        for (String columnName: annotationColumns) {
            annotateTips(tree, taxonMap, columnName, ignoreMissing);
        }

        Map<Node, Stats> nodeStatsMap = new HashMap<>();

        calculateStatistics(tree, tree.getRootNode(), nodeStatsMap);

        List<Stats> internalNodeStats = new ArrayList<>();
        for (Node key : nodeStatsMap.keySet()) {
            if (!tree.isExternal(key)) {
                internalNodeStats.add(nodeStatsMap.get(key));
            }
        }

        internalNodeStats.sort(Comparator.comparing(Stats::getMostRecentDate));

        for (Stats stats : internalNodeStats) {
            StringBuilder sb = new StringBuilder();
            sb.append(stats.getMostRecentDate().toString()).append("\t");
            sb.append(stats.tipCount).append("\t");
            sb.append(stats.admin0.size()).append("\t");
            sb.append(stats.admin1.size()).append("\t");
            sb.append(stats.admin2.size());
            outStream.println(sb.toString());
        }

//        if (outputMetadataFileName != null) {
//            try {
//                PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputMetadataFileName)));
//
//                writer.println("sequence_name," + lineageName);
//
//                for (Node node : tree.getExternalNodes()) {
//                    writer.print(tree.getTaxon(node).getName());
//                    writer.print(",");
//                    writer.print(node.getAttribute("new_lineage"));
//                    writer.println();
//                }
//
//                writer.close();
//            } catch (IOException e) {
//                errorStream.println("Error writing metadata file: " + e.getMessage());
//                System.exit(1);
//            }
//        }
        
    }


    private Stats calculateStatistics(RootedTree tree, Node node, Map<Node, Stats> nodeStatsMap) {
        Stats stats;

        if (tree.isExternal(node)) {
            stats = new Stats(node,
                    (String)node.getAttribute("sample_date"),
                    (String)node.getAttribute("country"),
                    (String)node.getAttribute("adm1"),
                    (String)node.getAttribute("adm2"));
        } else {
            List<Stats> statsList = new ArrayList<>();
            for (Node child : tree.getChildren(node)) {
                statsList.add(calculateStatistics(tree, child, nodeStatsMap));
            }
            stats = new Stats(node, statsList);
        }

        nodeStatsMap.put(node, stats);

        return stats;
    }


    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    class Stats {
        public Stats(Node node, String date, String admin0, String admin1, String admin2) {
            this.node = node;
            try {
                dates.add(dateFormat.parse(date));
            } catch (ParseException e) {
                errorStream.println("Error parsing sample date: " + e.getMessage());
                System.exit(1);
            }
            this.admin0.put(admin0 != null ? admin0 : "", 1);
            if (admin0.equalsIgnoreCase("UK")) {
                this.admin1.put(admin1 != null ? admin1 : "", 1);
                this.admin2.put(admin2 != null ? admin2 : "", 1);
            }

            tipCount = 1;
        }

        public Stats(Node node, Collection<Stats> stats) {
            this.node = node;
            int count = 0;
            for (Stats s: stats) {
                dates.addAll(s.dates);
                for (String admin0 : s.admin0.keySet()) {
                    this.admin0.put(admin0, this.admin0.getOrDefault(admin0, 0) + s.admin0.get(admin0));
                }
                for (String admin1 : s.admin1.keySet()) {
                    this.admin1.put(admin1, this.admin1.getOrDefault(admin1, 0) + s.admin1.get(admin1));
                }
                for (String admin2 : s.admin2.keySet()) {
                    this.admin2.put(admin2, this.admin2.getOrDefault(admin2, 0) + s.admin2.get(admin2));
                }
                count += s.tipCount;
            }
            dates.sort(Collections.reverseOrder());

            tipCount = count;
        }

        Date getMostRecentDate() {
            return dates.get(0);
        }

        final Node node;
        final List<Date> dates = new ArrayList<>();
        final Map<String, Integer> admin0 = new TreeMap<>();
        final Map<String, Integer> admin1 = new TreeMap<>();
        final Map<String, Integer> admin2 = new TreeMap<>();
        final int tipCount;
    }
}

