package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldOpType;
import skadistats.clarity.decoder.s2.HuffmanTree;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.s2.FieldPath;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.model.s2.field.Field;
import skadistats.clarity.model.s2.field.FieldType;
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

    public static final HuffmanTree HUFFMAN_TREE = new HuffmanTree();

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        if (ctx.getTick() == 50000) {
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

                    ps[0] = new PrintStream(new FileOutputStream("baselines/" + dtClass.getDtName() + ".txt"), true, "UTF-8");

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
                        List<FieldPath> fieldPaths = new ArrayList<>();
                        FieldPath fp = new FieldPath();
                        while (true) {
                            FieldOpType op = HUFFMAN_TREE.decodeOp(bs);
                            op.execute(fp, bs);
                            if (op == FieldOpType.FieldPathEncodeFinish) {
                                break;
                            }
                            fieldPaths.add(fp);
                            fp = new FieldPath(fp);
                        }

                        for (int r = 0; r < fieldPaths.size(); r++) {
                            fp = fieldPaths.get(r);
                            Field f = dtClass.getFieldForFieldPath(fp);
                            FieldType ft = dtClass.getTypeForFieldPath(fp);
                            t.setData(r, 0, fp);
                            t.setData(r, 1, dtClass.getNameForFieldPath(fp));
                            t.setData(r, 2, f.getLowValue());
                            t.setData(r, 3, f.getHighValue());
                            t.setData(r, 4, f.getBitCount());
                            t.setData(r, 5, f.getEncodeFlags() != null ? Integer.toHexString(f.getEncodeFlags()) : "-");
                            t.setData(r, 7, String.format("%s%s", ft.toString(true), f.getEncoder() != null ? String.format(" {%s}", f.getEncoder()) : ""));

                            int offsBefore = bs.pos();
                            Unpacker unpacker = dtClass.getUnpackerForFieldPath(fp);
                            if (unpacker == null) {
                                System.out.format("no unpacker for field %s with type %s!", f.getName(), f.getType());
                                System.exit(1);
                            }
                            Object data = unpacker.unpack(bs);
                            t.setData(r, 6, unpacker.getClass().getSimpleName().toString());
                            t.setData(r, 8, data);
                            t.setData(r, 9, bs.pos() - offsBefore);
                            t.setData(r, 10, bs.toString(offsBefore, bs.pos()));
                        }
                    } catch (Exception e) {
                        exx = e;
                    } finally {
                        for (PrintStream s : ps) {
                            if (s == null) {
                                continue;
                            }
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
