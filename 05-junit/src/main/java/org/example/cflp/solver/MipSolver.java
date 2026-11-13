package org.example.cflp.solver;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;
import org.example.cflp.model.SolveStatus;


/**
 * min  sum_j f_j y_j + sum_ij c_ij x_ij
 * s.t. sum_j x_ij = 1                 for all customers i
 *      sum_i d_i x_ij <= Q_j y_j       for all facilities j
 *      x_ij <= y_j                     (strengthens the LP relaxation)
 *      x, y binary
 */
public class MipSolver implements Solver {

    private final double timeLimit;
    private final int threads;

    public MipSolver(double timeLimit, int threads) {
        this.timeLimit = timeLimit;
        this.threads = threads;
    }

    @Override
    public String name() {
        return "mip";
    }

    @Override
    public Solution solve(Instance inst) {
        try {
            return solveModel(inst);
        } catch (GRBException e) {
            throw new IllegalStateException("Gurobi error " + e.getErrorCode() + ": " + e.getMessage(), e);
        }
    }

    private Solution solveModel(Instance inst) throws GRBException {
        int m = inst.numFacilities();
        int n = inst.numCustomers();

        // Env and model hold native memory and a license token: always dispose them
        GRBEnv env = new GRBEnv(true);
        env.set(GRB.DoubleParam.TimeLimit, timeLimit);
        env.set(GRB.IntParam.Threads, threads);
        env.start();
        GRBModel model = new GRBModel(env);
        try {
            GRBVar[] y = new GRBVar[m];
            GRBVar[][] x = new GRBVar[n][m];
            for (int j = 0; j < m; j++) {
                y[j] = model.addVar(0, 1, inst.facilities().get(j).fixedCost(), GRB.BINARY, "y" + j);
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    x[i][j] = model.addVar(0, 1, inst.cost(i, j), GRB.BINARY, "x" + i + "_" + j);
                }
            }
            for (int i = 0; i < n; i++) {
                GRBLinExpr assign = new GRBLinExpr();
                for (int j = 0; j < m; j++) {
                    assign.addTerm(1, x[i][j]);
                    model.addConstr(x[i][j], GRB.LESS_EQUAL, y[j], "link" + i + "_" + j);
                }
                model.addConstr(assign, GRB.EQUAL, 1, "assign" + i);
            }
            for (int j = 0; j < m; j++) {
                GRBLinExpr load = new GRBLinExpr();
                for (int i = 0; i < n; i++) {
                    load.addTerm(inst.customers().get(i).demand(), x[i][j]);
                }
                load.addTerm(-inst.facilities().get(j).capacity(), y[j]);
                model.addConstr(load, GRB.LESS_EQUAL, 0, "cap" + j);
            }

            model.optimize();

            if (model.get(GRB.IntAttr.SolCount) == 0) {
                throw new IllegalStateException("No feasible solution found, status " + model.get(GRB.IntAttr.Status));
            }
            int[] assignment = new int[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (x[i][j].get(GRB.DoubleAttr.X) > 0.5) {
                        assignment[i] = j;
                    }
                }
            }
            SolveStatus status = switch (model.get(GRB.IntAttr.Status)) {
                case GRB.Status.OPTIMAL -> SolveStatus.OPTIMAL;
                case GRB.Status.TIME_LIMIT -> SolveStatus.TIME_LIMIT;
                default -> SolveStatus.FEASIBLE;
            };
            return Solution.of(inst, name(), status, model.get(GRB.DoubleAttr.Runtime), assignment);
        } finally {
            model.dispose();
            env.dispose();
        }
    }
}
