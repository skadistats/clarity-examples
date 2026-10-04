package skadistats.clarity.examples.propertychange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityPropertyChanged;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints every change of the {@code m_lifeState} property of Dota 2 hero entities.
 *
 * <p>Demonstrates {@link OnEntityPropertyChanged} with its two regular-expression filters. Each output line holds
 * the tick, the entity's class name, the property name and the new value. The event fires for every property when an
 * entity is created (so each hero's initial state is printed) and then for each later change.
 *
 * <p>Dota 2 (the class pattern {@code CDOTA_Unit_Hero_.*} is Source 2 naming, and {@code m_lifeState} is a Dota/Source 2
 * property). Logs the total run time.
 *
 * <p>Run:
 * <pre>
 * ./gradlew :examples:propertychangeRun --args "path/to/replay.dem"
 * </pre>
 */
@UsesEntities
@Example(name = "propertychange", description = "Log property changes on hero life states", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    // Both patterns must match the whole name (full match, not a find): the DT class name of the entity and the
    // property name of the changed field path. Defaults are ".*". Clarity applies the filters before calling this method.
    @OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_lifeState")
    public void onEntityPropertyChanged(Context ctx, Entity e, FieldPath fp) {
        // fp is the changed property: its name and its new value are read back from the entity.
        System.out.format(
                "%6d %s: %s = %s\n",
                ctx.getTick(),
                e.getDtClass().getDtName(),
                e.getNameForFieldPath(fp),
                e.getPropertyForFieldPath(fp)
        );
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
