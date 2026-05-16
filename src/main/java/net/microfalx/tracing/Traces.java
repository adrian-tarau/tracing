package net.microfalx.tracing;

import net.microfalx.lang.IdGenerator;

import java.util.Collection;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.unmodifiableCollection;
import static net.microfalx.lang.ArgumentUtils.requireBounded;

/**
 * A facade to Manages traces.
 */
public class Traces {

    protected static IdGenerator GENERATOR = IdGenerator.get("tracing");

    static ThreadLocal<Stack<Trace>> ACTIVE_TRACES = ThreadLocal.withInitial(Stack::new);
    static final Queue<Trace> TRACES = new ConcurrentLinkedQueue<>();
    static final AtomicLong TRACE_ADD_COUNT = new AtomicLong();
    static final AtomicLong TRACE_SIZE = new AtomicLong();

    private volatile static int maximumTraces = 50;

    /**
     * Returns the number of registered traces.
     *
     * @return a positive integer
     */
    public static long getTraceCount() {
        return TRACE_ADD_COUNT.get();
    }

    /**
     * Changes the maximum traces stored in memory.
     *
     * @param maximumTraces a positive integer
     */
    public static void setMaximumTraces(int maximumTraces) {
        requireBounded(maximumTraces, 1, 10_000);
        Traces.maximumTraces = maximumTraces;
    }

    /**
     * Starts a new trace.
     *
     * @param name the name
     * @return a non-null instance
     */
    static Trace start(String name) {
        return new TraceImpl(name);
    }

    /**
     * Returns the available traces.
     *
     * @return a non-null instance
     */
    public static Collection<Trace> getTraces() {
        return unmodifiableCollection(TRACES);
    }

    /**
     * Registers an  active trace.
     *
     * @param trace the trace
     */
    @SuppressWarnings("resource")
    static void register(Trace trace) {
        Stack<Trace> threadTraces = ACTIVE_TRACES.get();
        if (!threadTraces.isEmpty()) {
            AbstractTrace parent = (AbstractTrace) threadTraces.peek();
            parent.add(trace);
        }
        threadTraces.push(trace);
    }

    /**
     * Unregisters an active trace.
     *
     * @param trace the trace
     */
    @SuppressWarnings("resource")
    static void unregister(Trace trace) {
        Stack<Trace> traces = ACTIVE_TRACES.get();
        if (!traces.isEmpty()) traces.pop();
        if (traces.isEmpty()) {
            TRACES.offer(trace);
            TRACE_SIZE.incrementAndGet();
            TRACE_ADD_COUNT.getAndIncrement();
        }
        while (TRACE_SIZE.get() > maximumTraces) {
            Trace removed = TRACES.poll();
            if (removed == null) return;
            TRACE_SIZE.decrementAndGet();
        }
    }

    /**
     * Clears the state of traces.
     * <p>
     * Mostly for unit tests.
     */
    static void clear() {
        ACTIVE_TRACES.get().clear();
        TRACES.clear();
        TRACE_SIZE.set(0);
        TRACE_ADD_COUNT.set(0);
    }
}
