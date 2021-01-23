package skadistats.clarity.examples.dumpbaselines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

@UsesDTClasses
@UsesStringTable("instancebaseline")
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();

        String demoName = args[0];

        SimpleRunner r = new SimpleRunner(new MappedFileSource(demoName)).runWith(this);

        Context ctx = r.getContext();

        File dir = new File(String.format("baselines%s%s", File.separator, ctx.getBuildNumber() == -1 ? "latest" : ctx.getBuildNumber()));
        if (!dir.exists()) {
            dir.mkdirs();
        }

        FieldReader fieldReader = ctx.getEngineType().getNewFieldReader();

        StringTables stringTables = ctx.getProcessor(StringTables.class);
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        StringTable baselines = stringTables.forName("instancebaseline");

        for (int i = 0; i < baselines.getEntryCount(); i++) {
            DTClass dtClass = dtClasses.forClassId(Integer.valueOf(baselines.getNameByIndex(i)));
            String fileName = String.format("%s%s%s.txt", dir.getPath(), File.separator, dtClass.getDtName());
            log.info("writing {}", fileName);
            fieldReader.DEBUG_STREAM = new PrintStream(new FileOutputStream(fileName), true, "UTF-8");
            BitStream bs = BitStream.createBitStream(baselines.getValueByIndex(i));
            try {
                fieldReader.readFields(bs, dtClass, true);
                if (bs.remaining() < 0 || bs.remaining() > 7) {
                    log.info("-- OFF: {} remaining", bs.remaining());
                }
            } catch (Exception e) {
                log.info("-- FAIL: {}", e.getMessage());
                e.printStackTrace(fieldReader.DEBUG_STREAM);
            } finally {
            }
        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
