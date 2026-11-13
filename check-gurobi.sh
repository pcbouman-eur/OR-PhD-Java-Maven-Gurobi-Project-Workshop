#!/bin/bash
# Checks the Gurobi-dependent parts of the final template on a machine with a Gurobi license:
# MIP tests, a MIP run, Gurobi's log in the per-run log file, and MIP events.
# Usage: ./check-gurobi.sh        (uses ~/gurobi.lic or $GRB_LICENSE_FILE)
set -u
cd "$(dirname "$0")/08-template"
WORK=$(mktemp -d)
ok()   { echo "  OK    $1"; }
fail() { echo "  FAIL  $1"; FAILED=1; }
FAILED=0

echo "1. Build and run the tests (MIP tests are skipped if no license is found)"
if mvn -B -q package > "$WORK/mvn.log" 2>&1; then
    ok "build and tests passed"
else
    fail "build or tests failed, see $WORK/mvn.log"; exit 1
fi
SKIPPED=$(grep -ho 'skipped="[0-9]*"' target/surefire-reports/TEST-*.xml | grep -o '[0-9]*' | awk '{s+=$1} END {print s+0}')
[ "$SKIPPED" = "0" ] && ok "no skipped tests: Gurobi license found" || fail "$SKIPPED tests skipped: no usable Gurobi license"

JAR=$PWD/target/cflp.jar
cd "$WORK"
echo "2. Solve a small instance with Gurobi (working directory $WORK)"
java -jar "$JAR" generate -f 20 -c 100 -o instances > /dev/null
if java -jar "$JAR" solve instances/cflp-20-100-1.instance.json --solver mip -e check > solve.log 2>&1; then
    grep -q "OPTIMAL\|TIME_LIMIT" solve.log && ok "$(grep -o '\(OPTIMAL\|TIME_LIMIT\) objective [0-9.]*' solve.log)" || fail "unexpected output, see $WORK/solve.log"
else
    fail "MIP run failed: $(grep -m1 -o "Gurobi error.*" solve.log)"
fi

echo "3. Validate the solution"
java -jar "$JAR" validate instances results > validate.log 2>&1 && ok "solution is feasible" || fail "validation failed, see $WORK/validate.log"

echo "4. Gurobi log forwarded into the run log"
LOG=logs/check.cflp-20-100-1.mip.log
grep -q " gurobi - " "$LOG" 2>/dev/null \
    && ok "$(grep -c ' gurobi - ' "$LOG") Gurobi lines in $LOG" \
    || fail "no Gurobi lines in $LOG"

echo "5. MIP events"
EVENTS=logs/check.cflp-20-100-1.mip.events.jsonl
grep -q '"event":"Incumbent"' "$EVENTS" 2>/dev/null && grep -q '"event":"MipSummary"' "$EVENTS" \
    && ok "$(grep -c '"Incumbent"' "$EVENTS") Incumbent events and a MipSummary in $EVENTS" \
    || fail "MIP events missing in $EVENTS"

echo
[ $FAILED = 0 ] && echo "All Gurobi checks passed." || echo "Some checks failed; files are in $WORK"
exit $FAILED
