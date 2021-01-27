package network.artic.clusterfunk;

import network.artic.clusterfunk.ClusterFunkOptions.Command;
import network.artic.clusterfunk.commands.*;
import org.apache.commons.cli.*;

import java.util.Arrays;

import static network.artic.clusterfunk.ClusterFunkOptions.*;

/**
 * Entrypoint class with main().
 */
class ClusterFunk {

    private final static String NAME = "jclusterfunk";
    private static final String VERSION = "v0.0.15";
    private static final String HEADER = NAME + " " + VERSION + "\nBunch of functions for trees\n\n";
    private static final String FOOTER = "";


    private static void printHelp(Command command, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        StringBuilder sb = new StringBuilder();
        sb.append(ClusterFunk.HEADER);

        if (command == Command.NONE) {
            sb.append("Available commands:\n ");
            for (Command c : Command.values()) {
                sb.append(" ")
                        .append(c);
            }
            sb.append("\n\nuse: <command> -h,--help to display individual options\n");

            formatter.printHelp(NAME + " <command> <options> [-h]", sb.toString(), options, ClusterFunk.FOOTER, false);
        } else {
            sb.append("Command: ")
                    .append(command)
                    .append("\n\n")
                    .append(command.getDescription())
                    .append("\n\n");
            formatter.printHelp(NAME + " " + command, sb.toString(), options, ClusterFunk.FOOTER, true);
        }

    }

