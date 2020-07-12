package network.artic.clusterfunk;

import org.apache.commons.cli.*;

import java.util.Arrays;

/**
 *
 */
class ClusterFunk {

    enum Command {
        NONE("", ""),
        PRUNE("prune", "Prune out subtrees based on tip annotations."),
        ANNOTATE("annotate", "Annotate tips and nodes from a metadata table.");

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

    private final static Option REPLACE_HEADERS =  Option.builder(  )
            .longOpt("replace-headers")
            .required(false)
            .desc( "replace the tip label headers rather than appending (default false)" )
            .type(String.class).build();

    private final static Option TIP_ATTRIBUTES =  Option.builder(  )
            .longOpt("tip-attributes")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc( "a list of metadata columns to add as tip attributes" )
            .type(String.class).build();

    private static final String VERSION = "v0.01a";
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

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;

        if (args.length > 0 && !args[0].startsWith("-")) {
            try {
                command = Command.valueOf(args[0].toUpperCase());

                options.addOption("h", "help", false, "display help");
                options.addOption("v", "verbose", false, "write analysis details to stderr");

                switch (command) {
                    case PRUNE:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);
                        options.addOption(OUTPUT_PREFIX);
                        options.addOption(ATTRIBUTE);
                        break;
                    case ANNOTATE:
                        options.addOption(INPUT);
                        options.addOption(OUTPUT_PATH);

                        OptionGroup metadataGroup = new OptionGroup();
                        metadataGroup.setRequired(true);
                        metadataGroup.addOption(METADATA);
                        metadataGroup.addOption(INDEX_COLUMN);
                        metadataGroup.addOption(INDEX_HEADER);
                        metadataGroup.addOption(HEADER_DELIMITER);
                        options.addOptionGroup(metadataGroup);

                        OptionGroup annotateGroup = new OptionGroup();
                        annotateGroup.setRequired(true);
                        annotateGroup.addOption(HEADER_FIELDS);
                        annotateGroup.addOption(REPLACE_HEADERS);
                        annotateGroup.addOption(TIP_ATTRIBUTES);
                        options.addOptionGroup(annotateGroup);
                        break;
                }

                commandLine = parser.parse( options, Arrays.copyOfRange(args, 1, args.length));

                if (commandLine.hasOption("h")) {
                    printHelp(command, options);
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
            printHelp(command, options);
            return;
        }

        boolean verbose = commandLine.hasOption("v");

        long startTime = System.currentTimeMillis();

        switch (command) {

            case PRUNE:
                new Prune(commandLine.getOptionValue("i"), commandLine.getOptionValue("a"), commandLine.getOptionValue("o"), commandLine.getOptionValue("p"), verbose);
                break;
            case ANNOTATE:
//                new Annotate();
                break;
        }

        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

        if (verbose) {
            System.err.println("Time taken: " + timeTaken + " secs");
        }

    }

}

