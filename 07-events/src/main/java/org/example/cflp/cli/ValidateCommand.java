package org.example.cflp.cli;

import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.validate.ValidationResult;
import org.example.cflp.validate.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "validate", description = "Check a solution against its instance. Exit code 1 if infeasible.")
public class ValidateCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(ValidateCommand.class);

    @Parameters(index = "0", description = "Instance file.")
    Path instanceFile;

    @Parameters(index = "1", description = "Solution file.")
    Path solutionFile;

    @Override
    public Integer call() {
        Instance inst = JsonIO.read(instanceFile, Instance.class);
        Solution sol = JsonIO.read(solutionFile, Solution.class);
        ValidationResult result = Validator.validate(inst, sol);
        if (result.feasible()) {
            log.info("{} is FEASIBLE, objective {}", solutionFile, result.objective());
        } else {
            log.error("{} is INFEASIBLE", solutionFile);
            result.violations().forEach(v -> log.error("  {}", v));
        }
        return result.feasible() ? 0 : 1;
    }
}
