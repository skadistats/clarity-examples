package skadistats.clarity.examples.position;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.io.Util;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.Vector;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.OnDTClassesComplete;
import skadistats.clarity.source.MappedFileSource;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints the position of every player's hero whenever it changes, in a Dota 2 Source 2 replay.
 *
 * <p>Demonstrates {@link OnEntityUpdated} together with field paths resolved once and compared against the changed
 * paths, instead of looking properties up by name on every update. Flow:
 * <ol>
 * <li>{@link OnDTClassesComplete}: look up the {@code CDOTA_PlayerResource} class in {@link DTClasses}.</li>
 * <li>When the player resource entity is updated, check for each of the 10 player slots whether
 * {@code m_vecPlayerTeamData.<i>.m_hSelectedHero} changed. If so, remember the hero entity that handle refers to.</li>
 * <li>When any other entity is updated, check whether it is one of the remembered heroes and whether one of its
 * {@code CBodyComponent.m_cellX/Y/Z} or {@code m_vecX/Y/Z} properties changed; if so print the position
 * {@code cell * 128 + vec} per axis.</li>
 * </ol>
 *
 * <p>Dota 2 Source 2 only (class {@code CDOTA_PlayerResource}, {@code CBodyComponent}, fixed 10 player slots).
 * Logs the total run time.
 *
 * <p>Run:
 * <pre>
 * ./gradlew :examples:positionRun --args "path/to/replay.dem"
 * </pre>
 */
@UsesEntities
@Example(name = "position", description = "Track and log hero position updates throughout match", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @Insert
    private DTClasses dtClasses;

    @Insert
    private Entities entities;

    private DTClass playerResourceClass;
    private PlayerResourceLookup[] playerLookup;
    private final HeroLookup[] heroLookup = new HeroLookup[10];
    private final List<Runnable> deferredActions = new ArrayList<>();

    // Fires once the entity classes exist, so they can be looked up by name before any entity is created.
    @OnDTClassesComplete
    protected void onDtClassesComplete() {
        playerResourceClass = dtClasses.forDtName("CDOTA_PlayerResource");
    }

    private void ensurePlayerLookups(Entity playerResource) {
        if (playerLookup == null) {
            playerLookup = new PlayerResourceLookup[10];
            for (int i = 0; i < 10; i++) {
                playerLookup[i] = new PlayerResourceLookup(playerResource, i);
            }
        }
    }

    // Fires after a packet was applied to an existing entity, with the field paths that changed. It is not raised for
    // entity creation. Without a classPattern it is called for every entity.
    @OnEntityUpdated
    protected void onEntityUpdated(Entity e, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        if (e.getDtClass() == playerResourceClass) {
            ensurePlayerLookups(e);
            for (int p = 0; p < 10; p++) {
                PlayerResourceLookup lookup = playerLookup[p];
                if (lookup.isSelectedHeroChanged(e, changedFieldPaths, nChangedFieldPaths)) {
                    int playerIndex = p;
                    // The handle is resolved to an entity later, at the end of the tick, not inside this callback.
                    deferredActions.add(() -> {
                        int heroHandle = lookup.getSelectedHeroHandle(e);
                        System.out.format("Player %02d got assigned hero %d\n", playerIndex, heroHandle);
                        Entity heroEntity = entities.getByHandle(heroHandle);
                        heroLookup[playerIndex] = new HeroLookup(heroEntity);
                    });
                }
            }
        } else {
            for (int p = 0; p < 10; p++) {
                HeroLookup lookup = heroLookup[p];
                if (lookup == null) continue;
                if (lookup.isPositionChanged(e, changedFieldPaths, nChangedFieldPaths)) {
                    Vector newPosition = lookup.getPosition();
                    System.out.format("Player %02d changed position to %s\n", p, newPosition.toString());
                }
            }

        }
    }

    // Runs the actions queued during the tick's entity updates.
    @OnTickEnd
    protected void onTickEnd(boolean synthetic) {
        deferredActions.forEach(Runnable::run);
        deferredActions.clear();
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource s = new MappedFileSource(replay)) {
            SimpleRunner runner = new SimpleRunner(s);
            runner.runWith(this);
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        try {
            new Main().run(args);
        } catch (Exception e) {
            Thread.sleep(1000);
            throw e;
        }
    }

    private static class PlayerResourceLookup {

        private final FieldPath fpSelectedHero;

        // The field path is resolved once per player; array elements are named with a four-digit index (for example 0003).
        private PlayerResourceLookup(Entity playerResource, int idx) {
            this.fpSelectedHero = playerResource.getFieldPathForName(
                    format("m_vecPlayerTeamData.%s.m_hSelectedHero", Util.arrayIdxToString(idx))
            );
        }

        // changedFieldPaths holds the changed paths in its first nChangedFieldPaths entries.
        private boolean isSelectedHeroChanged(Entity playerResource, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
            for (int f = 0; f < nChangedFieldPaths; f++) {
                FieldPath changedFieldPath = changedFieldPaths[f];
                if (changedFieldPath.equals(fpSelectedHero)) return true;
            }
            return false;
        }

        private int getSelectedHeroHandle(Entity playerResource) {
            return playerResource.getPropertyForFieldPath(fpSelectedHero);
        }

    }

    private static class HeroLookup {

        private final Entity heroEntity;
        private final FieldPath fpCellX;
        private final FieldPath fpCellY;
        private final FieldPath fpCellZ;
        private final FieldPath fpVecX;
        private final FieldPath fpVecY;
        private final FieldPath fpVecZ;

        private HeroLookup(Entity heroEntity) {
            this.heroEntity = heroEntity;
            this.fpCellX = getBodyComponentFieldPath(heroEntity, "cellX");
            this.fpCellY = getBodyComponentFieldPath(heroEntity, "cellY");
            this.fpCellZ = getBodyComponentFieldPath(heroEntity, "cellZ");
            this.fpVecX = getBodyComponentFieldPath(heroEntity, "vecX");
            this.fpVecY = getBodyComponentFieldPath(heroEntity, "vecY");
            this.fpVecZ = getBodyComponentFieldPath(heroEntity, "vecZ");
        }

        private FieldPath getBodyComponentFieldPath(Entity entity, String which) {
            return entity.getFieldPathForName(format("CBodyComponent.m_%s", which));
        }

        private boolean isPositionChanged(Entity e, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
            if (e != heroEntity) return false;
            for (int f = 0; f < nChangedFieldPaths; f++) {
                FieldPath changedFieldPath = changedFieldPaths[f];
                if (changedFieldPath.equals(fpCellX)) return true;
                if (changedFieldPath.equals(fpCellY)) return true;
                if (changedFieldPath.equals(fpCellZ)) return true;
                if (changedFieldPath.equals(fpVecX)) return true;
                if (changedFieldPath.equals(fpVecY)) return true;
                if (changedFieldPath.equals(fpVecZ)) return true;
            }
            return false;
        }

        private Vector getPosition() {
            return new Vector(
                    getPositionComponent(fpCellX, fpVecX),
                    getPositionComponent(fpCellY, fpVecY),
                    getPositionComponent(fpCellZ, fpVecZ)
            );
        }

        // Source 2 stores a position as a cell index plus an offset within the cell; one cell is 128 units wide.
        private float getPositionComponent(FieldPath fpCell, FieldPath fpVec) {
            int cell = heroEntity.getPropertyForFieldPath(fpCell);
            float vec = heroEntity.getPropertyForFieldPath(fpVec);
            return cell * 128.0f + vec;
        }
    }

}
