package org.example.cflp.solver.sa;

import org.example.cflp.model.Instance;
import org.example.cflp.model.InstanceGenerator;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Same package as State, so the test can use the package-private class. */
class StateTest {

    @RepeatedTest(5)
    void incrementalCostMatchesRecomputation(RepetitionInfo repetition) {
        Instance inst = InstanceGenerator.generate("state", 10, 50, 3.0, repetition.getCurrentRepetition());
        Random rnd = new Random(repetition.getCurrentRepetition());
        State state = new State(inst, SimulatedAnnealing.greedy(inst), 0);   // penalty 0: cost = objective

        for (int step = 0; step < 1_000; step++) {
            int[] before = state.snapshot();
            state.mark();
            state.move(rnd.nextInt(inst.numCustomers()), rnd.nextInt(inst.numFacilities()));
            state.move(rnd.nextInt(inst.numCustomers()), rnd.nextInt(inst.numFacilities()));
            if (rnd.nextBoolean()) {
                state.rollback();
                assertArrayEquals(before, state.snapshot());
            }
            assertEquals(inst.objective(state.snapshot()), state.cost(), 1e-6);
        }
    }
}
