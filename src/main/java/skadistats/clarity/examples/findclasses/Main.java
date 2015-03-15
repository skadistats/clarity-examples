package skadistats.clarity.examples.findclasses;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        Logger log = LoggerFactory.getLogger("findclasses");
        Set<Class<? extends GeneratedMessage>> foundClasses = new HashSet<>();
        for (File demoFile : new File("replays").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".dem");
            }
        })) {
            log.info("processing {}", demoFile.getName());
            PacketTypeStream pts = new PacketTypeStream(new FileInputStream(demoFile));
            pts.bootstrap();
            Class<? extends GeneratedMessage> foundClass;
            while ((foundClass = pts.read()) != null) {
                if (foundClasses.add(foundClass)) {
                    log.info("-- new class " + foundClass.getSimpleName());
                }
            }
        }
    }
}
