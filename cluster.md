## Labelling the clusters (internal nodes)

```
jclusterfunk grapevine-label-clusters -i cog_global_2020-11-28_tree.nexus -o ./ -p cog_global_2020-11-28_labelled
```

This takes an annotated tree, `cog_global_2020-10-28_tree.nexus`, and creates a new tree called `cog_global_2020-11-28_labelled_tree.nexus` plus a list of clusters and representative taxa called `cog_global_2020-11-28_labelled_clusters.csv`. 

This file will look like this:
```
cluster_id,representative,depth,reserve0,reserve_depth0,reserve1,reserve_depth1,reserve2,reserve_depth2,reserve3,reserve_depth3,reserve4,reserve_depth4,tip_count,status
bd6827,England/PHEC-1B415/2020,0,England/PHEC-17D0F/2020,0,England/PHEC-19101/2020,0,England/CAMB-76A61/2020,0,England/CAMB-845C9/2020,1,England/CAMB-8457E/2020,1,6,
.
.
.
```

`cluster_id` is a hex code that is used to 'label' the cluster (the tree file has these labels at the corresponding internal nodes.

## Assigning the UK lineages

```
jclusterfunk grapevine-assign-lineages -i cog_global_2020-11-28_labelled_tree.nexus -m cog_global_2020-11-27_lineages.csv -o ./ -p cog_global_2020-11-29_lineages
```

This takes the cluster labelled tree from the previous step and an existing table of lineages `cog_global_2020-11-27_lineages.csv` (i.e., from a previous run) and then annotates the tree with them. 

The lineages file will look like this:
```
uk_lineage,cluster_id,del_trans,uk_tip_count,tip_count
UK9284,1f2233,null,12,12
UK6482,ede78c,null,22,22
UK5914,a5cb24,null,8,8
.
.
.
```

This function will find the internal node labelled with the `cluster_id` and then label everything descended from that node with the `uk_lineage` name. It will also find new lineages (deltrans introductions with no existing lineage labelling). 

The output files will be:

`cog_global_2020-11-29_lineages.csv` – an updated lineages csv containing the existing lineages and new ones.
`cog_global_2020-11-29_metadata.csv` – an metadata csv containing the lineages designation for each tip.
`cog_global_2020-11-29_tree.nexus` – the tree file with the lineages added as annotations

## Labelling the clusters (continuing)

For the next run, the existing clusters can be used to ensure continuity of cluster labelling (and thus lineage designation):

```
jclusterfunk grapevine-label-clusters -i cog_global_2020-11-29_tree.nexus -m cog_global_2020-11-28_labelled_clusters.csv -l cog_global_2020-11-28_lineages.csv -o ./clusters -p cog_global_2020-11-29_labelled
```

This will take the new tree, `cog_global_2020-11-29_tree.nexus`, and annotated with the clusters in `cog_global_2020-11-28_labelled_clusters.csv`. It will also prioritise clusters used by the lineages given in `cog_global_2020-11-28_lineages.csv` (this is in case multiple existing clusters point to the same node which may happen as the tree rearranges with new data).

It will write an updated `cog_global_2020-11-29_labelled_clusters.csv` with the existing clusters and any new ones.

This in turn can be used with:

```
jclusterfunk grapevine-assign-lineages -i cog_global_2020-11-29_labelled_tree.nexus -m cog_global_2020-11-28_lineages.csv -o ./ -p cog_global_2020-11-29_lineages
```

to label the lineages again.
