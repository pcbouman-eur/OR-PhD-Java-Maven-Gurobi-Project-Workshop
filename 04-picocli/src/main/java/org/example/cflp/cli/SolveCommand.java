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

@Command(name = "solve", description = "Solve a single instance.")
public class SolveCommand implements Runnable {

    @Parameters(index = "0", description = "Instance file.")
    Path instanceFile;

    @Option(names = {"-o", "--output-dir"}, defaultValue = "solutions", description = "Output directory (default: ${DEFAULT-VALUE}).")
    Path outputDir;

    @Mixin
    SolverOptions solverOptions;

    @Override
    public void run() {
        Instance inst = JsonIO.read(instanceFile, Instance.class);
        Solver solver = solverOptions.create();
        Solution sol = solver.solve(inst);
        Path file = FileNames.solutionFile(outputDir, inst.name(), solver.name());
        JsonIO.write(file, sol);
        System.out.printf("%s %s %s objective %.2f in %.2fs -> %s%n",
                inst.name(), sol.solver(), sol.status(), sol.objective(), sol.runtimeSeconds(), file);
    }
}
