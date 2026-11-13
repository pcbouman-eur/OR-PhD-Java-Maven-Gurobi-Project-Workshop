---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 7/8 Structured logging'
---

<!-- _class: lead -->
# Step 7: Structured logging

Log **objects**, not sentences, and analyse them later.

---

# Logs for humans, events for analysis

```
11:01:16 INFO  SimulatedAnnealing - Best solution: cost 2260.42 after 0.44s     ← for you
```
```json
{"time":"2026-11-13T11:42:10.92Z","logger":"events","level":"INFO","run":"cflp-30-150-1.sa",
 "event":"NewBest","iteration":34,"operator":"close","cost":3935.30,"seconds":0.035}
```

- Questions after an experiment: *how often does an operator improve? when are new incumbents found?*
- Regular expressions over log text break as soon as you change a message
- **Event** = a record with the numbers you care about, written as one JSON line ("JSON Lines")
- Same logging framework, same per-run files, just a different output format

---

# Events in code

```java
public record NewBest(long iteration, String operator, double cost, double seconds) { }

if (state.feasible() && state.cost() < bestCost) {           // inside the SA loop
    ...
    Events.log(new NewBest(it, op.name(), bestCost, elapsed()));
}
```

Or count in a plain object and log it **once** at the end (cheap: no I/O in the hot loop):

```java
s.attempts++;  if (delta < 0) s.improvement(-delta); ...    // OperatorStats per operator
for (OperatorStats s : stats) Events.log(s);
```

`Events` is a 10-line helper: `log.info("{} {}", kv("event", obj.getClass().getSimpleName()), fields(obj))`
on the logger `"events"`; `kv` and `fields` come from **logstash-logback-encoder**.

---

# Routing events: `logback.xml`

```xml
<logger name="events" level="INFO" additivity="false">    <!-- not to console/normal log -->
    <appender-ref ref="EVENTS"/>                            <!-- logs/<run>.events.jsonl -->
</logger>
```

```xml
<appender name="EVENTS-${run}" class="ch.qos.logback.core.FileAppender">
    <file>${LOG_DIR}/${run}.events.jsonl</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>   <!-- JSON via Jackson -->
</appender>
```

```json
{"time":"...","logger":"events","level":"INFO","run":"cflp-30-150-1.sa","event":"NewBest","iteration":34,...}
```

- Every line: time, logger, level, the MDC (`run`, later `runId`), and the object's own fields
- Switch events off with `level="OFF"`; the de facto standard for JSON logging with Logback

---

# The MIP too: callbacks

```java
} else if (where == GRB.CB_MIPSOL) {                     // GurobiCallback: new incumbent
    Events.log(new Incumbent(getDoubleInfo(GRB.CB_RUNTIME),
                             getDoubleInfo(GRB.CB_MIPSOL_OBJ), getDoubleInfo(GRB.CB_MIPSOL_OBJBND)));
}
```

```java
Events.log(new MipSummary(objVal, objBound, mipGap, nodeCount, solCount, runtime));   // after optimize()
```

Same idea for column generation (`PricingRound(iteration, columns, reducedCost)`) or cuts (`CutsAdded(family, count)`).

---

# From events to insight

```python
import pandas as pd
ev = pd.read_json("logs/cflp-30-150-1.sa.events.jsonl", lines=True)
ev[ev.event == "OperatorStats"][["operator", "attempts", "notApplicable", "improved", "newBest"]]
ev[ev.event == "NewBest"].plot(x="seconds", y="cost")        # convergence plot
```

```bash
jq -c 'select(.event == "OperatorStats") | {operator, improved}' logs/*.events.jsonl
```

**Next:** tying files together: IDs, labels, tags → the final template.
