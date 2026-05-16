package net.microfalx.tracing;

import com.google.common.base.MoreObjects;
import net.microfalx.lang.CollectionUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static java.time.Duration.ofNanos;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.CollectionUtils.immutableCollection;
import static net.microfalx.lang.CollectionUtils.immutableSet;
import static net.microfalx.lang.TimeUtils.toLocalDateTime;
import static net.microfalx.lang.TimeUtils.toZonedDateTime;

/**
 * The implementation of a trace.
 */
final class TraceImpl extends AbstractTrace {

    private final String id = Traces.GENERATOR.nextAsString();
    private final String name;
    private final long startTime = currentTimeMillis();
    private final long start = nanoTime();
    private long end;
    private Set<String> tags;
    private Collection<Event> events;
    private Collection<Trace> traces;

    private String description;

    TraceImpl(String name) {
        requireNonNull(name);
        this.name = name;
        Traces.register(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Adds a description to the trace.
     *
     * @param description the new description
     * @return self
     */
    public Trace setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public ZonedDateTime getStartedAt() {
        return toZonedDateTime(startTime);
    }

    @Override
    public ZonedDateTime getEndedAt() {
        return getStartedAt().plus(getDuration());
    }

    @Override
    public Duration getDuration() {
        if (end == 0) {
            return ofNanos(nanoTime() - start);
        } else {
            return ofNanos(end - start);
        }
    }

    @Override
    public Trace add(Event event) {
        requireNonNull(event);
        if (events == null) events = new ArrayList<>();
        events.add(event);
        return this;
    }

    @Override
    public Trace add(String tag) {
        requireNonNull(tag);
        if (tags == null) tags = new HashSet<>();
        tags.add(tag);
        return this;
    }

    @Override
    public Set<String> getTags() {
        return immutableSet(tags);
    }

    @Override
    public Collection<Event> getEvents() {
        return immutableCollection(events);
    }

    @Override
    public Collection<Trace> getTraces() {
        return immutableCollection(traces);
    }

    @Override
    public void stop() {
        end = nanoTime();
        Traces.unregister(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("startTime", toLocalDateTime(startTime))
                .add("start", start)
                .add("end", end)
                .add("tags", tags)
                .add("events", CollectionUtils.size(events))
                .add("traces", CollectionUtils.size(traces))
                .add("description", description)
                .toString();
    }

    protected void add(Trace trace) {
        requireNonNull(trace);
        if (traces == null) traces = new ArrayList<>();
        traces.add(trace);
    }
}
