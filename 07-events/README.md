# Step 7: Structured logging with event objects

```bash
mvn package
java -jar target/cflp.jar generate -n 3 -f 30 -c 150
java -jar target/cflp.jar batch instances/ --solver sa      # logs/<run>.log and logs/<run>.events.jsonl
head -3 logs/cflp-30-150-1.sa.events.jsonl
```

Each line of an `.events.jsonl` file is one JSON object: `NewBest` (SA), `OperatorStats` (SA, one per operator at
the end), `Incumbent` and `MipSummary` (MIP). Read them with `pandas.read_json(file, lines=True)` or `jq`.
