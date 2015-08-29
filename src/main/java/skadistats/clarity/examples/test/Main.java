package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldPathDecoder;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
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
import java.util.ArrayList;
import java.util.List;

@UsesDTClasses
@UsesStringTable("instancebaseline")
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        if (ctx.getTick() == 1) {
            //System.out.println(new HuffmanGraph(FieldPathDecoder.HUFFMAN_TREE).generate());
            StringTables stringTables = ctx.getProcessor(StringTables.class);
            DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
            StringTable baseline = stringTables.forName("instancebaseline");


            PrintStream[] ps = new PrintStream[] {
                System.out,
                null,
            };

            List<String> onlyThese = new ArrayList<>();
            //onlyThese = Arrays.asList("CBaseAnimating");

            Exception exx;
            for (int idx = 0; idx < baseline.getEntryCount(); idx++) {
                int clsId = Integer.valueOf(baseline.getNameByIndex(idx));
                if (baseline.getValueByIndex(idx) != null) {
                    S2DTClass dtClass = (S2DTClass) dtClasses.forClassId(clsId);

                    if (onlyThese.size() != 0 && !onlyThese.contains(dtClass.getDtName())) {
                        continue;
                    }

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
                    exx = null;
                    try {
                        List<FieldPath> fieldPaths = FieldPathDecoder.decode(bs);
                        int r = 0;
                        for (FieldPath fp : fieldPaths) {
                            Field f = dtClass.getFieldForFieldPath(fp);
                            t.setData(r, 0, fp);
                            t.setData(r, 1, dtClass.getNameForFieldPath(fp));
                            t.setData(r, 2, f.getLowValue());
                            t.setData(r, 3, f.getHighValue());
                            t.setData(r, 4, f.getBitCount());
                            t.setData(r, 5, Integer.toHexString(f.getEncodeFlags()));
                            t.setData(r, 7, String.format("%s%s%s", f.getType().getBaseType(), (f.getType().isPointer() ? "*" : ""), f.getEncoder() != null ? String.format(" (%s)", f.getEncoder()) : ""));

                            int offsBefore = bs.pos();
                            Unpacker unpacker = S2UnpackerFactory.createUnpacker(f);
                            Object data = unpacker.unpack(bs);
                            t.setData(r, 6, unpacker.getClass().getSimpleName().toString());
                            t.setData(r, 8, data);
                            t.setData(r, 9, bs.pos() - offsBefore);
                            t.setData(r, 10, bs.toString(offsBefore, bs.pos()));
                            r++;
                        }
                    } catch (Exception e) {
                        exx = e;
                    } finally {
                        for (PrintStream s : ps) {
                            t.print(s);
                            s.format("%s/%s remaining: %s\n", bs.remaining(), bs.len(), bs.toString(bs.pos(), bs.len()));
                            if (exx != null) {
                                exx.printStackTrace(s);
                            }
                            s.format("\n\n\n");
                        }
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

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
