package skadistats.clarity.examples.livesource;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.LiveSource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        String srcFile = args[0];
        String dstFile = args[1];
        createWriterThread(srcFile, dstFile);

        LiveSource source = new LiveSource(dstFile, 5, TimeUnit.SECONDS);
        new SimpleRunner(source).runWith(new Object() {
            @OnMessage
            public void onMessage(GeneratedMessage msg) {
                System.out.println(msg.getClass().getSimpleName());
            }
        });
    }

    private void createWriterThread(final String srcFile, final String dstFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream src = new FileInputStream(srcFile);
                    FileOutputStream dst = new FileOutputStream(dstFile);
                    byte[] buf = new byte[8192];
                    int n = buf.length;
                    while (n == buf.length) {
                        n = src.read(buf);
                        dst.write(buf, 0, n);
                        dst.flush();
                        Thread.sleep(25);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }


    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
