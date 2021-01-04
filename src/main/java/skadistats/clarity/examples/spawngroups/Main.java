package skadistats.clarity.examples.spawngroups;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.util.LZSS;
import skadistats.clarity.wire.common.proto.NetworkBaseTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

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

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_Load.class)
    public void onLoad(NetworkBaseTypes.CNETMsg_SpawnGroup_Load message) throws IOException {
        System.out.println("LOAD ----------------------------------------------------------------------------------------------");
        System.out.println(message);
        parse(message.getSpawngroupmanifest());
        loaded.add(message.getSpawngrouphandle());
        if (!message.getManifestincomplete()) {
            complete.add(message.getSpawngrouphandle());
        }
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted.class)
    public void onLoadCompleted(NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted message) {
        System.out.println("LOADCOMPLETED ----------------------------------------------------------------------------------------------");
        System.out.println(message);
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate.class)
    public void onManifestUpdate(NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate message) throws IOException {
        System.out.println("MANIFEST UPDATE ----------------------------------------------------------------------------------------------");
        System.out.println(message);
        parse(message.getSpawngroupmanifest());
        if (!message.getManifestincomplete()) {
            complete.add(message.getSpawngrouphandle());
        }
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick.class)
    public void onSetCreationTick(NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick message) {
        System.out.println("SET CREATION TICK  ----------------------------------------------------------------------------------------------");
        System.out.println(message);
        created.add(message.getSpawngrouphandle());
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_Unload.class)
    public void onUnload(NetworkBaseTypes.CNETMsg_SpawnGroup_Unload message) {
        System.out.println("UNLOAD  ----------------------------------------------------------------------------------------------");
        System.out.println(message);
    }

    public void run(String[] args) throws Exception {
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
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
