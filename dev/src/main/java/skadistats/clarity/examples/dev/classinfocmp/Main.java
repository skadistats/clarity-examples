package skadistats.clarity.examples.dev.classinfocmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.util.ArrayList;
import java.util.List;

@Example(name = "classinfocmp", description = "Compare CDemoClassInfo vs CSVCMsg_ClassInfo class lists", category = Category.DEV)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    record ClassEntry(int classId, String name, String tableName) {}

    private List<ClassEntry> demoClasses;
    private List<ClassEntry> svcClasses;

    @OnMessage(Demo.CDemoClassInfo.class)
    public void onDemoClassInfo(Demo.CDemoClassInfo message) {
        demoClasses = message.getClassesList().stream()
            .map(c -> new ClassEntry(c.getClassId(), c.getNetworkName(), c.getTableName()))
            .toList();
        log.info("CDemoClassInfo: {} classes", demoClasses.size());
    }

    @OnMessage(CommonNetMessages.CSVCMsg_ClassInfo.class)
    public void onSvcClassInfo(CommonNetMessages.CSVCMsg_ClassInfo message) {
        svcClasses = message.getClassesList().stream()
            .map(c -> new ClassEntry(c.getClassId(), c.getClassName(), c.getDataTableName()))
            .toList();
        log.info("CSVCMsg_ClassInfo: {} classes", svcClasses.size());
    }

    public void run(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource s = new MappedFileSource(replay)) {
            new SimpleRunner(s).runWith(this);
        }

        if (demoClasses == null && svcClasses == null) {
            log.info("neither message arrived");
            return;
        }
        if (demoClasses == null) { log.info("only CSVCMsg_ClassInfo arrived"); return; }
        if (svcClasses == null)  { log.info("only CDemoClassInfo arrived"); return; }

        log.info("both messages arrived — comparing...");

        int maxSize = Math.max(demoClasses.size(), svcClasses.size());
        List<String> diffs = new ArrayList<>();

        for (int i = 0; i < maxSize; i++) {
            if (i >= demoClasses.size()) {
                diffs.add("svc[%d] extra: %s".formatted(i, svcClasses.get(i)));
            } else if (i >= svcClasses.size()) {
                diffs.add("demo[%d] extra: %s".formatted(i, demoClasses.get(i)));
            } else {
                var d = demoClasses.get(i);
                var s = svcClasses.get(i);
                if (d.classId() != s.classId() || !d.name().equals(s.name()) || !d.tableName().equals(s.tableName())) {
                    diffs.add("mismatch at [%d]: demo=%s  svc=%s".formatted(i, d, s));
                }
            }
        }

        if (diffs.isEmpty()) {
            log.info("EQUAL: all {} classes match", demoClasses.size());
        } else {
            log.info("DIFFERENT: {} mismatches", diffs.size());
            diffs.forEach(log::info);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
