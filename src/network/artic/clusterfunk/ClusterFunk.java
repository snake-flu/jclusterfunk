package network.artic.clusterfunk;

import org.apache.commons.cli.*;

import java.util.Arrays;

/**
 *
 */
class ClusterFunk {

    enum Command {
        NONE("", ""),
        ANNOTATE("annotate", "Annotate tips and nodes from a metadata table."),
        CONTEXT("context", "Extract trees of the neighbourhoods or contexts of a set of tips."),
        CONVERT("convert", "Convert tree from one format to another."),
        PRUNE("prune", "Prune out taxa from a list or based on metadata."),
        REORDER("reorder", "Re-order nodes in ascending or descending clade size."),
        REROOT("reroot", "Re-root the tree using an outgroup."),
        SPLIT("split", "Split out subtrees based on tip annotations.");

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

    private final static Option INDEX_COLUMN = Option.builder( "c" )
            .longOpt("index-column")
            .argName("column name")
            .hasArg()
            .required(false)
            .desc( "metadata column to use to match tip labels (default first column)" )
            .type(String.class).build();

    private final static Option INDEX_HEADER = Option.builder( "h" )
            .longOpt("index-header")
            .argName("header number")
            .hasArg()
            .required(false)
            .desc( "tip label header to use to match metadata (default = 0)" )
            .type(Integer.class).build();

    private final static Option HEADER_DELIMITER = Option.builder( "d" )
            .longOpt("header-delimited")
            .argName("delimiter")
            .hasArg()
            .required(false)
            .desc( "tip label header delimiter (default = '|')" )
            .type(String.class).build();

    private final static Option OUTPUT_PATH = Option.builder( "o" )
            .longOpt("output")
            .argName("output_path")
            .hasArg()
            .required(true)
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

    private final static Option ATTRIBUTE =  Option.builder( )
            .longOpt("attribute")
            .argName("attribute")
            .hasArg()
            .required(true)
            .desc( "the attribute name" )
            .type(String.class).build();

    private final static Option HEADER_FIELDS =  Option.builder(  )
            .longOpt("header-fields")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc( "a list of metadata columns to add as tip label header fields" )
            .type(String.class).build();

    private final static Option TIP_ATTRIBUTES =  Option.builder(  )
            .longOpt("tip-attributes")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc( "a list of metadata columns to add as tip attributes" )
            .type(String.class).build();

    private final static Option OUTGROUPS =  Option.builder(  )
            .longOpt("outgroups")
            .argName("tips")
            .hasArgs()
            .required(true)
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

    private static final String VERSION = "v0.0.1";
    private static final String HEADER = "\nClusterFunk " + VERSION + "\nBunch of functions for trees\n\n";
    private static final String FOOTER = "";

    private static void printHelp(Command command, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        StringBuilder sb = new StringBuilder();
        sb.append(ClusterFunk.HEADER);

        if (command == Command.NONE) {
            sb.append("Available commands:\n ");
            for (Command c : Command.values()) {
                sb.append(" ");
                sb.append(c);
            }
            sb.append("\n\nuse: <command> -h,--help to display individual options\n");

            formatter.printHelp("clusterfunk <command> <options> [-h]", sb.toString(), options, ClusterFunk.FOOTER, false);
        } else {
            sb.append("Command: " + command + "\n\n");
            sb.append(command.getDescription() + "\n\n");
            formatter.printHelp("clusterfunk " + command, sb.toString(), options, ClusterFunk.FOOTER, true);
        }

    }

    public static void main(String[] args) {

        Command command = Command.NONE;

        // create Options object
        Options options = new Options();
        options.addOption("h", "help", false, "display help");
        options.addOption("v", "version", false, "display version");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;

        if (args.length > 0 && !args[0].startsWith("-")) {
            try {
                command = Command.valueOf(args[0].toUpperCase());

                options.addOption("verbose", false, "write analysis details to stderr");

                switch (command) {
                    case ANNOTATE:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_HEADER);
                        options.addOption(HEADER_DELIMITER);
                        OptionGroup annotateGroup = new OptionGroup();
                        annotateGroup.addOption(HEADER_FIELDS);
                        annotateGroup.addOption(TIP_ATTRIBUTES);
                        options.addOptionGroup(annotateGroup);
                        options.addOption(REPLACE);
                        break;
                    case CONTEXT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_FORMAT);
                        break;
                    case CONVERT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_FORMAT);
                        break;
                    case PRUNE:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(METADATA);
                        options.addOption(INDEX_COLUMN);
                        options.addOption(INDEX_HEADER);
                        options.addOption(HEADER_DELIMITER);
                        break;
                    case REORDER:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_FORMAT);
                        OptionGroup orderGroup = new OptionGroup();
                        orderGroup.addOption(INCREASING);
                        orderGroup.addOption(DECREASING);
                        options.addOptionGroup(orderGroup);
                        break;
                    case REROOT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(INDEX_HEADER);
                        options.addOption(HEADER_DELIMITER);
                        options.addOption(OUTGROUPS);
                        break;
                    case SPLIT:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(OUTPUT_FORMAT);
                        options.addOption(ATTRIBUTE);
                        break;
                }

