package br.unb.cic.tdp.proof.util;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class MoveTreeNode {
    public MoveTreeNode parent;
    public final int mu;
    public MoveTreeNode[] children;
    private String pathToRoot;

    public MoveTreeNode(int mu, MoveTreeNode[] children, MoveTreeNode parent) {
        this.mu = mu;
        this.children = children;
        this.parent = parent;
        pathToRoot();
    }

    public int getHeight() {
        return maxDepth(this);
    }

    private int maxDepth(final MoveTreeNode move) {
        if (move.children.length == 0)
            return 1;

            int lDepth = maxDepth(move.children[0]);
        int rDepth = move.children.length == 1 ? 1 : maxDepth(move.children[1]);

        if (lDepth > rDepth)
            return (lDepth + 1);
        else
            return (rDepth + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveTreeNode m = (MoveTreeNode) o;
        return mu == m.mu;
    }

    public String pathToRoot() {
        if (pathToRoot == null) {
            final var list = new ArrayList<String>();
            var current = this;
            while (current != null) {
                list.add(Integer.toString(current.mu));
                current = current.parent;
            }

            pathToRoot = list.stream().sorted().collect(Collectors.joining());
        }
        return pathToRoot;
    }

    @Override
    public String toString() {
        return Integer.toString(mu);
    }
}