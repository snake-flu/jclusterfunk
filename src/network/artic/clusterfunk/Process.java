package network.artic.clusterfunk;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Andrew Rambaut
 * @version $
 */
public class Process {
    final static PrintStream errorStream = System.err;
    final static PrintStream outStream = System.out;

    static CSVRecord headerRecord = null;

    static protected List<CSVRecord> readCSV(String fileName, String indexColumn) {
        return new ArrayList<>(readCSVMap(fileName, indexColumn).values());
    }

    static protected Map<String, CSVRecord> readCSVMap(String fileName, String indexColumn) {
        return readCSVTSVMap(fileName, indexColumn, false);
    }
    static protected Map<String, CSVRecord> readTSVMap(String fileName, String indexColumn){
        return readCSVTSVMap(fileName, indexColumn, true);

    }
    static protected Map<String, CSVRecord> readCSVTSVMap(String fileName, String indexColumn, boolean tsv) {
        Map<String, CSVRecord> csv = new HashMap<>();
        try {
            Reader in = new FileReader(fileName);
            CSVParser parser;
            if (tsv) {
                parser = CSVFormat.MONGODB_TSV.withFirstRecordAsHeader().parse(in);
            } else {
                parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            }
            if (indexColumn != null) {
                // a particular column is used to index - check it is there for the first record
                // and use it to key the records

                boolean first = true;
                for (CSVRecord record : parser) {
                    if (first) {
                        headerRecord = record;
                        if (record.get(indexColumn) == null) {
                            errorStream.println("Index column, " + indexColumn + " not found in metadata table");
                            System.exit(1);
                        }
                        first = false;
                    }
                    String key = record.get(indexColumn);
                    if (csv.containsKey(key)) {
//                        errorStream.println("Duplicate index value, " + key + " in metadata table");
//                        System.exit(1);
                    }
                    csv.put(key, record);
                }
            } else {
                // key the records against the first column
                boolean first = true;
                for (CSVRecord record : parser) {
                    if (first) {
                        headerRecord = record;
                        first = false;
                    }
                    String key = record.get(0);
                    if (csv.containsKey(key)) {
                        errorStream.println("Duplicate index value, " + key + " in metadata table");
//                        System.exit(1);
                    }
                    csv.put(key, record);
                }
            }
        } catch (IOException e) {
            errorStream.println("Error reading metadata file: " + e.getMessage());
            System.exit(1);
        }
        return csv;
    }

    static protected Map<String, List<String>> readCSVColumns(String fileName, String indexColumn, String[] valueColumns) {
        Map<String, List<String>> columnMap = new HashMap<>();
        try {
            Reader in = new FileReader(fileName);
            CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

            // a particular column is used to index - check it is there for the first record
            // and use it to key the records

            boolean first = true;
            for (CSVRecord record : parser) {
                if (first) {
                    if (record.get(indexColumn) == null) {
                        errorStream.println("Index column, " + indexColumn + " not found in metadata table");
                        System.exit(1);
                    }
                    first = false;
                }
                String key = record.get(indexColumn);
//                    if (csv.containsKey(key)) {
//                        errorStream.println("Duplicate index value, " + key + " in metadata table");
////                        System.exit(1);
//                    }
                List<String> values = new ArrayList<>();
                for (String column: valueColumns) {
                    values.add(record.get(column));
                }
                columnMap.put(key, values);
            }

        } catch (IOException e) {
            errorStream.println("Error reading metadata file: " + e.getMessage());
            System.exit(1);
        }
        return columnMap;
    }

    static List<CSVRecord> filter(List<CSVRecord> rows, String column, String value) {
        return filter(rows, column, Collections.singleton(value));
    }


    static List<CSVRecord> filter(List<CSVRecord> rows, String column, String[] values) {
        List<String> valueList = Arrays.asList(values);
        return filter(rows, column, new HashSet<>(valueList));
    }

