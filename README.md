# Java + Gurobi on Snellius

A sequence of eight small Maven projects for OR PhD students. It starts from a single Gurobi model and ends with
a template for algorithmic experiments that runs on Snellius, the Dutch national supercomputer (Slurm).
The running example is the **single-source capacitated facility location problem**, solved with a Gurobi MIP and
with simulated annealing.

Each `0N-*` directory is a complete, standalone project: open it in IntelliJ, or run `mvn package` in it.
Each step copies the previous one and adds one idea.

**Start with the slides:** [`slides/out/workshop.pdf`](slides/out/workshop.pdf) (or `workshop.html` in a browser).
The talk is followed by a try-it section with all commands, exercises, and an appendix with more details.
Each step also has a short, more detailed deck of its own (`slides.md`, rendered in `slides/out/`).

## Requirements

- Java 21+ and Maven 3.9+ (IntelliJ ships both); internet access the first time, to download dependencies
- A Gurobi license for the MIP. An academic **WLS** license in `~/gurobi.lic` works everywhere, including Snellius.
  No Gurobi installation is needed. Without a license, everything except the MIP works, and the MIP tests are skipped.
- Optional: Python 3 for the analysis scripts (stdlib only)

## Quick start

```bash
cd 08-template
mvn package                                              # compile, test, build target/cflp.jar
java -jar target/cflp.jar --help
java -jar target/cflp.jar generate --count 5 -f 30 -c 150
java -jar target/cflp.jar batch instances/ -e demo --solver sa
java -jar target/cflp.jar validate instances/ results/
python3 analysis/collect.py results/
```

To check the Gurobi parts on a machine with a license (MIP tests, a MIP run, the Gurobi log in the run log, MIP events):

```bash
./check-gurobi.sh
```

## The steps and their main points

### 1. Maven + Gurobi → one jar (`01-maven-gurobi`)
- `pom.xml` declares dependencies; Maven downloads them. The standard layout is `src/main/java` and `src/test/java`.
- The official `com.gurobi:gurobi` package contains the Java API **and** the native libraries for all platforms.
- The shade plugin builds a *fat jar* containing all dependencies. Filtering out the non-Linux natives brings it from about 120 MB to about 15 MB.
  On Snellius you then only need `java -jar cflp.jar` and a license file.
- Always `dispose()` the Gurobi model and env. They hold native memory and a (WLS) license token.
- Gurobi uses all cores of a node by default, so set `Threads` to `$SLURM_CPUS_PER_TASK`.

### 2. JSON with Jackson (`02-jackson`)
- Records (`record Facility(double x, double y, ...)`) are immutable data classes that Jackson maps to JSON with no extra code.
- Enums are written as names (`"OPTIMAL"`). `List`, `Map` and nested records work out of the box.
- One shared `JsonMapper`. Files are named `name.instance.json` and `name.solution.json`, and are readable from Python, R, JavaScript and `jq`.

### 3. A heuristic and a validator (`03-heuristic`)
- The `Solver` interface means that I/O, the CLI and the tests don't care which algorithm runs.
- Simulated annealing with operators (shift, swap, close, open) works on a `State` that keeps its cost incrementally
  and can roll back rejected moves.
- Seed everything and use an iteration budget instead of a time limit. Runs are then **deterministic**.
- An independent `Validator` checks feasibility and recomputes the objective. Never trust the solver's own claims.

### 4. Command line with picocli (`04-picocli`)
- Subcommands `generate`, `solve`, `batch`, `validate`, built from annotated fields with defaults and generated `--help`.
- Enums map to options (`--solver mip`), `@Mixin` shares options between commands, and the exit code signals success to scripts.
- `DevMain` runs the same commands from the IDE, with the debugger. The `.run/` folder holds shared IntelliJ run configurations.
- Slurm: one `batch` job, or an array job with one task per instance (`snellius/`).

### 5. Testing with JUnit (`05-junit`)
- Tests live in `src/test/java`, in the same package as the code under test. `mvn test` runs them, and so does `mvn package`.
- Unit tests: the validator on a hand-made instance, JSON round trips (`assertEquals` works on records), and a check that
  the incremental cost in `State` matches a full recomputation.
