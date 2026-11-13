package org.example.cflp.cli;

import org.example.cflp.io.FileNames;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

@Command(name = "validate",
        description = "Check solutions against their instances, matched by instance id. Exit code 1 if any fails.")
public class ValidateCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(ValidateCommand.class);

    @Parameters(arity = "1..*", description = "Instance and solution files, and/or directories containing them.")
    List<Path> inputs;

    @Override
    public Integer call() {
        Map<UUID, Instance> instances = FileNames.expand(inputs, FileNames::isInstance).stream()
                .map(f -> JsonIO.read(f, Instance.class))
                .collect(Collectors.toMap(Instance::id, Function.identity()));
        List<Path> solutionFiles = FileNames.expand(inputs, FileNames::isSolution);

        int failed = 0;
        for (Path file : solutionFiles) {
            Solution sol = JsonIO.read(file, Solution.class);
            Instance inst = instances.get(sol.instanceId());
            if (inst == null) {
                log.error("{}: instance {} ({}) not found", file, sol.instanceName(), sol.instanceId());
                failed++;
                continue;
            }
            ValidationResult result = Validator.validate(inst, sol);
            if (result.feasible()) {
                log.info("{}: FEASIBLE, objective {}", file, String.format("%.2f", result.objective()));
            } else {
                log.error("{}: INFEASIBLE", file);
                result.violations().forEach(v -> log.error("  {}", v));
                failed++;
            }
        }
        log.info("Validated {} solutions against {} instances: {} failed", solutionFiles.size(), instances.size(), failed);
        return failed == 0 ? 0 : 1;
    }
}
