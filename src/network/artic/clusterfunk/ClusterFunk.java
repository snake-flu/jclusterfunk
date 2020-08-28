package network.artic.clusterfunk;

import network.artic.clusterfunk.commands.*;
import org.apache.commons.cli.*;

import java.util.Arrays;

/**
 * Entrypoint class with main().
 */
class ClusterFunk {

    private final static String NAME = "jclusterfunk";
    private static final String VERSION = "v0.0.4pre";
    private static final String HEADER = NAME + " " + VERSION + "\nBunch of functions for trees\n\n";
    private static final String FOOTER = "";

    enum Command {
        NONE("", ""),
        ANNOTATE("annotate", "Annotate tips and nodes from a metadata table."),
        CLUSTER("cluster", "label clusters by number based on node attributes."),
        SUBCLUSTER("subcluster", "split existing clusters into subclusters."),
        CONTEXT("context", "Extract trees of the neighbourhoods or contexts of a set of tips."),
        CONVERT("convert", "Convert tree from one format to another."),
        INSERT("insert", "Insert tips into the tree."),
        GRAPEVINE_ASSIGN_LINEAGES("grapevine-assign-lineages", "Assign UK tips without lineages to a UK lineage."),
        GRAPEVINE_SUBLINEAGES("grapevine-sublineages", "split existing UK lineages into sub-lineages."),
        PRUNE("prune", "Prune out taxa from a list or based on metadata."),
        RACCOON_DOG("raccoon-dog", "CoG-UK lineage designations."),
        RECONSTRUCT("reconstruct", "Reconstruct internal node annotations."),
        REORDER("reorder", "Re-order nodes in ascending or descending clade size."),
//        REROOT("reroot", "Re-root the tree using an outgroup."),
        SPLIT("split", "Split out subtrees based on tip annotations."),
        STATISTICS("statistics", "Extract statistics and information from trees.");

        Command(final String name, final String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Command getCommand(String name) {
            for (Command command : values()) {
                if (name.equalsIgnoreCase(command.getName())) {
                    return command;
                }
            }
            throw new IllegalArgumentException("Command not found");
        }

        private final String name;
        private final String description;
    }

    private final static Option INPUT = Option.builder( "i" )
            .longOpt("input")
            .argName("file")
            .hasArg()
            .required(true)
            .desc( "input tree file" )
            .type(String.class).build();

    private final static Option METADATA = Option.builder( "m" )
            .longOpt("metadata")
            .argName("file")
            .hasArg()
            .required(true)
            .desc( "input metadata file" )
            .type(String.class).build();

    private final static Option TAXON_FILE = Option.builder( )
            .longOpt("taxon-file")
            .argName("file")
            .hasArg()
            .required(false)
            .desc( "file of taxa (in a CSV table or tree)" )
            .type(String.class).build();

    private final static Option TAXA =  Option.builder( "t" )
            .longOpt("taxa")
            .argName("taxon-ids")
            .hasArgs()
            .required(false)
            .desc( "a list of taxon ids" )
            .type(String.class).build();

    private final static Option INDEX_COLUMN = Option.builder( "c" )
            .longOpt("id-column")
            .argName("column name")
            .hasArg()
            .required(false)
            .desc( "metadata column to use to match tip labels (default first column)" )
            .type(String.class).build();

    private final static Option INDEX_FIELD = Option.builder(  )
            .longOpt("id-field")
            .argName("field number")
            .hasArg()
            .required(false)
            .desc( "tip label field to use to match metadata (default = whole label)" )
            .type(Integer.class).build();

    private final static Option HEADER_DELIMITER = Option.builder(  )
            .longOpt("field-delimiter")
            .argName("delimiter")
            .hasArg()
            .required(false)
            .desc( "the delimiter used to specify fields in the tip labels (default = '|')" )
            .type(String.class).build();

    private final static Option OUTPUT_FILE = Option.builder( "o" )
            .longOpt("output")
            .argName("file")
            .hasArg()
            .required(true)
            .desc( "output file" )
            .type(String.class).build();

    private final static Option OUTPUT_PATH = Option.builder( "o" )
            .longOpt("output")
            .argName("path")
            .hasArg()
            .required(false)
            .desc( "output path" )
            .type(String.class).build();

    private final static Option OUTPUT_PREFIX = Option.builder( "p" )
            .longOpt("prefix")
            .argName("file_prefix")
            .hasArg()
            .required(true)
            .desc( "output file prefix" )
            .type(String.class).build();

    private final static Option OUTPUT_FORMAT = Option.builder( "f" )
            .longOpt("format")
            .argName("nexus|newick")
            .hasArg()
            .required(false)
            .desc( "output file format (nexus or newick)" )
            .type(String.class).build();

