package org.example.cflp.solver;

/** Event: the final state of branch-and-bound. */
public record MipSummary(double objective, double bound, double gap, double nodes, int solutions, double runtime) {
}