    static List<CSVRecord> filter(List<CSVRecord> rows, String column, Set<String> values) {
        List<CSVRecord> filteredRows = new ArrayList<>();

        for (CSVRecord row : rows) {
            if (values.contains(row.get(column))) {
                filteredRows.add(row);
            }
        }

        return filteredRows;
    }

    static List<String> createDateSet(int year1, int month1, int day1, int year2, int month2, int day2) {
        List<String> dateSet = new ArrayList<>();
        for (int year = year1; year <= year2; year +=1) {
            for (int month = month1; month <= month2; month +=1) {
                int startDay = (month == month1 ? day1 : 1);
                int endDay = (month == month2 ? day2 : (month == 2? 28 : (month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31)));

                for (int day = startDay; day <= endDay; day +=1) {
                    String date = "" + year + "-" +
                            (month < 10 ? "0" + month : month) + "-" +
                            (day < 10 ? "0" + day : day);
                    dateSet.add(date);
                    //System.out.println(date);
                }
            }
        }
        return dateSet;
    }

    static Map<String, Map<String, Integer>> count(List<CSVRecord> rows, String column1, Set<String> values1, String column2, Set<String> values2) {

        Map<String, Map<String, Integer>> countsMap = new HashMap<>();

        for (CSVRecord row : rows) {
            if (values1.contains(row.get(column1))) {
                String value1 = row.get(column1);
                Map<String, Integer> counts = countsMap.get(value1);
                if (counts == null) {
                    counts = new HashMap<>();
                }

                if (values2.contains(row.get(column2))) {
                    String value2 = row.get(column2);
                    counts.put(value2, counts.getOrDefault(value2, 0) + 1);
                } else {
                    counts.put("other", counts.getOrDefault("other", 0) + 1);
                }

                countsMap.put(value1, counts);
            }
        }

        return countsMap;
    }

    static final String[] COLUMNS = { /*"sequence_name","cog_id","gisaid_id",*/
            "sample_date","epi_week","NUTS1", "utla",
            "is_pillar_2","is_surveillance","lineage", "collection_date",
            "collection_pillar", "received_date", "submission_org_code"};

    static final String[] GEO_COLUMNS = { "NUTS1","longitude","latitude","utla","location" };

