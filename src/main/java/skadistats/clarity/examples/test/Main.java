package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldPathDecoder;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;

@UsesDTClasses
@UsesStringTable("instancebaseline")
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) {
        if (ctx.getTick() == 1) {
            StringTables stringTables = ctx.getProcessor(StringTables.class);
            DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
            StringTable baseline = stringTables.forName("instancebaseline");

            for (int idx = 10; idx < baseline.getEntryCount(); idx++) {
                int clsId = Integer.valueOf(baseline.getNameByIndex(idx));
                System.out.println(dtClasses.forClassId(clsId).getDtName());
                BitStream bs = new BitStream(baseline.getValueByIndex(idx));
                FieldPathDecoder.decode(bs);
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
