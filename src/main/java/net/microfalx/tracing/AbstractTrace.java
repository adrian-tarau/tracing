package net.microfalx.tracing;

/**
 * Base class for all traces.
 */
public abstract class AbstractTrace implements Trace {

    protected abstract void add(Trace trace);
}
