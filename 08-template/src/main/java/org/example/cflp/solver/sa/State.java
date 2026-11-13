package org.example.cflp.solver.sa;

import org.example.cflp.model.Instance;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Assignment with incrementally maintained cost. A facility is open iff it serves a customer.
 * Capacity violations are allowed but penalised; moves since {@link #mark()} can be undone.
 */
final class State {

    private record Change(int customer, int from) {
    }

    final Instance inst;
    private final int[] assignment;
    private final double[] load;
    private final int[] served;
    private final double penalty;
    private final Deque<Change> journal = new ArrayDeque<>();
    private double fixedCost;
    private double assignmentCost;
    private double overload;

    State(Instance inst, int[] initial, double penalty) {
        this.inst = inst;
        this.penalty = penalty;
        this.assignment = new int[initial.length];
        this.load = new double[inst.numFacilities()];
        this.served = new int[inst.numFacilities()];
        java.util.Arrays.fill(assignment, -1);
        for (int i = 0; i < initial.length; i++) {
            place(i, initial[i]);
        }
    }

    double cost() {
        return fixedCost + assignmentCost + penalty * overload;
    }

    boolean feasible() {
        return overload < 1e-9;
    }

    int facilityOf(int customer) {
        return assignment[customer];
    }

    boolean isOpen(int facility) {
        return served[facility] > 0;
    }

    double residual(int facility) {
        return inst.facilities().get(facility).capacity() - load[facility];
    }

    int[] snapshot() {
        return assignment.clone();
    }

    /** Reassign customer i to facility j (and remember how to undo it). */
    void move(int i, int j) {
        int from = assignment[i];
        if (from == j) {
            return;
        }
        journal.push(new Change(i, from));
        unplace(i);
        place(i, j);
    }

    void mark() {
        journal.clear();
    }

    void rollback() {
        while (!journal.isEmpty()) {
            Change c = journal.pop();
            unplace(c.customer());
            place(c.customer(), c.from());
        }
    }

    private void place(int i, int j) {
        assignment[i] = j;
        if (served[j]++ == 0) {
            fixedCost += inst.facilities().get(j).fixedCost();
        }
        assignmentCost += inst.cost(i, j);
        changeLoad(j, inst.customers().get(i).demand());
    }

    private void unplace(int i) {
        int j = assignment[i];
        if (--served[j] == 0) {
            fixedCost -= inst.facilities().get(j).fixedCost();
        }
        assignmentCost -= inst.cost(i, j);
        changeLoad(j, -inst.customers().get(i).demand());
    }

    private void changeLoad(int j, double delta) {
        double capacity = inst.facilities().get(j).capacity();
        overload -= Math.max(0, load[j] - capacity);
        load[j] += delta;
        overload += Math.max(0, load[j] - capacity);
    }
}
