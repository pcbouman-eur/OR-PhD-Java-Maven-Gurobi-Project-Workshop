package org.example.cflp.solver;

import org.example.cflp.model.Instance;
import org.example.cflp.model.InstanceGenerator;
import org.example.cflp.model.Solution;
import org.example.cflp.solver.sa.SaParams;
import org.example.cflp.solver.sa.SimulatedAnnealing;
import org.example.cflp.validate.ValidationResult;
import org.example.cflp.validate.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Generate -> solve -> validate, for every solver on a few deterministic instances. */
class SolverRegressionTest {

    static Solver create(SolverType type) {
        return switch (type) {
            case MIP -> new MipSolver(30, 1);
            case SA -> new SimulatedAnnealing(new SaParams(50_000, 2.0, 0.001, 10, 42));
        };
    }

    static Stream<Arguments> solversAndSeeds() {
        return Arrays.stream(SolverType.values())
                .flatMap(type -> Stream.of(1L, 2L, 3L).map(seed -> Arguments.of(type, seed)));
    }

    @ParameterizedTest(name = "{0} on seed {1}")
    @MethodSource("solversAndSeeds")
    void producesFeasibleSolution(SolverType type, long seed) {
        assumeTrue(type != SolverType.MIP || GurobiLicense.isAvailable(), "No Gurobi license");
        Instance inst = InstanceGenerator.generate("reg", 10, 40, 2.0, seed);

        Solution sol = create(type).solve(inst);
        ValidationResult result = Validator.validate(inst, sol);

        assertTrue(result.feasible(), () -> result.violations().toString());
    }

    @Test
    void simulatedAnnealingIsDeterministic() {
        Instance inst = InstanceGenerator.generate("det", 10, 40, 2.0, 1);

        Solution a = create(SolverType.SA).solve(inst);
        Solution b = create(SolverType.SA).solve(inst);

        assertEquals(a.assignment(), b.assignment());
    }

    /** Reference values in src/test/resources: fails if a change makes SA worse. */
    @ParameterizedTest(name = "SA on {0}x{1} seed {2} <= {3}")
    @CsvFileSource(resources = "/sa-reference.csv", numLinesToSkip = 1)
    void simulatedAnnealingNotWorseThanReference(int facilities, int customers, long seed, double reference) {
        Instance inst = InstanceGenerator.generate("ref", facilities, customers, 3.0, seed);

        Solution sol = create(SolverType.SA).solve(inst);

        assertTrue(sol.objective() <= reference + 1e-6,
                () -> "objective " + sol.objective() + " is worse than reference " + reference);
    }

    @Test
    void simulatedAnnealingCloseToOptimal() {
        assumeTrue(GurobiLicense.isAvailable(), "No Gurobi license");
        Instance inst = InstanceGenerator.generate("gap", 10, 40, 2.0, 1);

        double optimal = create(SolverType.MIP).solve(inst).objective();
        double heuristic = create(SolverType.SA).solve(inst).objective();

        assertTrue(heuristic <= 1.05 * optimal, "SA more than 5% above optimal");
    }
}
