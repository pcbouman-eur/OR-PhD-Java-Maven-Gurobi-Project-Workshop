package org.example.cflp.solver;

import org.example.cflp.model.Instance;
import org.example.cflp.model.Solution;

/** Common interface, so the rest of the code doesn't care which algorithm runs. */
public interface Solver {

    String name();

    /** All parameters (typically a record), stored with every solution. */
    Object parameters();

    Solution solve(Instance instance);
}
