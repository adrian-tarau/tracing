package net.microfalx.tracing;

import com.google.common.base.MoreObjects;
import net.microfalx.lang.Descriptable;
import net.microfalx.lang.Nameable;
import net.microfalx.lang.Timestampable;

import java.time.ZonedDateTime;

import static java.lang.System.currentTimeMillis;
import static net.microfalx.lang.TimeUtils.toZonedDateTime;

/**
 * An event within a trace.
 */
public class Event implements Nameable, Descriptable, Timestampable<ZonedDateTime> {

    private final String name;
    private final String description;
    private final long timestamp = currentTimeMillis();

    /**
     * Creates a new event.
     *
     * @param name the name of the event
     * @return a non-null instance
     */
    public static Event create(String name) {
        return new Event(name, null);
    }

    Event(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return toZonedDateTime(timestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("timestamp", getCreatedAt())
                .toString();
    }
}
