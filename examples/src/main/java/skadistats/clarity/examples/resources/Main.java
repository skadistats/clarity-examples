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
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Resolves the model of every entity to its resource path.
 *
 * <p>In Source 2, entities refer to models by a 64-bit resource handle (the property
 * {@code CBodyComponent.m_hModel}). The handle is the MurmurHash of the full resource path, so it cannot be
 * turned back into a path directly. The {@link Resources} processor collects the resource manifests sent by the
 * server (game session manifest and spawn group manifests) and keeps a handle-to-path lookup.</p>
 *
 * <p>For each entity that has a non-zero model handle, the example prints a line like
 * {@code model for entity at <index> (<handle>): <path>.vmdl}. If the handle is not listed in any manifest,
 * {@link Resources#getEntryForResourceHandle(long)} returns {@code null} and {@code null} is printed.</p>
 *
 * <p>Shows: {@link UsesResources} (activates the {@link Resources} processor), {@link Insert} (injects it),
 * {@link OnEntityCreated}. Works with the Source 2 engines (Dota 2, CS2, Deadlock); {@link Resources} is not
 * available for Source 1 replays.</p>
 *
 * <p>Run: {@code ./gradlew :examples:resourcesRun --args "path/to/replay.dem"}</p>
 */
@UsesResources
@UsesEntities
@Example(name = "resources", description = "Resolve and display model resources for entities", category = Category.DOCS)
public class Main {

    // Injects the Resources processor; it is instantiated because of @UsesResources on the class.
    @Insert
    private Resources resources;

    @OnEntityCreated
    public void onCreated(Entity e) {
        // Returns null for entities without a model component.
        FieldPath fp = e.getFieldPathForName("CBodyComponent.m_hModel");
        if (fp == null) {
            return;
        }
        Long resourceHandle = e.getPropertyForFieldPath(fp);
        if (resourceHandle == null || resourceHandle == 0L) {
            return;
        }
        // Handle 0 means "no model".
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
