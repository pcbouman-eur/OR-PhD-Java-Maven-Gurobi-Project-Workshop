package org.example.cflp.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single-source capacitated facility location instance.
 * Serving customer i from facility j costs demand_i * distance(i, j).
 *
 * @param id         globally unique; solutions refer to their instance by this id
 * @param properties free-form metadata, e.g. the generator settings, handy for grouping in analyses.
 *                   Strings, because untyped numbers don't survive a round trip (a long may come back as Integer)
 */
public record Instance(UUID id,
                       String name,
                       Map<String, String> properties,
                       List<Facility> facilities,
                       List<Customer> customers) {

    public int numFacilities() {
        return facilities.size();
    }

    public int numCustomers() {
        return customers.size();
    }

    /** Assignment cost of serving customer i from facility j. */
    public double cost(int i, int j) {
        Customer c = customers.get(i);
        Facility f = facilities.get(j);
        return c.demand() * Math.hypot(c.x() - f.x(), c.y() - f.y());
    }

    /** Fixed costs of all used facilities plus all assignment costs. */
    public double objective(int[] assignment) {
        boolean[] open = new boolean[numFacilities()];
        double total = 0;
        for (int i = 0; i < assignment.length; i++) {
            total += cost(i, assignment[i]);
            open[assignment[i]] = true;
        }
        for (int j = 0; j < open.length; j++) {
            if (open[j]) {
                total += facilities.get(j).fixedCost();
            }
        }
        return total;
    }
}
