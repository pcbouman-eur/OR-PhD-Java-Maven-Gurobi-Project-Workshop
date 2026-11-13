package org.example.cflp.cli;

import org.example.cflp.io.FileNames;
import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.solver.Solver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;

@Command(name = "batch", description = "Solve many instances, one after the other.")
public class BatchCommand implements Runnable {

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
        for (Path instanceFile : files) {
            Instance inst = JsonIO.read(instanceFile, Instance.class);
            Solution sol = solver.solve(inst);
            JsonIO.write(FileNames.solutionFile(outputDir, inst.name(), solver.name()), sol);
            System.out.printf("%-20s %-4s %-10s %12.2f %8.2fs%n",
                    inst.name(), sol.solver(), sol.status(), sol.objective(), sol.runtimeSeconds());
        }
    }
}
