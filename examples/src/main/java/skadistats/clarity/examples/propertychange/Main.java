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

@UsesEntities
@Example(name = "propertychange", description = "Log property changes on hero life states", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_lifeState")
    public void onEntityPropertyChanged(Context ctx, Entity e, FieldPath fp) {
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
