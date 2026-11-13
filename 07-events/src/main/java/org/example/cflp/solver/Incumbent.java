package org.example.cflp.solver;

/** Event: Gurobi found a new incumbent (callback, where == MIPSOL). */
public record Incumbent(double runtime, double objective, double bound) {
}
