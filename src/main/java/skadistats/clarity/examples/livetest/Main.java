package skadistats.clarity.examples.livetest;

import com.google.protobuf.ByteString;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.model.*;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.Runner;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.common.proto.NetworkBaseTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@UsesDTClasses
public class Main {

    private final Map<Integer, BaselineEntry> baselineEntries = new HashMap<>();
    private  Entity[] entities;
    private FieldReader fieldReader;
    private EngineType engineType;

    private final FieldPath[] fieldPaths = new FieldPath[FieldReader.MAX_PROPERTIES];

    private class BaselineEntry {
        private ByteString rawBaseline;
        private Object[] baseline;
        public BaselineEntry(ByteString rawBaseline) {
            this.rawBaseline = rawBaseline;
            this.baseline = null;
        }
    }

    private void initEngineDependentFields(Context ctx) {
        if (fieldReader == null) {
            engineType = ctx.getEngineType();
            fieldReader = ctx.getEngineType().getNewFieldReader();
            entities = new Entity[1 << engineType.getIndexBits()];
        }
    }

    @OnStringTableEntry("instancebaseline")
    public void onBaselineEntry(Context ctx, StringTable table, int index, String key, ByteString value) {
        baselineEntries.put(Integer.valueOf(key), new BaselineEntry(value));
    }

    private int serverTick = -1;

    @OnMessage(NetworkBaseTypes.CNETMsg_Tick.class)
    public void onNetTick(Context ctx, NetworkBaseTypes.CNETMsg_Tick message) {
        serverTick = message.getTick();
        System.out.format("(%s) NET TICK %d\n", ctx.getTick(), serverTick);

    }

    private final Set<Integer> ticksWithClientFrames = new HashSet<>();

    @OnMessage(NetMessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, NetMessages.CSVCMsg_PacketEntities message) {
        BitStream stream = new BitStream(message.getEntityData());
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        int updateCount = message.getUpdatedEntries();
        int entityIndex = -1;

        int cmd;
        DTClass cls;
        int serial;
        Object[] state;
        Entity entity;


        System.out.format("(%s) TICK %s, delta from %s, baseline %d, update_baseline %s\n", ctx.getTick(), serverTick, message.getDeltaFrom(), message.getBaseline(), message.getUpdateBaseline());
        if (message.getIsDelta()) {
            if (!ticksWithClientFrames.contains(message.getDeltaFrom())) {
                System.out.println("----------> NO CLIENT FRAME FOR " + message.getDeltaFrom());
            }
        }
        ticksWithClientFrames.add(serverTick);
        while (updateCount-- != 0) {
            entityIndex += stream.readUBitVar() + 1;
            cmd = stream.readUBitInt(2);
            if ((cmd & 1) == 0) {
                if ((cmd & 2) != 0) {
                    if (entityIndex == 690)
                        System.out.format("create %d\n", entityIndex);
                    cls = dtClasses.forClassId(stream.readUBitInt(dtClasses.getClassBits()));
                    serial = stream.readUBitInt(engineType.getSerialBits());
                    if (engineType.getSerialExtraBits() != 0) {
                        // TODO: there is an extra 15 bits encoded here for S2, figure out what it is
                        stream.skip(engineType.getSerialExtraBits());
                    }
                    state = Util.clone(getBaseline(dtClasses, cls.getClassId()));
                    fieldReader.readFields(stream, cls, fieldPaths, state, false);
                    entity = new Entity(ctx.getEngineType(), entityIndex, serial, cls, true, state);
                    entities[entityIndex] = entity;
                } else {
                    if (entityIndex == 690)
                        System.out.format("update for %d\n", entityIndex);
                    entity = entities[entityIndex];
                    if (entity == null) {
                        System.out.format("----------> oops, entity to update not found for idx %d. %d updates remaining. Man the liveboats!\n", entityIndex, updateCount);
                        return;
                        //throw new RuntimeException("oops, entity to update not found (" + entityIndex + ")");
                    }
                    cls = entity.getDtClass();
                    state = entity.getState();
                    int nChanged = fieldReader.readFields(stream, cls, fieldPaths, state, false);
                    if (!entity.isActive()) {
                        entity.setActive(true);
                    }
                }
            } else {
                if (entityIndex == 690)
                    System.out.format("leave%s for %d\n", (cmd & 2) != 0 ? "+delete" : "", entityIndex);
                entity = entities[entityIndex];
                if (entity != null && entity.isActive()) {
                    entity.setActive(false);
                }
                if ((cmd & 2) != 0) {
                    entities[entityIndex] = null;
                }
            }
        }

//        if (message.getIsDelta()) {
//            while (stream.readBitFlag()) {
//                entityIndex = stream.readUBitInt(engineType.getIndexBits());
//                if (evDeleted != null) {
//                    evDeleted.raise(entities[entityIndex]);
//                }
//                entities[entityIndex] = null;
//            }
//        }
    }

    private Object[] getBaseline(DTClasses dtClasses, int clsId) {
        BaselineEntry be = baselineEntries.get(clsId);
        if (be == null) {

            throw new RuntimeException("oops, no baseline for this class? " + dtClasses.forClassId(clsId).getDtName());
        }
        if (be.baseline == null) {
            DTClass cls = dtClasses.forClassId(clsId);
            BitStream stream = new BitStream(be.rawBaseline);
            be.baseline = cls.getEmptyStateArray();
            fieldReader.readFields(stream, cls, fieldPaths, be.baseline, false);
        }
        return be.baseline;
    }

    public void run(String[] args) throws Exception {
        Runner r = new SimpleRunner(new MappedFileSource(args[0]));
        engineType = r.getEngineType();
        fieldReader = engineType.getNewFieldReader();
        entities = new Entity[1 << engineType.getIndexBits()];
        r.runWith(this);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
