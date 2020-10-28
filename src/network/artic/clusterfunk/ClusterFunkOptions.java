package network.artic.clusterfunk;

import network.artic.clusterfunk.commands.Polecat;
import org.apache.commons.cli.Option;

/**
 * @author Andrew Rambaut
 * @version $
 */
class ClusterFunkOptions {

    enum Command {
        NONE("", ""),
        ANNOTATE("annotate", "Annotate tips and nodes from a metadata table."),
        ASSIGN("assign", "Clean and assign lineage annotations."),
        CLUSTER("cluster", "label clusters by number based on node attributes."),
        SUBCLUSTER("subcluster", "split existing clusters into subclusters."),
        CONTEXT("context", "Extract trees of the neighbourhoods or contexts of a set of tips."),
        CONVERT("convert", "Convert tree from one format to another."),
        DIVIDE("divide", "Divide tree into approximately equal sized subtrees."),
        INSERT("insert", "Insert tips into the tree."),
        JOIN("join", "Join up previously divided subtrees."),
        GRAPEVINE_ASSIGN_LINEAGES("grapevine-assign-lineages", "Assign UK tips without lineages to a UK lineage."),
        GRAPEVINE_ASSIGN_HAPLOTYPES("grapevine-assign-haplotypes", "Assign haplotype names to internal nodes."),
        GRAPEVINE_SUBLINEAGES("grapevine-sublineages", "split existing UK lineages into sub-lineages."),
        POLECAT("polecat", "Write out stats for all clusters"),
        PRUNE("prune", "Prune out taxa from a list or based on metadata."),
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

    final static Option INPUT = Option.builder("i")
            .longOpt("input")
            .argName("file")
            .hasArg()
            .required(true)
            .desc("input tree file")
            .type(String.class).build();

    final static Option INPUT_PATH = Option.builder("i")
            .longOpt("input")
            .argName("path")
            .hasArg()
            .required(true)
            .desc("input path")
            .type(String.class).build();

    final static Option METADATA = Option.builder("m")
            .longOpt("metadata")
            .argName("file")
            .hasArg()
            .required(true)
            .desc("input metadata file")
            .type(String.class).build();

    final static Option TAXON_FILE = Option.builder()
            .longOpt("taxon-file")
            .argName("file")
            .hasArg()
            .required(false)
            .desc("file of taxa (in a CSV table or tree)")
            .type(String.class).build();

    final static Option TAXA = Option.builder("t")
            .longOpt("taxa")
            .argName("taxon-ids")
            .hasArgs()
            .required(false)
            .desc("a list of taxon ids")
            .type(String.class).build();

    final static Option INDEX_COLUMN = Option.builder("c")
            .longOpt("id-column")
            .argName("column name")
            .hasArg()
            .required(false)
            .desc("metadata column to use to match tip labels (default first column)")
            .type(String.class).build();

    final static Option INDEX_FIELD = Option.builder()
            .longOpt("id-field")
            .argName("field number")
            .hasArg()
            .required(false)
            .desc("tip label field to use to match metadata (default = whole label)")
            .type(Integer.class).build();

    final static Option HEADER_DELIMITER = Option.builder()
            .longOpt("field-delimiter")
            .argName("delimiter")
            .hasArg()
            .required(false)
            .desc("the delimiter used to specify fields in the tip labels (default = '|')")
            .type(String.class).build();

    final static Option OUTPUT_FILE = Option.builder("o")
            .longOpt("output")
            .argName("file")
            .hasArg()
            .required(true)
            .desc("output file")
            .type(String.class).build();

    final static Option OUTPUT_PATH = Option.builder("o")
            .longOpt("output")
            .argName("path")
            .hasArg()
            .required(false)
            .desc("output path")
            .type(String.class).build();

    final static Option OUTPUT_PREFIX = Option.builder("p")
            .longOpt("prefix")
            .argName("file_prefix")
            .hasArg()
            .required(true)
            .desc("output file prefix")
            .type(String.class).build();

