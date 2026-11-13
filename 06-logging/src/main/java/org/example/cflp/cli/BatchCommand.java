package org.example.cflp.cli;

import org.example.cflp.io.FileNames;
import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.solver.Solver;
import org.example.cflp.util.RunLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;

@Command(name = "batch", description = "Solve many instances, one after the other.")
public class BatchCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BatchCommand.class);

    @Parameters(arity = "1..*", description = "Instance files and/or directories containing *.instance.json files.")
    List<Path> inputs;

    @Option(names = {"-o", "--output-dir"}, defaultValue = "solutions", description = "Output directory (default: ${DEFAULT-VALUE}).")
    Path outputDir;

    @Mixin
    SolverOptions solverOptions;

    @Override
    public void run() {
        List<Path> files = FileNames.expandInstances(inputs);
        Solver solver = solverOptions.create();
        log.info("Solving {} instances with {}", files.size(), solver.name());
        int failed = 0;
        for (Path instanceFile : files) {
            Instance inst = JsonIO.read(instanceFile, Instance.class);
            try (RunLog ignored = RunLog.open(inst.name() + "." + solver.name())) {
                Solution sol = solver.solve(inst);
                JsonIO.write(FileNames.solutionFile(outputDir, inst.name(), solver.name()), sol);
                log.info("{} objective {} in {}s", sol.status(), String.format("%.2f", sol.objective()),
                        String.format("%.2f", sol.runtimeSeconds()));
            } catch (RuntimeException e) {
                failed++;
                log.error("Failed to solve {}", instanceFile, e);   // exception as last argument: stack trace
            }
        }
        log.info("Done: {} solved, {} failed", files.size() - failed, failed);
    }
}
