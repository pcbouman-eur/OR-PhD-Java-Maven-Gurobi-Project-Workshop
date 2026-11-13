package org.example.cflp;

import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.InstanceGenerator;
import org.example.cflp.model.Solution;
import org.example.cflp.solver.MipSolver;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        // Usage:
        //   java -jar cflp.jar generate <facilities> <customers> <seed> <out.instance.json>
        //   java -jar cflp.jar solve <in.instance.json> <out.solution.json> [threads]
        switch (args[0]) {
            case "generate" -> {
                int facilities = Integer.parseInt(args[1]);
                int customers = Integer.parseInt(args[2]);
                long seed = Long.parseLong(args[3]);
                Path out = Path.of(args[4]);
                String name = "cflp-" + facilities + "-" + customers + "-" + seed;
                JsonIO.write(out, InstanceGenerator.generate(name, facilities, customers, 3.0, seed));
            }
            case "solve" -> {
                Instance inst = JsonIO.read(Path.of(args[1]), Instance.class);
                int threads = args.length > 3 ? Integer.parseInt(args[3]) : 1;
                Solution sol = new MipSolver(60, threads).solve(inst);
                JsonIO.write(Path.of(args[2]), sol);
                System.out.println(sol.status() + " " + sol.objective());
            }
            default -> System.err.println("Unknown command " + args[0]);
        }
    }
}
