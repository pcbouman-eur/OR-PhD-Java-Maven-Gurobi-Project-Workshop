---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 5/8 Testing with JUnit'
---

<!-- _class: lead -->
# Step 5: Unit tests with JUnit

Change your solver on Monday, still trust the results on Friday.

---

# Where tests live

```
05-junit/
├── pom.xml                         junit-jupiter, <scope>test</scope>
├── src/main/java/...               shipped in cflp.jar
├── src/test/java/...               tests: same packages, never in the jar
│   └── org/example/cflp/solver/sa/StateTest.java
└── src/test/resources/
    └── sa-reference.csv
```

- `mvn test` runs every `*Test` class (Surefire plugin); `mvn package` runs them too
- IntelliJ: green ▶ next to each test class/method; right-click folder → *Run tests*
- Tests are in the **same package** → package-private classes are testable

---

# Anatomy of a test

```java
class ValidatorTest {
    private final Instance tiny = TestInstances.tiny();      // hand-made, answer known

    @Test
    void detectsCapacityViolation() {
        // arrange
        Solution sol = Solution.of(tiny, "test", SolveStatus.FEASIBLE, 0, new int[]{0, 0, 0});
        // act
        ValidationResult result = Validator.validate(tiny, sol);
        // assert
        assertFalse(result.feasible());
        assertTrue(result.violations().getFirst().contains("exceeds capacity"));
    }
}
```

Good first targets: the validator, I/O round trips (`assertEquals(original, read)` thanks to records), and incremental bookkeeping.

---

# Testing the tricky bits: incremental cost

```java
@RepeatedTest(5)
void incrementalCostMatchesRecomputation(RepetitionInfo rep) {
    Instance inst = InstanceGenerator.generate("state", 10, 50, 3.0, rep.getCurrentRepetition());
    State state = new State(inst, SimulatedAnnealing.greedy(inst), 0);
    for (int step = 0; step < 1_000; step++) {
        int[] before = state.snapshot();
        state.mark();
        state.move(rnd.nextInt(n), rnd.nextInt(m));
        if (rnd.nextBoolean()) {
            state.rollback();
            assertArrayEquals(before, state.snapshot());
        }
        assertEquals(inst.objective(state.snapshot()), state.cost(), 1e-6);
    }
}
```

Forget one `fixedCost -= ...` in `State` → this test fails within milliseconds.

---

# Generate → solve → validate, for every solver

```java
static Stream<Arguments> solversAndSeeds() {          // MIP×{1,2,3}, SA×{1,2,3}
    return Arrays.stream(SolverType.values())
            .flatMap(t -> Stream.of(1L, 2L, 3L).map(seed -> Arguments.of(t, seed)));
}

@ParameterizedTest(name = "{0} on seed {1}")
@MethodSource("solversAndSeeds")
void producesFeasibleSolution(SolverType type, long seed) {
    assumeTrue(type != SolverType.MIP || GurobiLicense.isAvailable(), "No Gurobi license");
    Instance inst = InstanceGenerator.generate("reg", 10, 40, 2.0, seed);
    ValidationResult result = Validator.validate(inst, create(type).solve(inst));
    assertTrue(result.feasible(), () -> result.violations().toString());
}
```

`assumeTrue` → **skipped**, not failed, on a laptop without a license.

---

# Catching regressions in solution quality

```csv
facilities,customers,seed,reference        ← src/test/resources/sa-reference.csv
10,50,1,1124.75
20,100,1,1727.94
```

```java
@ParameterizedTest
@CsvFileSource(resources = "/sa-reference.csv", numLinesToSkip = 1)
void simulatedAnnealingNotWorseThanReference(int facilities, int customers, long seed, double reference) {
    Instance inst = InstanceGenerator.generate("ref", facilities, customers, 3.0, seed);
    assertTrue(create(SolverType.SA).solve(inst).objective() <= reference + 1e-6);
}
```

- Possible only because SA is **deterministic** (fixed seed, iteration budget)
- Improved the solver? Lower the references and commit them with the change

---

# Try it

```bash
mvn test
```
```
Tests run: 4,  Failures: 0, Errors: 0, Skipped: 0 -- in ValidatorTest
Tests run: 12, Failures: 0, Errors: 0, Skipped: 4 -- in SolverRegressionTest
Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0 -- in StateTest
Tests run: 2,  Failures: 0, Errors: 0, Skipped: 0 -- in JsonIOTest
```

Keep tests fast (seconds): small instances, few iterations. `mvn package -DskipTests` when in a hurry.

**Next:** `System.out.println` → a logging framework, with a log file per run.
