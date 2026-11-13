---
marp: true
theme: workshop
paginate: true
footer: 'Java + Gurobi on Snellius · 2/8 JSON with Jackson'
---

<!-- _class: lead -->
# Step 2: Instances and solutions as JSON

Stop writing parsers by hand.

---

# Why bother?

- Hand-written text formats: a parser **and** a writer per format, and they drift apart
- JSON is self-describing: field names travel with the data
- Readable from Python, R, JavaScript, `jq`, ... → analysis outside Java
- Convention: `name.instance.json`, `name.solution.json`

```xml
<dependency>
    <groupId>tools.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>3.2.4</version>
</dependency>
```

<span class="small">Jackson 3 (`tools.jackson.*`) is the successor of Jackson 2 (`com.fasterxml.jackson.*`); most tutorials online still show 2, and the API is nearly identical.</span>

---

# Records: data classes for free

```java
public record Facility(double x, double y, double capacity, double fixedCost) { }

public record Instance(String name, List<Facility> facilities, List<Customer> customers) {
    public double cost(int i, int j) { ... }       // methods are allowed
}
```

A record gets a constructor, accessors (`f.capacity()`), `equals`, `hashCode` and `toString`.
Fields are `final`: an instance cannot change after it's loaded.

Jackson maps record components ↔ JSON fields:

```json
{ "name" : "cflp-3-4-1",
  "facilities" : [ { "x" : 0.116, "y" : 0.771, "capacity" : 70.0, "fixedCost" : 126.0 }, ... ],
  "customers"  : [ { "x" : 0.1, "y" : 0.407, "demand" : 18.0 }, ... ] }
```

---

# Enums and nested types

```java
public enum SolveStatus { OPTIMAL, FEASIBLE, TIME_LIMIT, INFEASIBLE }

public record Solution(String instanceName, String solver, SolveStatus status,
                       double objective, double runtimeSeconds,
                       List<Integer> openFacilities, List<Integer> assignment) { }
```

```json
{ "instanceName" : "cflp-20-100-1", "solver" : "mip", "status" : "OPTIMAL",
  "objective" : 2711.4, "runtimeSeconds" : 0.8,
  "openFacilities" : [ 2, 5, 11 ], "assignment" : [ 5, 2, 2, 11, ... ] }
```

- Enums are written by name, and unknown names fail on reading → no "magic integers"
- `List`, `Map`, `Optional`, `Instant`, `Duration`, nested records: all work out of the box

---

# Reading and writing

```java
public final class JsonIO {
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();                         // create once: thread-safe, expensive to build

    public static void write(Path file, Object value) {
        MAPPER.writeValue(file.toFile(), value);
    }
    public static <T> T read(Path file, Class<T> type) {
        return MAPPER.readValue(file.toFile(), type);
    }
}
```

```java
Instance inst = JsonIO.read(Path.of("a.instance.json"), Instance.class);
JsonIO.write(Path.of("a.solution.json"), solver.solve(inst));
```

Need something special? `@JsonProperty("cap")`, `@JsonIgnore`, `@JsonInclude(NON_NULL)`.

---

# The same files from Python

```python
import json
inst = json.load(open("a.instance.json"))
sol  = json.load(open("a.solution.json"))
print(sol["status"], sol["objective"], len(sol["openFacilities"]))
```

```bash
jq '.objective' solutions/*.solution.json        # quick look on Snellius
```

- Your solver stays in Java; plots and tables happen in pandas, R, or a web page
- The **solution file** is the experimental record, so store more than you need now

---

# Try it

```bash
mvn package
java -jar target/cflp.jar generate 20 100 1 a.instance.json
java -jar target/cflp.jar solve a.instance.json a.solution.json
python analysis/inspect.py a.instance.json a.solution.json
```

**Next:** a second solver (simulated annealing) and a validator that checks any solution.
