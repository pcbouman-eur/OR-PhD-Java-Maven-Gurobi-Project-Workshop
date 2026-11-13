package org.example.cflp.validate;

import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;

import java.util.ArrayList;
import java.util.List;

/** Independent check of a solution: never trust what a solver claims about itself. */
public final class Validator {

    private Validator() {
    }

    public static ValidationResult validate(Instance inst, Solution sol) {
        List<String> violations = new ArrayList<>();
        List<Integer> assignment = sol.assignment();

        if (!inst.id().equals(sol.instanceId())) {
            violations.add("Solution is for instance " + sol.instanceId() + ", not " + inst.id());
            return new ValidationResult(false, Double.NaN, violations);
        }

        if (assignment.size() != inst.numCustomers()) {
            violations.add("Expected " + inst.numCustomers() + " assignments, got " + assignment.size());
            return new ValidationResult(false, Double.NaN, violations);
        }
        double[] load = new double[inst.numFacilities()];
        for (int i = 0; i < assignment.size(); i++) {
            int j = assignment.get(i);
            if (j < 0 || j >= inst.numFacilities()) {
                violations.add("Customer " + i + " assigned to unknown facility " + j);
                return new ValidationResult(false, Double.NaN, violations);
            }
            load[j] += inst.customers().get(i).demand();
        }
        for (int j = 0; j < load.length; j++) {
            double capacity = inst.facilities().get(j).capacity();
            if (load[j] > capacity + 1e-6) {
                violations.add("Facility " + j + " load " + load[j] + " exceeds capacity " + capacity);
            }
        }
        List<Integer> used = assignment.stream().distinct().sorted().toList();
        if (!used.equals(sol.openFacilities())) {
            violations.add("openFacilities " + sol.openFacilities() + " does not match assignment " + used);
        }
        double objective = inst.objective(sol.assignmentArray());
        if (Math.abs(objective - sol.objective()) > 1e-6 * Math.max(1, Math.abs(objective))) {
            violations.add("Reported objective " + sol.objective() + " but actual objective is " + objective);
        }
        return new ValidationResult(violations.isEmpty(), objective, violations);
    }
}
