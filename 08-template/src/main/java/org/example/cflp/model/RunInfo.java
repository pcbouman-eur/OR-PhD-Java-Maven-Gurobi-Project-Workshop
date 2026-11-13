package org.example.cflp.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Everything needed to trace a result back to how it was produced.
 *
 * @param experiment groups runs that belong together, e.g. "cooling-study"
 * @param label      short human-readable name of this variant, e.g. "sa-slow"
 * @param tags       machine-readable treatment/parameter groups, e.g. {cooling=slow, operators=all}
 * @param parameters all solver parameters, including defaults
 */
public record RunInfo(UUID runId,
                      String experiment,
                      String label,
                      Map<String, String> tags,
                      Map<String, Object> parameters,
                      Instant startedAt,
                      String host,
                      String slurmJobId) {
}
