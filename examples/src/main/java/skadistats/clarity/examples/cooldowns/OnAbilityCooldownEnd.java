package skadistats.clarity.examples.cooldowns;

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
 * Custom event raised by {@link Cooldowns} when a cooldown expires naturally.
 *
 * <p>Handler signature: {@code void onX(Entity ability, Entity owner)}. {@code owner} may be {@code null} if it cannot be resolved.
 *
 * <p>The annotation is an event listener marker ({@link UsagePointType#EVENT_LISTENER}); the nested
 * {@code Listener} interface defines the handler signature and the nested {@code Event} class is what
 * the provider calls via {@code raise} to invoke all listeners, with a listener's exception routed to
 * {@code handleListenerException}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
public @interface OnAbilityCooldownEnd {

    interface Listener {
        void invoke(Entity ability, Entity owner);
    }

    final class Event extends skadistats.clarity.event.Event<OnAbilityCooldownEnd> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnAbilityCooldownEnd> eventType, Set<EventListener<OnAbilityCooldownEnd>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(Entity ability, Entity owner) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(ability, owner);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
