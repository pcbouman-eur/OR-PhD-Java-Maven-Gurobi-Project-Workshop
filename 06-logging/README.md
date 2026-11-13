# Step 6: Logging with SLF4J + Logback

```bash
mvn package
java -jar target/cflp.jar generate -n 3 -f 30 -c 150
java -jar target/cflp.jar batch instances/ --solver sa
ls logs/                                   # one log file per instance and solver
java -DLOG_DIR=/tmp/mylogs -jar target/cflp.jar batch instances/   # other log directory
```

Configuration: `src/main/resources/logback.xml` (tests use `src/test/resources/logback-test.xml`).