                commandLine = parser.parse( options, Arrays.copyOfRange(args, 1, args.length));

                if (commandLine.hasOption("h")) {
                    printHelp(command, options);
                    return;
                }
                if (commandLine.hasOption("v")) {
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

            if (commandLine.hasOption("v")) {
                System.out.println(VERSION);
                return;
            }

            printHelp(command, options);
            return;

        }

        boolean isVerbose = commandLine.hasOption("verbose");
        Format format = Format.NEXUS;

        if (commandLine.hasOption("f")) {
            try {
                format = Format.valueOf(commandLine.getOptionValue("f").toUpperCase());
            } catch (IllegalArgumentException iae) {
                System.out.println("Unrecognised output format: " + commandLine.getOptionValue("f") + "\n");
                printHelp(command, options);
                return;
            }
        }

        long startTime = System.currentTimeMillis();

        switch (command) {

            case ANNOTATE:
                new Annotate(
                        commandLine.getOptionValue("i"),
                        commandLine.getOptionValue("m"),
                        commandLine.getOptionValue("o"),
                        format,
                        commandLine.getOptionValue("index-column", null),
                        Integer.parseInt(commandLine.getOptionValue("index-header", "0")),
                        commandLine.getOptionValue("header-delimeter", "|"),
                        commandLine.getOptionValues("header-fields"),
                        commandLine.getOptionValues("tip-attributes"),
                        Boolean.parseBoolean(commandLine.getOptionValue("replace", "false")),
                        isVerbose);
                break;
            case CONTEXT:
//                new Context(
//                        commandLine.getOptionValue("i"),
//                        commandLine.getOptionValue("o"),
//                        format,
//                        isVerbose);
                break;
            case CONVERT:
//                new Convert(
//                        commandLine.getOptionValue("i"),
//                        commandLine.getOptionValue("o"),
//                        format,
//                        isVerbose);
                break;
            case PRUNE:
                new Prune(
                        commandLine.getOptionValue("i"),
                        commandLine.getOptionValue("m"),
                        commandLine.getOptionValue("o"),
                        format,
                        commandLine.getOptionValue("index-column", null),
                        Integer.parseInt(commandLine.getOptionValue("index-header", "0")),
                        commandLine.getOptionValue("header-delimeter", "|"),
                        Boolean.parseBoolean(commandLine.getOptionValue("replace", "false")),
                        isVerbose);
                break;
            case REORDER:
//                new Reorder(
//                        commandLine.getOptionValue("i"),
//                        commandLine.getOptionValue("o"),
//                        format,
//                        Integer.parseInt(commandLine.getOptionValue("index-header", "0")),
//                        commandLine.getOptionValue("header-delimeter", "|"),
//                        commandLine.getOptionValues("outgroups"),
//                        isVerbose);
                break;
            case REROOT:
                new Reroot(
                        commandLine.getOptionValue("i"),
                        commandLine.getOptionValue("o"),
                        format,
                        Integer.parseInt(commandLine.getOptionValue("index-header", "0")),
                        commandLine.getOptionValue("header-delimeter", "|"),
                        commandLine.getOptionValues("outgroups"),
                        isVerbose);
                break;
            case SPLIT:
                new Split(
                        commandLine.getOptionValue("i"),
                        commandLine.getOptionValue("a"),
                        commandLine.getOptionValue("o"),
                        commandLine.getOptionValue("p"),
                        isVerbose);
                break;
        }

        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

        if (isVerbose) {
            System.err.println("Time taken: " + timeTaken + " secs");
        }

    }

}
