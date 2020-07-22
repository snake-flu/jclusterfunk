package network.artic.clusterfunk.commands;

import jebl.evolution.trees.RootedTree;

/**
 * @author Andrew Rambaut
 * @version $
 */
public interface TreeFunction {
    RootedTree processTree(RootedTree tree);
}