    final static Option OUTPUT_FORMAT = Option.builder("f")
            .longOpt("format")
            .argName("nexus|newick")
            .hasArg()
            .required(false)
            .desc("output file format (nexus or newick)")
            .type(String.class).build();

    final static Option OUTPUT_METADATA = Option.builder("d")
            .longOpt("output-metadata")
            .argName("file")
            .hasArg()
            .required(false)
            .desc("output a metadata file to match the output tree")
            .type(String.class).build();

    final static Option OUTPUT_TAXA = Option.builder()
            .longOpt("output-taxa")
            .required(false)
            .desc("output a text file of taxon names to match each output tree")
            .type(String.class).build();

    final static Option ATTRIBUTE = Option.builder()
            .longOpt("attribute")
            .argName("attribute_name")
            .hasArg()
            .required(true)
            .desc("the attribute name")
            .type(String.class).build();

    final static Option VALUE = Option.builder()
            .longOpt("value")
            .argName("attribute_value")
            .hasArg()
            .required(true)
            .desc("the attribute value")
            .type(String.class).build();

    final static Option CLUSTER_NAME = Option.builder()
            .longOpt("cluster-name")
            .argName("name")
            .hasArg()
            .required(true)
            .desc("the cluster name")
            .type(String.class).build();

    final static Option CLUSTER_PREFIX = Option.builder()
            .longOpt("cluster-prefix")
            .argName("prefix")
            .hasArg()
            .required(false)
            .desc("the cluster prefix (default = just a number)")
            .type(String.class).build();

    final static Option OUT_ATTRIBUTE = Option.builder()
            .longOpt("out-attribute")
            .argName("name")
            .hasArg()
            .required(true)
            .desc("the new attribute name in output")
            .type(String.class).build();

    final static Option LABEL_FIELDS = Option.builder()
            .longOpt("label-fields")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc("a list of metadata columns to add as tip label fields")
            .type(String.class).build();

    final static Option TIP_ATTRIBUTES = Option.builder()
            .longOpt("tip-attributes")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc("a list of metadata columns to add as tip attributes")
            .type(String.class).build();

    final static Option MRCA = Option.builder()
            .longOpt("mrca")
            .required(false)
            .desc("include the entire clade from the MRCA of the target taxa")
            .type(String.class).build();

    final static Option MAX_PARENT_LEVEL = Option.builder()
            .longOpt("max-parent")
            .argName("level")
            .hasArg()
            .required(false)
            .desc("maximum parent level to include in context trees (default = 1)")
            .type(Integer.class).build();

    final static Option MAX_CHILD_LEVEL = Option.builder()
            .longOpt("max-child")
            .argName("level")
            .hasArg()
            .required(false)
            .desc("maximum level of children to include in subtrees (default = unlimited)")
            .type(Integer.class).build();

    final static Option MAX_SIBLING = Option.builder()
            .longOpt("max-siblings")
            .argName("level")
            .hasArg()
            .required(false)
            .desc("maximum number of siblings to include in subtrees (default = unlimited)")
            .type(Integer.class).build();

    final static Option COLLAPSE_BY = Option.builder()
            .longOpt("collapse-by")
            .argName("attribute_name")
            .hasArg()
            .required(false)
            .desc("an attribute to collapse children by")
            .type(String.class).build();

    final static Option MIN_SUBTREE_SIZE = Option.builder()
            .longOpt("min-size")
            .argName("size")
            .hasArg()
            .required(false)
            .desc("minimum number of tips in a subtree")
            .type(Integer.class).build();

    final static Option MAX_SUBTREE_COUNT = Option.builder()
            .longOpt("max-count")
            .argName("count")
            .hasArg()
            .required(false)
            .desc("maximum number of subtrees")
            .type(Integer.class).build();

    final static Option MIDPOINT = Option.builder()
            .longOpt("midpoint")
            .required(false)
            .desc("midpoint root the tree")
            .type(String.class).build();

