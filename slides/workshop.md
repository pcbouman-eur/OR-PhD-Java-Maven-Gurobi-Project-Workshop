---
marp: true
theme: workshop
paginate: true
footer: 'Research software plumbing'
---

<!-- _class: lead -->
# Research software plumbing

Java + Gurobi on Snellius: everything *around* your algorithm
that makes experiments reliable, repeatable and easy to analyse.

---

# Today

<div class="columns">
<div>

**Talk (~30 min)**
1. The terminal
2. Build tools: Maven, Gurobi, one jar, Slurm
3. Data files: JSON with Jackson
4. Solver interface, validator, tests (JUnit)
5. A command line (picocli)
6. Logging and structured logging
7. Traceable experiments: IDs and run metadata

</div>
<div>

**Then: discussion + hands-on**
- Try-it guide at the end of the slides
- Appendix: details we skip today

**Running example**: single-source capacitated facility location, a Gurobi MIP and simulated annealing.
You know these algorithms; today is about the plumbing.

Code: `01-maven-gurobi/` … `08-template/`, one idea per step.

</div>
</div>

<!-- Timing: ~2 min per section, ~4 min for tests and logging. -->

---

<!-- footer: 'Research software plumbing · The terminal' -->

# Why the terminal?

- Snellius (and every other cluster) has **no GUI**: you get `ssh` and a shell
- A command is a **recipe**: copy it into a job script, a README, or your paper's appendix
- Loop over 200 instances or 10 seeds without clicking
- **Glue between languages**: the output of a Java program is the input of a Python or R script

```bash
java -jar cflp.jar generate -n 10 -o instances              # Java: make instances
java -jar cflp.jar batch instances/ --solver sa > run.txt   # Java: solve, output into a file
python3 analysis/collect.py results/ && Rscript plots.R     # Python, then R (only if Python succeeded)
for s in 1 2 3; do java -jar cflp.jar batch instances/ --seed $s -l seed$s; done
```

<span class="small">`>` writes output to a file · `|` feeds output into the next program · `&&` runs the next command only on success (exit code 0)</span>

---

# Useful commands to be familiar with

| Command | Does | Command | Does |
|---|---|---|---|
| `pwd` | where am I? | `cat file` | print a file |
| `ls -l` | list files | `less file` | scroll (`q` quit, `/` search) |
| `cd dir`, `cd ..` | change directory | `head`, `tail -f` | start / follow the end of a file |
| `mkdir`, `cp`, `mv`, `rm` | manage files | `grep text files` | find lines containing *text* |
| `ssh`, `scp` | remote login, copy | `wc -l` | count lines |
| `cmd --help` | documentation | `find . -name x` | find files |

```bash
grep -c '"NewBest"' logs/*.events.jsonl | sort -t: -k2 -n | tail -3   # which runs improved most often?
```

<span class="small">**Tab** completes names, **↑** / **Ctrl+R** recall earlier commands. On Windows: Git Bash or WSL.</span>

---

<!-- footer: 'Research software plumbing · Build tools' -->

# Build tools: what do they bring?

- **Dependencies**: declare *name + version*, the tool downloads them (and *their* dependencies)
- **Standard layout**: `src/main/java`, `src/test/java`; everyone finds their way
- **One build command** that works the same on your laptop, a colleague's, and Snellius
- **IDE-independent**: IntelliJ, VS Code and Eclipse all read the same project file
- **Plugins**: run tests, build a jar, generate docs, ...

Every language has them: for Java **Maven** (today) or Gradle, for Python pip, uv or conda, for R renv.

---

# Maven + Gurobi → one jar

```xml
<dependency>
    <groupId>com.gurobi</groupId>
    <artifactId>gurobi</artifactId>          <!-- Java API + native libraries, all platforms -->
    <version>13.0.3</version>
</dependency>
```

- No Gurobi installation, no `module load gurobi`, no `LD_LIBRARY_PATH`
- The **shade plugin** packs your code and all dependencies into one *fat jar*;
  dropping the non-Linux natives brings it from ~120 MB to ~15 MB
- `mvn package` → `target/cflp.jar` → `scp` it to Snellius. Done.
- Only the **license file** lives outside the jar: academic WLS license in `~/gurobi.lic`

```java
try { ... model.optimize(); }
finally { model.dispose(); env.dispose(); }   // frees native memory and the WLS license token
```

---

# Running it on Snellius: a Slurm job

