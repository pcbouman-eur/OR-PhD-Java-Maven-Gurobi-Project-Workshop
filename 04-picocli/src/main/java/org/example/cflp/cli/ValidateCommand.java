package org.example.cflp.cli;

import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.validate.ValidationResult;
import org.example.cflp.validate.Validator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "validate", description = "Check a solution against its instance. Exit code 1 if infeasible.")
public class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Instance file.")
    Path instanceFile;

    @Parameters(index = "1", description = "Solution file.")
    Path solutionFile;

    @Override
    public Integer call() {
        Instance inst = JsonIO.read(instanceFile, Instance.class);
        Solution sol = JsonIO.read(solutionFile, Solution.class);
        ValidationResult result = Validator.validate(inst, sol);
        System.out.println(result.feasible() ? "FEASIBLE, objective " + result.objective() : "INFEASIBLE");
        result.violations().forEach(v -> System.out.println("  " + v));
        return result.feasible() ? 0 : 1;
    }
}
