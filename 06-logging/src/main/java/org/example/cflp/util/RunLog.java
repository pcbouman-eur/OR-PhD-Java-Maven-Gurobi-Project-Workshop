package org.example.cflp.util;

import ch.qos.logback.classic.ClassicConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Routes all log messages of the current thread to LOG_DIR/&lt;run&gt;.log while open:
 * <pre>try (RunLog ignored = RunLog.open("cflp-20-100-1.sa")) { ... }</pre>
 */
public final class RunLog implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RunLog.class);

    private RunLog() {
    }

    public static RunLog open(String run) {
        MDC.put("run", run);
        return new RunLog();
    }

    @Override
    public void close() {
        // Tells the SiftingAppender this run is over, so it closes the file right away
        log.debug(ClassicConstants.FINALIZE_SESSION_MARKER, "Closing run log");
        MDC.remove("run");
    }
}