    public static void main(String[] args) {

        Command command = Command.NONE;

        // create Options object
        Options options = new Options();
        options.addOption("h", "help", false, "display help");
        options.addOption(null, "version", false, "display version");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;

        if (args.length > 0 && !args[0].startsWith("-")) {
            try {
                command = Command.getCommand(args[0]);

                options.addOption("v","verbose", false, "write analysis details to console");

                switch (command) {
                    case ANNOTATE:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(LABEL_FIELDS);
                        options.addOption(TIP_ATTRIBUTES);
                        options.addOption(REPLACE);
                        options.addOption(IGNORE_MISSING);
                        break;
                    case ASSIGN:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(ATTRIBUTE);
                        options.addOption(OUT_ATTRIBUTE);
                        break;
                    case CLUSTER:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(ATTRIBUTE);
                        options.addOption(VALUE);
                        options.addOption(CLUSTER_NAME);
                        options.addOption(CLUSTER_PREFIX);
                        break;
                    case CONQUER:
                        options.addOption(INPUT_PATH);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        break;
                    case CONTEXT:
                        options.addOption(INPUT);
                        options.addOption(TAXON_FILE);
                        options.addOption(TAXA);
                        METADATA.setRequired(false);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_TAXA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(MRCA);
                        options.addOption(MAX_PARENT_LEVEL);
                        options.addOption(MAX_CHILD_LEVEL);
                        options.addOption(MAX_SIBLING);
                        options.addOption(COLLAPSE_BY);
                        options.addOption(IGNORE_MISSING);
                        break;
                    case CONVERT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        break;
                    case DIVIDE:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        OptionGroup divideGroup = new OptionGroup();
                        divideGroup.addOption(MAX_SUBTREE_COUNT);
                        divideGroup.addOption(MIN_SUBTREE_SIZE);
                        options.addOptionGroup(divideGroup);
                        options.addOption(REQUIRE_OUTGROUP);
                        break;
                    case EXTRACT:
                        options.addOption(INPUT);
                        options.addOption(TAXON_FILE);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(TIP_ATTRIBUTES);
                        options.addOption(IGNORE_MISSING);
                        break;
                    case GRAPEVINE_ASSIGN_LINEAGES:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        break;
                    case GRAPEVINE_ASSIGN_REPRESENTATIVES:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(IGNORE_MISSING);
                        options.addOption(ATTRIBUTE);
                        break;
                    case GRAPEVINE_LABEL_CLUSTERS:
                        METADATA.setRequired(false);
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        LINEAGE_FILE.setRequired(false);
                        options.addOption(LINEAGE_FILE);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        break;
                    case GRAPEVINE_SUBLINEAGES:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(MIN_CLUSTER_SIZE);
                        break;
                    case INSERT:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(UNIQUE_ONLY);
                        options.addOption(IGNORE_MISSING);
                        break;
                    case POLECAT:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(MIN_CLUSTER_SIZE);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(MIN_CLUSTER_SIZE);
                        options.addOption(MAX_CLUSTER_SIZE);
                        options.addOption(MAX_CLUSTER_AGE);
                        options.addOption(MIN_CLUSTER_RECENCY);
                        options.addOption(MAX_CLUSTER_RECENCY);
                        options.addOption(MIN_UK);
                        options.addOption(OPTIMIZE_BY);
                        options.addOption(RANK_BY);
                        options.addOption(MAX_CLUSTER_COUNT);
                        options.addOption(MAX_DIVERGENCE);
                        options.addOption(IGNORE_MISSING);
                        break;
                    case PRUNE:
                        options.addOption(INPUT);
                        options.addOption(TAXON_FILE);
                        METADATA.setRequired(false);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(KEEP_TAXA);
                        break;
                    case RECONSTRUCT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(ATTRIBUTE);
                        options.addOption(OUT_ATTRIBUTE);
                        break;
                    case REORDER:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        OptionGroup orderGroup = new OptionGroup();
                        orderGroup.addOption(INCREASING);
                        orderGroup.addOption(DECREASING);
                        orderGroup.addOption(SORT_COLUMNS);
                        options.addOptionGroup(orderGroup);
                        break;
                    case REROOT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        OptionGroup orderGroup2= new OptionGroup();
                        orderGroup2.addOption(OUTGROUPS);
                        orderGroup2.addOption(MIDPOINT);
                        options.addOption(ROOT_LOCATION);
                        options.addOptionGroup(orderGroup2);
                        break;
                    case SAMPLE:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(KEEP_TAXA);
                        break;
                    case SPLIT:
                        options.addOption(INPUT);
                        METADATA.setRequired(false);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(ATTRIBUTE);
                        break;
                    case STATISTICS:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(STATISTICS);
                        break;
                    case SUBCLUSTER:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(ATTRIBUTE);
                        options.addOption(CLUSTER_PREFIX);
                        options.addOption(MIN_CLUSTER_SIZE);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown enum value, " + command);
                }

                commandLine = parser.parse( options, Arrays.copyOfRange(args, 1, args.length));

                if (commandLine.hasOption("help")) {
                    printHelp(command, options);
                    return;
                }
                if (commandLine.hasOption("version")) {
                    System.out.println(VERSION);
                    return;
                }

            } catch (IllegalArgumentException iae) {
                System.out.println("Unrecognised command: " + args[0] + "\n");
                printHelp(command, options);
                return;
            } catch (ParseException pe) {
                System.out.println(pe.getMessage() + "\n");
                printHelp(command, options);
                return;
            }
        } else {
            try {
                commandLine = parser.parse(options, args);
            } catch (ParseException pe) {
                System.out.println(pe.getMessage() + "\n");
                printHelp(command, options);
                return;
            }

            if (commandLine.hasOption("version")) {
                System.out.println(VERSION);
                return;
            }

            printHelp(command, options);
            return;

        }

        boolean isVerbose = commandLine.hasOption("verbose");
        FormatType format = FormatType.NEXUS;

        if (commandLine.hasOption("f")) {
            try {
                format = FormatType.valueOf(commandLine.getOptionValue("f").toUpperCase());
            } catch (IllegalArgumentException iae) {
                System.out.println("Unrecognised output format: " + commandLine.getOptionValue("f") + "\n");
                printHelp(command, options);
                return;
            }
        }

        if (isVerbose) {
            System.out.println("Command: " + command);
        }

        long startTime = System.currentTimeMillis();

        switch (command) {

            case ANNOTATE:
                new Annotate(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.getOptionValues("label-fields"),
                        commandLine.getOptionValues("tip-attributes"),
                        commandLine.hasOption("replace"),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case ASSIGN:
                new Assign(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("output-metadata"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.getOptionValue("attribute"),
                        commandLine.getOptionValue("out-attribute"),
                        true,
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case CLUSTER:
                new Cluster(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("output-metadata"),
                        commandLine.getOptionValue("attribute"),
                        commandLine.getOptionValue("value"),
                        commandLine.getOptionValue("cluster-name"),
                        commandLine.getOptionValue("cluster-prefix"),
                        0,
                        isVerbose);
                break;
            case CONTEXT:
                new Context(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("taxon-file"),
                        commandLine.getOptionValues("taxa"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("prefix"),
                        format,
                        commandLine.hasOption("output-taxa"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.hasOption("mrca"),
                        Integer.parseInt(commandLine.getOptionValue("max-parent", "1")),
                        Integer.parseInt(commandLine.getOptionValue("max-child", "0")),
                        Integer.parseInt(commandLine.getOptionValue("max-siblings", "0")),
                        commandLine.getOptionValue("collapse-by", null),
                        Integer.parseInt(commandLine.getOptionValue("tip-budget", "0")),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case CONVERT:
                new Reorder(
                        commandLine.getOptionValue("input"),
                        null,
                        commandLine.getOptionValue("output"),
                        format,
                        null,
                        0,
                        null,
                        OrderType.UNCHANGED,
                        null,
                        isVerbose);
                break;
            case DIVIDE:
                new Divide(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("prefix"),
                        format,
                        Integer.parseInt(commandLine.getOptionValue("max-count", "0")),
                        Integer.parseInt(commandLine.getOptionValue("min-size", "0")),
                        commandLine.hasOption("require-outgroup"),
                        isVerbose);
                break;
            case CONQUER:
                new Conquer(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        isVerbose);
                break;
            case EXTRACT:
                new Extract(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("taxon-file"),
                        commandLine.getOptionValues("taxa"),
                        commandLine.getOptionValues("tip-attributes"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case INSERT:
                new Insert(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.hasOption("unique-only"),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case GRAPEVINE_ASSIGN_LINEAGES:
                new GrapevineAssignLineages(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("prefix"),
                        format,
                        isVerbose);
                break;
            case GRAPEVINE_ASSIGN_REPRESENTATIVES:
                new GrapevineAssignRepresentatives(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case GRAPEVINE_LABEL_CLUSTERS:
                new GrapevineLabelClusters(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("lineages"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("prefix"),
                        format,
                        isVerbose);
                break;
            case GRAPEVINE_SUBLINEAGES:
                new GrapevineSublineages(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("prefix"),
                        format,
                        Integer.parseInt(commandLine.getOptionValue("min-size", "50")),
                        isVerbose);
                break;
            case POLECAT:
                Polecat.Optimization optimization = Polecat.Optimization.MAXIMUM;
                Polecat.Criterion optimizationCriterion = Polecat.Criterion.getValue(commandLine.getOptionValue("optimize-by", "growth-rate"));
                if (optimizationCriterion == null) {
                    System.err.println("Unrecognized optimize-by criterion: " + commandLine.getOptionValue("optimize-by"));
                    System.exit(1);
                }
                Polecat.Optimization ranking = Polecat.Optimization.MAXIMUM;
                String rankBy = commandLine.getOptionValue("rank-by", "^recency");
                if (rankBy.startsWith("^")) {
                    ranking = Polecat.Optimization.MINIMUM;
                    rankBy = rankBy.substring(1);
                }
                Polecat.Criterion rankCiterion = Polecat.Criterion.getValue(rankBy);
                if (rankCiterion == null) {
                    System.err.println("Unrecognized rank-by criterion: " + commandLine.getOptionValue("rank-by"));
                    System.exit(1);
                }

                new Polecat(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        Integer.parseInt(commandLine.getOptionValue("min-size", "10")),
                        Integer.parseInt(commandLine.getOptionValue("max-size", "-1")),
                        Integer.parseInt(commandLine.getOptionValue("max-age", "90")),
                        Integer.parseInt(commandLine.getOptionValue("min-recency", "-1")),
                        Integer.parseInt(commandLine.getOptionValue("max-recency", "-1")),
                        Double.parseDouble(commandLine.getOptionValue("min-UK", "0.5")),
                        optimization,
                        optimizationCriterion,
                        ranking,
                        rankCiterion,
                        Integer.parseInt(commandLine.getOptionValue("max-count", "-1")),
                        Double.parseDouble(commandLine.getOptionValue("max-divergence", "1.5")),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case PRUNE:
                new Prune(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("taxon-file"),
                        commandLine.getOptionValues("taxa"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("output-metadata"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.hasOption("keep-taxa"),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
//            case RACCOON_DOG:
//                new RaccoonDog(
//                        commandLine.getOptionValue("input"),
//                        commandLine.getOptionValue("metadata"),
//                        commandLine.getOptionValue("output"),
//                        format,
//                        commandLine.getOptionValue("output-metadata"),
//                        commandLine.getOptionValue("id-column", null),
//                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
//                        commandLine.getOptionValue("field-delimeter", "\\|"),
//                        commandLine.getOptionValue("attribute"),
//                        commandLine.getOptionValue("value"),
//                        commandLine.getOptionValue("cluster-name"),
//                        commandLine.getOptionValue("cluster-prefix"),
//                        Integer.parseInt(commandLine.getOptionValue("max-child", "0")),
//                        isVerbose);
//                break;
            case RECONSTRUCT:
                new Reconstruct(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("attribute"),
                        commandLine.getOptionValue("attribute"),
                        isVerbose);
                break;
            case REORDER:
                OrderType orderType = (commandLine.hasOption("increasing") ?
                        OrderType.INCREASING :
                        (commandLine.hasOption("decreasing") ? OrderType.DECREASING : OrderType.UNCHANGED));
                new Reorder(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        orderType,
                        commandLine.getOptionValues("sort-by"),
                        isVerbose);
                break;
            case REROOT:
                RootType rootType = commandLine.hasOption("midpoint") ? RootType.MIDPOINT : RootType.OUTGROUP;
                double rootLocation = Double.parseDouble(commandLine.getOptionValue("root-location", "0.5"));
                new Reroot(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        Integer.parseInt(commandLine.getOptionValue("index-field", "0")),
                        commandLine.getOptionValue("field-delimeter", "\\|"),
                        rootType,
                        rootLocation,
                        commandLine.getOptionValues("outgroups"),
                        isVerbose);
                break;
            case SAMPLE:
                new Sample(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("prefix"),
                        format,
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", "\\|"),
                        1000,
                        "adm0",
                        "adm1",
                        "adm2",
                        "adm2",
                        false,
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case SPLIT:
                new Split(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValue("prefix"),
                        format,
                        commandLine.getOptionValue("output-metadata"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", DEFAULT_DELIMITER),
                        commandLine.getOptionValue("attribute"),
                        isVerbose);
                break;
            case STATISTICS:
                new Statistics(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        commandLine.getOptionValues("stats"),
                        isVerbose);
                break;
            case SUBCLUSTER:
                new Subcluster(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("output-metadata"),
                        commandLine.getOptionValue("cluster-prefix"),
                        commandLine.getOptionValue("attribute"),
                        Integer.parseInt(commandLine.getOptionValue("min-size", "10")),
                        isVerbose);
                break;
            default:
                throw new IllegalArgumentException("Unknown enum value, " + command);
        }

        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

        if (isVerbose) {
            System.err.println("Time taken: " + timeTaken + " secs");
        }

    }

}

