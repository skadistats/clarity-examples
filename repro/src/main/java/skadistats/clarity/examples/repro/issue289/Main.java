package skadistats.clarity.examples.repro.issue289;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Reproducer for https://github.com/skadistats/clarity/issues/289
 *
 * Hypothesis: ControllableRunner / AbstractFileRunner never closes its Source.
 * MappedFileSource therefore leaks one FileChannel + one MappedByteBuffer per
 * replay until the JVM Cleaner happens to run. Eventually one of:
 *   - RLIMIT_NOFILE (file descriptors)
 *   - vm.max_map_count
 *   - virtual address space
 * is exhausted, and the next mmap either fails or yields a mapping that reads
 * back as zeros — which surfaces as the "invalid tag (zero)" / "invalid wire
 * type" / Snappy FAILED_TO_UNCOMPRESS errors reported in #289.
 *
 * Run with first arg = path to a .dem file, optional second arg "close" to
 * close the source after each iteration (control: should NOT leak).
 *
 * Tip: launch via `bash -c 'ulimit -n 256 && ./gradlew issue289Run --args ...'`
 * to make the FD leak surface within seconds.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public void run(String[] args) throws Exception {
        String path = args[0];
        boolean closeSource = args.length > 1 && "close".equals(args[1]);
        log.info("reproducer: file={} closeSource={}", path, closeSource);

        Path selfFd = Paths.get("/proc/self/fd");
        Path selfMaps = Paths.get("/proc/self/maps");

        for (int i = 1; i <= 5000; i++) {
            MappedFileSource source = new MappedFileSource(path);
            ControllableRunner runner = new ControllableRunner(source).runWith(this);
            // touch a few ticks so the runner actually reads from the file
            for (int t = 0; t < 5 && !runner.isAtEnd(); t++) {
                runner.tick();
            }
            runner.halt();
            if (closeSource) {
                source.close();
            }

            if (i % 25 == 0) {
                long fds = -1, maps = -1;
                try (Stream<Path> s = Files.list(selfFd)) { fds = s.count(); } catch (Exception ignored) {}
                try (Stream<String> s = Files.lines(selfMaps)) { maps = s.count(); } catch (Exception ignored) {}
                log.info("iter={} fds={} maps={}", i, fds, maps);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
