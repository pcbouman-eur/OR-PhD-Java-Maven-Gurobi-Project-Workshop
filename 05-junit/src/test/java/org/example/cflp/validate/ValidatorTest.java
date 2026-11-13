package org.example.cflp.validate;

import org.example.cflp.TestInstances;
import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.model.SolveStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    private final Instance tiny = TestInstances.tiny();

    @Test
    void acceptsFeasibleSolution() {
        Solution sol = Solution.of(tiny, "test", SolveStatus.FEASIBLE, 0, new int[]{0, 0, 1});

        ValidationResult result = Validator.validate(tiny, sol);

        assertTrue(result.feasible(), () -> result.violations().toString());
        // fixed 5 + 5, assignment 6*0 + 4*0.1 + 3*0
        assertEquals(10.4, result.objective(), 1e-9);
    }

    @Test
    void detectsCapacityViolation() {
        Solution sol = Solution.of(tiny, "test", SolveStatus.FEASIBLE, 0, new int[]{0, 0, 0});

        ValidationResult result = Validator.validate(tiny, sol);

        assertFalse(result.feasible());
        assertTrue(result.violations().getFirst().contains("exceeds capacity"));
    }

    @Test
    void detectsWrongObjective() {
        Solution sol = new Solution("tiny", "test", SolveStatus.FEASIBLE, 1.0, 0,
                List.of(0, 1), List.of(0, 0, 1));

        assertFalse(Validator.validate(tiny, sol).feasible());
    }

    @Test
    void detectsUnknownFacility() {
        Solution sol = new Solution("tiny", "test", SolveStatus.FEASIBLE, 0, 0,
                List.of(0, 7), List.of(0, 0, 7));

        assertFalse(Validator.validate(tiny, sol).feasible());
    }
}
