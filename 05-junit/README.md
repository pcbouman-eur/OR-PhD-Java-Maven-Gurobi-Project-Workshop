# Step 5: Testing with JUnit

```bash
mvn test                      # runs all tests in src/test/java
mvn package -DskipTests       # build the jar without running tests
```

MIP tests are skipped (not failed) when no Gurobi license is available.
