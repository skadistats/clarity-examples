package skadistats.clarity.examples.dev.packetentitiesprobe;

import com.google.protobuf.ByteString;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages.CSVCMsg_PacketEntities;

import java.io.FileOutputStream;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

@Example(name = "packetentitiesprobe", description = "Probe PacketEntities structure (internal)", category = Category.DEV)
public class Main {

    private int totalPackets;
    private boolean dumpedInitial = false;

    // UBitVar-as-deltas hypothesis stats
    private int ntPacketsTested;
    private int ntPacketsExactFit;          // exactly header_count values consumed all bytes (or all bytes within last byte)
    private int ntPacketsMonotonic;         // produced monotonically increasing indices
    private int ntPacketsInRange;           // all indices < 16384
    private int ntPacketsAllGood;           // exact + monotonic + in range

    // Variant: UBitVar + N trailing bits
    private final int[] variantAllGood = new int[5]; // for trailing-bit counts 0..4
    private final int[] variantExactFit = new int[5];

    private int serPacketsTested;
    private int serPacketsMonotonic;
    private int serPacketsInRange;

    private int serVarintExactFit;
    private int serVarintTested;
    private long serVarintTotalCount;
    private final TreeMapInt varintFirstByteHist = new TreeMapInt();
    private final TreeMapInt varintCountHist = new TreeMapInt();

    static class TreeMapInt extends java.util.TreeMap<Integer, Integer> {
        void bump(int k) { merge(k, 1, Integer::sum); }
    }

    @OnMessage(CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, CSVCMsg_PacketEntities m) {
        totalPackets++;

        // Dump first non-trivial initial frame
        if (!dumpedInitial && !m.getIsDelta() && m.hasSerializedEntities() && m.getSerializedEntities().size() > 100) {
            try (FileOutputStream s = new FileOutputStream("/tmp/serialized_initial.bin")) {
                m.getSerializedEntities().writeTo(s);
            } catch (Exception e) { e.printStackTrace(); }
            try (FileOutputStream s = new FileOutputStream("/tmp/nt_initial.bin")) {
                m.getNonTransmittedEntities().getData().writeTo(s);
            } catch (Exception e) { e.printStackTrace(); }
            System.out.format("dumped initial frame: tick=%d serialized=%dB nt=%dB ntHeaderCount=%d%n",
                ctx.getTick(),
                m.getSerializedEntities().size(),
                m.getNonTransmittedEntities().getData().size(),
                m.getNonTransmittedEntities().getHeaderCount());
            dumpedInitial = true;
        }

        // Test NT hypothesis: data = header_count UBitVar deltas
        if (m.hasNonTransmittedEntities()) {
            CSVCMsg_PacketEntities.non_transmitted_entities_t nt = m.getNonTransmittedEntities();
            int hc = nt.getHeaderCount();
            ByteString data = nt.getData();
            if (hc > 0 && data.size() > 0) {
                ntPacketsTested++;
                tryDecodeUBitVarDeltas(data, hc, true);
                for (int tb = 0; tb <= 4; tb++) {
                    tryVariant(data, hc, tb);
                }
            }
        }

        // Test serialized_entities the same way (less confident)
        if (m.hasSerializedEntities() && m.getSerializedEntities().size() > 1) {
            serPacketsTested++;
            // We don't know the count for serialized — just test monotonicity / range until exhaustion
            tryDecodeUBitVarStream(m.getSerializedEntities());

            // Try as a pure varint sequence
            tryDecodeVarints(m.getSerializedEntities());
        }
    }

    private void tryDecodeUBitVarDeltas(ByteString data, int expectedCount, boolean isNt) {
        try {
            BitStream bs = BitStream.createBitStream(data);
            int totalBits = data.size() * 8;
            int idx = -1;
            boolean monotonic = true;
            boolean inRange = true;
            for (int i = 0; i < expectedCount; i++) {
                if (bs.pos() >= totalBits) { monotonic = false; inRange = false; break; }
                int delta = bs.readUBitVar();
                int newIdx = idx + delta + 1;
                if (newIdx <= idx) monotonic = false;
                if (newIdx >= 16384) inRange = false;
                idx = newIdx;
            }
            // Did we consume essentially all bits? (allow up to 7 leftover bits = byte padding)
            int leftover = totalBits - bs.pos();
            boolean exact = leftover >= 0 && leftover < 8;
            if (monotonic) ntPacketsMonotonic++;
            if (inRange) ntPacketsInRange++;
            if (exact) ntPacketsExactFit++;
            if (monotonic && inRange && exact) ntPacketsAllGood++;
        } catch (Exception e) {
            // decode failed
        }
    }

    private void tryVariant(ByteString data, int expectedCount, int trailingBits) {
        try {
            BitStream bs = BitStream.createBitStream(data);
            int totalBits = data.size() * 8;
            int idx = -1;
            boolean ok = true;
            for (int i = 0; i < expectedCount; i++) {
                if (bs.pos() + 6 + trailingBits > totalBits) { ok = false; break; }
                int delta = bs.readUBitVar();
                int newIdx = idx + delta + 1;
                if (newIdx <= idx || newIdx >= 16384) { ok = false; break; }
                idx = newIdx;
                if (trailingBits > 0) bs.readUBitInt(trailingBits);
            }
            int leftover = totalBits - bs.pos();
            boolean exact = ok && leftover >= 0 && leftover < 8;
            if (exact) variantExactFit[trailingBits]++;
            if (ok && exact) variantAllGood[trailingBits]++;
        } catch (Exception e) { /* ignore */ }
    }

