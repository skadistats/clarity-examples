package skadistats.clarity.examples.lifestate;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider of the custom events {@link OnEntitySpawned}, {@link OnEntityDying} and {@link OnEntityDied}.
 *
 * <p>Tracks the {@code m_lifeState} property of every entity that has it and raises an event when
 * the value changes to 0 (spawned), 1 (dying) or 2 (died). The previous value defaults to 2, so an
 * entity that is created already dead raises nothing.
 *
 * <p>Pattern shown here: {@link Provides} declares which annotations this class provides. For each
 * of them an {@link Initializer} method runs when a listener for it exists and creates the event
 * object with {@link Context#createEvent(Class)}; an event field stays {@code null} if nobody
 * listens, hence the null checks before {@code raise}. {@link UsesEntities} registers the entities
 * processor, whose events drive this class.
 */
@UsesEntities
@Provides({ OnEntitySpawned.class, OnEntityDying.class, OnEntityDied.class })
public class SpawnsAndDeaths {

    // class id -> m_lifeState path (null value: the class has no such property); resolved once per class
    private final Map<Integer, FieldPath> lifeStatePaths = new HashMap<>();
    // entity index -> last seen m_lifeState
    private final Map<Integer, Integer> currentLifeState = new HashMap<>();

    private OnEntitySpawned.Event evSpawned;
    private OnEntityDying.Event evDying;
    private OnEntityDied.Event evDied;

    // runs once per listener of the annotation; creates the event object that raises it
    @Initializer(OnEntitySpawned.class)
    public void initOnEntitySpawned(final Context ctx, final EventListener<OnEntitySpawned> eventListener) {
        init(ctx);
        evSpawned = ctx.createEvent(OnEntitySpawned.class);
    }

    @Initializer(OnEntityDying.class)
    public void initOnEntityDying(final Context ctx, final EventListener<OnEntityDying> eventListener) {
        init(ctx);
        evDying = ctx.createEvent(OnEntityDying.class);
    }

    @Initializer(OnEntityDied.class)
    public void initOnEntityDied(final Context ctx, final EventListener<OnEntityDied> eventListener) {
        init(ctx);
        evDied = ctx.createEvent(OnEntityDied.class);
    }

    @OnEntityCreated
    public void onCreated(Context ctx, Entity e) {
        clearCachedState(e);
        ensureFieldPathForEntityInitialized(e);
        FieldPath p = getFieldPathForEntity(e);
        if (p != null) {
            processLifeStateChange(e, p);
        }
    }

    // entity indices are reused, so drop the cached state when an entity goes away
    @OnEntityDeleted
    public void onDeleted(Context ctx, Entity e) {
        clearCachedState(e);
    }

    // updates may touch other properties only, so check that m_lifeState is among the changed paths
    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] fieldPaths, int num) {
        FieldPath p = getFieldPathForEntity(e);
        if (p != null) {
            for (int i = 0; i < num; i++) {
                if (fieldPaths[i].equals(p)) {
                    processLifeStateChange(e, p);
                    break;
                }
            }
        }
    }

    private void init(Context ctx) {
    }

    private void ensureFieldPathForEntityInitialized(Entity e) {
        Integer cid = e.getDtClass().getClassId();
        if (!lifeStatePaths.containsKey(cid)) {
            lifeStatePaths.put(cid, e.getFieldPathForName("m_lifeState"));
        }
    }

    private FieldPath getFieldPathForEntity(Entity e) {
        return lifeStatePaths.get(e.getDtClass().getClassId());
    }

    private void clearCachedState(Entity e) {
        currentLifeState.remove(e.getIndex());
    }

    private void processLifeStateChange(Entity e, FieldPath p) {
        // default 2: an entity first seen dead is not reported as died
        int oldState = currentLifeState.containsKey(e.getIndex()) ? currentLifeState.get(e.getIndex()) : 2;
        int newState = e.getPropertyForFieldPath(p);
        if (oldState != newState) {
            currentLifeState.put(e.getIndex(), newState);
            switch(newState) {
                case 0:
                    if (evSpawned != null) {
                        evSpawned.raise(e);
                    }
                    break;
                case 1:
                    if (evDying != null) {
                        evDying.raise(e);
                    }
                    break;
                case 2:
                    if (evDied != null) {
                        evDied.raise(e);
                    }
                    break;
            }
        }
    }

}
