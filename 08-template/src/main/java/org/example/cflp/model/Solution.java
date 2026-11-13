package org.example.cflp.model;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @param instanceId     id of the instance this solution belongs to
 * @param run            how this solution was produced; filled in after solving
 * @param assignment     assignment.get(i) = facility serving customer i
 * @param openFacilities derived from assignment, stored for convenience of other tools
 */
public record Solution(UUID instanceId,
                       String instanceName,
                       RunInfo run,
                       String solver,
                       SolveStatus status,
                       double objective,
                       double runtimeSeconds,
                       List<Integer> openFacilities,
                       List<Integer> assignment) {

    public static Solution of(Instance inst, String solver, SolveStatus status,
                              double runtimeSeconds, int[] assignment) {
        return new Solution(inst.id(), inst.name(), null, solver, status, inst.objective(assignment), runtimeSeconds,
                Arrays.stream(assignment).distinct().sorted().boxed().toList(),
                Arrays.stream(assignment).boxed().toList());
    }

    /** Records are immutable: "changing" a field means creating a copy. */
    public Solution withRun(RunInfo run) {
        return new Solution(instanceId, instanceName, run, solver, status, objective, runtimeSeconds,
                openFacilities, assignment);
    }

    public int[] assignmentArray() {
        return assignment.stream().mapToInt(Integer::intValue).toArray();
    }
}
