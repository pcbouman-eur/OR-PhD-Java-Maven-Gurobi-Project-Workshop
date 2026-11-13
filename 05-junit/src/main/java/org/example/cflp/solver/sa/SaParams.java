package org.example.cflp.solver.sa;

/**
 * @param startTemperature relative to the average cost per customer of the initial solution
 * @param endTemperature   idem; temperature decreases geometrically from start to end
 * @param penalty          cost per unit of demand exceeding a capacity
 */
public record SaParams(long iterations, double startTemperature, double endTemperature,
                       double penalty, long seed) {

    public static SaParams defaults() {
        return new SaParams(200_000, 2.0, 0.001, 10, 42);
    }
}
