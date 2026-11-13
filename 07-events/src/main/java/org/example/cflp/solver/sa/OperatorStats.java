package org.example.cflp.solver.sa;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Event: what one operator achieved in one run; counted in the SA loop, logged once at the end. */
@JsonPropertyOrder({"operator", "attempts", "notApplicable", "rejected", "acceptedWorse", "improved", "newBest"})
public class OperatorStats {

    public final String operator;
    public long attempts;
    public long notApplicable;
    public long rejected;
    public long acceptedWorse;
    public long improved;
    public long newBest;
    public double totalImprovement;
    public double maxImprovement;

    OperatorStats(String operator) {
        this.operator = operator;
    }

    void improvement(double amount) {
        improved++;
        totalImprovement += amount;
        maxImprovement = Math.max(maxImprovement, amount);
    }
}
