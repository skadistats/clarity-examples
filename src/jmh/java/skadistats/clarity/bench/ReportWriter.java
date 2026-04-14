package skadistats.clarity.bench;

import org.openjdk.jmh.results.RunResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public final class ReportWriter {

    private ReportWriter() {}

    public static void write(Collection<RunResult> results, Path out) throws IOException {
        var sw = new StringWriter();
        var w = new PrintWriter(sw);

        w.println("EntityStateParseBench");
        w.println("generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        w.println();

        w.println("=== Per-replay results ===");
        w.printf("  %-60s %9s %9s %9s %9s %18s%n",
            "replay", "median", "min", "max", "p95", "score ± err (ms)");
        for (var r : results) {
            var stats = r.getPrimaryResult().getStatistics();
            var replay = r.getParams().getParam("replay");
            w.printf("  %-60s %7.1f ms %6.1f ms %6.1f ms %6.1f ms %8.1f ± %-7.1f%n",
                shorten(replay, 60),
                stats.getPercentile(50),
                stats.getMin(),
                stats.getMax(),
                stats.getPercentile(95),
                r.getPrimaryResult().getScore(),
                r.getPrimaryResult().getScoreError());
        }
        w.println();

        w.println("=== Memory / GC pressure ===");
        w.printf("  %-60s %10s %10s %8s %8s%n",
            "replay", "alloc/op", "alloc rate", "GCs", "GC time");
        for (var r : results) {
            var replay = r.getParams().getParam("replay");
            var allocNorm = sec(r, "gc.alloc.rate.norm");
            var allocRate = sec(r, "gc.alloc.rate");
            var gcCount = sec(r, "gc.count");
            var gcTime = sec(r, "gc.time");
            w.printf("  %-60s %10s %8.0f MB/s %8.0f %7.0f ms%n",
                shorten(replay, 60),
                fmtBytes(allocNorm),
                allocRate == null ? 0.0 : allocRate,
                gcCount == null ? 0.0 : gcCount,
                gcTime == null ? 0.0 : gcTime);
        }

        w.flush();
        Files.writeString(out, sw.toString());
        System.out.println();
        System.out.println(sw);
    }

    private static Double sec(RunResult r, String key) {
        var map = r.getSecondaryResults();
        var sr = map.get(key);
        if (sr == null) sr = map.get("·" + key);
        return sr == null ? null : sr.getScore();
    }

    private static String fmtBytes(Double b) {
        if (b == null) return "?";
        if (b >= 1024.0 * 1024 * 1024) return String.format("%.2f GB", b / (1024.0 * 1024 * 1024));
        if (b >= 1024.0 * 1024) return String.format("%.1f MB", b / (1024.0 * 1024));
        if (b >= 1024.0) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.0f B", b);
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : "…" + s.substring(s.length() - (max - 1));
    }
}
