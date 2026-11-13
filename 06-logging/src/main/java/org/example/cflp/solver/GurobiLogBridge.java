package org.example.cflp.solver;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import com.gurobi.gurobi.GRBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

/** Forwards every line Gurobi would print to the slf4j logger named "gurobi". */
public class GurobiLogBridge extends GRBCallback {

    private static final Logger log = LoggerFactory.getLogger("gurobi");

    // MDC is per thread: copy it, in case Gurobi calls back from a different thread
    private final Map<String, String> mdc = MDC.getCopyOfContextMap();

    @Override
    protected void callback() {
        if (where == GRB.CB_MESSAGE) {
            try {
                String line = getStringInfo(GRB.CB_MSG_STRING).stripTrailing();
                if (!line.isEmpty()) {
                    if (mdc != null && MDC.get("run") == null) {
                        MDC.setContextMap(mdc);
                    }
                    log.info(line);
                }
            } catch (GRBException e) {
                log.warn("Could not read Gurobi message", e);
            }
        }
    }
}
