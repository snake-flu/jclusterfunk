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
        Map<String, CSVRecord> csv = new HashMap<>();
        try {
            Reader in = new FileReader(fileName);
            CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
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
                        errorStream.println("Duplicate index value, " + key + " in metadata table");
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
                int endDay = (month == month2 ? day2 : (month == 2? 28 : (month == 3 ? 31 : (month == 4 ? 30 : 31))));

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

    static final String[] COLUMNS = { /*"sequence_name","cog_id","gisaid_id",*/"sample_date","epi_week","adm2","NUTS1","longitude","latitude","location","pillar_2","is_surveillance","travel_history","lineage" };

    static final String[] FILE_NAMES = {
            "cog_global_2021-05-11_consortium.csv",
            "cog_global_2021-05-12_consortium.csv",
            "cog_global_2021-05-13_consortium.csv",
            "cog_global_2021-05-14_consortium.csv",
            "cog_global_2021-05-15_consortium.csv",
            "cog_global_2021-05-16_consortium.csv",
            "cog_global_2021-05-17_consortium.csv"
    };

    public static void main(String[] args) {
        List<String> dateSet = createDateSet(2021, 3, 1, 2021, 5, 17);
        List<String> dateAddedList = createDateSet(2021, 5, 11, 2021, 5, 17);

        String[] lineages = { "B.1.617.2", "B.1.1.7"};
        Set<String> lineageSet = new HashSet<String>(Arrays.asList(lineages));

        Map<String, CSVRecord> rowMap = new HashMap<>();

        Map<String, String> dateAddedMap = new HashMap<>();

        for (String dateAdded : dateAddedList) {
            String filename = "cog_global_" + dateAdded + "_consortium.csv";
            List<CSVRecord> rows = readCSV(filename, "sequence_name");
            outStream.println("Read metadata table: " + filename);
            outStream.println("               Rows: " + rows.size());

            rows = filter(rows, "country", "UK");
            outStream.println("            UK only: " + rows.size());

            rows = filter(rows, "sample_date", new HashSet<>(dateSet));
            outStream.println("         since march only: " + rows.size());


            int newCount = 0;
            for (CSVRecord row : rows) {
                if (!rowMap.containsKey(row.get("cog_id"))) {
                    dateAddedMap.put(row.get("cog_id"), dateAdded);
                    newCount += 1;
                }
                rowMap.put(row.get("cog_id"), row);
            }
            outStream.println("         new sequences: " + newCount);
            outStream.println();
        }

        Map<String, CSVRecord> travelMap = readCSVMap("travel_history.csv", "cog_id");

//        Map<String, Map<String, Integer>> countMap = count(rows, "sample_date", new HashSet<String>(dateSet), "lineage", lineageSet);
//
//        System.out.print("sample_date");
//        for (String lineage : lineageSet) {
//            System.out.print("," + lineage);
//        }
//        System.out.println();
//
//        for (String date : countMap.keySet()) {
//            System.out.print(date);
//
//            Map<String, Integer> counts = countMap.get(date);
//            for (String lineage : lineageSet) {
//                System.out.print("," + counts.getOrDefault(lineage, 0));
//            }
//            System.out.println();
//        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get("adm2.csv")));

            writer.println("epi_week,adm2,lineage,count");

            for (int week = 52; week <= 72; week += 1) {
                Map<String, Integer> locationCountsB117 = new HashMap<>();
                Map<String, Integer> locationCountsB117Travel = new HashMap<>();
                Map<String, Integer> locationCountsB16172 = new HashMap<>();
                Map<String, Integer> locationCountsB16172Travel = new HashMap<>();
                for (String key : rowMap.keySet()) {
                    CSVRecord row = rowMap.get(key);
                    String cogId = row.get("cog_id");
                    boolean travel = travelMap.get(cogId) != null && "India".equals(travelMap.get(cogId).get("travel_history"));
                    String adm2 = row.get("adm2");
                    adm2 = adm2.replace(' ', '_');
                    if (!adm2.isEmpty() && !adm2.contains("|")) {
                        if (row.get("epi_week").equals(Integer.toString(week))) {
                            if (travel) {
                                if ("B.1.1.7".equals(row.get("lineage"))) {
                                    locationCountsB117Travel.put(adm2,
                                            locationCountsB117Travel.getOrDefault(adm2, 0) + 1);
                                } else if ("B.1.617.2".equals(row.get("lineage"))) {
                                    locationCountsB16172Travel.put(adm2,
                                            locationCountsB16172Travel.getOrDefault(adm2, 0) + 1);
                                }
                            } else {
                                if ("B.1.1.7".equals(row.get("lineage"))) {
                                    locationCountsB117.put(adm2,
                                            locationCountsB117.getOrDefault(adm2, 0) + 1);
                                } else if ("B.1.617.2".equals(row.get("lineage"))) {
                                    locationCountsB16172.put(adm2,
                                            locationCountsB16172.getOrDefault(adm2, 0) + 1);
                                }
                            }
                        }
                    }
                }

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
                locations = new TreeSet<>(locationCountsB117Travel.keySet());
                for (String location : locations) {
                    writer.print(week);
                    writer.print("," + location);
                    writer.print(",B.1.1.7_travel");
                    writer.print("," + locationCountsB117Travel.getOrDefault(location, 0));
                    writer.println();
                }
                locations = new TreeSet<>(locationCountsB16172Travel.keySet());
                for (String location : locations) {
                    writer.print(week);
                    writer.print("," + location);
                    writer.print(",B.1.617.2_travel");
                    writer.print("," + locationCountsB16172Travel.getOrDefault(location, 0));
                    writer.println();
                }           }
            writer.close();
        } catch (IOException ioe) {
            errorStream.println("Error opening output file: " + ioe.getMessage());
            System.exit(1);
        }

        try {
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get("adm2_travel.csv")));

            writer.println("adm2,B.1.1.7,B.1.617.2,other,travel");

            Map<String, Integer> locationCountsB117 = new HashMap<>();
            Map<String, Integer> locationCountsB16172 = new HashMap<>();
            Map<String, Integer> locationCountsOther = new HashMap<>();
            Map<String, Integer> locationCountsTravel = new HashMap<>();
            for (String key : rowMap.keySet()) {
                CSVRecord row = rowMap.get(key);
                String cogId = row.get("cog_id");
                boolean travel = travelMap.get(cogId) != null && "India".equals(travelMap.get(cogId).get("travel_history"));
                String adm2 = row.get("adm2");
                adm2 = adm2.replace(' ', '_');
                if (!adm2.isEmpty() && !adm2.contains("|")) {
                    if (travel) {
                        locationCountsTravel.put(adm2,
                                locationCountsTravel.getOrDefault(adm2, 0) + 1);
                    } else {
                        if ("B.1.1.7".equals(row.get("lineage"))) {
                            locationCountsB117.put(adm2,
                                    locationCountsB117.getOrDefault(adm2, 0) + 1);
                        } else if ("B.1.617.2".equals(row.get("lineage"))) {
                            locationCountsB16172.put(adm2,
                                    locationCountsB16172.getOrDefault(adm2, 0) + 1);
                        } else {
                            locationCountsOther.put(adm2,
                                    locationCountsOther.getOrDefault(adm2, 0) + 1);
                        }
                    }
                }
            }

            Set<String> locations = new TreeSet<>(locationCountsB117.keySet());
            locations.addAll(locationCountsB16172.keySet());
            locations.addAll(locationCountsOther.keySet());
            locations.addAll(locationCountsTravel.keySet());
            for (String location : locations) {
                writer.print(location);
                writer.print("," + locationCountsB117.getOrDefault(location, 0));
                writer.print("," + locationCountsB16172.getOrDefault(location, 0));
                writer.print("," + locationCountsOther.getOrDefault(location, 0));
                writer.print("," + locationCountsTravel.getOrDefault(location, 0));
                writer.println();
            }

            writer.close();
        } catch (IOException ioe) {
            errorStream.println("Error opening output file: " + ioe.getMessage());
            System.exit(1);
        }

        try {
            writer = new PrintWriter(Files.newBufferedWriter(Paths.get("data.csv")));

            writer.print(String.join(",", COLUMNS));
            writer.print(",date_added");
            writer.println();

            for (String key : rowMap.keySet()) {
                CSVRecord row = rowMap.get(key);
                String cogId = row.get("cog_id");
                boolean travel = travelMap.get(cogId) != null && "India".equals(travelMap.get(cogId).get("travel_history"));
                boolean first = true;
                for (String column : COLUMNS) {
                    writer.print(first ? "" : ",");
                    first = false;
                    writer.print(column.equals("travel_history") ? (travel ? "Y" : "N") : row.get(column));
                }
                writer.print("," + dateAddedMap.get(key));
                writer.println();
            }

            writer.close();
        } catch (IOException ioe) {
            errorStream.println("Error opening output file: " + ioe.getMessage());
            System.exit(1);
        }

    }


}
