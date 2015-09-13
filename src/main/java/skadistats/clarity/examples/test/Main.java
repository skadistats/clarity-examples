package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnTickStart
    public void onMessage(Context ctx, boolean synthetic) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
    }

    public void run(String[] args) throws Exception {
        File folder = new File("D:\\replays");
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".dem")) {
                System.out.println(file.getPath());
                new SimpleRunner(new MappedFileSource(file)).runWith(this);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
