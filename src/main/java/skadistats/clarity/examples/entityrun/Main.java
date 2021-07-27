package skadistats.clarity.examples.entityrun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        MappedFileSource s = new MappedFileSource(args[0]);
        new SimpleRunner(s).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        s.close();
    }

    public static void main(String[] args) throws Exception {
        try {
            //ClarityPlatform.setBitStreamConstructor(data -> new BitStream32(new UnsafeBuffer.B32(data)));
            //System.out.println("press key to start"); System.in.read();
            new Main().run(args);
        } catch (Exception e) {
            Thread.sleep(200);
            throw e;
        }
    }

}
