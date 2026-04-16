package skadistats.clarity.examples.dev.s1sendtables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.io.s1.ReceiveProp;
import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.OnDTClassesComplete;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.source.MappedFileSource;

import java.io.FileOutputStream;
import java.io.PrintStream;

@UsesDTClasses
@Example(name = "s1sendtables", description = "Dump S1 ReceiveProps with type/flags/numBits for string-inline analysis", category = Category.DEV)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private boolean dumped = false;

    @OnDTClassesComplete
    public void onDTClassesComplete(Context ctx) throws Exception {
        if (dumped) return;
        dumped = true;
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);

        String fileName = String.format("s1sendprops_%s.txt", ctx.getBuildNumber());
        log.info("writing {}", fileName);
        try (PrintStream out = new PrintStream(new FileOutputStream(fileName), true, "UTF-8")) {
            out.println("# S1 send-prop dump (one line per ReceiveProp)");
            out.println("# fields: dtClass | propIdx | varName | type | numBits | numElements | flags | low | high | dtName");
            var it = dtClasses.iterator();
            while (it.hasNext()) {
                var dt = it.next();
                if (!(dt instanceof S1DTClass s1dt)) continue;
                var props = s1dt.getReceiveProps();
                if (props == null) continue;
                for (int i = 0; i < props.length; i++) {
                    ReceiveProp rp = props[i];
                    SendProp sp = rp.getSendProp();
                    PropType t = sp.getType();
                    out.printf("%s\t%d\t%s\t%s\t%d\t%d\t0x%x\t%.3f\t%.3f\t%s%n",
                        s1dt.getDtName(),
                        i,
                        rp.getVarName(),
                        t,
                        sp.getNumBits(),
                        sp.getNumElements(),
                        sp.getFlags(),
                        sp.getLowValue(),
                        sp.getHighValue(),
                        sp.getDtName() == null ? "-" : sp.getDtName()
                    );
                }
            }
        }
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        }
        log.info("total time taken: {}s", (System.currentTimeMillis() - tStart) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
