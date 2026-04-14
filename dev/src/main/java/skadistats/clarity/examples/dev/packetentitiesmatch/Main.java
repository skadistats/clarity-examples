package skadistats.clarity.examples.dev.packetentitiesmatch;

import com.google.protobuf.ByteString;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.entities.OnEntityUpdatesCompleted;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages.CSVCMsg_PacketEntities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

@UsesEntities
@UsesDTClasses
@Example(name = "packetentitiesmatch", description = "Test hypotheses on PacketEntities encoding", category = Category.DEV)
public class Main {

    private long[] lastVarints;
    private int lastTick;
    private boolean lastWasFull;

    private int testedAfter;
    private int matchHandlesCount;
    private int matchIndicesCount;
    private int matchSerialsCount;
    private int matchClassIdsCount;

    // For per-class statistics: maps varint count to a stable per-tick "type" guess
    private long totalVarints;
    private long varintsThatLookLikeHandles;   // (val >> indexBits) is a small serial, (val & mask) is an active index
    private long varintsThatAreActiveIndices;
    private long varintsThatAreClassIds;

    private int dumps = 3;
    private boolean classDumpDone = false;
    private final java.util.Map<Long, Long> varintValueHist = new java.util.HashMap<>();
    // For hypothesis: each varint = a class id; check if all varints in tick are valid class ids
    private int classIdHypTested;
    private int classIdHypAllValid;
    private long classIdHypTotal;
    private long classIdHypValid;

    // Cumulative-delta hypothesis stats
    private int cdTested;
    private int cdAllActive;
    private int cdAllExistent;
    private int cdAllInRange;
    private long cdTotalIndices;
    private long cdActiveHits;

    @OnMessage(CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, CSVCMsg_PacketEntities m) {
        if (!m.hasSerializedEntities() || m.getSerializedEntities().size() < 2) return;
        ByteString data = m.getSerializedEntities();
        long[] vs = decodeVarints(data);
        lastVarints = vs;
        lastTick = ctx.getTick();
        lastWasFull = !m.getIsDelta();
    }

    @OnEntityUpdatesCompleted
    public void onUpdatesCompleted(Context ctx) {
        if (lastVarints == null) return;
        Entities entities = ctx.getProcessor(Entities.class);

        // Snapshot active entities
        Set<Integer> activeIndices = new HashSet<>();
        Set<Integer> activeHandles = new HashSet<>();
        Set<Integer> activeSerials = new HashSet<>();
        Set<Integer> activeClassIds = new HashSet<>();
        List<Entity> active = new ArrayList<>();
        for (int i = 0; i < 16384; i++) {
            Entity e = entities.getByIndex(i);
            if (e != null && e.isActive()) {
                active.add(e);
                activeIndices.add(e.getIndex());
                activeHandles.add(e.getHandle());
                activeSerials.add(e.getSerial());
                activeClassIds.add(e.getDtClass().getClassId());
            }
        }

        int handleHits = 0, indexHits = 0, serialHits = 0, classHits = 0;
        for (long lv : lastVarints) {
            int v = (int) lv;
            if (activeHandles.contains(v)) handleHits++;
            if (activeIndices.contains(v)) indexHits++;
            if (activeSerials.contains(v)) serialHits++;
            if (activeClassIds.contains(v)) classHits++;
        }

        testedAfter++;
        matchHandlesCount += handleHits;
        matchIndicesCount += indexHits;
        matchSerialsCount += serialHits;
        matchClassIdsCount += classHits;
        totalVarints += lastVarints.length;

        if (dumps > 0 && lastVarints.length > 5) {
            dumps--;
            System.out.format("%n=== tick=%d full=%s active=%d varints=%d ===%n",
                lastTick, lastWasFull, active.size(), lastVarints.length);
            System.out.format("hits: handle=%d index=%d serial=%d classId=%d%n",
                handleHits, indexHits, serialHits, classHits);
            System.out.print("first 20 varints:");
            for (int i = 0; i < Math.min(20, lastVarints.length); i++) System.out.format(" %d", lastVarints[i]);
            System.out.println();
            System.out.print("first 10 active entities (idx/serial/handle/classId/dtName):");
            for (int i = 0; i < Math.min(10, active.size()); i++) {
                Entity e = active.get(i);
                System.out.format("%n  idx=%d serial=%d handle=%d cls=%d %s",
                    e.getIndex(), e.getSerial(), e.getHandle(), e.getDtClass().getClassId(), e.getDtClass().getDtName());
            }
            System.out.println();
        }

        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);

        // Class-id hypothesis: skip first varint (header), then each remaining varint should be a valid classId
        if (lastVarints.length >= 2) {
            classIdHypTested++;
            int valid = 0;
            int n = lastVarints.length - 1;
            boolean allValid = true;
            for (int i = 1; i < lastVarints.length; i++) {
                long v = lastVarints[i];
                if (v >= 0 && v < 65536 && dtClasses.forClassId((int)v) != null) valid++;
                else allValid = false;
                varintValueHist.merge(v, 1L, Long::sum);
            }
            classIdHypTotal += n;
            classIdHypValid += valid;
            if (allValid) classIdHypAllValid++;
        }

