package skadistats.clarity.examples.dumpmana;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.HashMap;
import java.util.Map;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints hero mana and max mana of a Dota 2 replay whenever either value is created or changes.
 *
 * <p>Demonstrates the entity API: {@link OnEntityCreated} and {@link OnEntityUpdated} handlers,
 * {@link UsesEntities} to have the entities processor registered, and reading properties through a
 * {@link FieldPath} resolved once per DTClass by name ({@link Entity#getFieldPathForName(String)}).
 * Heroes are recognised by the DT class name prefix {@code CDOTA_Unit_Hero}. Each output line is
 * {@code <class name> (<mana>/<maxMana>)}.
 *
 * <p>The property names {@code m_flMana} and {@code m_flMaxMana} are Dota 2 names.
 *
 * <p>Run: {@code ./gradlew :examples:dumpmanaRun --args "path/to/replay.dem"}
 */
@UsesEntities
@Example(name = "dumpmana", description = "Print hero mana/max-mana values over time", category = Category.DOCS)
public class Main {

    private record ManaPaths(FieldPath mana, FieldPath maxMana) {}

    private final Map<DTClass, ManaPaths> manaPaths = new HashMap<>();

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }

    // Resolving a name to a FieldPath is a lookup; do it once per DTClass (field paths can differ between
    // classes) and reuse the paths for reads and for comparing against the changed paths of an update.
    private ManaPaths pathsFor(Entity e) {
        return manaPaths.computeIfAbsent(e.getDtClass(), c -> new ManaPaths(
                e.getFieldPathForName("m_flMana"),
                e.getFieldPathForName("m_flMaxMana")
        ));
    }

    // fires once per new entity, with its state already populated
    @OnEntityCreated
    public void onCreated(Entity e) {
        if (!isHero(e)) {
            return;
        }
        ManaPaths p = pathsFor(e);
        System.out.format("%s (%s/%s)\n", e.getDtClass().getDtName(), e.getPropertyForFieldPath(p.mana()), e.getPropertyForFieldPath(p.maxMana()));
    }

    // updatedPaths holds the field paths changed by the packet; only the first updateCount entries are valid
    @OnEntityUpdated
    public void onUpdated(Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (!isHero(e)) {
            return;
        }
        ManaPaths p = pathsFor(e);
        boolean update = false;
        for (int i = 0; i < updateCount; i++) {
            if (updatedPaths[i].equals(p.mana()) || updatedPaths[i].equals(p.maxMana())) {
                update = true;
                break;
            }
        }
        if (update) {
            System.out.format("%s (%s/%s)\n", e.getDtClass().getDtName(), e.getPropertyForFieldPath(p.mana()), e.getPropertyForFieldPath(p.maxMana()));
        }
    }


    public void run(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