```bash
#!/bin/bash
#SBATCH --job-name=cflp
#SBATCH --partition=rome
#SBATCH --cpus-per-task=4
#SBATCH --time=00:10:00

module load 2024
module load Java/21.0.5

java -jar $HOME/cflp/cflp.jar batch instances/ --solver mip --threads $SLURM_CPUS_PER_TASK
```

- `sbatch job.sh` · `squeue -u $USER` · `scancel <id>`
- ⚠️ Gurobi uses **all cores of the node** by default → set `Threads`
- ⚠️ WLS checks a token over the internet: test with one small job, and check how many concurrent sessions your license allows

---

<!-- footer: 'Research software plumbing · JSON' -->

# What is JSON, and why should you care?

```json
{ "name" : "cflp-3-4-1",
  "capacityRatio" : 2.0,
  "facilities" : [ { "x" : 0.116, "y" : 0.771, "capacity" : 70.0, "fixedCost" : 126.0 }, ... ],
  "tags" : [ "small", "test" ],
  "solved" : false,  "note" : null }
```

- Six building blocks: **objects** `{}`, **arrays** `[]`, strings, numbers, `true`/`false`, `null`
- **Self-describing**: field names travel with the data; adding a field doesn't break old readers
- **Nested** data (a list of facilities, each with 4 numbers) where CSV is flat
- **Every language** reads it: Python `json`, R `jsonlite`, JavaScript, `jq` on the command line
- No more hand-written parser *and* writer per file format that slowly drift apart

---

# Jackson: the classic way

```java
public class Facility {
    private final double x, y, capacity, fixedCost;

    @JsonCreator                                         // reading: use this constructor
    public Facility(@JsonProperty("x") double x, @JsonProperty("y") double y,
                    @JsonProperty("capacity") double capacity,
                    @JsonProperty("fixedCost") double fixedCost) { ... }

    public double getX() { return x; }                   // writing: getters → "x", "y", ...
    public double getY() { return y; }
    @JsonGetter("capacity")                              // a getter with a non-standard name
    public double maxLoad() { return capacity; }
    public double getFixedCost() { return fixedCost; }

    @JsonIgnore                                          // derived value: don't write it
    public double getCostPerUnit() { return fixedCost / capacity; }
}
```

```java
mapper.writeValue(file, facility);       Facility f = mapper.readValue(file, Facility.class);
```

---

# By the way: the short way in modern Java

```java
public record Facility(double x, double y, double capacity, double fixedCost) {
    public double costPerUnit() { return fixedCost / capacity; }    // methods are allowed
}
```

A **record** (Java 16+) is a class for immutable data. You get, for free:
- a constructor, accessors `f.capacity()`, `equals`, `hashCode` and `toString`
- `final` fields: an instance can't change after it's loaded
- Jackson support without annotations: the components *are* the JSON fields
  (annotations like `@JsonProperty("cap")` still work when you need them)

Enums (`"status" : "OPTIMAL"`), `List`, `Map`, `UUID`, `Instant` and nested records work out of the box.

---

<!-- footer: 'Research software plumbing · Solvers, validator, tests' -->

# One interface for all solvers, one independent judge

```java
public interface Solver {
    String name();
    Solution solve(Instance instance);
}
public class MipSolver implements Solver { ... }
public class SimulatedAnnealing implements Solver { ... }
```

```java
public record ValidationResult(boolean feasible, double objective, List<String> violations) { }
ValidationResult r = Validator.validate(instance, solution);   // uses only the instance and the solution
```

- The CLI, file I/O, logging and tests only know `Solver` → a new algorithm is one new class
- **Never trust the solver's own claims**: the validator recomputes feasibility and objective
- And the validator makes a great first thing to test →

---

# JUnit in a Maven project

```
src/main/java/...      shipped in cflp.jar
src/test/java/...      tests, same packages, never in the jar      ← mvn test, mvn package
```

```java
class ValidatorTest {
    @Test
    void detectsCapacityViolation() {
        Instance tiny = TestInstances.tiny();                            // hand-made, answer known
        Solution sol = Solution.of(tiny, "test", SolveStatus.FEASIBLE, 0, new int[]{0, 0, 0});
        ValidationResult result = Validator.validate(tiny, sol);
        assertFalse(result.feasible());
    }
}
```

- `mvn package` runs all tests first: a broken build never reaches Snellius
- IntelliJ: the green ▶ next to a test class or method
- Good first targets: the validator, JSON round trips (`assertEquals(original, readBack)`)
- With the `Solver` interface, one test can check *every* solver (appendix)

