package org.example.cflp.cli;

import org.example.cflp.solver.MipParams;
import org.example.cflp.solver.MipSolver;
import org.example.cflp.solver.Solver;
import org.example.cflp.solver.SolverType;
import org.example.cflp.solver.sa.SaParams;
import org.example.cflp.solver.sa.SimulatedAnnealing;
import picocli.CommandLine.Option;

/** Options shared by 'solve' and 'batch', included in both with @Mixin. */
public class SolverOptions {

    private static final SaParams SA = SaParams.defaults();

    @Option(names = "--solver", defaultValue = "SA", description = "Solver: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
    SolverType solver;

    @Option(names = "--time-limit", defaultValue = "60", description = "[mip] Time limit in seconds (default: ${DEFAULT-VALUE}).")
    double timeLimit;

    // Inside a Slurm job, default to the number of cores allocated to the job
    @Option(names = "--threads", defaultValue = "${env:SLURM_CPUS_PER_TASK:-1}",
            description = "[mip] Number of threads (default: $$SLURM_CPUS_PER_TASK or 1, now ${DEFAULT-VALUE}).")
    int threads;

    @Option(names = "--iterations", description = "[sa] Number of iterations (default: ${DEFAULT-VALUE}).")
    long iterations = SA.iterations();

    @Option(names = "--t-start", description = "[sa] Start temperature, relative (default: ${DEFAULT-VALUE}).")
    double startTemperature = SA.startTemperature();

    @Option(names = "--t-end", description = "[sa] End temperature, relative (default: ${DEFAULT-VALUE}).")
    double endTemperature = SA.endTemperature();

    @Option(names = "--penalty", description = "[sa] Penalty per unit of overloaded capacity (default: ${DEFAULT-VALUE}).")
    double penalty = SA.penalty();

    @Option(names = "--seed", description = "[sa] Random seed (default: ${DEFAULT-VALUE}).")
    long seed = SA.seed();

    public Solver create() {
        return switch (solver) {
            case MIP -> new MipSolver(new MipParams(timeLimit, threads));
            case SA -> new SimulatedAnnealing(new SaParams(iterations, startTemperature, endTemperature, penalty, seed));
        };
    }
}
