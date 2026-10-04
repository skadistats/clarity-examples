package skadistats.clarity.examples.cooldowns;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provider of the custom events {@link OnAbilityCooldownStart}, {@link OnAbilityCooldownReset} and
 * {@link OnAbilityCooldownEnd}, for ability and item cooldown transitions in Dota 2.
 *
 * <p>Watches the {@code m_fCooldown} property (the absolute game time at which
 * the cooldown expires) on every entity that has it. Three events are raised:
 *
 * <ul>
 *   <li>{@link OnAbilityCooldownStart} — when {@code m_fCooldown} changes to
 *       a value greater than the previously stored value. The owning entity
 *       (resolved via {@code m_hOwnerEntity}, may be null) and the new cooldown
 *       end time (in game seconds) are passed along.</li>
 *   <li>{@link OnAbilityCooldownReset} — when {@code m_fCooldown} drops back
 *       to {@code 0}, e.g. after a Refresher Orb. The owner entity is passed.</li>
 *   <li>{@link OnAbilityCooldownEnd} — when a tracked cooldown reaches its
 *       natural expiration. Since this is not a property change, it is
 *       implemented by polling the current game time at the end of each
 *       tick and firing for every pending cooldown whose end time has
 *       passed.</li>
 * </ul>
 *
 * <p>Pattern shown here: {@link Provides} declares the provided annotations, an {@link Initializer}
 * per annotation creates its event via {@link Context#createEvent(Class)} when a listener exists
 * (an event field stays {@code null} otherwise), and {@link Insert} injects the {@link Entities}
 * processor. The game time for the end check is read from {@code CDOTAGamerulesProxy}
 * ({@code m_pGameRules.m_fGameTime}), which is a Source 2 property path.
 */
@UsesEntities
@Provides({ OnAbilityCooldownStart.class, OnAbilityCooldownReset.class, OnAbilityCooldownEnd.class })
public class Cooldowns {

    // class id -> resolved field path (null value: the class has no such property)
    private final Map<Integer, FieldPath> cooldownPaths = new HashMap<>();
    private final Map<Integer, FieldPath> ownerPaths = new HashMap<>();
    // entity index -> last seen m_fCooldown
    private final Map<Integer, Float> currentCooldown = new HashMap<>();
    // entity index -> end time of the running cooldown, checked against game time at each tick end
    private final Map<Integer, Float> pendingExpiration = new HashMap<>();

    private FieldPath gameTimePath;

    // injects the Entities processor (available because of @UsesEntities)
    @Insert
    private Entities entities;

    private OnAbilityCooldownStart.Event evStart;
    private OnAbilityCooldownReset.Event evReset;
    private OnAbilityCooldownEnd.Event evEnd;

    // runs once per listener of the annotation; creates the event object that raises it
    @Initializer(OnAbilityCooldownStart.class)
    public void initOnStart(final Context ctx, final EventListener<OnAbilityCooldownStart> el) {
        evStart = ctx.createEvent(OnAbilityCooldownStart.class);
    }

    @Initializer(OnAbilityCooldownReset.class)
    public void initOnReset(final Context ctx, final EventListener<OnAbilityCooldownReset> el) {
        evReset = ctx.createEvent(OnAbilityCooldownReset.class);
    }

    @Initializer(OnAbilityCooldownEnd.class)
    public void initOnEnd(final Context ctx, final EventListener<OnAbilityCooldownEnd> el) {
        evEnd = ctx.createEvent(OnAbilityCooldownEnd.class);
    }

    @OnEntityCreated
    public void onCreated(Context ctx, Entity e) {
        clearCachedState(e);
        FieldPath p = ensureFieldPathForEntityInitialized(e);
        if (p != null) {
            processCooldownChange(e, p);
        }
    }

    // entity indices are reused, so drop the cached state when an entity goes away
    @OnEntityDeleted
    public void onDeleted(Context ctx, Entity e) {
        clearCachedState(e);
    }

    // updates may touch other properties only, so check that m_fCooldown is among the changed paths
    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] fieldPaths, int num) {
        FieldPath p = getFieldPathForEntity(e);
        if (p == null) return;
        for (int i = 0; i < num; i++) {
            if (fieldPaths[i].equals(p)) {
                processCooldownChange(e, p);
                break;
            }
        }
    }

    private FieldPath ensureFieldPathForEntityInitialized(Entity e) {
        Integer cid = e.getDtClass().getClassId();
        if (!cooldownPaths.containsKey(cid)) {
            cooldownPaths.put(cid, e.getFieldPathForName("m_fCooldown"));
            ownerPaths.put(cid, e.getFieldPathForName("m_hOwnerEntity"));
        }
        return cooldownPaths.get(cid);
    }

    private Entity resolveOwner(Entity e) {
        FieldPath op = ownerPaths.get(e.getDtClass().getClassId());
        if (op == null) return null;
        // m_hOwnerEntity is an entity handle (index plus serial), not an index
        Integer handle = e.getPropertyForFieldPath(op);
        if (handle == null) return null;
        return entities.getByHandle(handle);
    }

    private FieldPath getFieldPathForEntity(Entity e) {
        return cooldownPaths.get(e.getDtClass().getClassId());
    }

    private void clearCachedState(Entity e) {
        currentCooldown.remove(e.getIndex());
        pendingExpiration.remove(e.getIndex());
    }

    private void processCooldownChange(Entity e, FieldPath p) {
        float oldEnd = currentCooldown.getOrDefault(e.getIndex(), 0f);
        float newEnd = e.getPropertyForFieldPath(p);
        if (oldEnd == newEnd) return;
        currentCooldown.put(e.getIndex(), newEnd);
        if (newEnd > oldEnd) {
            pendingExpiration.put(e.getIndex(), newEnd);
            if (evStart != null) {
                evStart.raise(e, resolveOwner(e), newEnd);
            }
        } else if (newEnd == 0f) {
            pendingExpiration.remove(e.getIndex());
            if (evReset != null) {
                evReset.raise(e, resolveOwner(e));
            }
        }
    }

    @OnTickEnd
    public void onTickEnd(Context ctx, boolean synthetic) {
        if (pendingExpiration.isEmpty() || evEnd == null) return;
        // expiry is not a property change, so compare the pending end times with the current game time
        Entity rules = entities.stream()
                .filter(Entities.byDtName("CDOTAGamerulesProxy"))
                .findFirst()
                .orElse(null);
        if (rules == null) return;
        if (gameTimePath == null) {
            gameTimePath = rules.getFieldPathForName("m_pGameRules.m_fGameTime");
            if (gameTimePath == null) return;
        }
        Float gameTime = rules.getPropertyForFieldPath(gameTimePath);
        if (gameTime == null) return;
        Iterator<Map.Entry<Integer, Float>> it = pendingExpiration.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Float> entry = it.next();
            if (entry.getValue() > gameTime) continue;
            Entity ability = entities.getByIndex(entry.getKey());
            it.remove();
            if (ability != null) {
                evEnd.raise(ability, resolveOwner(ability));
            }
        }
    }
}
