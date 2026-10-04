package skadistats.clarity.examples.dev.dumpbaselines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.examples.shared.ReplayChooser;
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

/**
 * Decodes every entry of the {@code instancebaseline} string table and writes a field-level trace per
 * entity class.
 * <p>After the replay has been parsed, each baseline entry (name {@code <classId>} or
 * {@code <classId>:<suffix>}) is read with a {@link FieldReader} whose {@code FieldReader.Debug.STREAM} is
 * redirected to {@code baselines/<buildNumber|latest>/<dtName>[_<suffix>].txt} (relative to the working
 * directory, which Gradle sets to the project root). Decode failures and baselines that leave an unexpected
 * number of unread bits ({@code OFF}) are noted in the file and logged.
 * <p>Arguments: {@code [replay]}.
 * <p>Run: {@code ./gradlew :dev:dumpbaselinesRun --args "path/to/replay.dem"}
 */
@UsesDTClasses
@UsesStringTable("instancebaseline")
@Example(name = "dumpbaselines", description = "Export entity baselines to text files", category = Category.DEV)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class);

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();

        String demoName = ReplayChooser.choose(args);
        if (demoName == null) return;

        try (MappedFileSource source = new MappedFileSource(demoName)) {
            SimpleRunner r = new SimpleRunner(source).runWith(this);

            Context ctx = r.getContext();

            File dir = new File(String.format("baselines%s%s", File.separator, ctx.getGameVersion() == -1 ? "latest" : ctx.getBuildNumber()));
            if (!dir.exists()) {
                dir.mkdirs();
            }

            StringTables stringTables = ctx.getProcessor(StringTables.class);
            DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
            FieldReader fieldReader = ctx.newFieldReader();
            StringTable baselines = stringTables.forName("instancebaseline");

            for (int i = 0; i < baselines.getEntryCount(); i++) {
                DTClass dtClass;
                String fileName;
                String nameByIndex = baselines.getNameByIndex(i);
                if (nameByIndex.contains(":")) {
                    String[] split = nameByIndex.split(":");
                    dtClass = dtClasses.forClassId(Integer.valueOf(split[0]));
                    fileName = String.format("%s%s%s_%s.txt", dir.getPath(), File.separator, dtClass.getDtName(), split[1]);
                } else {
                    dtClass = dtClasses.forClassId(Integer.valueOf(nameByIndex));
                    fileName = String.format("%s%s%s.txt", dir.getPath(), File.separator, dtClass.getDtName());
                }

                log.info("writing {}", fileName);
                FieldReader.Debug.STREAM = new PrintStream(new FileOutputStream(fileName), true, "UTF-8");
                BitStream bs = BitStream.createBitStream(baselines.getValueByIndex(i));
                try {
                    fieldReader.readFields(bs, dtClass, ctx.newEntityState(dtClass), true);
                    if (bs.remaining() < 0 || bs.remaining() > 7) {
                        FieldReader.Debug.STREAM.println("-- OFF: " + bs.remaining() + " remaining");
                        log.info("-- OFF: {} remaining", bs.remaining());
                    }
                } catch (Exception e) {
                    log.info("-- FAIL: {}", e.getMessage());
                    e.printStackTrace(FieldReader.Debug.STREAM);
                }
            }
        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
