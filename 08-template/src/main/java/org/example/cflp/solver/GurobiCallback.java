package org.example.cflp.solver;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import com.gurobi.gurobi.GRBException;
import org.example.cflp.util.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

/** Forwards Gurobi's log to slf4j and logs every new incumbent as an event. */
public class GurobiCallback extends GRBCallback {

    private static final Logger log = LoggerFactory.getLogger("gurobi");

    // MDC is per thread: copy it, in case Gurobi calls back from a different thread
    private final Map<String, String> mdc = MDC.getCopyOfContextMap();

    @Override
    protected void callback() {
        try {
            if (mdc != null && MDC.get("run") == null) {
                MDC.setContextMap(mdc);
            }
            if (where == GRB.CB_MESSAGE) {
                String line = getStringInfo(GRB.CB_MSG_STRING).stripTrailing();
                if (!line.isEmpty()) {
                    log.info(line);
                }
            } else if (where == GRB.CB_MIPSOL) {
                Events.log(new Incumbent(getDoubleInfo(GRB.CB_RUNTIME), getDoubleInfo(GRB.CB_MIPSOL_OBJ),
                        getDoubleInfo(GRB.CB_MIPSOL_OBJBND)));
            }
        } catch (GRBException e) {
            log.warn("Error in Gurobi callback", e);
        }
    }
}
