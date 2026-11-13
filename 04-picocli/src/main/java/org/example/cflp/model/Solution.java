package org.example.cflp.model;

import java.util.Arrays;
import java.util.List;

/**
 * @param assignment     assignment.get(i) = facility serving customer i
 * @param openFacilities derived from assignment, stored for convenience of other tools
 */
public record Solution(String instanceName,
                       String solver,
                       SolveStatus status,
                       double objective,
                       double runtimeSeconds,
                       List<Integer> openFacilities,
                       List<Integer> assignment) {

    public static Solution of(Instance inst, String solver, SolveStatus status,
                              double runtimeSeconds, int[] assignment) {
        return new Solution(inst.name(), solver, status, inst.objective(assignment), runtimeSeconds,
                Arrays.stream(assignment).distinct().sorted().boxed().toList(),
                Arrays.stream(assignment).boxed().toList());
    }

    public int[] assignmentArray() {
        return assignment.stream().mapToInt(Integer::intValue).toArray();
    }
}
