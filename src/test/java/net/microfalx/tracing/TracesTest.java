package net.microfalx.tracing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.microfalx.lang.ThreadUtils.sleepMicros;
import static net.microfalx.lang.ThreadUtils.sleepMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracesTest {

    @BeforeEach
    void setUp() {
        Traces.clear();
    }

    @Test
    void getTracesCount() {
        try (Trace trace1 = Trace.start("trace1")) {
            sleepMillis(5);
        }
        assertEquals(1, Traces.getTraces().size());
        assertEquals(1, Traces.getTraceCount());
        try (Trace trace1 = Trace.start("trace1")) {
            try (Trace trace2 = Trace.start("trace2")) {
                try (Trace trace3 = Trace.start("trace3")) {
                    sleepMillis(5);
                }
            }
        }
        assertEquals(2, Traces.getTraces().size());
        assertEquals(2, Traces.getTraceCount());
    }

    @Test
    void purgeTraces() {
        for (int i = 0; i < 100; i++) {
            try (Trace ignored = Trace.start("trace1")) {
                sleepMicros(5);
            }
        }
        assertEquals(50, Traces.getTraces().size());
        assertEquals(100, Traces.getTraceCount());

        Traces.setMaximumTraces(10);
        for (int i = 0; i < 100; i++) {
            try (Trace ignored = Trace.start("trace1")) {
                sleepMicros(5);
            }
        }
        assertEquals(10, Traces.getTraces().size());
        assertEquals(200, Traces.getTraceCount());
    }

    @Test
    void getTracesMultipleClosed() {
        try (Trace trace1 = Trace.start("trace1")) {
            try (Trace trace2 = Trace.start("trace2")) {
                try (Trace trace3 = Trace.start("trace3")) {
                    sleepMillis(5);
                }
            }
        }
        assertEquals(1, Traces.getTraces().size());
        assertTrue(Traces.getTraces().stream().anyMatch(t -> "trace1".equals(t.getName())));
    }

}