---

<!-- footer: 'Research software plumbing · Command line' -->

# What is `String[] args`?

```java
public class Main {
    public static void main(String[] args) {           // args = the words after the program name
        int facilities = Integer.parseInt(args[0]);
        int customers  = Integer.parseInt(args[1]);
        long seed      = Long.parseLong(args[2]);
        ...
```

```bash
java -jar cflp.jar 20 100 1          # → args = { "20", "100", "1" }   (always Strings)
```

- The shell splits the line at spaces; quotes keep a space inside one word: `"my file.json"`
- In IntelliJ: *Run → Edit Configurations → Program arguments*
- Fine for three numbers. With ten parameters, defaults and files: *what was `args[7]` again?*
  → let a library do the parsing

---

# A real command-line interface

```bash
java -jar cflp.jar <command> [options] [files]
java -jar cflp.jar batch instances/ --solver sa --iterations 500000 -o results
```

- **Named options** with defaults, in any order, instead of `args[3]`
- Subcommands: `generate`, `solve`, `batch`, `validate`; `--help` for free
- Every experiment is one reproducible line in a job script

```java
@Command(name = "solve", description = "Solve a single instance.")
public class SolveCommand implements Runnable {
    @Parameters(index = "0", description = "Instance file.")
    Path instanceFile;                                              // String → Path conversion

    @Option(names = {"-o", "--output-dir"}, defaultValue = "solutions",
            description = "Output directory (default: ${DEFAULT-VALUE}).")
    Path outputDir;

    public void run() { ... }
}
```

---

# ... and still click *Run* in IntelliJ

```java
public class DevMain {
    public static void main(String[] args) {
        run("generate", "--count", "3", "--facilities", "30", "-o", "work/instances");
        run("batch", "work/instances", "--solver", "sa", "-o", "work/results");
        run("validate", "work/instances", "work/results");
    }
    private static void run(String... args) { App.commandLine().execute(args); }
}
```

- The **same commands** as on Snellius, with breakpoints and the debugger
- `.run/*.run.xml`: ready-made IntelliJ run configurations, shared through git
- Laptop, IDE and cluster run *exactly* the same code path

---

<!-- footer: 'Research software plumbing · Logging' -->

# Why not `System.out.println`?

- No timestamps, no levels: *was this an error or progress info?*
- All or nothing: debug output must be deleted, and re-added, by hand
- A batch of 200 instances → `slurm-123.out` is one long mess

A logging framework separates **what** you log (in the code) from **where and how much** (in a configuration file):
**SLF4J** is the API you call, **Logback** writes the output (to the console, a file, or both).

Every message gets a **level**; the configuration decides which levels go where:

| `ERROR` | `WARN` | `INFO` | `DEBUG` |
|---|---|---|---|
| something failed | suspicious, continuing | milestones, results | progress inside the algorithm |

---

<!-- _class: dense -->

# Logging in a class

```java
public class SimulatedAnnealing implements Solver {
    private static final Logger log = LoggerFactory.getLogger(SimulatedAnnealing.class);

    public Solution solve(Instance inst) {
        log.info("Solving {} with {}", inst.name(), params);                   // {} = placeholder
        for (long it = 0; it < params.iterations(); it++, temperature *= cooling) {
            if (it % reportEvery == 0) {                                        // cheap when DEBUG is off
                log.debug("it {} T {} current {} best {}", it, temperature, state.cost(), bestCost);
            }
            ...
        }
        log.info("Best solution: cost {} after {}s", bestCost, runtime);
```

```
$ cat logs/cflp-30-150-1.sa.log
2026-11-13 14:01:06.055 INFO  [main] o.e.c.solver.sa.SimulatedAnnealing - Solving cflp-30-150-1 with SaParams[...]
2026-11-13 14:01:06.086 DEBUG [main] o.e.c.solver.sa.SimulatedAnnealing - it 0 T 55.6937 current 4177.03 best 4177.03
2026-11-13 14:01:06.174 DEBUG [main] o.e.c.solver.sa.SimulatedAnnealing - it 20000 T 26.0437 current 3341.45 best 3129.86
...
2026-11-13 14:01:06.532 INFO  [main] o.e.c.solver.sa.SimulatedAnnealing - Best solution: cost 2656.41 after 0.49s
```

Console: `INFO` and up. File: everything, with time, level, thread and class for free.

