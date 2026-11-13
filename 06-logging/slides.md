---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 6/8 Logging'
---

<!-- _class: lead -->
# Step 6: Logging with SLF4J + Logback

A log file per run, Gurobi's log included.

---

# Why not `System.out.println`?

- No timestamps, no levels: *was this an error or progress info?*
- All or nothing: debugging output must be deleted (and re-added) by hand
- In a batch of 200 instances, `slurm-123.out` becomes one long mess

A logging framework separates **what** you log (in code) from **where/how much** (in config):

| Level | Use for |
|---|---|
| `ERROR` | something failed (an instance could not be solved) |
| `WARN` | suspicious, but continuing |
| `INFO` | milestones: start, result, timing |
| `DEBUG` | progress inside the algorithm |

---

# API vs. backend

```xml
<dependency>  <!-- the API your code uses -->
    <groupId>org.slf4j</groupId> <artifactId>slf4j-api</artifactId> <version>2.0.19</version>
</dependency>
<dependency>  <!-- the implementation that writes the output -->
    <groupId>ch.qos.logback</groupId> <artifactId>logback-classic</artifactId> <version>1.6.6</version>
</dependency>
```

```java
private static final Logger log = LoggerFactory.getLogger(SimulatedAnnealing.class);

log.info("Solving {} with {}", inst.name(), params);           // {} = placeholder
log.debug("it {} T {} best {}", it, temperature, bestCost);     // cheap when DEBUG is off
log.error("Failed to solve {}", file, e);                       // exception last → stack trace
```

Libraries log via SLF4J too → one configuration controls everything.

---

# Configuration: `src/main/resources/logback.xml`

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>INFO</level>                                  <!-- console: concise -->
    </filter>
    <encoder><pattern>%d{HH:mm:ss.SSS} %-5level [%X{run:-main}] %logger{0} - %msg%n</pattern></encoder>
</appender>

<logger name="gurobi" level="INFO"/>                         <!-- per-logger levels -->

<root level="DEBUG">                                         <!-- file: everything -->
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="RUN_FILE"/>
</root>
```

`src/test/resources/logback-test.xml` replaces it during tests (WARN only, no files).

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

```
logs/cflp.log   logs/cflp-30-150-1.sa.log   logs/cflp-30-150-2.sa.log   ...
```

`java -DLOG_DIR=/scratch-shared/$USER/logs -jar cflp.jar batch ...`

---

# Gurobi's log → your log

```java
env.set(GRB.IntParam.LogToConsole, 0);       // Gurobi stops printing itself...
model.setCallback(new GurobiLogBridge());    // ...and hands us every line instead

public class GurobiLogBridge extends GRBCallback {
    private static final Logger log = LoggerFactory.getLogger("gurobi");
    protected void callback() {
        if (where == GRB.CB_MESSAGE) {
            log.info(getStringInfo(GRB.CB_MSG_STRING).stripTrailing());
        }
    }
}
```

- Gurobi lines land in the same per-run file, between your own messages
- Silence them with `<logger name="gurobi" level="WARN"/>`
- Alternative: `env.set(GRB.StringParam.LogFile, "run.gurobi.log")` for a separate file

---

# Try it

```bash
java -jar target/cflp.jar generate -n 3 -f 30 -c 150
java -jar target/cflp.jar batch instances/ --solver sa
cat logs/cflp-30-150-2.sa.log
```
```
18:20:30.143 DEBUG [main] o.e.c.solver.sa.SimulatedAnnealing - it 0 T 52.3389 current 3925.42 best 3925.42
18:20:30.159 DEBUG [main] o.e.c.solver.sa.SimulatedAnnealing - it 10000 T 24.4749 current 3109.60 best 2757.27
...
18:20:30.263 INFO  [main] o.e.c.solver.sa.SimulatedAnnealing - Best solution: cost 2218.60 after 0.12s
```

Logs are for **humans**. For numbers you want to analyse → **next:** structured logging with event objects.
