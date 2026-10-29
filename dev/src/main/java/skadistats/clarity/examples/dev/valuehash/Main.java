package skadistats.clarity.examples.dev.valuehash;

import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.Arrays;
import java.util.Objects;

/**
 * Regression check for changes to entity state layout, decoders or write paths. Folds the decoded value of every
 * property on entity creation and of every changed property on update, together with entity index and field path,
 * into one hash per replay. Run on the old and the new build; the hashes must match. Field-path-only checks
 * (e.g. a path hash) miss values written to the wrong slot.
 * <p>Arguments: {@code replay...}.
 * <p>Run: {@code ./gradlew :dev:valuehashRun --args "path/to/replay.dem"}
 */
@Example(name = "valuehash", description = "Hash all decoded entity property values of a replay", category = Category.DEV)
public class Main {

    @UsesEntities
    public static class Listener {
        long hash;
        long updates;

        void fold(Entity e, FieldPath fp) {
            hash = hash * 31 + e.getIndex();
            hash = hash * 31 + fp.hashCode();
            hash = hash * 31 + valueHash(e.getPropertyForFieldPath(fp));
            updates++;
        }

        @OnEntityCreated
        public void onCreated(Entity e) {
            var it = e.getState().fieldPathIterator();
            while (it.hasNext()) fold(e, it.next());
        }

        @OnEntityUpdated
        public void onUpdated(Entity e, FieldPath[] fps, int n) {
            for (var i = 0; i < n; i++) fold(e, fps[i]);
        }
    }

    static int valueHash(Object v) {
        if (v == null) return 0;
        if (v instanceof Object[] a) return Arrays.deepHashCode(a);
        if (v instanceof int[] a) return Arrays.hashCode(a);
        if (v instanceof long[] a) return Arrays.hashCode(a);
        if (v instanceof float[] a) return Arrays.hashCode(a);
        if (v instanceof byte[] a) return Arrays.hashCode(a);
        if (v instanceof Serializer s) return Objects.hash(s.getId().toString(), s.getFieldCount());
        if (v instanceof Float f) return Float.floatToIntBits(f);
        if (v.hashCode() == System.identityHashCode(v)) return v.getClass().getName().hashCode();
        return v.hashCode();
    }

    public static void main(String[] args) throws Exception {
        for (var replay : args) {
            var listener = new Listener();
            try (var source = new MappedFileSource(replay)) {
                new SimpleRunner(source).runWith(listener);
            }
            System.out.println(replay + " valuehash " + listener.hash + " updates " + listener.updates);
        }
    }

}