- A parameterized generate → solve → validate test for every solver. `assumeTrue` skips the MIP tests without a license.
- Regression on quality: SA must not be worse than reference values in `src/test/resources/sa-reference.csv`.

### 6. Logging with SLF4J + Logback (`06-logging`)
- Code uses the SLF4J API (`log.info("Solving {}", name)`). Logback writes the output, configured in `logback.xml`.
- The console shows INFO and above; the files get DEBUG. Tests use a quiet `logback-test.xml`.
- **One log file per run**: `RunLog` puts the run name in the MDC, and a `SiftingAppender` picks the file from it.
- **Gurobi's log** goes through a callback (`CB_MESSAGE`) into the logger named `gurobi`, so it lands in the same per-run file.

### 7. Structured logging (`07-events`)
- Next to log *sentences* for humans, log *objects* for analysis: `Events.log(new NewBest(it, op, cost, seconds))`.
- `Events` is a small wrapper: the object goes to the logger `events` as structured arguments of
  [logstash-logback-encoder](https://github.com/logfellow/logstash-logback-encoder): `kv("event", className)` and
  `fields(obj)` (serialized by Jackson). Its `LogstashEncoder` writes JSON Lines to `logs/<run>.events.jsonl`, one file
  per run (same MDC + SiftingAppender trick as the normal log), with time, level, logger and MDC on every line.
- SA: a `NewBest` event per improvement, and `OperatorStats` (counted in plain fields, logged once at the end) per operator.
  MIP: an `Incumbent` event from the `MIPSOL` callback, and a `MipSummary` (bound, gap, nodes) after `optimize()`.
- Read with `pandas.read_json(file, lines=True)` or `jq`. No extra library; events are switched off with one line of config.

### 8. The template (`08-template`)
- **UUIDs**: every instance has an `id`, and every solution stores its `instanceId`. `validate instances/ results/` matches solutions to instances by id.
- **Run metadata** in every solution: run id, experiment (`-e`), label (`-l`), tags (`-t key=value`), all parameters,
  start time, host and Slurm job id.
- File layout `results/<experiment>/<instance>.<label>.solution.json`. Instances may be `.json.gz`.
- The MDC also holds the `runId`, so every event line can be joined with the run's solution.
- `analysis/collect.py` produces `results.csv` (one row per run).
- `viewer/index.html`: drop instance and solution files on it to see the solution drawn. No server needed.
- To reuse the template, replace `model/`, `solver/` and `validate/`, and keep the rest.

## Running on Snellius

```bash
scp 08-template/target/cflp.jar snellius:~/cflp/          # plus instances/ and snellius/*.sh
ssh snellius
cd ~/cflp
sbatch snellius/job.sh                                     # one job, all instances
sbatch --array=0-99 snellius/array.sh                      # one task per instance
```

Put your WLS license in `~/gurobi.lic`. Before your first real run, check the module names (`module avail Java`) and
partitions in the SURF documentation. Also test that WLS works from a compute node with a small job, and check how
many concurrent sessions your license allows before submitting large array jobs.

## Slides

The slides are [Marp](https://marp.app) Markdown: `---` separates slides. VS Code's Marp extension gives a live preview.

```bash
./build-slides.sh            # -> slides/out/*.html (open in a browser; F for fullscreen); needs Node.js
./build-slides.sh --pdf      # also slides/out/*.pdf; needs Chrome/Chromium (or CHROME_PATH)
```

| Deck | Topic |
|---|---|
| `slides/workshop.md` | **The single deck**: ~30 min talk, discussion questions, try-it guide, appendix |
| `slides/00-intro.md` | Overview of the per-step decks |
| `01-maven-gurobi/slides.md` … `08-template/slides.md` | One deck per step |

## License

- Code (all Java, Python, shell scripts, configuration and the viewer): [MIT](LICENSE).
  Use the template for your own research as you like.
- Slides and texts (`slides/`, `*/slides.md`, the README files): [CC BY 4.0](LICENSE-CC-BY-4.0.txt).
