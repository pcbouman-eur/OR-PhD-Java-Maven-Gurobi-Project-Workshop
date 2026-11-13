package org.example.cflp;

import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.InstanceGenerator;
import org.example.cflp.model.Solution;
import org.example.cflp.solver.MipSolver;
import org.example.cflp.solver.Solver;
import org.example.cflp.solver.sa.SaParams;
import org.example.cflp.solver.sa.SimulatedAnnealing;
import org.example.cflp.validate.ValidationResult;
import org.example.cflp.validate.Validator;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        // Usage:
        //   java -jar cflp.jar generate <facilities> <customers> <seed> <out.instance.json>
        //   java -jar cflp.jar solve <mip|sa> <in.instance.json> <out.solution.json>
        //   java -jar cflp.jar validate <in.instance.json> <in.solution.json>
        switch (args[0]) {
            case "generate" -> {
                int facilities = Integer.parseInt(args[1]);
                int customers = Integer.parseInt(args[2]);
                long seed = Long.parseLong(args[3]);
                String name = "cflp-" + facilities + "-" + customers + "-" + seed;
                JsonIO.write(Path.of(args[4]), InstanceGenerator.generate(name, facilities, customers, 3.0, seed));
            }
            case "solve" -> {
                Solver solver = switch (args[1]) {
                    case "mip" -> new MipSolver(60, 1);
                    case "sa" -> new SimulatedAnnealing(SaParams.defaults());
                    default -> throw new IllegalArgumentException("Unknown solver " + args[1]);
                };
                Instance inst = JsonIO.read(Path.of(args[2]), Instance.class);
                Solution sol = solver.solve(inst);
                JsonIO.write(Path.of(args[3]), sol);
                System.out.println(sol.solver() + " " + sol.status() + " " + sol.objective());
            }
            case "validate" -> {
                Instance inst = JsonIO.read(Path.of(args[1]), Instance.class);
                Solution sol = JsonIO.read(Path.of(args[2]), Solution.class);
                ValidationResult result = Validator.validate(inst, sol);
                System.out.println(result.feasible() ? "FEASIBLE " + result.objective() : "INFEASIBLE");
                result.violations().forEach(v -> System.out.println("  " + v));
            }
            default -> System.err.println("Unknown command " + args[0]);
        }
    }
}
