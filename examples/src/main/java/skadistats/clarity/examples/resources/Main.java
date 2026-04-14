package skadistats.clarity.examples.resources;

import skadistats.clarity.event.Insert;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.resources.Resources;
import skadistats.clarity.processor.resources.UsesResources;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;

@UsesResources
@UsesEntities
public class Main {

    @Insert
    private Resources resources;

    @OnEntityCreated
    public void onCreated(Entity e) {
        FieldPath fp = e.getFieldPathForName("CBodyComponent.m_hModel");
        if (fp == null) {
            return;
        }
        Long resourceHandle = e.getPropertyForFieldPath(fp);
        if (resourceHandle == null || resourceHandle == 0L) {
            return;
        }
        Resources.Entry entry = resources.getEntryForResourceHandle(resourceHandle);
        System.out.format("model for entity at %d (%d): %s\n", e.getIndex(), resourceHandle, entry);
    }

    public void run(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            SimpleRunner runner = new SimpleRunner(source);
            runner.runWith(this);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
