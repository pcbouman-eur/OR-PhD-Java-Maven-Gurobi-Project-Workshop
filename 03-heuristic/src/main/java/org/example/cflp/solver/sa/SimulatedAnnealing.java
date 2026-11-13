package org.example.cflp.solver.sa;

import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.model.SolveStatus;
import org.example.cflp.solver.Solver;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class SimulatedAnnealing implements Solver {

    private final SaParams params;
    private final List<Operator> operators = Operators.all();

    public SimulatedAnnealing(SaParams params) {
        this.params = params;
    }

    @Override
    public String name() {
        return "sa";
    }

    @Override
    public Solution solve(Instance inst) {
        long start = System.nanoTime();
        Random rnd = new Random(params.seed());     // fixed seed => reproducible runs
        State state = new State(inst, greedy(inst), params.penalty());

        int[] best = state.snapshot();
        double bestCost = state.feasible() ? state.cost() : Double.POSITIVE_INFINITY;

        double scale = state.cost() / inst.numCustomers();
        double temperature = params.startTemperature() * scale;
        double cooling = Math.pow(params.endTemperature() / params.startTemperature(), 1.0 / params.iterations());

        for (long it = 0; it < params.iterations(); it++, temperature *= cooling) {
            Operator op = operators.get(rnd.nextInt(operators.size()));
            double before = state.cost();
            state.mark();
            if (!op.apply(state, rnd)) {
                continue;
            }
            double delta = state.cost() - before;
            if (delta <= 0 || rnd.nextDouble() < Math.exp(-delta / temperature)) {
                if (state.feasible() && state.cost() < bestCost - 1e-9) {
                    bestCost = state.cost();
                    best = state.snapshot();
                }
            } else {
                state.rollback();
            }
        }
        double runtime = (System.nanoTime() - start) / 1e9;
        return Solution.of(inst, name(), SolveStatus.FEASIBLE, runtime, best);
    }

    /** Largest demand first, to the facility with the lowest (opening +) assignment cost that has room. */
    static int[] greedy(Instance inst) {
        int[] assignment = new int[inst.numCustomers()];
        double[] residual = inst.facilities().stream().mapToDouble(f -> f.capacity()).toArray();
        boolean[] open = new boolean[inst.numFacilities()];
        Integer[] order = IntStream.range(0, inst.numCustomers()).boxed()
                .sorted(Comparator.comparingDouble(i -> -inst.customers().get(i).demand()))
                .toArray(Integer[]::new);
        for (int i : order) {
            double demand = inst.customers().get(i).demand();
            int best = -1;
            double bestCost = Double.POSITIVE_INFINITY;
            for (int j = 0; j < inst.numFacilities(); j++) {
                double c = inst.cost(i, j) + (open[j] ? 0 : inst.facilities().get(j).fixedCost());
                if (residual[j] >= demand && c < bestCost) {
                    best = j;
                    bestCost = c;
                }
            }
            if (best < 0) {   // nothing fits: least overloaded facility, penalty handles the rest
                best = IntStream.range(0, residual.length).boxed()
                        .max(Comparator.comparingDouble(j -> residual[j])).orElseThrow();
            }
            assignment[i] = best;
            residual[best] -= demand;
            open[best] = true;
        }
        return assignment;
    }
}