    public static void main(String[] args) {
        List<String> dateSet = createDateSet(2021, 4, 1, 2021, 6, 17);
        List<String> dateAddedList = createDateSet(2021, 6, 3, 2021, 6, 17);

        String[] lineages = { "B.1.617.2", "B.1.1.7"};
        Set<String> lineageSet = new HashSet<>(Arrays.asList(lineages));

//        Map<String, List<String>> collectionDateMap = readCSVColumns("cog_2021-05-22_all_metadata.csv",
//                "central_sample_id", new String[] {"collection_date", "received_date", "submission_org"});

        Map<String, CSVRecord> rowMap = new HashMap<>();

        Map<String, String> dateAddedMap = new HashMap<>();

        String latestDateAdded = "";

        for (String dateAdded : dateAddedList) {
            String filename = "cog_" + dateAdded + "_all_metadata.csv";
            if (Files.exists(Paths.get(filename))) {
                List<CSVRecord> rows = readCSV(filename, "sequence_name");
                outStream.println("Read metadata table: " + filename);
                outStream.println("               Rows: " + rows.size());

//            rows = filter(rows, "country", "UK");
//            outStream.println("            UK only: " + rows.size());

                rows = filter(rows, "sample_date", new HashSet<>(dateSet));
                outStream.println("         since march only: " + rows.size());


                int newCount = 0;
                for (CSVRecord row : rows) {
                    String cogId = row.get("central_sample_id");

                    if (!rowMap.containsKey(cogId)) {
                        dateAddedMap.put(cogId, dateAdded);
                        newCount += 1;
                    }
                    rowMap.put(cogId, row);
                }
                outStream.println("         new sequences: " + newCount);
                outStream.println();
                latestDateAdded = dateAdded;
            }
        }

        Map<String, CSVRecord> travelMap = readCSVMap("travel_history.csv", "cog_id");

        String filename = "cog_global_" + latestDateAdded + "_consortium.csv";
        Map<String, CSVRecord> metadataMap = readCSVMap(filename, "cog_id");
        List<String> metadataColumns = metadataMap.get(metadataMap.keySet().iterator().next()).getParser().getHeaderNames();

        filename = "cog_global_" + latestDateAdded + "_geography.csv";
        Map<String, CSVRecord> geographyMap = readCSVMap(filename, "cog_id");
        List<String> geoColumns = geographyMap.get(geographyMap.keySet().iterator().next()).getParser().getHeaderNames();

        filename = "majora." + latestDateAdded + ".metadata.matched.tsv";
        filename = filename.replace("-", "");
        Map<String, CSVRecord> majoraMap = readTSVMap(filename, "central_sample_id");

        Map<String, CSVRecord> ctList = readCSVMap("ct.csv", null);

        Map<String, Map<String, Integer>> countMap = count(new ArrayList<>(rowMap.values()), "sample_date", new HashSet<String>(dateSet), "lineage", lineageSet);

//        System.out.print("sample_date,n_B.1.1.7,n_B.1.617.2,n_B.1.1.7_adj,n_B.1.617.2_adj,p_ct30_sgn,p_ct30_sgp,n_sgn,n_sgp");
//
//        System.out.println();
//
//        for (String date : dateSet) {
//            System.out.print(date);
//
//            Map<String, Integer> counts = countMap.get(date);
//            CSVRecord ct = ctList.get(date);
//            if (ct == null) {
//                System.err.println("CT missing for date " + date);
//                System.exit(1);
//            }
//            int countB117 = 0;
//            int countB16172 = 0;
//            if (counts != null) {
//                countB117 = counts.getOrDefault("B.1.1.7", 0);
//                countB16172 = counts.getOrDefault("B.1.617.2", 0);
//            }
//            double propCT30SGNegative = Double.parseDouble(ct.get("p_SGTF_ct30"));
//            double propCT30SGPositive = Double.parseDouble(ct.get("p_SGP_ct30"));
//            double countSGNegative = Integer.parseInt(ct.get("N_SGTF"));
//            double countSGPositive = Integer.parseInt(ct.get("N_SGP"));
//
//            double correctedB117 = (1.0 + propCT30SGNegative) * countB117;
//            double correctedB16172 = (1.0 + propCT30SGPositive) * countB16172;
//
//
//            System.out.println( "," + countB117 + "," + countB16172 + "," + correctedB117 + "," + correctedB16172 + "," +
//                    propCT30SGNegative + "," + propCT30SGPositive + "," + countSGNegative + "," + countSGPositive);
//        }
//        System.out.println();

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get("utla.csv")));

            writer.println("epi_week,utla,lineage,count");

            for (int week = 52; week <= 77; week += 1) {
                Map<String, Integer> locationCountsB117 = new HashMap<>();
//                Map<String, Integer> locationCountsB117Travel = new HashMap<>();
                Map<String, Integer> locationCountsB16172 = new HashMap<>();
                //               Map<String, Integer> locationCountsB16172Travel = new HashMap<>();
                int count = 0;
                for (String key : rowMap.keySet()) {
                    CSVRecord row = rowMap.get(key);
//                    String cogId = row.get("cog_id");
                    String cogId = row.get("central_sample_id");
                    boolean travel = travelMap.get(cogId) != null && "India".equals(travelMap.get(cogId).get("travel_history"));
//                    String adm2 = row.get("adm2");
//                    adm2 = adm2.replace(' ', '_');
                    CSVRecord metadata = metadataMap.get(key);
                    if (metadata != null) {
                        String location = metadata.get("utla");

                        if (!location.isEmpty() && !location.contains("|")) {
                            if (row.get("epi_week").equals(Integer.toString(week))) {
//                            if (travel) {
//                                if ("B.1.1.7".equals(row.get("lineage"))) {
//                                    locationCountsB117Travel.put(adm2,
//                                            locationCountsB117Travel.getOrDefault(adm2, 0) + 1);
//                                } else if ("B.1.617.2".equals(row.get("lineage"))) {
//                                    locationCountsB16172Travel.put(adm2,
//                                            locationCountsB16172Travel.getOrDefault(adm2, 0) + 1);
//                                }
//                            } else {
                                if ("B.1.1.7".equals(row.get("lineage"))) {
                                    locationCountsB117.put(location,
                                            locationCountsB117.getOrDefault(location, 0) + 1);
                                } else if ("B.1.617.2".equals(row.get("lineage"))) {
                                    locationCountsB16172.put(location,
                                            locationCountsB16172.getOrDefault(location, 0) + 1);
                                }
                                count += 1;
//                            }
                            }
                        }
                    }
                }

                System.out.println("Epiweek: " + week + ": " + count);

//                Set<String> locations = new TreeSet<>(locationCountsB117.keySet());
//                locations.addAll(locationCountsB1617.keySet());
//                for (String location : locations) {
//                    writer.print(week);
//                    writer.print("," + location);
//                    writer.print("," + locationCountsB117.getOrDefault(location, 0));
//                    writer.print("," + locationCountsB1617.getOrDefault(location, 0));
//                    writer.println();
//                }
                Set<String> locations = new TreeSet<>(locationCountsB117.keySet());
                for (String location : locations) {
                    writer.print(week);
                    writer.print("," + location);
                    writer.print(",B.1.1.7");
                    writer.print("," + locationCountsB117.getOrDefault(location, 0));
                    writer.println();
                }
                locations = new TreeSet<>(locationCountsB16172.keySet());
                for (String location : locations) {
                    writer.print(week);
                    writer.print("," + location);
                    writer.print(",B.1.617.2");
                    writer.print("," + locationCountsB16172.getOrDefault(location, 0));
                    writer.println();
                }
//                locations = new TreeSet<>(locationCountsB117Travel.keySet());
//                for (String location : locations) {
//                    writer.print(week);
//                    writer.print("," + location);
//                    writer.print(",B.1.1.7_travel");
//                    writer.print("," + locationCountsB117Travel.getOrDefault(location, 0));
//                    writer.println();
//                }
//                locations = new TreeSet<>(locationCountsB16172Travel.keySet());
//                for (String location : locations) {
//                    writer.print(week);
//                    writer.print("," + location);
//                    writer.print(",B.1.617.2_travel");
//                    writer.print("," + locationCountsB16172Travel.getOrDefault(location, 0));
//                    writer.println();
//                }
            }
            writer.close();
        } catch (IOException ioe) {
            errorStream.println("Error opening output file: " + ioe.getMessage());
            System.exit(1);
        }

