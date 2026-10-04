package skadistats.clarity.examples.spawngroups;

import skadistats.clarity.protobuf.ByteString;
import skadistats.clarity.protobuf.ZeroCopy;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.util.LZSS;
import skadistats.clarity.wire.shared.s2.proto.S2NetworkBaseTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints the spawn group messages of a Source 2 replay and decodes their resource manifests.
 *
 * <p>A spawn group is a set of entities and resources (for example a map or a part of it) that the server
 * loads on the client. The server announces them with {@code CNETMsg_SpawnGroup_*} messages: {@code Load},
 * {@code ManifestUpdate}, {@code LoadCompleted}, {@code SetCreationTick} and {@code Unload}. {@code Load} and
 * {@code ManifestUpdate} carry a manifest, a (possibly LZSS-compressed) bit-packed list of resource paths.</p>
 *
 * <p>For every message the example prints its name and content. For {@code Load} and {@code ManifestUpdate} it
 * also decodes the manifest by hand (compression flag, size, type and directory tables, then one
 * directory/name/type entry per resource) and prints the dirs, types and all resource paths. At the end it
 * lists the spawn group handles that were loaded, completed (manifest flagged complete), created
 * (creation tick set), loaded but not created, and loaded but not completed.</p>
 *
 * <p>Clarity decodes the same manifests in its {@code Resources} processor; see the {@code resources}
 * example for the high-level API. This one shows the raw format. Works with Source 2 replays (Dota 2, CS2,
 * Deadlock); the messages do not exist in Source 1.</p>
 *
 * <p>Run: {@code ./gradlew :examples:spawngroupsRun --args "path/to/replay.dem"}</p>
 */
@Example(name = "spawngroups", description = "Parse spawn group data from replay packets", category = Category.DOCS)
public class Main {

    // Manifest layout: 1 bit compressed flag, 24 bit size, then the payload (LZSS-packed if flagged).
    private void parse(ByteString raw) throws IOException {
        BitStream bs = BitStream.createBitStream(raw);
        boolean isCompressed = bs.readBitFlag();
        int size = bs.readUBitInt(24);

        byte[] data;
        if (isCompressed) {
            data = LZSS.unpack(bs);
        } else {
            data = new byte[size];
            bs.readBitsIntoByteArray(data, size * 8);
        }
        bs = BitStream.createBitStream(ZeroCopy.wrap(data));

        List<String> types = new ArrayList<>();
        List<String> dirs = new ArrayList<>();

        int nTypes = bs.readUBitInt(16);
        int nDirs = bs.readUBitInt(16);
        int nEntries = bs.readUBitInt(16);
        for (int i = 0; i < nTypes; i++) {
            types.add(bs.readString(Integer.MAX_VALUE));
        }
        for (int i = 0; i < nDirs; i++) {
            dirs.add(bs.readString(Integer.MAX_VALUE));
        }
        // Entries index into the dir/type tables with the minimum number of bits needed.
        int bitsForType = Math.max(1, Util.calcBitsNeededFor(types.size() - 1));
        int bitsForDir = Math.max(1, Util.calcBitsNeededFor(dirs.size() - 1));
        System.out.format("\n\nbitsForType: %d, bitsForDir: %d, nEntries: %d\n", bitsForType, bitsForDir, nEntries);
        System.out.printf("dirs: %s\n", dirs);
        System.out.printf("types: %s\n", types);
        for (int i = 0; i < nEntries; i++) {
            int x = bs.readUBitInt(bitsForDir);
            String s = bs.readString(Integer.MAX_VALUE);
            int y = bs.readUBitInt(bitsForType);
            System.out.format("[%03d] %s%s.%s\n", i, dirs.get(x), s, types.get(y));
        }
        System.out.format("finished %d/%d\n\n", bs.pos(), bs.len());
    }

    private final Set<Integer> loaded = new LinkedHashSet<>();
    private final Set<Integer> complete = new LinkedHashSet<>();
    private final Set<Integer> created = new LinkedHashSet<>();

    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Load.class)
    public void onLoad(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Load message) throws IOException {
        System.out.println("LOAD ----------------------------------------------------------------------------------------------");
        System.out.println(message);
        parse(message.getSpawngroupmanifest());
        loaded.add(message.getSpawngrouphandle());
        if (!message.getManifestincomplete()) {
            complete.add(message.getSpawngrouphandle());
        }
    }

    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted.class)
    public void onLoadCompleted(S2NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted message) {
        System.out.println("LOADCOMPLETED ----------------------------------------------------------------------------------------------");
        System.out.println(message);
    }

    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate.class)
    public void onManifestUpdate(S2NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate message) throws IOException {
        System.out.println("MANIFEST UPDATE ----------------------------------------------------------------------------------------------");
        System.out.println(message);
        parse(message.getSpawngroupmanifest());
        if (!message.getManifestincomplete()) {
            complete.add(message.getSpawngrouphandle());
        }
    }

    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick.class)
    public void onSetCreationTick(S2NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick message) {
        System.out.println("SET CREATION TICK  ----------------------------------------------------------------------------------------------");
        System.out.println(message);
        created.add(message.getSpawngrouphandle());
    }

    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Unload.class)
    public void onUnload(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Unload message) {
        System.out.println("UNLOAD  ----------------------------------------------------------------------------------------------");
        System.out.println(message);
    }

    public void run(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        }
        System.out.println("LOADED " + loaded);
        System.out.println("COMPLETED " + complete);
        System.out.println("CREATED " + created);

        HashSet<Integer> lbnc = new HashSet<>(loaded);
        lbnc.removeAll(created);
        System.out.println("LOADED BUT NOT CREATED " + lbnc);

        lbnc = new HashSet<>(loaded);
        lbnc.removeAll(complete);
        System.out.println("LOADED BUT NOT COMPLETED " + lbnc);

    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
