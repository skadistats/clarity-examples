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

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @Insert
    private DTClasses dtClasses;

    @Insert
    private Entities entities;

    private DTClass playerResourceClass;
    private final PlayerResourceLookup[] playerLookup = new PlayerResourceLookup[10];
    private final HeroLookup[] heroLookup = new HeroLookup[10];
    private final List<Runnable> deferredActions = new ArrayList<>();

    @OnDTClassesComplete
    protected void onDtClassesComplete() {
        playerResourceClass = dtClasses.forDtName("CDOTA_PlayerResource");
        for (int i = 0; i < 10; i++) {
            playerLookup[i] = new PlayerResourceLookup(playerResourceClass, i);
        }
    }

    @OnEntityUpdated
    protected void onEntityUpdated(Entity e, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        if (e.getDtClass() == playerResourceClass) {
            for (int p = 0; p < 10; p++) {
                PlayerResourceLookup lookup = playerLookup[p];
                if (lookup.isSelectedHeroChanged(e, changedFieldPaths, nChangedFieldPaths)) {
                    int playerIndex = p;
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

    @OnTickEnd
    protected void onTickEnd(boolean synthetic) {
        deferredActions.forEach(Runnable::run);
        deferredActions.clear();
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        MappedFileSource s = new MappedFileSource(args[0]);
        SimpleRunner runner = new SimpleRunner(s);
        runner.runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        s.close();
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

        private PlayerResourceLookup(DTClass playerResourceClass, int idx) {
            this.fpSelectedHero = playerResourceClass.getFieldPathForName(
                    format("m_vecPlayerTeamData.%s.m_hSelectedHero", Util.arrayIdxToString(idx))
            );
        }

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
            DTClass heroClass = heroEntity.getDtClass();
            this.fpCellX = getBodyComponentFieldPath(heroClass, "cellX");
            this.fpCellY = getBodyComponentFieldPath(heroClass, "cellY");
            this.fpCellZ = getBodyComponentFieldPath(heroClass, "cellZ");
            this.fpVecX = getBodyComponentFieldPath(heroClass, "vecX");
            this.fpVecY = getBodyComponentFieldPath(heroClass, "vecY");
            this.fpVecZ = getBodyComponentFieldPath(heroClass, "vecZ");
        }

        private FieldPath getBodyComponentFieldPath(DTClass heroClass, String which) {
            return heroClass.getFieldPathForName(format("CBodyComponent.m_%s", which));
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

        private float getPositionComponent(FieldPath fpCell, FieldPath fpVec) {
            int cell = heroEntity.getPropertyForFieldPath(fpCell);
            float vec = heroEntity.getPropertyForFieldPath(fpVec);
            return cell * 128.0f + vec;
        }
    }

}
