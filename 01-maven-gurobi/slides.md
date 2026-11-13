---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 1/8 Maven & Gurobi'
---

<!-- _class: lead -->
# Step 1: Maven + Gurobi → one jar on Snellius

Goal: `java -jar cflp.jar` runs on any Linux node.
Only the **license file** is set up separately.

---

# Running example: single-source CFLP

$$
\begin{aligned}
\min\ & \textstyle\sum_j f_j y_j + \sum_{i,j} d_i\,\mathrm{dist}(i,j)\, x_{ij} \\
\text{s.t. } & \textstyle\sum_j x_{ij} = 1 && \forall i \\
& \textstyle\sum_i d_i x_{ij} \le Q_j y_j && \forall j \\
& x_{ij} \le y_j,\quad x, y \in \{0,1\}
\end{aligned}
$$

- Small model, but hard enough for real branch-and-bound
- Natural local search moves (shift, swap, open, close) for later steps
- Feasibility is easy to check → good for testing

---

# Maven in one slide

```
01-maven-gurobi/
├── pom.xml                  ← what to build, which dependencies
├── src/main/java/...        ← your code
├── src/test/java/...        ← tests (step 5)
└── target/cflp.jar          ← output of `mvn package`
```

- **Dependencies** are downloaded from Maven Central into `~/.m2`
- **Lifecycle:** `compile` → `test` → `package` (each runs the earlier ones)
- IntelliJ reads `pom.xml` directly: *Open* the folder and it works

---

# The official Gurobi package

```xml
<dependency>
    <groupId>com.gurobi</groupId>
    <artifactId>gurobi</artifactId>
    <version>13.0.3</version>
</dependency>
```

- Contains the Java API **and** the native libraries
  (`native/v13.0.3/linux64/libgurobi130.so`, win64, macOS, arm)
- No Gurobi installation, `module load gurobi` or `LD_LIBRARY_PATH` needed
- The `core` classifier has no natives, if you do want to use a local install

---

# A fat jar with the shade plugin

```xml
<plugin>
  <artifactId>maven-shade-plugin</artifactId>
  ...
  <transformer implementation="...ManifestResourceTransformer">
    <mainClass>org.example.cflp.Main</mainClass>   <!-- java -jar entry point -->
  </transformer>
  <filter>
    <artifact>com.gurobi:gurobi</artifact>
    <excludes>                                       <!-- Snellius = linux64 -->
      <exclude>native/*/win64/**</exclude>
      <exclude>native/*/macos_universal2/**</exclude>
      <exclude>native/*/armlinux64/**</exclude>
    </excludes>
  </filter>
```

`mvn package` → `target/cflp.jar` (13 MB) → `scp` it to Snellius. Done.

---

# Gurobi from Java: always clean up

```java
GRBEnv env = new GRBEnv(true);          // true = don't start yet
env.set(GRB.DoubleParam.TimeLimit, timeLimit);
env.set(GRB.IntParam.Threads, threads);
env.start();                            // license is checked here
GRBModel model = new GRBModel(env);
try {
    GRBVar y = model.addVar(0, 1, fixedCost, GRB.BINARY, "y" + j);
    ...
    model.optimize();
} finally {
    model.dispose();                    // frees native memory
    env.dispose();                      // releases the (WLS) license token
}
```

---

# License: the only thing outside the jar

Academic **WLS** license → download `gurobi.lic` from the Gurobi portal:

```
WLSACCESSID=...
WLSSECRET=...
LICENSEID=...
```

- Store it as `~/gurobi.lic` (the default location) or point to it:
  `export GRB_LICENSE_FILE=$HOME/gurobi.lic`
- WLS checks a token over HTTPS → **test with one small job first**
- Check how many concurrent sessions your license allows *before* submitting a 500-task array job

---

# Slurm job script

```bash
#!/bin/bash
#SBATCH --job-name=cflp
#SBATCH --partition=rome
#SBATCH --cpus-per-task=4
#SBATCH --time=00:10:00
#SBATCH --output=slurm-%j.out

module load 2024
module load Java/21.0.5
export GRB_LICENSE_FILE=$HOME/gurobi.lic

java -jar $HOME/cflp/cflp.jar 50 200 1 $SLURM_CPUS_PER_TASK
```

- `sbatch job.sh`, `squeue -u $USER`, `scancel <id>`
- ⚠️ Gurobi uses **all cores of the node** by default → set `Threads`

---

# Try it

```bash
mvn package
java -jar target/cflp.jar 20 100 1     # facilities customers seed [threads]
```

**Next:** `args[0]`, `args[1]`, … and printing to stdout don't scale.
→ Step 2: storing instances and solutions as JSON files.