    private void tryDecodeVarints(ByteString data) {
        serVarintTested++;
        varintFirstByteHist.bump(data.byteAt(0) & 0xff);
        int p = 0;
        int n = data.size();
        int count = 0;
        boolean ok = true;
        while (p < n) {
            int shift = 0;
            long val = 0;
            int start = p;
            boolean done = false;
            while (p < n && shift < 64) {
                int b = data.byteAt(p++) & 0xff;
                val |= ((long)(b & 0x7f)) << shift;
                if ((b & 0x80) == 0) { done = true; break; }
                shift += 7;
            }
            if (!done) { ok = false; break; }
            count++;
            if (count > 4096) { ok = false; break; }
        }
        if (ok && p == n) {
            serVarintExactFit++;
            serVarintTotalCount += count;
            varintCountHist.bump(count);
        }
    }

    private void tryDecodeUBitVarStream(ByteString data) {
        try {
            BitStream bs = BitStream.createBitStream(data);
            int totalBits = data.size() * 8;
            int idx = -1;
            boolean monotonic = true;
            boolean inRange = true;
            int count = 0;
            // Read until we'd run out of bits for another UBitVar (need at least 6)
            while (bs.pos() + 6 <= totalBits) {
                int delta;
                try { delta = bs.readUBitVar(); } catch (Exception e) { break; }
                int newIdx = idx + delta + 1;
                if (newIdx <= idx) { monotonic = false; break; }
                if (newIdx >= 16384) { inRange = false; break; }
                idx = newIdx;
                count++;
                if (count > 4096) break;
            }
            if (monotonic) serPacketsMonotonic++;
            if (inRange) serPacketsInRange++;
        } catch (Exception e) { /* ignore */ }
    }

    public static void main(String[] args) throws Exception {
        Main p = new Main();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (Source source = new MappedFileSource(replay)) {
            SimpleRunner runner = new SimpleRunner(source);
            runner.runWith(p);
        }

        System.out.format("%n=== Summary ===%n");
        System.out.format("total packet_entities: %d%n", p.totalPackets);
        System.out.format("%n--- non_transmitted: UBitVar-delta hypothesis ---%n");
        System.out.format("tested:     %d%n", p.ntPacketsTested);
        System.out.format("exactFit:   %d  (%.1f%%)%n", p.ntPacketsExactFit, pct(p.ntPacketsExactFit, p.ntPacketsTested));
        System.out.format("monotonic:  %d  (%.1f%%)%n", p.ntPacketsMonotonic, pct(p.ntPacketsMonotonic, p.ntPacketsTested));
        System.out.format("inRange:    %d  (%.1f%%)%n", p.ntPacketsInRange, pct(p.ntPacketsInRange, p.ntPacketsTested));
        System.out.format("ALL GOOD:   %d  (%.1f%%)%n", p.ntPacketsAllGood, pct(p.ntPacketsAllGood, p.ntPacketsTested));

        System.out.format("%n--- variant: UBitVar + N trailing bits ---%n");
        for (int tb = 0; tb <= 4; tb++) {
            System.out.format("trailingBits=%d  exactFit=%d (%.1f%%)  allGood=%d (%.1f%%)%n",
                tb,
                p.variantExactFit[tb], pct(p.variantExactFit[tb], p.ntPacketsTested),
                p.variantAllGood[tb], pct(p.variantAllGood[tb], p.ntPacketsTested));
        }

        System.out.format("%n--- serialized_entities: UBitVar-delta hypothesis ---%n");
        System.out.format("tested:     %d%n", p.serPacketsTested);
        System.out.format("monotonic:  %d  (%.1f%%)%n", p.serPacketsMonotonic, pct(p.serPacketsMonotonic, p.serPacketsTested));
        System.out.format("inRange:    %d  (%.1f%%)%n", p.serPacketsInRange, pct(p.serPacketsInRange, p.serPacketsTested));

        System.out.format("%n--- serialized_entities: pure-varint hypothesis ---%n");
        System.out.format("tested:     %d%n", p.serVarintTested);
        System.out.format("exactFit:   %d  (%.1f%%)%n", p.serVarintExactFit, pct(p.serVarintExactFit, p.serVarintTested));
        if (p.serVarintExactFit > 0) {
            System.out.format("avg varints/packet: %.2f%n", (double)p.serVarintTotalCount / p.serVarintExactFit);
        }
        System.out.format("first byte distribution (top 10):%n");
        p.varintFirstByteHist.entrySet().stream()
            .sorted(java.util.Map.Entry.<Integer,Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> System.out.format("  0x%02x : %d%n", e.getKey(), e.getValue()));
        System.out.format("varint count distribution (top 10):%n");
        p.varintCountHist.entrySet().stream()
            .sorted(java.util.Map.Entry.<Integer,Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> System.out.format("  %4d varints : %d packets%n", e.getKey(), e.getValue()));
    }

    private static double pct(int n, int d) { return d == 0 ? 0 : 100.0 * n / d; }
}
