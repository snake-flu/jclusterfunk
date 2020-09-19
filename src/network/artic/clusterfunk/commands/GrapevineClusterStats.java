package network.artic.clusterfunk.commands;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 *
 */
public class GrapevineClusterStats extends Command {
    private final static double GENOME_LENGTH = 29903;

    private final static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public GrapevineClusterStats(String treeFileName,
                                 String metadataFileName,
                                 String outputMetadataFileName,
                                 String indexColumn,
                                 int indexHeader,
                                 String headerDelimiter,
                                 boolean ignoreMissing,
                                 boolean isVerbose) {

        super(metadataFileName, null, indexColumn, indexHeader, headerDelimiter, isVerbose);

        int minSize = 10;
        int maxAge = 1000;
        double minUKProportion = 0.95;

        RootedTree tree = readTree(treeFileName);

        Map<Taxon, String> taxonMap = getTaxonMap(tree);

        String[] annotationColumns = new String[] { "sample_date", "country", "adm1", "adm2", "lineage", "uk_lineage" };

        for (String columnName: annotationColumns) {
            annotateTips(tree, taxonMap, columnName, ignoreMissing);
        }

        int i = 1;
        for (Node node : tree.getInternalNodes()) {
            node.setAttribute("number", i);
            i += 1;
        }


        Map<Node, Stats> nodeStatsMap = new HashMap<>();

        calculateStatistics(tree, tree.getRootNode(), 0.0, nodeStatsMap);

        List<Stats> internalNodeStats = new ArrayList<>();
        for (Node key : nodeStatsMap.keySet()) {
            if (!tree.isExternal(key)) {
                internalNodeStats.add(nodeStatsMap.get(key));
            }
        }

        internalNodeStats.sort(Comparator.comparing(Stats::getMostRecentDate));

//        for (Stats stats : internalNodeStats) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(stats.getMostRecentDate().toString()).append("\t");
//            sb.append(stats.tipCount).append("\t");
//            sb.append(stats.admin0.size()).append("\t");
//            sb.append(stats.admin1.size()).append("\t");
//            sb.append(stats.admin2.size());
//            outStream.println(sb.toString());
//        }

        if (outputMetadataFileName != null) {
            try {
                PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputMetadataFileName)));

                if (outputMetadataFileName.endsWith("csv")) {
                    writer.println("node_number,parent_number,most_recent_tip,day_range,recency,age," +
                            "tip_count,divergence_ratio,mean_tip_divergence,stem_length," +
                            "lineage,uk_lineage,proportion_uk,admin0_count,admin1_count,admin2_count," +
                            "admin0_mode,admin1_mode,admin2_mode," +
                            "admin1_entropy,admin2_entropy,tips");
                    for (Stats stats : internalNodeStats) {
                        if (stats.tipCount >= minSize && stats.getAge() < maxAge && stats.ukProportion >= minUKProportion) {
                            writer.print(
                                    stats.node.getAttribute("number") + "," +
                                            (stats.parent != null ? stats.parent.getAttribute("number") : "root") + "," +
                                            dateFormat.format(stats.getMostRecentDate()) + "," +
                                            stats.dateRange + "," +
                                            stats.getRecency() + "," +
                                            stats.getAge() + "," +
                                            stats.tipCount + "," +
                                            stats.divergenceRatio + "," +
                                            stats.meanTipDivergence + "," +
                                            stats.stemLength + "," +
                                            stats.modalLineage + "," +
                                            stats.modalUKLineage + "," +
                                            stats.ukProportion + "," +
                                            stats.admin0.size() + "," +
                                            stats.admin1.size() + "," +
                                            stats.admin2.size() + "," +
                                            stats.modalAdmin0 + "," +
                                            stats.modalAdmin1 + "," +
                                            stats.modalAdmin2 + "," +
                                            stats.admin1Entropy + "," +
                                            stats.admin2Entropy + ",");
                            writer.println(String.join("|", stats.tipSet));
                        }
                    }

                } else if (outputMetadataFileName.endsWith("json")) {

                    writer.println("{");
                    writer.println("  \"data\": {");
                    writer.println("    \"values\": [");
                    boolean first = true;
                    for (Stats stats : internalNodeStats) {
                        if (stats.tipCount >= minSize && stats.getAge() < maxAge && stats.ukProportion >= minUKProportion) {
                            if (first) {
                                first = false;
                            } else {
                                writer.println(",");
                            }
                            writer.print("      {" +
                                    "\"node_number\": \"" + stats.node.getAttribute("number") + "\", " +
                                    "\"parent_number\": \"" + (stats.parent != null ? stats.parent.getAttribute("number") : "root") + "\", " +
                                    "\"most_recent_tip\": \"" + dateFormat.format(stats.getMostRecentDate()) + "\", " +
                                    "\"day_range\": \"" + stats.dateRange + "\", " +
                                    "\"recency\": \"" + stats.getRecency() + "\", " +
                                    "\"age\": \"" + stats.getAge() + "\", " +
                                    "\"tip_count\": \"" + stats.tipCount + "\", " +
                                    "\"stem_length\": \"" + stats.stemLength + "\", " +
                                    "\"mean_tip_divergence\": \"" + stats.meanTipDivergence + "\", " +
                                    "\"divergence_ratio\": \"" + stats.divergenceRatio + "\", " +
                                    "\"lineage\": \"" + stats.modalLineage + "\", " +
                                    "\"uk_lineage\": \"" + stats.modalUKLineage + "\", " +
                                    "\"proportion_uk\": \"" + stats.ukProportion + "\", " +
                                    "\"admin0_count\": \"" + stats.admin0.size() + "\", " +
                                    "\"admin1_count\": \"" + stats.admin1.size() + "\", " +
                                    "\"admin2_count\": \"" + stats.admin2.size() + "\", " +
                                    "\"admin0_mode\": \"" + stats.modalAdmin0 + "\", " +
                                    "\"admin1_mode\": \"" + stats.modalAdmin1 + "\", " +
                                    "\"admin2_mode\": \"" + stats.modalAdmin2 + "\", " +
                                    "\"admin1_entropy\": \"" + stats.admin1Entropy + "\", " +
                                    "\"admin2_entropy\": \"" + stats.admin2Entropy + "\", ");
                            List<String> tips = new ArrayList<>();
                            stats.tipSet.forEach (s -> tips.add("\"" + s + "\""));
                            writer.print("\"tips\": [" + String.join(",", tips) + "]}");
                        }
                    }
                    writer.println();
                    writer.println("    ]");
                    writer.println("  }");
                    writer.println("}");
                }

