"""Collect all runs below a directory into one CSV file (stdlib only):

  results.csv  one row per run: experiment, label, tags, parameters, instance, solver, objective, ...

Usage: python collect.py results/ [output-dir]
Then e.g.: df = pandas.read_csv("results.csv"); df.groupby(["experiment", "label"]).objective.mean()
Events (logs/*.events.jsonl) carry the runId, so they join on this table:
  ev = pandas.read_json("logs/....events.jsonl", lines=True); ev.merge(df, on="runId")
"""
import csv
import json
import sys
from pathlib import Path

root = Path(sys.argv[1])
out = Path(sys.argv[2]) if len(sys.argv) > 2 else root


def write_csv(path, rows, first_columns):
    columns = first_columns + sorted({k for r in rows for k in r} - set(first_columns))
    with path.open("w", newline="") as f:
        writer = csv.DictWriter(f, columns)
        writer.writeheader()
        writer.writerows(rows)
    print(f"Wrote {len(rows)} rows to {path}")


runs = []
for file in sorted(root.rglob("*.solution.json")):
    sol = json.loads(file.read_text())
    run = sol["run"] or {}
    runs.append({
        "runId": run.get("runId"), "experiment": run.get("experiment"), "label": run.get("label"),
        **{f"tag.{k}": v for k, v in (run.get("tags") or {}).items()},
        **{f"param.{k}": v for k, v in (run.get("parameters") or {}).items()},
        "instance": sol["instanceName"], "instanceId": sol["instanceId"], "solver": sol["solver"],
        "status": sol["status"], "objective": sol["objective"], "runtimeSeconds": sol["runtimeSeconds"],
        "openFacilities": len(sol["openFacilities"]),
        "startedAt": run.get("startedAt"), "host": run.get("host"), "slurmJobId": run.get("slurmJobId"),
    })
write_csv(out / "results.csv", runs, ["runId", "experiment", "label", "instance", "solver", "objective"])

# Quick summary: mean objective per experiment and label
groups = {}
for r in runs:
    groups.setdefault((r["experiment"], r["label"]), []).append(r["objective"])
print(f"\n{'experiment':<16}{'label':<12}{'runs':>6}{'mean objective':>16}")
for (experiment, label), values in sorted(groups.items(), key=lambda kv: str(kv[0])):
    print(f"{experiment!s:<16}{label!s:<12}{len(values):>6}{sum(values) / len(values):>16.2f}")
