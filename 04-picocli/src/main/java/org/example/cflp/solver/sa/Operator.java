package org.example.cflp.solver.sa;

import java.util.random.RandomGenerator;

/** A local search move. Changes the state via {@link State#move}; SA decides to keep or roll back. */
interface Operator {

    String name();

    /** @return false if the move is not applicable in this state (nothing changed) */
    boolean apply(State state, RandomGenerator rnd);
}