    final static Option OUTGROUPS = Option.builder()
            .longOpt("outgroups")
            .argName("tips")
            .hasArgs()
            .required(false)
            .desc("a list of tips to use as an outgroup for re-rooting")
            .type(String.class).build();

    final static Option INCREASING = Option.builder()
            .longOpt("increasing")
            .desc("order nodes by increasing clade size")
            .type(String.class).build();

    final static Option DECREASING = Option.builder()
            .longOpt("decreasing")
            .desc("order nodes by decreasing clade size")
            .type(String.class).build();

    final static Option SORT_COLUMNS = Option.builder()
            .longOpt("sort-by")
            .argName("columns")
            .hasArgs()
            .required(false)
            .desc("a list of metadata columns to sort by (prefix by ^ to reverse order)")
            .type(String.class).build();

    final static Option REPLACE = Option.builder("r")
            .longOpt("replace")
            .required(false)
            .desc("replace the annotations or tip label headers rather than appending (default false)")
            .type(String.class).build();

    final static Option STATISTICS = Option.builder()
            .longOpt("stats")
            .required(true)
            .desc("a list of statistics to include in the output (see docs for details)")
            .type(String.class).build();

    final static Option IGNORE_MISSING = Option.builder()
            .longOpt("ignore-missing")
            .required(false)
            .desc("ignore any missing matches in annotations table (default false)")
            .type(String.class).build();

    final static Option UNIQUE_ONLY = Option.builder()
            .longOpt("unique-only")
            .required(false)
            .desc("only place tips that have an unique position (default false)")
            .type(String.class).build();

    final static Option KEEP_TAXA = Option.builder("k")
            .longOpt("keep-taxa")
            .required(false)
            .desc("keep only the taxa specifed (default false)")
            .type(String.class).build();

    // polecat cluster stats options
    final static Option MIN_CLUSTER_SIZE = Option.builder()
            .longOpt("min-size")
            .argName("size")
            .hasArg()
            .required(false)
            .desc("minimum number of tips in a subcluster (default = 10)")
            .type(Integer.class).build();

    final static Option MAX_CLUSTER_SIZE = Option.builder()
            .longOpt("max-size")
            .argName("size")
            .hasArg()
            .required(false)
            .desc("maximum number of tips in a subcluster (default = none)")
            .type(Integer.class).build();

    final static Option MAX_CLUSTER_AGE = Option.builder()
            .longOpt("max-age")
            .argName("days")
            .hasArg()
            .required(false)
            .desc("maximum age of a cluster")
            .type(Integer.class).build();

    final static Option MAX_CLUSTER_RECENCY = Option.builder()
            .longOpt("max-recency")
            .argName("days")
            .hasArg()
            .required(false)
            .desc("maximum recency of a cluster")
            .type(Integer.class).build();

    final static Option MIN_UK = Option.builder()
            .longOpt("min-UK")
            .argName("proportion")
            .hasArg()
            .required(false)
            .desc("minimum proportion of UK tips")
            .type(Integer.class).build();

    final static Option OPTIMIZE_BY = Option.builder()
            .longOpt("optimize-by")
            .argName("statistic")
            .hasArg()
            .required(false)
            .desc("Cluster statistic to optimize across ancestry by. Options: " + Polecat.Criterion.getValuesString())
            .type(String.class).build();

    final static Option RANK_BY = Option.builder()
            .longOpt("rank-by")
            .argName("statistic")
            .hasArg()
            .required(false)
            .desc("cluster statistic to rank clusters by (append ^ to sort up). Options: " + Polecat.Criterion.getValuesString())
            .type(String.class).build();

    final static Option MAX_CLUSTER_COUNT = Option.builder()
            .longOpt("max-count")
            .argName("count")
            .hasArg()
            .required(false)
            .desc("maximum number of clusters to report (default = all)")
            .type(Integer.class).build();

    final static Option MAX_DIVERGENCE = Option.builder()
            .longOpt("max-divergence")
            .argName("divergence")
            .hasArg()
            .required(false)
            .desc("maximum divergence to include in counts/stats (default = 1.5)")
            .type(Double.class).build();
}