---

# Gurobi's log → your log

```java
env.set(GRB.IntParam.LogToConsole, 0);        // Gurobi stops printing by itself...
model.setCallback(new GurobiLogBridge());     // ...and hands every line to us

public class GurobiLogBridge extends GRBCallback {
    private static final Logger log = LoggerFactory.getLogger("gurobi");
    protected void callback() {
        if (where == GRB.CB_MESSAGE) {
            log.info(getStringInfo(GRB.CB_MSG_STRING).stripTrailing());
        }
    }
}
```

- Gurobi's lines appear in the same file, between your own messages
- Silence them with `<logger name="gurobi" level="WARN"/>`

---

# Structured logging: log objects, not sentences

Text logs are for humans. For numbers you want to analyse, log an **event object** instead:

```java
public record NewBest(long iteration, String operator, double cost, double seconds) { }
Events.log(new NewBest(it, op.name(), bestCost, elapsed()));     // → one line in the events file
```

```
$ head -1 logs/cflp-30-150-1.sa.events.jsonl          one JSON object per line (wrapped here)
{"time":"2026-11-13T11:42:10.92Z","logger":"events","level":"INFO","run":"cflp-30-150-1.sa",
 "event":"NewBest","iteration":34,"operator":"close","cost":3935.30,"seconds":0.035}
$ grep OperatorStats logs/cflp-30-150-1.sa.events.jsonl                         (shortened)
{"time":...,"event":"OperatorStats","operator":"swap","attempts":50001,"notApplicable":6292,...}
```

- No regular expressions needed: Python, R or a spreadsheet read this as a table, one row per event
- Or **count** in a plain object inside the loop and log it once at the end (`OperatorStats`)
- MIP: an `Incumbent` event from the `MIPSOL` callback

---

<!-- footer: 'Research software plumbing · Traceable experiments' -->

# Which instance does this solution belong to?

```
results/cflp-30-150-1.sa.solution.json     ← "cflp-30-150-1" ... but which one?
```

- You regenerated the instances after changing the generator: **same name, different data**
- A colleague sends `instance_3.json`, and you already have one
- 400 result files after a weekend on Snellius: which settings produced which file?

→ Give every instance and every run a **unique ID**, and store the context *inside* the file.

---

# UUIDs

```java
UUID id = UUID.randomUUID();      // 468397b1-3ab5-4506-a899-b2bb49eaddfd
```

- 128 bits, generated anywhere without coordination; collisions are negligible in practice
- Versions: **v4** random (used here) · v5 derived from a name (deterministic) · v7 time-ordered (sortable)
- Jackson reads and writes a `UUID` as a string

```java
public record Instance(UUID id, String name, ...) { }
public record Solution(UUID instanceId, String instanceName, RunInfo run, ...) { }
```

```
$ java -jar cflp.jar validate instances/ results/        # matches solutions to instances by id
cooling/cflp-30-150-3.sa-warm.solution.json: instance cflp-30-150-3 (a122522d-...) not found
```

---

# Run metadata: experiment, label, tags

```bash
java -jar cflp.jar batch instances/ -e cooling -l sa-warm -t start=warm --t-start 2.0
```

```json
"run" : {
  "runId" : "09fbe0bc-63bc-42ee-8ea0-dd581098c804",
  "experiment" : "cooling",  "label" : "sa-warm",  "tags" : { "start" : "warm" },
  "parameters" : { "iterations" : 200000, "startTemperature" : 2.0, "seed" : 42, ... },
  "startedAt" : "2026-11-02T18:26:26.404Z",  "host" : "tcn123",  "slurmJobId" : "1234567_8"
}
```

- **Label**: for humans and file names · **tags**: treatment groups for analysis · **parameters**: all of them, including defaults
- The `runId` is also in every event line → events and results can be matched later
- `results/<experiment>/<instance>.<label>.solution.json`: one file per run, so parallel jobs never clash

---

<!-- footer: 'Research software plumbing · Wrap-up' -->

# Recap

| Plumbing | Tool | Gives you |
|---|---|---|
| Terminal | bash | recipes, automation, glue between languages |
| Build | Maven + shade | dependencies, one jar, only a license outside |
| Data | Jackson, records | instances and solutions any language can read |
| Quality | interface + validator + JUnit | every solver tested, regressions caught |
| Interface | picocli + `DevMain` | the same commands in IntelliJ, shell, Slurm |
| Logging | SLF4J + Logback | readable logs, Gurobi included, events as data |
| Traceability | UUIDs, run metadata | every number traceable to its instance and settings |


