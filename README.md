# jclusterfunk

###

The general command line for running jclusterfunk is:

`jclusterfunk <command> [options]`

### commands

| command         | description                                                                                                                                                                                                                          |
|:----------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `annotate`      | Take data fields from a metadata file and apply either to the tip labels of the tree or as annotations as used by [FigTree](http://tree.bio.ed.ac.uk/software/figtree).                                                              |
| `cluster`       | Finds and annotates monophyletic clusters of taxa have the specified annotation value. The cluster number is an incrementing value.                                                                                                  |
| `collapse`      | Collapses branches less than a threshold into a polytomy.                                                                                                                                                                            |
| `context`       | Extracts subtrees that are close ancestors, siblings or children of a set of tip.                                                                                                                                                    |
| `convert`       | Convert the tree from one format to another without changing it.                                                                                                                                                                     |
| `divide`        | Divides up a tree into roughly equal sized subtrees.                                                                                                                                                                                 |
| `extract`       | Extracts metadata fields from the tips of a tree.                                                                                                                                                                                    |
| `insert`        | Replaces a tip in a tree with a polytomy of specified taxa.                                                                                                                                                                          |
| `merge`         | Merges two metadata tables based on an index column (usually taxon names).                                                                                                                                                           |
| `prune`         | Prune out sets of tips from a tree.                                                                                                                                                                                                  |
| `Reconstructs`  | Reconstructs annotation values at internal nodes using parsimony.                                                                                                                                                                    |
| `reorder`       | Reorder branches at each node to have increasing or decreasing numbers of child nodes.                                                                                                                                               |
| `reroot`        | Reroot the tree using an outgroup or at the midpoint.                                                                                                                                                                                |
| `sample`        | Sample taxa down using metadata attributes.                                                                                                                                                                                                                                    |
| `scale`         | Scales branch lengths of a tree by a factor or to a specified root height.                                                                                                                                                           |
| `split`         | Split the tree into subtrees defined by annotations of the tips or the nodes.                                                                                                                                                        |
| `statistics`    | Writes out a list of statistics and information about a tree.                                                                                                                                                                        |
| `tmrca`         | Finds the time of most recent common ancestor of a set of taxa.                                                                                                                                                                      |

annotate assign cluster collapse extract subcluster conquer context
convert divide insert merge prune reconstruct reorder reroot sample scale
split statistics tmrca

### general options

`-h` / `--help` List the available options and stop. Combine with a command to get help for that command.

`--version` Print the version number and stop.

`-v` / `--verbose` Print extended information about analysis performed.

`-i` / `--input <filename>` Specify the input tree file.

`-m` / `--metadata <filename>` Specify a metadata table in CSV format where required.

`-t` / `--taxa <filename>` Specify a list of taxa in CSV format or as a tree where required.

`-o` / `--output <output_path>` Output filename or path to a directory if multiple output files will be produced.

`-f` / `--format <nexus|newick>` Output tree file format (nexus or newick)

`-p` / `--prefix <file_prefix>` Output file prefix when multiple output files are produced.

### taxa matching options

`-c` / `--index-column <column name>` Metadata column to use to match tip labels (default first column)

`--index-field <field number>` The tip label field to use to match metadata rows indexed from 1 (default = whole label)

`--field-delimiter <delimiter>` The delimiter used to specify fields in the tip labels (default `|`)

### command specific options

#### `annotate`

`--label-fields <columns>` A list of metadata columns to add as tip label header fields.

`--tip-attributes <columns>` A list of metadata columns to add as tip attributes.

`--ignore-missing` Ignore any tips that don't have a match in the annotations table (default: stop and report missing metadata).

`--replace` Replace the existing annotations or tip labels rather than appending (default: append).

#### `context`

#### `prune`

`-k` `--keep-taxa` Keep the taxa specifed (default: prune specified taxa)

#### `reorder`

`--decreasing` Order nodes by decreasing clade size.
`--increasing` Order nodes by increasing clade size.

#### `reroot`

`--outgroup <outgroup taxa>` Root tree using specified outgroup
`--midpoint` Root tree at the branch-length midpoint.

#### `split`

`--attribute <attribute>`

## Installation

The easiest way to install is using `conda`:

`conda install cov-ert::jclusterfunk`

Alternatively, the download the latest binary:
https://github.com/cov-ert/jclusterfunk/releases/latest

This contains two files:
`jclusterfunk` an executable shell file
`jclusterfunk.jar` the Java jar file

Both of these can be copies to a `bin` directory on the path such as `/usr/local/bin` or `~/bin`