        // Dump dtClass names for the most-popular varint values, once
        if (!classDumpDone && classIdHypTested > 1000) {
            classDumpDone = true;
            System.out.format("%n=== Top varint values vs DTClasses ===%n");
            varintValueHist.entrySet().stream()
                .sorted(java.util.Map.Entry.<Long,Long>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> {
                    long v = e.getKey();
                    String name;
                    try {
                        var dc = (v >= 0 && v < 65536) ? dtClasses.forClassId((int)v) : null;
                        name = dc != null ? dc.getDtName() : "<no class>";
                    } catch (Exception ex) { name = "<err>"; }
                    System.out.format("  %5d : count=%d  %s%n", v, e.getValue(), name);
                });
        }

        // Cumulative-delta-as-entity-index hypothesis
        // Skip first varint as header, then treat rest as deltas: idx = -1 + (delta+1) cumulatively
        if (lastVarints.length >= 2) {
            cdTested++;
            int idx = -1;
            boolean allActive = true, allExistent = true, allInRange = true;
            int hits = 0;
            int n = lastVarints.length - 1;
            for (int i = 1; i < lastVarints.length; i++) {
                long d = lastVarints[i];
                if (d > 16384) { allInRange = false; break; }
                idx += (int)d + 1;
                if (idx >= 16384) { allInRange = false; break; }
                Entity e = entities.getByIndex(idx);
                if (e == null) { allExistent = false; allActive = false; }
                else {
                    if (!e.isExistent()) allExistent = false;
                    if (!e.isActive()) allActive = false;
                    else hits++;
                }
            }
            cdTotalIndices += n;
            cdActiveHits += hits;
            if (allInRange) {
                if (allExistent) cdAllExistent++;
                if (allActive) cdAllActive++;
                cdAllInRange++;
            }
        }

        lastVarints = null;
    }

    private static long[] decodeVarints(ByteString data) {
        List<Long> out = new ArrayList<>();
        int p = 0, n = data.size();
        while (p < n) {
            int shift = 0;
            long val = 0;
            while (p < n) {
                int b = data.byteAt(p++) & 0xff;
                val |= ((long)(b & 0x7f)) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            out.add(val);
        }
        long[] arr = new long[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }

    public static void main(String[] args) throws Exception {
        Main p = new Main();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (Source source = new MappedFileSource(replay)) {
            SimpleRunner runner = new SimpleRunner(source);
            runner.runWith(p);
        }
        System.out.format("%n=== Match Summary ===%n");
        System.out.format("ticks tested: %d%n", p.testedAfter);
        System.out.format("total varints: %d%n", p.totalVarints);
        System.out.format("varints matching active handle:   %d (%.1f%%)%n", p.matchHandlesCount, 100.0*p.matchHandlesCount/p.totalVarints);
        System.out.format("varints matching active index:    %d (%.1f%%)%n", p.matchIndicesCount, 100.0*p.matchIndicesCount/p.totalVarints);
        System.out.format("varints matching active serial:   %d (%.1f%%)%n", p.matchSerialsCount, 100.0*p.matchSerialsCount/p.totalVarints);
        System.out.format("varints matching active classId:  %d (%.1f%%)%n", p.matchClassIdsCount, 100.0*p.matchClassIdsCount/p.totalVarints);

        System.out.format("%n--- cumulative-delta-as-index hypothesis ---%n");
        System.out.format("ticks tested:    %d%n", p.cdTested);
        System.out.format("all in range:    %d (%.1f%%)%n", p.cdAllInRange, 100.0*p.cdAllInRange/p.cdTested);
        System.out.format("all existent:    %d (%.1f%%)%n", p.cdAllExistent, 100.0*p.cdAllExistent/p.cdTested);
        System.out.format("all active:      %d (%.1f%%)%n", p.cdAllActive, 100.0*p.cdAllActive/p.cdTested);
        System.out.format("active hit rate: %d/%d (%.1f%%)%n", p.cdActiveHits, p.cdTotalIndices, 100.0*p.cdActiveHits/p.cdTotalIndices);

        System.out.format("%n--- class-id hypothesis ---%n");
        System.out.format("ticks tested:    %d%n", p.classIdHypTested);
        System.out.format("all valid:       %d (%.1f%%)%n", p.classIdHypAllValid, 100.0*p.classIdHypAllValid/p.classIdHypTested);
        System.out.format("valid hit rate:  %d/%d (%.1f%%)%n", p.classIdHypValid, p.classIdHypTotal, 100.0*p.classIdHypValid/p.classIdHypTotal);
    }
}
