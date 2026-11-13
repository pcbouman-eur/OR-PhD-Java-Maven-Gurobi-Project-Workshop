package org.example.cflp.cli;

import org.example.cflp.io.FileNames;
import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.solver.Solver;
import org.example.cflp.util.RunLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** One run = one instance + one solver: own log and event files, solution written to disk. */
final class RunExecutor {

    private static final Logger log = LoggerFactory.getLogger(RunExecutor.class);

    private RunExecutor() {
    }

    static Solution run(Instance inst, Solver solver, Path outputDir) {
        try (RunLog ignored = RunLog.open(inst.name() + "." + solver.name())) {
            Solution sol = solver.solve(inst);
            JsonIO.write(FileNames.solutionFile(outputDir, inst.name(), solver.name()), sol);
            log.info("{} objective {} in {}s", sol.status(), String.format("%.2f", sol.objective()),
                    String.format("%.2f", sol.runtimeSeconds()));
            return sol;
        }
    }
}
