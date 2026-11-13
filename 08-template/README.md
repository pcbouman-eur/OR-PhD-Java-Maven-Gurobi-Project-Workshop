# CFLP experiment template

A template for algorithmic experiments in Java: a Gurobi MIP and a simulated annealing heuristic for the
single-source capacitated facility location problem, plus everything around them:
command line (picocli), JSON files (Jackson), tests (JUnit), logging (SLF4J + Logback) with structured events,
and traceable runs (UUIDs, experiment/label/tags).

## Quick start

```bash
mvn package                                              # tests + target/cflp.jar
java -jar target/cflp.jar generate --count 5 -f 30 -c 150
java -jar target/cflp.jar batch instances/ -e demo --solver sa
java -jar target/cflp.jar validate instances/ results/
python3 analysis/collect.py results/                     # results.csv, one row per run
```

Open `viewer/index.html` in a browser and drop instance and solution files on it.
In IntelliJ: run `DevMain` or use the run configurations in `.run/`.

## Layout

| Path | Contents |
|---|---|
| `src/main/java/.../model` | `Instance`, `Solution`, `RunInfo`, generator (problem specific) |
| `src/main/java/.../solver` | `Solver` interface, `MipSolver`, `sa/` simulated annealing (problem specific) |
| `src/main/java/.../validate` | independent feasibility and objective check (problem specific) |
| `src/main/java/.../cli` | picocli commands; `RunExecutor` handles one run (log, events, files) |
| `src/main/java/.../io`, `util` | JSON/gzip I/O, file naming, per-run logging, `Events` |
| `src/main/resources/logback.xml` | logging configuration (`-DLOG_DIR=...` to change the log directory) |
| `src/test` | unit and regression tests; MIP tests are skipped without a Gurobi license |
| `snellius/` | Slurm job scripts (batch and array job) |
| `analysis/`, `viewer/` | Python collector, static HTML viewer |

## Files

```
instances/<instance>.instance.json[.gz]
results/<experiment>/<instance>.<label>.solution.json
logs/<experiment>.<instance>.<label>.log
logs/<experiment>.<instance>.<label>.events.jsonl       one JSON object per line, each with the runId
```

## On Snellius

Put your Gurobi WLS license in `~/gurobi.lic`, copy `target/cflp.jar`, `instances/` and `snellius/*.sh` to `~/cflp`,
then `sbatch snellius/job.sh` or `sbatch --array=0-N snellius/array.sh`.
Check module names (`module avail Java`) and partitions in the SURF user documentation.
