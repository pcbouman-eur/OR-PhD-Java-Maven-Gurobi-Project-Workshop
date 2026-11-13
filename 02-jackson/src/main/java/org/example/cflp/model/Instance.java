package org.example.cflp.model;

import java.util.List;

/**
 * Single-source capacitated facility location instance.
 * Serving customer i from facility j costs demand_i * distance(i, j).
 */
public record Instance(String name, List<Facility> facilities, List<Customer> customers) {

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
}
