package org.example.cflp.solver.sa;

/** Event: the best feasible solution so far improved. */
public record NewBest(long iteration, String operator, double cost, double seconds) {
}
