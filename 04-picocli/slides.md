---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 4/8 Command line with picocli'
---

<!-- _class: lead -->
# Step 4: A real command-line interface

Same jar on your laptop, in IntelliJ, and in a Slurm script.

---

# Why a CLI?

On Snellius there is no IDE, only the shell:

```bash
java -jar cflp.jar <command> [options] [files]
```

- **Options** (`--solver mip`, `-o out/`) are named, have defaults, can come in any order
- **Parameters** (positional) are typically input files; the shell expands `instances/*.json`
- **Exit code** 0 = success, ≠ 0 = failure → usable in scripts (`&&`, `set -e`)
- Every experiment is one reproducible line in a job script

```xml
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>4.7.7</version>
</dependency>
```

---

# Subcommands

```java
@Command(name = "cflp", mixinStandardHelpOptions = true, version = "cflp 1.0",
         subcommands = { GenerateCommand.class, SolveCommand.class,
                         BatchCommand.class, ValidateCommand.class, HelpCommand.class })
public class App {
    public static void main(String[] args) {
        System.exit(new CommandLine(new App())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args));
    }
}
```

```
$ java -jar cflp.jar --help
Commands:
  generate  Generate random instances.
  solve     Solve a single instance.
  batch     Solve many instances, one after the other.
  validate  Check a solution against its instance. Exit code 1 if infeasible.
```

---

# A command = annotated fields + `run()`

```java
@Command(name = "solve", description = "Solve a single instance.")
public class SolveCommand implements Runnable {

    @Parameters(index = "0", description = "Instance file.")
    Path instanceFile;                                     // String → Path conversion

    @Option(names = {"-o", "--output-dir"}, defaultValue = "solutions",
            description = "Output directory (default: ${DEFAULT-VALUE}).")
    Path outputDir;

    @Mixin
    SolverOptions solverOptions;                           // shared with 'batch'

    public void run() {
        Instance inst = JsonIO.read(instanceFile, Instance.class);
        Solver solver = solverOptions.create();
        JsonIO.write(FileNames.solutionFile(outputDir, inst.name(), solver.name()),
                     solver.solve(inst));
    }
}
```

---

# Enums, mixins and exit codes

```java
public class SolverOptions {
    @Option(names = "--solver", defaultValue = "SA",
            description = "Solver: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
    SolverType solver;                                     // enum SolverType { MIP, SA }

    @Option(names = "--iterations", description = "[sa] ... (default: ${DEFAULT-VALUE}).")
    long iterations = SaParams.defaults().iterations();
    ...
}
```

```
$ java -jar cflp.jar solve a.json --solver foo
Invalid value for option '--solver': expected one of [MIP, SA] (case-insensitive) but was 'foo'
```

`ValidateCommand implements Callable<Integer>` → the return value is the exit code.

---

# Working from IntelliJ

```java
public class DevMain {
    public static void main(String[] args) {
        run("generate", "--count", "3", "--facilities", "30", "-o", "work/instances");
        run("batch", "work/instances", "--solver", "sa", "-o", "work/solutions");
        run("validate", "work/instances/cflp-30-150-1.instance.json", ...);
    }
    private static void run(String... args) { App.commandLine().execute(args); }
}
```

- The same commands as on Snellius, but with breakpoints and the debugger
- `.run/*.run.xml` → ready-made run configurations, shared through git:

<span class="small">`DevMain` · `cflp --help` · `cflp generate` · `cflp batch (SA)` · `cflp batch (MIP)`</span>

---

# On Snellius: batch or array job

```bash
# one job, all instances in sequence
java -jar cflp.jar batch instances/ --solver mip --threads $SLURM_CPUS_PER_TASK -o solutions
```

```bash
#SBATCH --array=0-99                       # one task per instance, run in parallel
FILES=(instances/*.instance.json)
java -jar cflp.jar solve "${FILES[$SLURM_ARRAY_TASK_ID]}" --solver mip \
     --threads $SLURM_CPUS_PER_TASK -o solutions
```

**Next:** how do we know a change didn't break a solver? → JUnit
