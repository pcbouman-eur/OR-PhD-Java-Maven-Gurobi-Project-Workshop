"""The JSON files are readable from any language, e.g. Python (stdlib only)."""
import json
import sys

inst = json.load(open(sys.argv[1]))
sol = json.load(open(sys.argv[2]))

print(f"{inst['name']}: {len(inst['facilities'])} facilities, {len(inst['customers'])} customers")
print(f"{sol['solver']} -> {sol['status']}, objective {sol['objective']:.1f}, "
      f"{len(sol['openFacilities'])} open in {sol['runtimeSeconds']:.1f}s")