//        try {
//            writer = new PrintWriter(Files.newBufferedWriter(Paths.get("adm2_travel.csv")));
//
//            writer.println("adm2,B.1.1.7,B.1.617.2,other,travel");
//
//            Map<String, Integer> locationCountsB117 = new HashMap<>();
//            Map<String, Integer> locationCountsB16172 = new HashMap<>();
//            Map<String, Integer> locationCountsOther = new HashMap<>();
//            Map<String, Integer> locationCountsTravel = new HashMap<>();
//            for (String key : rowMap.keySet()) {
//                CSVRecord row = rowMap.get(key);
//                String cogId = row.get("cog_id");
//                boolean travel = travelMap.get(cogId) != null && "India".equals(travelMap.get(cogId).get("travel_history"));
//                String adm2 = row.get("adm2");
//                adm2 = adm2.replace(' ', '_');
//                if (!adm2.isEmpty() && !adm2.contains("|")) {
//                    if (travel) {
//                        locationCountsTravel.put(adm2,
//                                locationCountsTravel.getOrDefault(adm2, 0) + 1);
//                    } else {
//                        if ("B.1.1.7".equals(row.get("lineage"))) {
//                            locationCountsB117.put(adm2,
//                                    locationCountsB117.getOrDefault(adm2, 0) + 1);
//                        } else if ("B.1.617.2".equals(row.get("lineage"))) {
//                            locationCountsB16172.put(adm2,
//                                    locationCountsB16172.getOrDefault(adm2, 0) + 1);
//                        } else {
//                            locationCountsOther.put(adm2,
//                                    locationCountsOther.getOrDefault(adm2, 0) + 1);
//                        }
//                    }
//                }
//            }
//
//            Set<String> locations = new TreeSet<>(locationCountsB117.keySet());
//            locations.addAll(locationCountsB16172.keySet());
//            locations.addAll(locationCountsOther.keySet());
//            locations.addAll(locationCountsTravel.keySet());
//            for (String location : locations) {
//                writer.print(location);
//                writer.print("," + locationCountsB117.getOrDefault(location, 0));
//                writer.print("," + locationCountsB16172.getOrDefault(location, 0));
//                writer.print("," + locationCountsOther.getOrDefault(location, 0));
//                writer.print("," + locationCountsTravel.getOrDefault(location, 0));
//                writer.println();
//            }
//
//            writer.close();
//        } catch (IOException ioe) {
//            errorStream.println("Error opening output file: " + ioe.getMessage());
//            System.exit(1);
//        }

        List<String> keys = new ArrayList<>(rowMap.keySet());
        Collections.shuffle(keys);

        try {
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get("data.csv")));

            writer.print(String.join(",", COLUMNS));
            writer.print(",date_added");
            writer.print(",has_collection_date");
            writer.println();

            for (String key : keys) {
                CSVRecord row = rowMap.get(key);
                String cogId = row.get("central_sample_id");

                CSVRecord geo = geographyMap.get(key);
                CSVRecord metadata = metadataMap.get(key);
                CSVRecord maj = majoraMap.get(cogId);
                if (maj != null) {

                    boolean travel = travelMap.get(cogId) != null && "India".equals(travelMap.get(cogId).get("travel_history"));
                    boolean first = true;
                    for (String column : COLUMNS) {
                        writer.print(first ? "" : ",");
                        first = false;
                        String value = "";
                        if (column.equals("collection_pillar")) {
                            if (maj != null) {
                                value = maj.get("collection_pillar");
                            }
                        } else if (column.equals("is_pillar_2")) {
                            if (maj != null) {
                                String collectionPillar = maj.get("collection_pillar");
                                value = ("2".equals(collectionPillar) /*|| collectionPillar.isEmpty()*/ ? "Y" : "N");
                            }
                        } else if (column.equals("travel_history")) {
                            value = (travel ? "Y" : "N");
                        } else {
                            if (metadataColumns.contains(column)) {
                                if (metadata != null) {
                                    value = metadata.get(column);
                                }
                            } else if (geoColumns.contains(column)) {
                                if (geo != null) {
                                    value = geo.get(column);
                                }
                            } else {
                                value = row.get(column);
                            }
                        }
                        writer.print(value);
                    }
                    writer.print("," + dateAddedMap.get(key));
                    writer.print("," + (row.get("collection_date").length() < 10 ? "N" : "Y"));
//                writer.print("," + (additionalValues.get(0)));
//                writer.print("," + (additionalValues.get(1)));
//                writer.print("," + (additionalValues.get(2)));

                    writer.println();
                } else {
                    //System.err.println(cogId + " missing from Majora table");
                }
            }


            writer.close();
        } catch (IOException ioe) {
            errorStream.println("Error opening output file: " + ioe.getMessage());
            System.exit(1);
        }

    }


}
