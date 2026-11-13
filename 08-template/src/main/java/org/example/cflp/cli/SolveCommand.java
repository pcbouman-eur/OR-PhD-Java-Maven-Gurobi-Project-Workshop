package org.example.cflp.cli;

import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

@Command(name = "solve", description = "Solve a single instance.")
public class SolveCommand implements Runnable {

    @Parameters(index = "0", description = "Instance file.")
    Path instanceFile;

    @Mixin
    RunOptions runOptions;

    @Mixin
    SolverOptions solverOptions;

    @Override
    public void run() {
        Instance inst = JsonIO.read(instanceFile, Instance.class);
        RunExecutor.run(inst, solverOptions.create(), runOptions);
    }
}
