package skadistats.clarity.examples.s2effectdispatch;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.processor.runner.Runner;
import skadistats.clarity.wire.shared.s2.proto.S2TempEntities.CMsgEffectData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Raised for every {@code CMsgTEEffectDispatch} sent by the Source 2 engine,
 * with the {@code effectname} uint32 already resolved against the
 * {@code EffectDispatch} string table to a human-readable handler name
 * (e.g. {@code ParticleEffect}, {@code csblood}, {@code Impact}).
 *
 * <p>Handler signature:</p>
 * <pre>
 * &#64;OnEffectDispatch
 * public void onEffectDispatch(String effectKind, CMsgEffectData data) { ... }
 * </pre>
 *
 * <p>Only raised for Source 2 replays (Dota 2, CS2, Deadlock); it is provided by {@link EffectDispatches}, which
 * is registered by passing an instance to {@code runWith(...)}.</p>
 *
 * <p>{@code effectKind} is {@code null} if the {@code EffectDispatch} string
 * table is missing or the index is out of range. The full
 * {@link CMsgEffectData} (with {@code origin}, {@code angles}, {@code entity},
 * {@code effectindex} hash, etc.) is passed as the second argument.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
// Marks this annotation as an event; methods annotated with it are collected as listeners.
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
public @interface OnEffectDispatch {

    /** Handler signature; listener methods are bound to this interface. */
    interface Listener {
        void invoke(String kind, CMsgEffectData data);
    }

    /** Hand-written event; {@link EffectDispatches} creates it and calls {@link #raise}. */
    final class Event extends skadistats.clarity.event.Event<OnEffectDispatch> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnEffectDispatch> eventType, Set<EventListener<OnEffectDispatch>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            // Bound listeners as the typed Listener interface, so raise() can call them directly.
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        /** Calls every listener; an exception thrown by one is handed to {@code handleListenerException}. */
        public void raise(String kind, CMsgEffectData data) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(kind, data);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