---

# Discussion

- What does *your* current experiment pipeline look like? Where does it hurt?
- How do you know that the numbers in your paper came from the code you have now?
- What would you put in an event? Columns per pricing round, cuts per family, ...?
- Which of these would you adopt first, and what stops you?
- Python / R / Julia users: what are the equivalents in your ecosystem?

---

<!-- _class: lead -->
<!-- footer: 'Research software plumbing · Try it' -->
# Try it

Each step is a complete project. Open the folder in IntelliJ, or use a terminal.

---

# Setup

- **Java 21+** and **Maven 3.9+**: IntelliJ ships both (*File → Open* the step folder). Optional: Python + pandas
- Gurobi **WLS** license → `~/gurobi.lic` (without it, only the MIP doesn't work)

```bash
cd 08-template
mvn package                          # compile, run the tests, build target/cflp.jar
java -jar target/cflp.jar --help
```

| Step | Folder | Step | Folder |
|---|---|---|---|
| Maven + Gurobi | `01-maven-gurobi` | Tests | `05-junit` |
| JSON | `02-jackson` | Logging | `06-logging` |
| Solver + validator | `03-heuristic` | Structured logging | `07-events` |
| Command line | `04-picocli` | The template | `08-template` |

---

# Commands per step

```bash
# 01: one jar
cd 01-maven-gurobi && mvn package && java -jar target/cflp.jar 20 100 1     # facilities customers seed
# 02: JSON
java -jar target/cflp.jar generate 20 100 1 a.instance.json
java -jar target/cflp.jar solve a.instance.json a.solution.json
# 03: two solvers, one validator
java -jar target/cflp.jar solve sa a.instance.json a.sa.solution.json
java -jar target/cflp.jar validate a.instance.json a.sa.solution.json
# 04: command line (and run DevMain in IntelliJ)
java -jar target/cflp.jar generate -n 3 -f 30 -c 150
java -jar target/cflp.jar batch instances/ --solver sa
# 05: tests
mvn test
# 06 + 07: logs and events
java -jar target/cflp.jar batch instances/ --solver sa
less logs/cflp-30-150-1.sa.log;  head logs/cflp-30-150-1.sa.events.jsonl
```

---

# The template, end to end

```bash
cd 08-template && mvn package
java -jar target/cflp.jar generate --count 5 -f 30 -c 150
java -jar target/cflp.jar batch instances/ -e cooling -l sa-cold -t start=cold --t-start 0.5
java -jar target/cflp.jar batch instances/ -e cooling -l sa-warm -t start=warm --t-start 2.0
java -jar target/cflp.jar validate instances/ results/
python3 analysis/collect.py results/                  # → results/results.csv, one row per run
```

- Open `viewer/index.html` and drop an instance and a solution file on it
- On Snellius: `scp target/cflp.jar` plus `instances/` and `snellius/*.sh` to `~/cflp`, then
  `sbatch snellius/job.sh` (one job) or `sbatch --array=0-4 snellius/array.sh` (one task per instance)

---

# Exercises: pick one

1. **JSON**: add a field to `Instance` (e.g. a `description`). Old files still load? Read it from Python.
2. **Tests**: break the validator on purpose (skip the capacity check). Which test catches it?
3. **CLI**: add an option `--cooling` or `--max-seconds` to the SA, and see it in `--help` and in the solution's `parameters`
4. **Events**: log a new event (e.g. `Reheat` or `Restart`), plot it with pandas
5. **Experiment**: compare two parameter settings with `-e`/`-l`/`-t`, pivot `results.csv` by tag
6. **Snellius**: run the array job on 20 instances; find the `slurmJobId` in a solution file
7. **Your own problem**: replace `model/`, `solver/`, `validate/` in the template

---

<!-- _class: lead -->
<!-- footer: 'Research software plumbing · Appendix' -->
# Appendix

Details we skipped: the heuristic, more picocli, more tests, more logging, conventions.

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

# picocli: enums, mixins and exit codes

```java
public class SolverOptions {
    @Option(names = "--solver", defaultValue = "SA",
            description = "Solver: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
    SolverType solver;                                     // enum SolverType { MIP, SA }
    ...
}

@Mixin SolverOptions solverOptions;                        // shared by 'solve' and 'batch'
```

```
$ java -jar cflp.jar solve a.json --solver foo
Invalid value for option '--solver': expected one of [MIP, SA] (case-insensitive) but was 'foo'
```

- `ValidateCommand implements Callable<Integer>` → the return value is the exit code (`&&`, `set -e`)
- `--threads` defaults to `${env:SLURM_CPUS_PER_TASK:-1}`
- `-t key=value` (repeatable) is parsed straight into a `Map<String, String>`

---

# Generate → solve → validate, for every solver

```java
@ParameterizedTest(name = "{0} on seed {1}")
@MethodSource("solversAndSeeds")                       // MIP×{1,2,3}, SA×{1,2,3}
void producesFeasibleSolution(SolverType type, long seed) {
    assumeTrue(type != SolverType.MIP || GurobiLicense.isAvailable(), "No Gurobi license");
    Instance inst = InstanceGenerator.generate("reg", 10, 40, 2.0, seed);
    ValidationResult result = Validator.validate(inst, create(type).solve(inst));
    assertTrue(result.feasible(), () -> result.violations().toString());
}
```

- Thanks to the `Solver` interface: add a solver to `SolverType` → it's tested automatically
- Refactor your heuristic on Monday, still trust the results on Friday
- `assumeTrue` → **skipped**, not failed, on a laptop without a license
- Keep it fast: small instances, few iterations

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

# Logging configuration: `logback.xml`

`src/main/resources/logback.xml`: where messages go, in which format, from which level

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter"><level>INFO</level></filter>
    <encoder><pattern>%d{HH:mm:ss.SSS} %-5level %logger{0} - %msg%n</pattern></encoder>
</appender>
<logger name="gurobi" level="INFO"/>                 <!-- per-logger levels -->
<root level="DEBUG"> <appender-ref ref="CONSOLE"/> <appender-ref ref="FILE"/> </root>
```

- Console: concise. File: everything. Tests: a quiet `logback-test.xml`
- Much more is possible by configuration only, e.g. one log file per run in a batch →

---

# One log file per run: MDC + SiftingAppender

```xml
<appender name="RUN_FILE" class="ch.qos.logback.classic.sift.SiftingAppender">
    <discriminator><key>run</key><defaultValue>cflp</defaultValue></discriminator>
    <sift>
        <appender name="FILE-${run}" class="ch.qos.logback.core.FileAppender">
            <file>${LOG_DIR}/${run}.log</file> ...
```

```java
// MDC = per-thread key/value context, attached to every log message
try (RunLog ignored = RunLog.open(inst.name() + "." + solver.name())) {   // MDC.put("run", ...)
    Solution sol = solver.solve(inst);           // all logging in here → logs/cflp-30-150-2.sa.log
}                                                // MDC.remove("run") + close the file
```

`java -DLOG_DIR=/scratch-shared/$USER/logs -jar cflp.jar batch ...`
The events file uses the same trick, with `LogstashEncoder` (logstash-logback-encoder) instead of a text pattern.

---

# Slurm array job: one task per instance

```bash
#SBATCH --array=0-99                       # 100 tasks, run in parallel
FILES=(instances/*.instance.json)
java -jar cflp.jar solve "${FILES[$SLURM_ARRAY_TASK_ID]}" --solver mip \
     --threads $SLURM_CPUS_PER_TASK -e big -o results
```

- Each task writes its own solution, log and events file → no locking needed
- A crashed task loses only itself; rerun just that index
- The solution records `slurmJobId` = `1234567_8` (job, task)

---

# Conventions and the rest of the template

```
instances/<instance>.instance.json[.gz]                    .gz is (de)compressed transparently
results/<experiment>/<instance>.<label>.solution.json
logs/<experiment>.<instance>.<label>.log
logs/<experiment>.<instance>.<label>.events.jsonl
```

| Keep as is | Replace for your problem |
|---|---|
| `cli/` commands, `RunExecutor`, `RunOptions` | `model/` Instance, generator |
| `io/` JSON, gzip, file names | `solver/` your algorithms |
| `util/` RunLog, Events, `logback.xml` | `validate/` your feasibility check |
| `snellius/` scripts, `.run/` configs | tests + reference values |

<span class="small">`analysis/collect.py` → `results.csv` · `viewer/index.html` draws a solution, no server needed.<br>
Also worth knowing: **JGraphT** (graphs), **fastutil** (primitive collections), **Commons Statistics**, **JMH** (benchmarks).</span>
