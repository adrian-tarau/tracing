package net.microfalx.tracing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static net.microfalx.lang.ThreadUtils.sleepMillis;
import static org.junit.jupiter.api.Assertions.*;

class TraceImplTest {

    private static final String TRACE_NAME = "test-trace";
    private static final String DESCRIPTION = "Test trace description";
    private static final String TAG = "important";
    private static final String ANOTHER_TAG = "production";

    @BeforeEach
    void setUp() {
        Traces.clear();
    }

    @Test
    void traceCanBeCreatedWithName() {
        String expectedName = "my-trace";
        try (Trace trace = Trace.start(expectedName)) {
            assertNotNull(trace);
            assertEquals(expectedName, trace.getName());
        }
    }

    @Test
    void traceHasDescriptionAsNullByDefault() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            assertNull(trace.getDescription());
        }
    }

    @Test
    void traceHasStartedAtTimestamp() {
        ZonedDateTime beforeCreation = ZonedDateTime.now();
        try (Trace trace = Trace.start(TRACE_NAME)) {
            ZonedDateTime startedAt = trace.getStartedAt();
            ZonedDateTime afterCreation = ZonedDateTime.now();
            assertNotNull(startedAt);
            assertTrue(beforeCreation.isBefore(startedAt) || beforeCreation.equals(startedAt));
            assertTrue(afterCreation.isAfter(startedAt) || afterCreation.equals(startedAt));
        }
    }

    @Test
    void traceHasEndedAtTimestampAfterStop() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            ZonedDateTime startedAt = trace.getStartedAt();
            sleepMillis(10);
            trace.stop();
            ZonedDateTime endedAt = trace.getEndedAt();
            assertNotNull(endedAt);
            assertTrue(endedAt.isAfter(startedAt) || endedAt.equals(startedAt));
        }
    }

    @Test
    void traceDurationIsPositiveAfterStop() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            sleepMillis(10);
            trace.stop();
            Duration duration = trace.getDuration();
            assertNotNull(duration);
            assertFalse(duration.isNegative());
            assertTrue(duration.toMillis() >= 10 || duration.toMillis() >= 5);
        }
    }

    @Test
    void traceDurationIncludesTimeFromStartUntilStop() {
        long sleepTime = 50;
        try (Trace trace = Trace.start(TRACE_NAME)) {
            sleepMillis(sleepTime);
            trace.stop();
            Duration duration = trace.getDuration();
            assertTrue(duration.toMillis() >= sleepTime - 10);
        }
    }

    @Test
    void traceDurationIsCalculatedEvenBeforeStop() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Duration durationBefore = trace.getDuration();
            Duration durationAfter = trace.getDuration();
            assertNotNull(durationBefore);
            assertNotNull(durationAfter);
            assertTrue(durationAfter.toNanos() >= durationBefore.toNanos());
        }
    }

    @Test
    void canAddEvent() {
        Event event = Event.create("event-1");
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Trace result = trace.add(event);
            assertNotNull(result);
            assertEquals(1, trace.getEvents().size());
            assertTrue(trace.getEvents().contains(event));
        }
    }

    @Test
    void addEventReturnsTraceForChaining() {
        Event event = Event.create("event-1");
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Trace result = trace.add(event);
            assertSame(trace, result);
        }
    }

    @Test
    void canAddMultipleEvents() {
        Event event1 = Event.create("event-1");
        Event event2 = Event.create("event-2");
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.add(event1).add(event2);
            Collection<Event> events = trace.getEvents();
            assertEquals(2, events.size());
            assertTrue(events.contains(event1));
            assertTrue(events.contains(event2));
        }
    }

    @Test
    void eventsCollectionIsImmutable() {
        Event event = Event.create("event-1");
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.add(event);
            Collection<Event> events = trace.getEvents();
            assertThrows(UnsupportedOperationException.class, () -> events.add(Event.create("event-2")));
        }
    }

    @Test
    void getEventsReturnsEmptyCollectionWhenNoEventsAdded() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Collection<Event> events = trace.getEvents();
            assertNotNull(events);
            assertTrue(events.isEmpty());
        }
    }

    @Test
    void canAddTag() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Trace result = trace.add(TAG);
            assertNotNull(result);
        }
    }

    @Test
    void addTagReturnsTraceForChaining() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Trace result = trace.add(TAG);
            assertSame(trace, result);
        }
    }

    @Test
    void canAddMultipleTags() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.add(TAG).add(ANOTHER_TAG);
            Set<String> tags = trace.getTags();
            assertEquals(2, tags.size());
            assertTrue(tags.contains(TAG));
            assertTrue(tags.contains(ANOTHER_TAG));
        }
    }

    @Test
    void tagsCollectionIsImmutable() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.add(TAG);
            Set<String> tags = trace.getTags();
            assertThrows(UnsupportedOperationException.class, () -> tags.add("another-tag"));
        }
    }

    @Test
    void getTagsReturnsEmptySetWhenNoTagsAdded() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Set<String> tags = trace.getTags();
            assertNotNull(tags);
            assertTrue(tags.isEmpty());
        }
    }

    @Test
    void childTracesAreAutomaticallyAddedToParent() {
        try (Trace parentTrace = Trace.start("parent")) {
            try (Trace childTrace1 = Trace.start("child-1")) {
                try (Trace childTrace2 = Trace.start("child-2")) {
                    assertEquals(1, parentTrace.getTraces().size());
                    assertEquals(1, childTrace1.getTraces().size());
                    assertTrue(parentTrace.getTraces().contains(childTrace1));
                    assertTrue(childTrace1.getTraces().contains(childTrace2));
                }
            }
        }
    }

    @Test
    void childTracesAreAutomaticallyAddedToCurrentActiveTrace() {
        try (Trace parentTrace = Trace.start("parent")) {
            try (Trace intermediateTrace = Trace.start("intermediate")) {
                try (Trace childTrace = Trace.start("child")) {
                    Collection<Trace> parentChildren = parentTrace.getTraces();
                    Collection<Trace> intermediateChildren = intermediateTrace.getTraces();

                    assertEquals(1, parentChildren.size());
                    assertTrue(parentChildren.contains(intermediateTrace));

                    assertEquals(1, intermediateChildren.size());
                    assertTrue(intermediateChildren.contains(childTrace));
                }
            }
        }
    }

    @Test
    void tracesCollectionIsImmutable() {
        try (Trace parentTrace = Trace.start("parent")) {
            try (Trace childTrace = Trace.start("child")) {
                Collection<Trace> traces = parentTrace.getTraces();
                assertThrows(UnsupportedOperationException.class, () -> traces.add(Trace.start("another")));
            }
        }
    }

    @Test
    void getTracesReturnsEmptyCollectionWhenNoChildTraces() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Collection<Trace> traces = trace.getTraces();
            assertNotNull(traces);
            assertTrue(traces.isEmpty());
        }
    }

    @Test
    void stopStopsTheTrace() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.stop();
            Duration durationAfterStop1 = trace.getDuration();
            sleepMillis(10);
            Duration durationAfterStop2 = trace.getDuration();
            assertEquals(durationAfterStop1, durationAfterStop2);
        }
    }

    @Test
    void closeStopsTheTrace() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.close();
            Duration durationAfterClose1 = trace.getDuration();
            sleepMillis(10);
            Duration durationAfterClose2 = trace.getDuration();
            assertEquals(durationAfterClose1, durationAfterClose2);
        }
    }

    @Test
    void traceCanBeUsedWithTryWithResources() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            sleepMillis(10);
        }
    }

    @Test
    void currentTraceReturnsTheTopActiveTrace() {
        try (Trace parentTrace = Trace.start("parent")) {
            try (Trace childTrace = Trace.start("child")) {
                Optional<Trace> currentTrace = Trace.current();
                assertTrue(currentTrace.isPresent());
                assertSame(childTrace, currentTrace.get());
            }
        }
    }

    @Test
    void topTraceReturnsTheRootTrace() {
        try (Trace parentTrace = Trace.start("parent")) {
            try (Trace childTrace = Trace.start("child")) {
                Optional<Trace> topTrace = Trace.top();
                assertTrue(topTrace.isPresent());
                assertSame(parentTrace, topTrace.get());
            }
        }
    }

    @Test
    void currentReturnsEmptyWhenNoTraceIsActive() {
        Traces.clear();
        Optional<Trace> currentTrace = Trace.current();
        assertFalse(currentTrace.isPresent());
    }

    @Test
    void topReturnsEmptyWhenNoTraceIsActive() {
        Traces.clear();
        Optional<Trace> topTrace = Trace.top();
        assertFalse(topTrace.isPresent());
    }

    @Test
    void multipleAttributesCanBeChainedTogether() {
        Event event = Event.create("event-1");
        try (Trace trace = Trace.start(TRACE_NAME)
                .add(TAG).add(ANOTHER_TAG)
                .add(event)) {
            assertEquals(TRACE_NAME, trace.getName());
            assertEquals(2, trace.getTags().size());
            assertEquals(1, trace.getEvents().size());
        }
    }

    @Test
    void descriptionCanBeSet() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.setDescription(DESCRIPTION);
            assertEquals(DESCRIPTION, trace.getDescription());
        }
    }

    @Test
    void setDescriptionReturnsTraceForChaining() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            Trace result = trace.setDescription(DESCRIPTION);
            assertSame(trace, result);
        }
    }

    @Test
    void descriptionCanBeUpdated() {
        String description1 = "First description";
        String description2 = "Updated description";
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.setDescription(description1);
            assertEquals(description1, trace.getDescription());
            trace.setDescription(description2);
            assertEquals(description2, trace.getDescription());
        }
    }

    @Test
    void toStringIsNotNull() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            String result = trace.toString();
            assertNotNull(result);
        }
    }

    @Test
    void toStringIncludesTraceName() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            String result = trace.toString();
            assertTrue(result.contains(TRACE_NAME));
        }
    }

    @Test
    void toStringIncludesDescription() {
        try (Trace trace = Trace.start(TRACE_NAME)) {
            trace.setDescription(DESCRIPTION);
            String result = trace.toString();
            assertTrue(result.contains(DESCRIPTION));
        }
    }
}