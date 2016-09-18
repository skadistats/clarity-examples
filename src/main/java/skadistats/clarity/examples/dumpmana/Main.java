package skadistats.clarity.examples.dumpmana;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private FieldPath mana;
    private FieldPath maxMana;

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }

    private void ensureFieldPaths(Entity e) {
        if (mana == null) {
            mana = e.getDtClass().getFieldPathForName("m_flMana");
            maxMana = e.getDtClass().getFieldPathForName("m_flMaxMana");
        }
    }

    @OnEntityCreated
    public void onCreated(Entity e) {
        if (!isHero(e)) {
            return;
        }
        ensureFieldPaths(e);
        System.out.format("%s (%s/%s)\n", e.getDtClass().getDtName(), e.getPropertyForFieldPath(mana), e.getPropertyForFieldPath(maxMana));
    }

    @OnEntityUpdated
    public void onUpdated(Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (!isHero(e)) {
            return;
        }
        ensureFieldPaths(e);
        boolean update = false;
        for (int i = 0; i < updateCount; i++) {
            if (updatedPaths[i].equals(mana) || updatedPaths[i].equals(maxMana)) {
                update = true;
                break;
            }
        }
        if (update) {
            System.out.format("%s (%s/%s)\n", e.getDtClass().getDtName(), e.getPropertyForFieldPath(mana), e.getPropertyForFieldPath(maxMana));
        }
    }


    public void run(String[] args) throws Exception {
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
