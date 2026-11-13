package org.example.cflp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.logstash.logback.argument.StructuredArguments.fields;
import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Structured logging: an event is any object (typically a record) that Jackson can serialize.
 * It goes to the logger "events"; logback.xml writes it as one JSON line to LOG_DIR/&lt;run&gt;.events.jsonl:
 * <pre>
 * Events.log(new NewBest(it, op.name(), cost, seconds));
 * {"time":"...","level":"INFO","logger":"events","run":"cflp-30-150-1.sa","event":"NewBest","iteration":5123,...}
 * </pre>
 */
public final class Events {

    private static final Logger log = LoggerFactory.getLogger("events");

    private Events() {
    }

    public static void log(Object event) {
        // kv(...) adds "event": the class name; fields(...) adds the object's fields (via Jackson) to the same line.
        log.info("{} {}", kv("event", event.getClass().getSimpleName()), fields(event));
    }
}
