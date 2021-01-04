package skadistats.clarity.examples.serializers;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnMessage(Demo.CDemoSendTables.class)
    public void onSendTables(Context ctx, Demo.CDemoSendTables message) throws IOException {
        CodedInputStream cis = CodedInputStream.newInstance(ZeroCopy.extract(message.getData()));
        int size = cis.readRawVarint32();
        S2NetMessages.CSVCMsg_FlattenedSerializer fs = Packet.parse(S2NetMessages.CSVCMsg_FlattenedSerializer.class, ZeroCopy.wrap(cis.readRawBytes(size)));

        Set<String> baseTypes = new TreeSet<>();
        for (S2NetMessages.ProtoFlattenedSerializer_t s : fs.getSerializersList()) {
            for (int fi : s.getFieldsIndexList()) {
                S2NetMessages.ProtoFlattenedSerializerField_t f = fs.getFields(fi);
                FieldType ft = new FieldType(fs.getSymbols(f.getVarTypeSym()));
                if (!f.hasFieldSerializerNameSym()) {
                    int l = 0;
                    do {
                        baseTypes.add(ft.getBaseType().toUpperCase());
                        if ("CUTLVECTOR".equals(ft.getBaseType().toUpperCase())) {
                            ft = ft.getGenericType();
                        } else {
                            ft = null;
                        }
                        l++;
                    } while (l <= 1 && ft != null);
                }
            }
        }
        dump(ctx, fs);
    }

    private void dump(Context ctx, S2NetMessages.CSVCMsg_FlattenedSerializer fs) throws FileNotFoundException {
        String fileName = "flattables_" + ctx.getBuildNumber() + ".txt";
        log.info("writing {}", fileName);
        PrintStream out = new PrintStream(new FileOutputStream(fileName));
        for (S2NetMessages.ProtoFlattenedSerializer_t s : fs.getSerializersList()) {
            out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            out.format("%s(%s)\n", fs.getSymbols(s.getSerializerNameSym()), s.getSerializerVersion());
            out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            for (int fi : s.getFieldsIndexList()) {
                S2NetMessages.ProtoFlattenedSerializerField_t f = fs.getFields(fi);
                String line = String.format(
                    "type: %-50s name: %-30s node: %-41s serializer: %-35s flags: %8s bitcount: %3s low: %9s high: %9s",
                    String.format("%s%s", fs.getSymbols(f.getVarTypeSym()), f.hasVarEncoderSym() ? String.format(" {%s}", fs.getSymbols(f.getVarEncoderSym())) : ""),
                    fs.getSymbols(f.getVarNameSym()),
                    fs.getSymbols(f.getSendNodeSym()),
                    f.hasFieldSerializerNameSym() ? String.format("%s(%s)", fs.getSymbols(f.getFieldSerializerNameSym()), f.getFieldSerializerVersion()) : "-",
                    f.hasEncodeFlags() ? Integer.toHexString(f.getEncodeFlags()) : "-",
                    f.hasBitCount() ? f.getBitCount() : "-",
                    f.hasLowValue() ? f.getLowValue() : "-",
                    f.hasHighValue() ? f.getHighValue() : "-"
                );
                out.println(line);
            }
            out.println();
            out.println();
        }
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        runner.tick();
        runner.halt();
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
