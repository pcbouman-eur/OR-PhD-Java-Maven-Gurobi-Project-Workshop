---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius'
---

<!-- _class: lead -->
# Java + Gurobi on Snellius

From a single `main` method to a template for algorithmic experiments, in 8 small steps.

---

# The plan

| Step | Adds |
|---|---|
| 1 | Maven, the official Gurobi package, a fat jar, a Slurm job |
| 2 | Instances and solutions as JSON (Jackson) |
| 3 | A heuristic behind a common `Solver` interface, a validator |
| 4 | A command-line interface (picocli), IntelliJ run configurations |
| 5 | Unit and regression tests (JUnit) |
| 6 | Logging, a log file per run, Gurobi's log included (SLF4J + Logback) |
| 7 | Algorithm statistics as data (structured logging) |
| 8 | UUIDs, experiment labels and tags: the final template |

Each step is a complete Maven project in its own directory: `01-maven-gurobi/` … `08-template/`.

---

# What you need

- Java 21 and Maven (IntelliJ ships both)
- A Gurobi academic **WLS** license → `~/gurobi.lic` (no Gurobi installation needed)
- A Snellius account, for the Slurm parts

```bash
cd 08-template
mvn package
java -jar target/cflp.jar --help
```
