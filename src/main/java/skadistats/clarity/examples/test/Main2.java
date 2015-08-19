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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

@UsesDTClasses
@UsesStringTable("instancebaseline")
public class Main2 {

    private final Logger log = LoggerFactory.getLogger(Main2.class.getPackage().getClass());

    private final Map<String, FieldDecoder<?>> decoders = new HashMap<>();

    {
        decoders.put("bool", new BoolDecoder());

        decoders.put("CDOTAGamerules", new BoolDecoder());
        decoders.put("CDOTAGameManager", new BoolDecoder());
        decoders.put("CDOTASpectatorGraphManager", new BoolDecoder());
        decoders.put("CDOTA_AbilityDraftAbilityState", new UInt64Decoder());
        decoders.put("C_DOTA_ItemStockInfo", new UInt64Decoder());

        decoders.put("uint8", new ConstantLengthDecoder(8));
        decoders.put("uint16", new VarUDecoder(16));
        decoders.put("uint32", new VarUDecoder(32));
        decoders.put("uint64", new UInt64Decoder());

        decoders.put("int8", new ConstantLengthDecoder(8));
        decoders.put("int16", new VarSDecoder(16));
        decoders.put("int32", new VarSDecoder(32));
        decoders.put("int64", new SInt64Decoder());

        decoders.put("CUtlSymbolLarge", new StringDecoder());
        decoders.put("char", new StringDecoder());

        decoders.put("float32", new QFloatDecoder());
        decoders.put("CNetworkedQuantizedFloat", new QFloatDecoder());

        decoders.put("gender_t", new UInt64Decoder());
        decoders.put("DamageOptions_t", new UInt64Decoder());
        decoders.put("RenderMode_t", new UInt64Decoder());
        decoders.put("RenderFx_t", new UInt64Decoder());
        decoders.put("SurroundingBoundsType_t", new UInt64Decoder());
        decoders.put("DOTA_SHOP_TYPE", new UInt64Decoder());
        decoders.put("DOTA_HeroPickState", new UInt64Decoder());
        decoders.put("attributeprovidertypes_t", new UInt64Decoder());
        decoders.put("CourierState_t", new UInt64Decoder());
        decoders.put("MoveCollide_t", new UInt64Decoder());
        decoders.put("MoveType_t", new UInt64Decoder());


        decoders.put("SolidType_t", new UInt64Decoder());


        decoders.put("CHandle", new VarUDecoder(32));
        decoders.put("CGameSceneNodeHandle", new VarUDecoder(32));


        decoders.put("HSequence", new SkipDecoder(1));
        decoders.put("CStrongHandle", new VarUDecoder(32));

        decoders.put("CEntityIdentity", new SkipDecoder(1));


        decoders.put("Color", new UInt64Decoder());
        decoders.put("color32", new UInt64Decoder());

        decoders.put("CBodyComponent", new BoolDecoder());
        decoders.put("CPhysicsComponent", new UInt64Decoder());
        decoders.put("CRenderComponent", new UInt64Decoder());

        //decoders.put("CUtlStringToken", new ConstantLengthDecoder(10));
        decoders.put("CUtlStringToken", new VarUDecoder(32));

        decoders.put("CUtlVector", new UInt64Decoder());
        decoders.put("Vector", new VectorDecoder());
        decoders.put("QAngle", new QAngleDecoder());

        decoders.put("DOTA_PlayerChallengeInfo", new UInt64Decoder());
        decoders.put("m_SpeechBubbles", new UInt64Decoder());


    }

