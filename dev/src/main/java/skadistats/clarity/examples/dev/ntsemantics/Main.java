package skadistats.clarity.examples.dev.ntsemantics;

import com.google.protobuf.ByteString;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityEntered;
import skadistats.clarity.processor.entities.OnEntityLeft;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.OnEntityUpdatesCompleted;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages.CSVCMsg_PacketEntities;

import java.util.HashSet;
import java.util.Set;

@UsesEntities
public class Main {

    private int[] ntIndices;
    private int[] ntFlags;
    private int lastTick = -1;
    private boolean lastIsDelta;

    private final Set<Integer> touched = new HashSet<>();
    private final Set<Integer> created = new HashSet<>();
    private final Set<Integer> updated = new HashSet<>();
    private final Set<Integer> entered = new HashSet<>();
    private final Set<Integer> left = new HashSet<>();
    private final Set<Integer> deleted = new HashSet<>();

    private int dumps = 8;
    private int totalTicks;
    private long ntInTouched;
    private long ntInActiveButNotTouched;
    private long ntNotActive;
    private long ntTotal;

    @OnMessage(CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, CSVCMsg_PacketEntities m) {
        lastTick = ctx.getTick();
        lastIsDelta = m.getIsDelta();
        ntIndices = null; ntFlags = null;
        if (!m.hasNonTransmittedEntities()) return;
        var nt = m.getNonTransmittedEntities();
        int hc = nt.getHeaderCount();
        if (hc == 0) return;
        ByteString data = nt.getData();
        BitStream bs = BitStream.createBitStream(data);
        ntIndices = new int[hc];
        ntFlags = new int[hc];
        int idx = -1;
        for (int i = 0; i < hc; i++) {
            int delta = bs.readUBitVar();
            idx += delta + 1;
            int flag = bs.readUBitInt(1);
            ntIndices[i] = idx;
            ntFlags[i] = flag;
        }
    }

    @OnEntityCreated public void onCreated(Entity e) { touched.add(e.getIndex()); created.add(e.getIndex()); }
    @OnEntityUpdated public void onUpdated(Entity e, skadistats.clarity.model.FieldPath[] fp, int n) { touched.add(e.getIndex()); updated.add(e.getIndex()); }
    @OnEntityEntered public void onEntered(Entity e) { touched.add(e.getIndex()); entered.add(e.getIndex()); }
    @OnEntityLeft public void onLeft(Entity e) { touched.add(e.getIndex()); left.add(e.getIndex()); }
    @OnEntityDeleted public void onDeleted(Entity e) { touched.add(e.getIndex()); deleted.add(e.getIndex()); }

    @OnEntityUpdatesCompleted
    public void onUpdatesCompleted(Context ctx) {
        if (ntIndices == null) {
            clear();
            return;
        }
        Entities entities = ctx.getProcessor(Entities.class);
        totalTicks++;

        int activeNotTouched = 0;
        int activeTotal = 0;
        for (int i = 0; i < 16384; i++) {
            Entity e = entities.getByIndex(i);
            if (e != null && e.isActive()) {
                activeTotal++;
                if (!touched.contains(i)) activeNotTouched++;
            }
        }

        int inTouched = 0, inActiveNotTouched = 0, notActive = 0;
        int flag0InActive = 0, flag1InActive = 0;
        for (int k = 0; k < ntIndices.length; k++) {
            int i = ntIndices[k];
            int flag = ntFlags[k];
            Entity e = entities.getByIndex(i);
            boolean isActive = e != null && e.isActive();
            if (touched.contains(i)) inTouched++;
            else if (isActive) inActiveNotTouched++;
            else notActive++;
            if (isActive) {
                if (flag == 0) flag0InActive++; else flag1InActive++;
            }
        }
        ntTotal += ntIndices.length;
        ntInTouched += inTouched;
        ntInActiveButNotTouched += inActiveNotTouched;
        ntNotActive += notActive;

        if (dumps > 0 && ntIndices.length > 1) {
            dumps--;
            System.out.format("%n=== tick=%d delta=%s active=%d ntCount=%d ===%n",
                lastTick, lastIsDelta, activeTotal, ntIndices.length);
            System.out.format("touched: created=%d updated=%d entered=%d left=%d deleted=%d  activeNotTouched=%d%n",
                created.size(), updated.size(), entered.size(), left.size(), deleted.size(), activeNotTouched);
            System.out.format("nt classification: inTouched=%d inActiveNotTouched=%d notActive=%d%n",
                inTouched, inActiveNotTouched, notActive);
            System.out.format("nt flag-vs-active: flag0AndActive=%d flag1AndActive=%d%n", flag0InActive, flag1InActive);
            System.out.print("first 15 nt: ");
            for (int k = 0; k < Math.min(15, ntIndices.length); k++) {
                int i = ntIndices[k];
                Entity e = entities.getByIndex(i);
                String st;
                if (e == null) st = "<null>";
                else st = (e.isActive() ? "A" : "I") + "/" + e.getDtClass().getDtName();
                System.out.format("[%d f%d %s%s] ", i, ntFlags[k], touched.contains(i) ? "T:" : "", st);
            }
            System.out.println();
        }

        clear();
    }

    private void clear() {
        touched.clear(); created.clear(); updated.clear(); entered.clear(); left.clear(); deleted.clear();
        ntIndices = null; ntFlags = null;
    }

    public static void main(String[] args) throws Exception {
        Main p = new Main();
        try (Source source = new MappedFileSource(args[0])) {
            SimpleRunner runner = new SimpleRunner(source);
            runner.runWith(p);
        }
        System.out.format("%n=== NT semantics summary ===%n");
        System.out.format("ticks with NT entries: %d%n", p.totalTicks);
        System.out.format("total NT entries: %d%n", p.ntTotal);
        System.out.format("  in touched (created/updated/entered/left/deleted): %d (%.1f%%)%n", p.ntInTouched, 100.0*p.ntInTouched/p.ntTotal);
        System.out.format("  active but not touched: %d (%.1f%%)%n", p.ntInActiveButNotTouched, 100.0*p.ntInActiveButNotTouched/p.ntTotal);
        System.out.format("  not active (or null): %d (%.1f%%)%n", p.ntNotActive, 100.0*p.ntNotActive/p.ntTotal);
    }
}