    private final static Option OUTPUT_METADATA = Option.builder( "d" )
            .longOpt("output-metadata")
            .argName("file")
            .hasArg()
            .required(false)
            .desc( "output a metadata file to match the output tree" )
            .type(String.class).build();

    private final static Option ATTRIBUTE =  Option.builder( )
            .longOpt("attribute")
            .argName("attribute_name")
            .hasArg()
            .required(true)
            .desc( "the attribute name" )
            .type(String.class).build();

    private final static Option VALUE =  Option.builder( )
            .longOpt("value")
            .argName("attribute_value")
            .hasArg()
            .required(true)
            .desc( "the attribute value" )
            .type(String.class).build();

    private final static Option CLUSTER_NAME =  Option.builder( )
            .longOpt("cluster-name")
            .argName("name")
            .hasArg()
            .required(true)
            .desc( "the cluster name" )
            .type(String.class).build();

    private final static Option CLUSTER_PREFIX =  Option.builder( )
            .longOpt("cluster-prefix")
            .argName("prefix")
            .hasArg()
            .required(false)
            .desc( "the cluster prefix (default = just a number)" )
            .type(String.class).build();

    private final static Option LABEL_FIELDS =  Option.builder(  )
            .longOpt("label-fields")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc( "a list of metadata columns to add as tip label fields" )
            .type(String.class).build();

    private final static Option TIP_ATTRIBUTES =  Option.builder(  )
            .longOpt("tip-attributes")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc( "a list of metadata columns to add as tip attributes" )
            .type(String.class).build();

    private final static Option MAX_PARENT_LEVEL = Option.builder(  )
            .longOpt("max-parent")
            .argName("level")
            .hasArg()
            .required(false)
            .desc( "maximum parent level to include in context trees (default = 1)" )
            .type(Integer.class).build();

    private final static Option MAX_CHILD_LEVEL = Option.builder(  )
            .longOpt("max-child")
            .argName("level")
            .hasArg()
            .required(false)
            .desc( "maximum level of children to include in subtrees (default = unlimited)" )
            .type(Integer.class).build();

    private final static Option MAX_SIBLING = Option.builder(  )
            .longOpt("max-siblings")
            .argName("level")
            .hasArg()
            .required(false)
            .desc( "maximum number of siblings to include in subtrees (default = unlimited)" )
            .type(Integer.class).build();

    private final static Option MIN_CLUSTER_SIZE = Option.builder(  )
            .longOpt("min-size")
            .argName("size")
            .hasArg()
            .required(false)
            .desc( "minimum number of tips in a subcluster (default = 10)" )
            .type(Integer.class).build();

    private final static Option MIDPOINT =  Option.builder( )
            .longOpt("midpoint")
            .required(false)
            .desc( "midpoint root the tree" )
            .type(String.class).build();

    private final static Option OUTGROUPS =  Option.builder(  )
            .longOpt("outgroups")
            .argName("tips")
            .hasArgs()
            .required(false)
            .desc( "a list of tips to use as an outgroup for re-rooting" )
            .type(String.class).build();

    private final static Option INCREASING =  Option.builder(  )
            .longOpt("increasing")
            .desc( "order nodes by increasing clade size" )
            .type(String.class).build();

    private final static Option DECREASING =  Option.builder( )
            .longOpt("decreasing")
            .desc( "order nodes by decreasing clade size" )
            .type(String.class).build();

    private final static Option REPLACE =  Option.builder( "r" )
            .longOpt("replace")
            .required(false)
            .desc( "replace the annotations or tip label headers rather than appending (default false)" )
            .type(String.class).build();

    private final static Option STATISTICS =  Option.builder( )
            .longOpt("stats")
            .required(true)
            .desc( "a list of statistics to include in the output (see docs for details)" )
            .type(String.class).build();

    private final static Option IGNORE_MISSING =  Option.builder( )
            .longOpt("ignore-missing")
            .required(false)
            .desc( "ignore any missing matches in annotations table (default false)" )
            .type(String.class).build();

    private final static Option UNIQUE_ONLY =  Option.builder( )
            .longOpt("unique-only")
            .required(false)
            .desc( "only place tips that have an unique position (default false)" )
            .type(String.class).build();

