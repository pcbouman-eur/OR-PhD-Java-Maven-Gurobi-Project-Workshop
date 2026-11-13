package org.example.cflp.solver.sa;

import java.util.List;
import java.util.random.RandomGenerator;

final class Operators {

    private Operators() {
    }

    static List<Operator> all() {
        return List.of(new Shift(), new Swap(), new Close(), new Open());
    }

    /** Move one customer to another facility. */
    record Shift() implements Operator {
        public String name() {
            return "shift";
        }

        public boolean apply(State s, RandomGenerator rnd) {
            int i = rnd.nextInt(s.inst.numCustomers());
            int j = rnd.nextInt(s.inst.numFacilities());
            if (j == s.facilityOf(i)) {
                return false;
            }
            s.move(i, j);
            return true;
        }
    }

    /** Exchange the facilities of two customers. */
    record Swap() implements Operator {
        public String name() {
            return "swap";
        }

        public boolean apply(State s, RandomGenerator rnd) {
            int a = rnd.nextInt(s.inst.numCustomers());
            int b = rnd.nextInt(s.inst.numCustomers());
            int fa = s.facilityOf(a);
            int fb = s.facilityOf(b);
            if (fa == fb) {
                return false;
            }
            s.move(a, fb);
            s.move(b, fa);
            return true;
        }
    }

    /** Close a facility; its customers go to the cheapest other open facility with room. */
    record Close() implements Operator {
        public String name() {
            return "close";
        }

        public boolean apply(State s, RandomGenerator rnd) {
            int closing = s.facilityOf(rnd.nextInt(s.inst.numCustomers()));
            for (int i = 0; i < s.inst.numCustomers(); i++) {
                if (s.facilityOf(i) != closing) {
                    continue;
                }
                double demand = s.inst.customers().get(i).demand();
                int best = -1;
                for (int j = 0; j < s.inst.numFacilities(); j++) {
                    if (j != closing && s.isOpen(j) && s.residual(j) >= demand
                            && (best < 0 || s.inst.cost(i, j) < s.inst.cost(i, best))) {
                        best = j;
                    }
                }
                if (best < 0) {
                    s.rollback();
                    return false;
                }
                s.move(i, best);
            }
            return true;
        }
    }

    /** Open a closed facility and move every customer to it that becomes cheaper, while it fits. */
    record Open() implements Operator {
        public String name() {
            return "open";
        }

        public boolean apply(State s, RandomGenerator rnd) {
            int opening = rnd.nextInt(s.inst.numFacilities());
            if (s.isOpen(opening)) {
                return false;
            }
            boolean moved = false;
            for (int i = 0; i < s.inst.numCustomers(); i++) {
                double demand = s.inst.customers().get(i).demand();
                if (s.inst.cost(i, opening) < s.inst.cost(i, s.facilityOf(i)) && s.residual(opening) >= demand) {
                    s.move(i, opening);
                    moved = true;
                }
            }
            return moved;
        }
    }
}
