# Step 1: Maven + Gurobi

```bash
mvn package                               # -> target/cflp.jar (fat jar, linux64 natives only)
java -jar target/cflp.jar 20 100 1        # facilities customers seed [threads]
```

On Snellius: copy `target/cflp.jar` and `snellius/job.sh`, put your WLS `gurobi.lic` in `$HOME`, then `sbatch job.sh`.
Check module and partition names with `module avail Java` and the SURF user documentation.
