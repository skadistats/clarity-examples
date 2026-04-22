package skadistats.clarity.examples.dev.pointerstats;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.wire.shared.s2.proto.S2NetMessages;

import java.io.IOException;
import java.util.TreeMap;

@Example(name = "pointerstats", description = "Count pointer fields by polymorphic type count", category = Category.DEV)
public class Main {

    @OnMessage(Demo.CDemoSendTables.class)
    public void onSendTables(Context ctx, Demo.CDemoSendTables message) throws IOException {
        CodedInputStream cis = CodedInputStream.newInstance(ZeroCopy.extract(message.getData()));
        int size = cis.readRawVarint32();
        var fs = Packet.parse(S2NetMessages.CSVCMsg_FlattenedSerializer.class, ZeroCopy.wrap(cis.readRawBytes(size)));

        // count unique pointer fields (by field index) bucketed by number of polymorphic types
        var seen = new java.util.BitSet(fs.getFieldsCount());
        // bucket: typeCount -> count of distinct fields
        var buckets = new TreeMap<Integer, Integer>();

        for (var s : fs.getSerializersList()) {
            for (int fi : s.getFieldsIndexList()) {
                if (seen.get(fi)) continue;
                seen.set(fi);
                var f = fs.getFields(fi);
                if (!f.hasFieldSerializerNameSym()) continue;
                var ft = new FieldType(fs.getSymbols(f.getVarTypeSym()));
                if (!ft.isPointer()) continue;
                int typeCount = f.getPolymorphicTypesCount() + 1;
                buckets.merge(typeCount, 1, Integer::sum);
                if (typeCount > 1) {
                    System.out.printf("  [polymorphic] %s %s: base=%s, types=%s%n",
                        fs.getSymbols(f.getVarTypeSym()),
                        fs.getSymbols(f.getVarNameSym()),
                        fs.getSymbols(f.getFieldSerializerNameSym()),
                        f.getPolymorphicTypesList().stream()
                            .map(p -> fs.getSymbols(p.getPolymorphicFieldSerializerNameSym()))
                            .toList()
                    );
                }
            }
        }

        System.out.printf("Pointer field stats for build %s:%n", ctx.getGameVersion());
        int total = 0;
        for (var e : buckets.entrySet()) {
            System.out.printf("  %d serializer(s): %d field(s)%n", e.getKey(), e.getValue());
            total += e.getValue();
        }
        System.out.printf("  total pointer fields: %d%n", total);
    }

    public void run(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            var runner = new ControllableRunner(source).runWith(this);
            try {
                runner.tick();
            } finally {
                runner.halt();
                runner.join();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