                writer.close();
            } catch (IOException e) {
                errorStream.println("Error writing metadata file: " + e.getMessage());
                System.exit(1);
            }
        }

    }

    private Stats calculateStatistics(RootedTree tree, Node node, double divergence, Map<Node, Stats> nodeStatsMap) {
        Stats stats;

        double length = tree.getLength(node) * GENOME_LENGTH;
        Node parent = tree.getParent(node);
        if (tree.isExternal(node)) {
//            if (tree.getTaxon(node).getName().contains("QEUH-96DABF")) {
//                System.out.println("QEUH-96DABF");
//            }
            stats = new Stats(node,
                    parent,
                    divergence,
                    length,
                    tree.getTaxon(node).getName(),
                    (String)node.getAttribute("sample_date"),
                    (String)node.getAttribute("country"),
                    (String)node.getAttribute("adm1"),
                    (String)node.getAttribute("adm2"),
                    (String)node.getAttribute("lineage"),
                    (String)node.getAttribute("uk_lineage")
            );
        } else {
            List<Stats> statsList = new ArrayList<>();
            for (Node child : tree.getChildren(node)) {
                statsList.add(calculateStatistics(tree, child, divergence + length, nodeStatsMap));
            }
            stats = new Stats(node, parent, divergence + length, length, statsList);
        }

        nodeStatsMap.put(node, stats);

        return stats;
    }

    class Stats {
        public Stats(Node node, Node parent, double divergence, double length, String tip, String date,
                     String admin0, String admin1, String admin2, String lineage, String ukLineage) {
            this.node = node;
            this.parent = parent;

            this.divergence = divergence;
            this.stemLength = length;
            this.divergences.add(divergence);
            this.meanTipDivergence = 0;
            this.divergenceRatio = 0.0;

            dates.add(LocalDate.parse(date, dateFormat));
            dateRange = 0;

            this.admin0.put(admin0 != null ? admin0 : "", 1);
            this.lineage.put(lineage != null ? lineage : "", 1);
            if (admin0.equalsIgnoreCase("UK")) {
                ukCount = 1;
                ukProportion = 1.0;
                this.admin1.put(admin1 != null ? admin1 : "", 1);
                this.admin2.put(admin2 != null ? admin2 : "", 1);
                this.ukLineage.put(ukLineage != null ? ukLineage : "", 1);
            } else {
                ukCount = 0;
                ukProportion = 0.0;
            }
            this.modalAdmin0 = null;
            this.modalAdmin1 = null;
            this.modalAdmin2 = null;
            this.modalLineage = null;
            this.modalUKLineage = null;
            admin1Entropy = 0;
            admin2Entropy = 0;

            tipSet.add(tip);
            tipCount = 1;

        }

        public Stats(Node node, Node parent, double divergence, double stemLength, Collection<Stats> stats) {
            this.node = node;
            this.parent = parent;
            this.divergence = divergence;
            this.stemLength = stemLength;

            int ukCount = 0;
            for (Stats s: stats) {
                divergences.addAll(s.divergences);
                dates.addAll(s.dates);
                ukCount += s.ukCount;
                for (String admin0 : s.admin0.keySet()) {
                    this.admin0.put(admin0, this.admin0.getOrDefault(admin0, 0) + s.admin0.get(admin0));
                }
                for (String admin1 : s.admin1.keySet()) {
                    this.admin1.put(admin1, this.admin1.getOrDefault(admin1, 0) + s.admin1.get(admin1));
                }
                for (String admin2 : s.admin2.keySet()) {
                    this.admin2.put(admin2, this.admin2.getOrDefault(admin2, 0) + s.admin2.get(admin2));
                }
                for (String lineage : s.lineage.keySet()) {
                    this.lineage.put(lineage, this.lineage.getOrDefault(lineage, 0) + s.lineage.get(lineage));
                }
                for (String ukLineage : s.ukLineage.keySet()) {
                    this.ukLineage.put(ukLineage, this.ukLineage.getOrDefault(ukLineage, 0) + s.ukLineage.get(ukLineage));
                }
                tipSet.addAll(s.tipSet);
            }
            dates.sort(Collections.reverseOrder());
            this.modalAdmin0 = getModal(admin0);
            this.modalAdmin1 = getModal(admin1);
            this.modalAdmin2 = getModal(admin2);
            this.modalLineage = getModal(lineage);
            this.modalUKLineage = getModal(ukLineage);

            tipCount = tipSet.size();
            this.ukCount = ukCount;
            this.ukProportion = ((double)ukCount) / tipCount;

            meanTipDivergence = meanDivergence();
            divergenceRatio = stemLength / (meanTipDivergence + 1);
            if (Double.isInfinite(divergenceRatio)) {
                System.out.println("inf");
            }
            dateRange = (double)ChronoUnit.DAYS.between(getLeastRecentDate(), getMostRecentDate());
            admin1Entropy = entropy(admin1);
            admin2Entropy = entropy(admin2);
        }

        String getModal(Map<String, Integer> countMap) {
            int maxCount = 0;
            String modalKey = "";
            for (String key : countMap.keySet()) {
                if (countMap.get(key) > maxCount) {
                    maxCount = countMap.get(key);
                    modalKey = key;
                }
            }
            return modalKey;
        }

        int getAge() {
            return (int)ChronoUnit.DAYS.between(getLeastRecentDate(), LocalDate.now());
        }

        int getRecency() {
            return (int)ChronoUnit.DAYS.between(getMostRecentDate(), LocalDate.now());
        }

        LocalDate getMostRecentDate() {
            return dates.get(0);
        }

        LocalDate getLeastRecentDate() {
            return dates.get(dates.size() - 1);
        }

        double meanDivergence() {
            double sum = 0.0;
            for (double d : divergences) {
                sum += (d - divergence);
            }
            return sum / divergences.size();
        }

        double entropy(Map<String, Integer> counts) {
            double sum = 0.0;
            for (int count : counts.values()) {
                sum += count;
            }

            double entropy = 0.0;
            for (int count : counts.values()) {
                double p = ((double)count) / sum;
                entropy -= p * Math.log(p);
            }

            return entropy;
        }

        final Node node;
        final Node parent;
        final double divergence;
        final double meanTipDivergence;
        final double divergenceRatio;
        final double stemLength;
        final List<LocalDate> dates = new ArrayList<>();
        final List<Double> divergences = new ArrayList<>();
        final int ukCount;
        final double ukProportion;
        final Map<String, Integer> admin0 = new TreeMap<>();
        final Map<String, Integer> admin1 = new TreeMap<>();
        final Map<String, Integer> admin2 = new TreeMap<>();
        final Map<String, Integer> lineage = new TreeMap<>();
        final Map<String, Integer> ukLineage = new TreeMap<>();
        final String modalAdmin0;
        final String modalAdmin1;
        final String modalAdmin2;
        final String modalLineage;
        final String modalUKLineage;
        final int tipCount;
        final Set<String> tipSet = new HashSet<>();
        final double dateRange;
        final double admin1Entropy;
        final double admin2Entropy;
    }
}

