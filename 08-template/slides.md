---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 8/8 The template'
---

<!-- _class: lead -->
# Step 8: Keeping experiments traceable

IDs, labels, tags, and a template to start from.

---

# Which instance does this solution belong to?

```
results/cflp-30-150-1.sa.solution.json     ← "cflp-30-150-1" ... but which one?
```

- You regenerated instances after changing the generator: **same name, different data**
- A colleague sends `instance_3.json` and you already have one
- 400 result files after a weekend on Snellius: which settings produced which file?

→ Give every instance and every run a **unique ID**, and store the context *inside* the file.

---

# UUIDs

```java
UUID id = UUID.randomUUID();      // 468397b1-3ab5-4506-a899-b2bb49eaddfd
```

- 128 bits, generated anywhere without coordination; collisions are negligible in practice
- Versions: **v4** random (used here) · v3/v5 derived from a name (deterministic) · v7 time-ordered (sortable)
- Jackson reads/writes `UUID` as a string out of the box

```java
public record Instance(UUID id, String name, Map<String, String> properties, ...) { }
public record Solution(UUID instanceId, String instanceName, RunInfo run, ...) { }
```

```
$ java -jar cflp.jar validate instances/ results/       # matches solutions to instances by id
results/cooling/cflp-30-150-3.sa-warm.solution.json: instance cflp-30-150-3 (a122522d-...) not found
```

---

# Run metadata: experiment, label, tags

```bash
java -jar cflp.jar batch instances/ -e cooling -l sa-warm -t start=warm --t-start 2.0
```

```java
@Option(names = {"-t", "--tag"}, description = "Tag as key=value, repeatable")
Map<String, String> tags = new LinkedHashMap<>();                    // picocli parses k=v
```

```json
"run" : {
  "runId" : "09fbe0bc-63bc-42ee-8ea0-dd581098c804",
  "experiment" : "cooling",  "label" : "sa-warm",  "tags" : { "start" : "warm" },
  "parameters" : { "iterations" : 200000, "startTemperature" : 2.0, "seed" : 42, ... },
  "startedAt" : "2026-11-02T18:26:26.404Z",  "host" : "tcn123",  "slurmJobId" : "1234567_8"
}
```

**Label** = for humans and file names · **tags** = treatment groups for analysis · **parameters** = everything, including defaults

---

# Conventions

```
instances/<instance>.instance.json[.gz]
results/<experiment>/<instance>.<label>.solution.json
logs/<experiment>.<instance>.<label>.log
logs/<experiment>.<instance>.<label>.events.jsonl      every line carries the runId
```

- `.gz` is (de)compressed transparently: `GZIPInputStream` around the file stream
  (instances with 10⁶ numbers shrink ~4×; Python: `gzip.open`, browsers: `DecompressionStream`)
- `--threads` defaults to `${env:SLURM_CPUS_PER_TASK:-1}` (picocli variable)
- One file per run: no locking, array jobs can't clash, a crashed run loses only itself

---

# From results to tables and pictures

```bash
python analysis/collect.py results/       # → results.csv (one row per run)
```
```
experiment      label         runs  mean objective
cooling         sa-cold          3         2977.65
cooling         sa-warm          3         2549.08
```

```python
df = pd.read_csv("results/results.csv")
df.pivot_table(index="instance", columns="tag.start", values="objective")
```

`viewer/index.html`: a static page; drop instance and solution files on it and it draws the facilities, customers and assignment. No server needed.

---

# Using the template

| Keep as is | Replace for your problem |
|---|---|
| `cli/` commands, `RunExecutor`, `RunOptions` | `model/` Instance, generator |
| `io/` JSON, gzip, file names | `solver/` your algorithms |
| `util/RunLog`, `util/Events`, `logback.xml` | `validate/` your feasibility check |
| `snellius/` scripts, `.run/` configs | tests + reference values |

Other libraries worth knowing: **JGraphT** (graphs, shortest paths, matching), **fastutil** (primitive collections), **Apache Commons Statistics**, **JMH** (micro-benchmarks).

---

# Recap

| Step | Tool | Gives you |
|---|---|---|
| 1 | Maven + Gurobi + shade | one jar, only a license outside |
| 2 | Jackson | instances/solutions as JSON |
| 3 | interfaces, seeds | interchangeable, reproducible solvers |
| 4 | picocli | a real CLI, IntelliJ run configs |
| 5 | JUnit | regression safety |
| 6 | SLF4J + Logback | a log file per run, Gurobi included |
| 7 | structured logging | algorithm statistics as data |
| 8 | UUIDs, metadata | traceable experiments |
