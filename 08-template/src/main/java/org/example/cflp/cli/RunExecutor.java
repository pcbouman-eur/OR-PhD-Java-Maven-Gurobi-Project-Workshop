package org.example.cflp.cli;

import org.example.cflp.io.FileNames;
import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.RunInfo;
import org.example.cflp.model.Solution;
import org.example.cflp.solver.Solver;
import org.example.cflp.util.RunLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/** One run = one instance + one solver: own log and event files, solution written to disk. */
final class RunExecutor {

    private static final Logger log = LoggerFactory.getLogger(RunExecutor.class);

    private RunExecutor() {
    }

    static Solution run(Instance inst, Solver solver, RunOptions options) {
        String label = options.labelOr(solver.name());
        Path dir = options.experimentDir();
        RunInfo info = new RunInfo(UUID.randomUUID(), options.experiment, label, options.tags,
                JsonIO.toMap(solver.parameters()), Instant.now(), hostName(), slurmJobId());
        try (RunLog ignored = RunLog.open(options.experiment + "." + inst.name() + "." + label);
             MDC.MDCCloseable runId = MDC.putCloseable("runId", info.runId().toString())) {   // in every event
            log.info("Run {} of {} ({}) with {} {}", info.runId(), inst.name(), inst.id(), solver.name(), info.parameters());
            Solution sol = solver.solve(inst).withRun(info);
            JsonIO.write(FileNames.solutionFile(dir, inst.name(), label), sol);
            log.info("{} objective {} in {}s", sol.status(), String.format("%.2f", sol.objective()),
                    String.format("%.2f", sol.runtimeSeconds()));
            return sol;
        }
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    /** "1234567" for a normal job, "1234567_8" for task 8 of an array job, null outside Slurm. */
    private static String slurmJobId() {
        String array = System.getenv("SLURM_ARRAY_JOB_ID");
        if (array != null) {
            return array + "_" + System.getenv("SLURM_ARRAY_TASK_ID");
        }
        return System.getenv("SLURM_JOB_ID");
    }
}
