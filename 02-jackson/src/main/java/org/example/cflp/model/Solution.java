package org.example.cflp.model;

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
}
