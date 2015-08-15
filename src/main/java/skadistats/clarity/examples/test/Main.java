package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.decoder.s2.FieldPathDecoder;
import skadistats.clarity.decoder.s2.prop.*;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.FieldPath;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.util.TextTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UsesDTClasses
@UsesStringTable("instancebaseline")
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private final Map<String, FieldDecoder<?>> decoders = new HashMap<>();
    {
        decoders.put("bool", new BoolDecoder());

        // POINTERS
        decoders.put("CDOTAGamerules", new BoolDecoder());
        decoders.put("CDOTAGameManager", new BoolDecoder());
        decoders.put("CDOTASpectatorGraphManager", new BoolDecoder());
        decoders.put("CEntityIdentity", new UInt64Decoder());

        decoders.put("uint8", new UInt64Decoder());
        decoders.put("uint16", new UInt64Decoder());
        decoders.put("uint32", new UInt64Decoder());
        decoders.put("uint64", new UInt64Decoder());

        decoders.put("int8", new SInt64Decoder());
        decoders.put("int16", new SInt64Decoder());
        decoders.put("int32", new SInt64Decoder());
        decoders.put("int64", new SInt64Decoder());

        decoders.put("float32", new Float32Decoder());
        decoders.put("CNetworkedQuantizedFloat", new Float32Decoder());

        decoders.put("gender_t", new UInt64Decoder());
        decoders.put("DamageOptions_t", new UInt64Decoder());
        decoders.put("MoveCollide_t", new UInt64Decoder());
        decoders.put("MoveType_t", new UInt64Decoder());
        decoders.put("RenderMode_t", new UInt64Decoder());
        decoders.put("RenderFx_t", new UInt64Decoder());
        decoders.put("SolidType_t", new UInt64Decoder());
        decoders.put("SurroundingBoundsType_t", new UInt64Decoder());
        decoders.put("DOTA_SHOP_TYPE", new UInt64Decoder());
        decoders.put("DOTA_HeroPickState", new UInt64Decoder());



        decoders.put("CUtlSymbolLarge", new StringDecoder());
        decoders.put("char", new UInt64Decoder());

        decoders.put("CHandle", new UInt64Decoder());
        decoders.put("CStrongHandle", new UInt64Decoder());
        decoders.put("CGameSceneNodeHandle", new UInt64Decoder());
        decoders.put("HSequence", new UInt64Decoder());

        decoders.put("Color", new UInt64Decoder());
        decoders.put("color32", new UInt64Decoder());

        decoders.put("CBodyComponent", new UInt64Decoder());
        decoders.put("CPhysicsComponent", new UInt64Decoder());
        decoders.put("CRenderComponent", new UInt64Decoder());

        decoders.put("CUtlStringToken", new UInt64Decoder());




        decoders.put("CUtlVector", new UInt64Decoder());
        decoders.put("Vector", new VectorDecoder());
        decoders.put("QAngle", new QAngleDecoder());


        decoders.put("DOTA_PlayerChallengeInfo", new UInt64Decoder());

        decoders.put("m_SpeechBubbles", new UInt64Decoder());
        decoders.put("C_DOTA_ItemStockInfo", new BoolDecoder());
        decoders.put("CDOTA_AbilityDraftAbilityState", new UInt64Decoder());



    }



    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) throws InterruptedException {
        if (ctx.getTick() == 1) {
            //System.out.println(new HuffmanGraph(FieldPathDecoder.HUFFMAN_TREE).generate());
            StringTables stringTables = ctx.getProcessor(StringTables.class);
            DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
            StringTable baseline = stringTables.forName("instancebaseline");

            for (int idx = 0; idx < baseline.getEntryCount(); idx++) {
                int clsId = Integer.valueOf(baseline.getNameByIndex(idx));
                if (baseline.getValueByIndex(idx) != null) {
                    S2DTClass dtClass = (S2DTClass) dtClasses.forClassId(clsId);

                    TextTable.Builder b = new TextTable.Builder();
                    b.setTitle(dtClass.getDtName());
                    b.addColumn("FieldPath");
                    b.addColumn("Name");
                    b.addColumn("Type");
                    b.addColumn("Low");
                    b.addColumn("High");
                    b.addColumn("BitCount");
                    b.addColumn("Flags");
                    b.addColumn("Decoder");
                    b.addColumn("Decoded Value");
                    b.addColumn("Bits Read");
                    TextTable t = b.build();

                    BitStream bs = new BitStream(baseline.getValueByIndex(idx));
                    List<FieldPath> fieldPaths = FieldPathDecoder.decode(bs);
                    int r = 0;
                    for (FieldPath fp : fieldPaths) {
                        Field f = dtClass.getFieldForFieldPath(fp);
                        FieldDecoder<?> fieldDecoder = decoders.get(f.getType().getBaseType());
                        t.setData(r, 0, fp);
                        t.setData(r, 1, dtClass.getNameForFieldPath(fp));
                        t.setData(r, 2, f.getType().getBaseType() + (f.getType().isPointer() ? " (POINTER)" : ""));
                        t.setData(r, 3, f.getLowValue());
                        t.setData(r, 4, f.getHighValue());
                        t.setData(r, 5, f.getBitCount());
                        t.setData(r, 6, f.getEncodeFlags());
                        if (fieldDecoder != null) {
                            t.setData(r, 7, fieldDecoder.getClass().getSimpleName());
                            int offsBefore = bs.pos();
                            t.setData(r, 8, fieldDecoder.decode(bs, f));
                            t.setData(r, 9, bs.pos() - offsBefore);
                        }
                        if (fieldDecoder == null) {
                            Thread.sleep(100L);
                            System.err.println("NO FIELD DECODER FOR " + f.getType().getBaseType());
                            System.exit(1);
                        }
                        r++;
                    }
                    t.print(System.out);
                    System.out.format("%s/%s remaining\n", bs.remaining(), bs.len());
                    System.out.println();
                    System.out.println();

                    //System.exit(1);
                }
            }
        }
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
