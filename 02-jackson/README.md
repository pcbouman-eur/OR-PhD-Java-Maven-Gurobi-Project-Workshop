# Step 2: JSON with Jackson

```bash
mvn package
java -jar target/cflp.jar generate 20 100 1 a.instance.json
java -jar target/cflp.jar solve a.instance.json a.solution.json
python3 analysis/inspect.py a.instance.json a.solution.json
```
