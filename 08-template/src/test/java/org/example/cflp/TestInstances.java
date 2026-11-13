package org.example.cflp;

import org.example.cflp.model.Customer;
import org.example.cflp.model.Facility;
import org.example.cflp.model.Instance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Small hand-made instances whose optimal solution is easy to see. */
public final class TestInstances {

    private TestInstances() {
    }

    /**
     * Two facilities at x=0 and x=1, capacity 10 each, fixed cost 5.
     * Customers at x=0 (demand 6), x=0.1 (demand 4) and x=1 (demand 3).
     */
    public static Instance tiny() {
        return new Instance(UUID.randomUUID(), "tiny", Map.of(),
                List.of(new Facility(0, 0, 10, 5), new Facility(1, 0, 10, 5)),
                List.of(new Customer(0, 0, 6), new Customer(0.1, 0, 4), new Customer(1, 0, 3)));
    }
}