    public static void main(String[] args) throws Exception {
        new Main2().run(args);
    }

    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        if (ctx.getTick() == 8000) {
            //System.out.println(new HuffmanGraph(FieldPathDecoder.HUFFMAN_TREE).generate());
            StringTables stringTables = ctx.getProcessor(StringTables.class);
            DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
            StringTable baseline = stringTables.forName("instancebaseline");


            PrintStream[] ps = new PrintStream[]{
                System.out,
                null
            };

            List<String> onlyThese = new ArrayList<>();
            onlyThese = Arrays.asList("CDOTA_BaseNPC");


            for (int skip = 1; skip < 500; skip++) {
                decoders.put("CEntityIdentity", new SkipDecoder(skip));

                for (int idx = 0; idx < baseline.getEntryCount(); idx++) {
                    int clsId = Integer.valueOf(baseline.getNameByIndex(idx));
                    if (baseline.getValueByIndex(idx) != null) {
                        S2DTClass dtClass = (S2DTClass) dtClasses.forClassId(clsId);

                        if (onlyThese.size() != 0 && !onlyThese.contains(dtClass.getDtName())) {
                            continue;
                        }
                        if (ps[1] == null)
                            ps[1] = new PrintStream(new FileOutputStream("baselines/" + dtClass.getDtName() + ".txt"), true, "UTF-8");

                        TextTable.Builder b = new TextTable.Builder();
                        b.setTitle(dtClass.getDtName());
                        b.setFrame(TextTable.FRAME_COMPAT);
                        b.setPadding(0, 0);
                        b.addColumn("FP");
                        b.addColumn("Name");
                        b.addColumn("L", TextTable.Alignment.RIGHT);
                        b.addColumn("H", TextTable.Alignment.RIGHT);
                        b.addColumn("BC", TextTable.Alignment.RIGHT);
                        b.addColumn("Flags", TextTable.Alignment.RIGHT);
                        b.addColumn("Decoder");
                        b.addColumn("Type");
                        b.addColumn("Value");
                        b.addColumn("#", TextTable.Alignment.RIGHT);
                        b.addColumn("read");
                        TextTable t = b.build();

                        BitStream bs = new BitStream(baseline.getValueByIndex(idx));
                        List<FieldPath> fieldPaths = FieldPathDecoder.decode(bs);

                        boolean allHandlesOk = true;
                        boolean found = false;

                        int r = 0;
                        for (FieldPath fp : fieldPaths) {
                            Field f = dtClass.getFieldForFieldPath(fp);
                            FieldDecoder<?> fieldDecoder = decoders.get(f.getType().getBaseType());
                            t.setData(r, 0, fp);
                            t.setData(r, 1, dtClass.getNameForFieldPath(fp));
                            t.setData(r, 2, f.getLowValue());
                            t.setData(r, 3, f.getHighValue());
                            t.setData(r, 4, f.getBitCount());
                            t.setData(r, 5, f.getEncodeFlags());
                            t.setData(r, 7, f.getType().getBaseType() + (f.getType().isPointer() ? "*" : ""));
                            Object value = null;
                            if (fieldDecoder != null) {
                                t.setData(r, 6, fieldDecoder.getClass().getSimpleName());
                                int offsBefore = bs.pos();
                                value = fieldDecoder.decode(bs, f);
                                t.setData(r, 8, value);
                                t.setData(r, 9, bs.pos() - offsBefore);
                                t.setData(r, 10, bs.toString(offsBefore, bs.pos()));
                            }
                            if (fieldDecoder == null) {
                                Thread.sleep(100L);
                                System.err.println("NO FIELD DECODER FOR " + f.getType().getBaseType());
                                System.exit(1);
                            }
                            r++;

                            if ((fp.path[0] == 37) && (long) value != 16777215L) {
                                allHandlesOk = false;
                            }
                        }
                        if (allHandlesOk || found) {
                            for (PrintStream s : ps) {
                                s.format("SKIP: %s %s/%s\n", skip, allHandlesOk ? "ALLZERO" : "", found ? "FOUND" : "");
                                t.print(s);
                                s.format("%s/%s remaining\n\n\n", bs.remaining(), bs.len());
                            }
                        }

                        //System.exit(1);
                    }
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

}
