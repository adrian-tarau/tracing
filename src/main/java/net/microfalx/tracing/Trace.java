package net.microfalx.tracing;

import net.microfalx.lang.Descriptable;
import net.microfalx.lang.ExecutionAware;
import net.microfalx.lang.Identifiable;
import net.microfalx.lang.Nameable;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import static net.microfalx.tracing.Traces.ACTIVE_TRACES;

/**
 * An interface for the execution flow and performance of a program.
 */
public interface Trace extends Identifiable<String>, Nameable, Descriptable, ExecutionAware<ZonedDateTime>, Closeable {

    /**
     * Starts a new trace.
     *
     * @param name the name
     * @return a non-null instance
     */
    static Trace start(String name) {
        return Traces.start(name);
    }

    /**
     * Returns the top (root) trace for the current thread.
     * <p>
     * Only root trace will be persisted. If there is no trace opened, a new one is created.
     *
     * @return a non-null instance
     */
    static Optional<Trace> top() {
        Stack<Trace> traces = ACTIVE_TRACES.get();
        if (traces.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(traces.get(0));
        }
    }

    /**
     * Returns the current trace for the current thread.
     * <p>
     * If there is no trace opened, a new one is created.
     *
     * @return a non-null instance
     */
    static Optional<Trace> current() {
        Stack<Trace> traces = ACTIVE_TRACES.get();
        if (traces.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(traces.peek());
        }
    }

    /**
     * Changes the description of the trace.
     *
     * @param description the new description
     * @return self
     */
    Trace setDescription(String description);

    /**
     * Adds a new event.
     *
     * @param event the event
     */
    Trace add(Event event);

    /**
     * Adds a new tag.
     *
     * @param tag the new tag
     */
    Trace add(String tag);

    /**
     * Returns the tags associated with the trace.
     *
     * @return a non-null instance
     */
    Set<String> getTags();

    /**
     * Returns the events associated with the trace.
     *
     * @return a non-null instance
     */
    Collection<Event> getEvents();

    /**
     * Returns child traces.
     *
     * @return a non-null instance
     */
    Collection<Trace> getTraces();

    /**
     * Stops the trace.
     */
    void stop();

    /**
     * Closes the trace.
     * <p>
     * Auto-closable pattern which calls {@link #stop()}.
     */
    default void close() {
        stop();
    }
}
