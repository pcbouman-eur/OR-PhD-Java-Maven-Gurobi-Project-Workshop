package org.example.cflp;

import org.example.cflp.model.Instance;
import org.example.cflp.model.InstanceGenerator;
import org.example.cflp.solver.MipSolver;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws Exception {
        // Usage: java -jar cflp.jar [facilities] [customers] [seed] [threads]
        int facilities = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int customers = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 1;
        int threads = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        Instance inst = InstanceGenerator.generate("rnd", facilities, customers, 3.0, seed);
        int[] assignment = new MipSolver(60, threads).solve(inst);

        System.out.println("Open facilities: " + Arrays.toString(Arrays.stream(assignment).distinct().sorted().toArray()));
    }
}
