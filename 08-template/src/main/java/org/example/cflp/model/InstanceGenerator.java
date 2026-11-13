package org.example.cflp.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Random instances on the unit square, in the spirit of Cornuéjols et al. (1991). */
public final class InstanceGenerator {

    private InstanceGenerator() {
    }

    /**
     * @param capacityRatio total capacity / total demand (must be > 1 for feasibility)
     */
    public static Instance generate(String name, int numFacilities, int numCustomers,
                                    double capacityRatio, long seed) {
        Random rnd = new Random(seed);

        List<Customer> customers = new ArrayList<>();
        double totalDemand = 0;
        for (int i = 0; i < numCustomers; i++) {
            double demand = 5 + rnd.nextInt(31);
            totalDemand += demand;
            customers.add(new Customer(coord(rnd), coord(rnd), demand));
        }

        // Draw raw capacities, then scale them so that sum(capacity) = ratio * sum(demand)
        double[] raw = new double[numFacilities];
        double rawSum = 0;
        for (int j = 0; j < numFacilities; j++) {
            raw[j] = 10 + 150 * rnd.nextDouble();
            rawSum += raw[j];
        }
        List<Facility> facilities = new ArrayList<>();
        for (int j = 0; j < numFacilities; j++) {
            double capacity = Math.ceil(raw[j] / rawSum * capacityRatio * totalDemand);
            // Larger facilities are more expensive, with economies of scale
            double fixedCost = Math.round(capacity * (10 + 10 * rnd.nextDouble()) / Math.sqrt(capacity) * 10) / 10.0;
            facilities.add(new Facility(coord(rnd), coord(rnd), capacity, fixedCost));
        }
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("generator", "uniform");
        properties.put("facilities", String.valueOf(numFacilities));
        properties.put("customers", String.valueOf(numCustomers));
        properties.put("capacityRatio", String.valueOf(capacityRatio));
        properties.put("seed", String.valueOf(seed));
        return new Instance(UUID.randomUUID(), name, properties, facilities, customers);
    }

    /** Coordinates rounded to 3 decimals keep the files readable. */
    private static double coord(Random rnd) {
        return Math.round(rnd.nextDouble() * 1000) / 1000.0;
    }
}