    private final static Option KEEP_TAXA =  Option.builder( "k" )
            .longOpt("keep-taxa")
            .required(false)
            .desc( "keep only the taxa specifed (default false)" )
            .type(String.class).build();

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
                        OptionGroup annotateGroup = new OptionGroup();
                        annotateGroup.addOption(LABEL_FIELDS);
                        annotateGroup.addOption(TIP_ATTRIBUTES);
                        options.addOptionGroup(annotateGroup);
                        options.addOption(REPLACE);
                        options.addOption(IGNORE_MISSING);
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
                    case CONTEXT:
                        options.addOption(INPUT);
                        options.addOption(TAXON_FILE);
                        options.addOption(TAXA);
                        METADATA.setRequired(false);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(MAX_PARENT_LEVEL);
                        options.addOption(MAX_CHILD_LEVEL);
                        options.addOption(MAX_SIBLING);
                        options.addOption(IGNORE_MISSING);
                        break;
                    case CONVERT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        break;
                    case GRAPEVINE_ASSIGN_LINEAGES:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_FIELD);
                        options.addOption(HEADER_DELIMITER);
                        break;
                    case GRAPEVINE_SUBLINEAGES:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
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
                    case RACCOON_DOG:
                        options.addOption(INPUT);
                        options.addOption(METADATA);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(OUTPUT_METADATA);
//                        options.addOption(ATTRIBUTE);
//                        options.addOption(VALUE);
//                        options.addOption(CLUSTER_NAME);
//                        options.addOption(CLUSTER_PREFIX);
//                        options.addOption(MAX_CHILD_LEVEL);
                        break;
                    case RECONSTRUCT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(TIP_ATTRIBUTES);
                        break;
                    case REORDER:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_FILE);
                        options.addOption(OUTPUT_FORMAT);
                        OptionGroup orderGroup = new OptionGroup();
                        orderGroup.addOption(INCREASING);
                        orderGroup.addOption(DECREASING);
                        options.addOptionGroup(orderGroup);
                        break;
//                    case REROOT:
//                        options.addOption(INPUT);
//                        options.addOption(OUTPUT_FILE);
//                        options.addOption(OUTPUT_FORMAT);
//                        options.addOption(INDEX_HEADER);
//                        options.addOption(HEADER_DELIMITER);
//                        OptionGroup orderGroup2= new OptionGroup();
//                        orderGroup2.addOption(OUTGROUPS);
//                        orderGroup2.addOption(MIDPOINT);
//                        options.addOptionGroup(orderGroup2);
//                        break;
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
                        commandLine.getOptionValue("field-delimeter", "\\|"),
                        commandLine.getOptionValues("label-fields"),
                        commandLine.getOptionValues("tip-attributes"),
                        commandLine.hasOption("replace"),
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
                        commandLine.getOptionValue("output-metadata"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", "\\|"),
                        Integer.parseInt(commandLine.getOptionValue("max-parent", "1")),
                        Integer.parseInt(commandLine.getOptionValue("max-child", "0")),
                        Integer.parseInt(commandLine.getOptionValue("max-siblings", "0")),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case CONVERT:
                new Reorder(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        OrderType.UNCHANGED,
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
                        commandLine.getOptionValue("field-delimeter", "\\|"),
                        commandLine.hasOption("unique-only"),
                        commandLine.hasOption("ignore-missing"),
                        isVerbose);
                break;
            case GRAPEVINE_ASSIGN_LINEAGES:
                new GrapevineAssignLineages(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("metadata"),
                        commandLine.getOptionValue("output"),
                        format,
                        commandLine.getOptionValue("output-metadata"),
                        commandLine.getOptionValue("id-column", null),
                        Integer.parseInt(commandLine.getOptionValue("id-field", "0")),
                        commandLine.getOptionValue("field-delimeter", "\\|"),
                        commandLine.getOptionValue("attribute"),
                        commandLine.getOptionValue("value"),
                        commandLine.getOptionValue("cluster-name"),
                        commandLine.getOptionValue("cluster-prefix"),
                        isVerbose);
                break;
            case GRAPEVINE_SUBLINEAGES:
                new GrapevineSublineages(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        Integer.parseInt(commandLine.getOptionValue("min-size", "50")),
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
                        commandLine.getOptionValue("field-delimeter", "\\|"),
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
                        commandLine.getOptionValues("tip-attributes"),
                        isVerbose);
                break;
            case REORDER:
                OrderType orderType = commandLine.hasOption("increasing") ? OrderType.INCREASING : OrderType.DECREASING;
                new Reorder(
                        commandLine.getOptionValue("input"),
                        commandLine.getOptionValue("output"),
                        format,
                        orderType,
                        isVerbose);
                break;
//            case REROOT:
//                RootType rootType = commandLine.hasOption("midpoint") ? RootType.MIDPOINT : RootType.OUTGROUP;
//                new Reroot(
//                        commandLine.getOptionValue("input"),
//                        commandLine.getOptionValue("output"),
//                        format,
//                        Integer.parseInt(commandLine.getOptionValue("index-field", "0")),
//                        commandLine.getOptionValue("field-delimeter", "\\|"),
//                        rootType,
//                        commandLine.getOptionValues("outgroups"),
//                        isVerbose);
//                break;
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
                        commandLine.getOptionValue("field-delimeter", "\\|"),
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

