package org.example.cflp.solver;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;

/** Lets MIP tests skip (instead of fail) on machines without a Gurobi license. */
public final class GurobiLicense {

    private static Boolean available;

    private GurobiLicense() {
    }

    public static synchronized boolean isAvailable() {
        if (available == null) {
            try {
                GRBEnv env = new GRBEnv(true);
                env.set(GRB.IntParam.OutputFlag, 0);
                env.start();
                env.dispose();
                available = true;
            } catch (GRBException e) {
                available = false;
            }
        }
        return available;
    }
}
