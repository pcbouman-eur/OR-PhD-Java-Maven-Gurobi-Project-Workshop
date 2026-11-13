# Step 4: Command line with picocli

```bash
mvn package
java -jar target/cflp.jar --help
java -jar target/cflp.jar generate --count 5 -o instances
java -jar target/cflp.jar batch instances/ --solver sa -o solutions
java -jar target/cflp.jar validate instances/cflp-20-100-1.instance.json solutions/cflp-20-100-1.sa.solution.json
```

In IntelliJ: run `DevMain`, or pick one of the run configurations from `.run/`.
On Snellius: `snellius/job.sh` (one batch job) or `snellius/array.sh` (one array task per instance).
