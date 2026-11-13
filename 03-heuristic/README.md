# Step 3: Simulated annealing + validator

```bash
mvn package
java -jar target/cflp.jar generate 50 200 1 a.instance.json
java -jar target/cflp.jar solve sa a.instance.json a.solution.json     # or: solve mip ...
java -jar target/cflp.jar validate a.instance.json a.solution.json
```
