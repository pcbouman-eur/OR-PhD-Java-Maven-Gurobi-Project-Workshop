---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 3/8 A second solver'
---

<!-- _class: lead -->
# Step 3: A heuristic and a validator

Two solvers behind one interface, one independent judge.

---

# One interface, many algorithms

```java
public interface Solver {
    String name();
    Solution solve(Instance instance);
}

public class MipSolver implements Solver { ... }
public class SimulatedAnnealing implements Solver { ... }
```

- The rest of the code (I/O, CLI, tests, batch runs) only knows `Solver`
- Adding a third algorithm = one new class
- `MipSolver` wraps the checked `GRBException` in an unchecked exception

---

# Simulated annealing in 15 lines

```java
for (long it = 0; it < params.iterations(); it++, temperature *= cooling) {
    Operator op = operators.get(rnd.nextInt(operators.size()));
    double before = state.cost();
    state.mark();
    if (!op.apply(state, rnd)) continue;           // move not applicable

    double delta = state.cost() - before;
    if (delta <= 0 || rnd.nextDouble() < Math.exp(-delta / temperature)) {
        if (state.feasible() && state.cost() < bestCost) {
            bestCost = state.cost();
            best = state.snapshot();
        }
    } else {
        state.rollback();                          // undo all moves since mark()
    }
}
```

---

# Operators and an undoable state

```java
interface Operator {
    String name();
    boolean apply(State state, RandomGenerator rnd);   // calls state.move(i, j)
}
```

| Operator | Move |
|---|---|
| `shift` | one customer → random other facility |
| `swap` | two customers exchange facilities |
| `close` | all customers of a facility → cheapest open facility with room |
| `open` | open a closed facility, pull in customers that get cheaper |

`State` keeps cost, loads and the capacity overload (× penalty) **incrementally**, plus a journal of moves → `rollback()` makes compound moves cheap to reject.

---

# Reproducibility: seed everything

```java
public record SaParams(long iterations, double startTemperature, double endTemperature,
                       double penalty, long seed) { }

Random rnd = new Random(params.seed());
```

- Same instance + same parameters + same seed ⇒ **identical** solution
- An iteration budget instead of a time limit keeps runs deterministic
  (wall-clock limits depend on the machine and its load)
- Varying the seed gives independent replications for your statistics

---

# Validator: never trust the solver

```java
public record ValidationResult(boolean feasible, double objective, List<String> violations) { }

ValidationResult r = Validator.validate(instance, solution);
```

Checks, using only the instance and the solution file:
- every customer assigned to an existing facility
- loads ≤ capacities
- `openFacilities` consistent with the assignment
- reported objective = recomputed objective

Bugs in incremental bookkeeping (`State`) are common. Here an independent check is cheap.

---

# Try it

```bash
java -jar target/cflp.jar generate 50 200 1 a.instance.json
java -jar target/cflp.jar solve sa  a.instance.json a.sa.solution.json
java -jar target/cflp.jar solve mip a.instance.json a.mip.solution.json
java -jar target/cflp.jar validate a.instance.json a.sa.solution.json
```

`main(String[] args)` keeps growing: `args[1]`, `args[2]`… positional, no help, no defaults.
**Next:** a real command-line interface with picocli.
