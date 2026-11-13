package org.example.cflp.cli;

import org.example.cflp.io.FileNames;
import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.solver.Solver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;

@Command(name = "batch", description = "Solve many instances, one after the other.")
public class BatchCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BatchCommand.class);

    @Parameters(arity = "1..*", description = "Instance files and/or directories containing *.instance.json[.gz] files.")
    List<Path> inputs;

    @Mixin
    RunOptions runOptions;

    @Mixin
    SolverOptions solverOptions;

    @Override
    public void run() {
        List<Path> files = FileNames.expand(inputs, FileNames::isInstance);
        Solver solver = solverOptions.create();
        log.info("Solving {} instances with {}", files.size(), solver.name());
        int failed = 0;
        for (Path instanceFile : files) {
            try {
                RunExecutor.run(JsonIO.read(instanceFile, Instance.class), solver, runOptions);
            } catch (RuntimeException e) {
                failed++;
                log.error("Failed to solve {}", instanceFile, e);   // exception as last argument: stack trace
            }
        }
        log.info("Done: {} solved, {} failed", files.size() - failed, failed);
    }
}
