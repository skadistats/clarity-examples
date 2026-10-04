package skadistats.clarity.examples.lifestate;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Custom event raised by {@link SpawnsAndDeaths} when the {@code m_lifeState} of an entity changes to 2.
 *
 * <p>Handler signature: {@code void onX(Entity e)}.
 *
 * <p>The annotation is an event listener marker ({@link UsagePointType#EVENT_LISTENER}); the nested
 * {@code Listener} interface defines the handler signature and the nested {@code Event} class is what
 * the provider calls via {@code raise} to invoke all listeners, with a listener's exception routed to
 * {@code handleListenerException}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
public @interface OnEntityDied {

    interface Listener {
        void invoke(Entity e);
    }

    final class Event extends skadistats.clarity.event.Event<OnEntityDied> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnEntityDied> eventType, Set<EventListener<OnEntityDied>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(Entity e) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(e);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
