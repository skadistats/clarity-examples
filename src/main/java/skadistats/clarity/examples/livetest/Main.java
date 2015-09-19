package skadistats.clarity.examples.livetest;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.StringTable;
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
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@UsesDTClasses
public class Main {

    private final Map<Integer, BaselineEntry> baselineEntries = new HashMap<>();
    private  Entity[] entities;
    private int[] deletions;
    private FieldReader fieldReader;
    private EngineType engineType;

    private int serverTick = -1;


    private static Set<Integer> indicesToLog = new HashSet<>();
    private static Set<Integer> dumpTicks = new HashSet<>();
    static {
        //indicesToLog.add(748);
        //dumpTicks.add(2482);
    }

    private class BaselineEntry {
        private ByteString rawBaseline;
        private Object[] baseline;
        public BaselineEntry(ByteString rawBaseline) {
            this.rawBaseline = rawBaseline;
            this.baseline = null;
        }
    }

    @OnStringTableEntry("instancebaseline")
    public void onBaselineEntry(Context ctx, StringTable table, int index, String key, ByteString value) {
        System.out.format("(%s) instancebaseline updated for %s, %s\n", ctx.getTick(), key, value == null ? "NULL" : String.valueOf(value.size()) + " bytes");
        baselineEntries.put(Integer.valueOf(key), new BaselineEntry(value));
    }

    @OnMessage
    public void onMessage(Context ctx, GeneratedMessage message) {
        if (message instanceof NetworkBaseTypes.CNETMsg_Tick) {
            NetworkBaseTypes.CNETMsg_Tick tickMsg = (NetworkBaseTypes.CNETMsg_Tick) message;
            serverTick = tickMsg.getTick();
            System.out.format("\n(%s) %s %s\n", ctx.getTick(), message.getClass().getSimpleName(), serverTick);
        } else if (message instanceof S2NetMessages.CSVCMsg_CreateStringTable) {
            S2NetMessages.CSVCMsg_CreateStringTable cstMsg = (S2NetMessages.CSVCMsg_CreateStringTable) message;
            System.out.format("\n(%s) %s %s flags: %s\n", ctx.getTick(), message.getClass().getSimpleName(), cstMsg.getName(), Integer.toHexString(cstMsg.getFlags()));
        } else {
            //System.out.format("\n(%s) %s\n", ctx.getTick(), message.getClass().getSimpleName());
        }
    }

    private final Set<Integer> ticksWithClientFrames = new HashSet<>();

    @OnMessage(NetMessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, NetMessages.CSVCMsg_PacketEntities message) {
        BitStream stream = BitStream.createBitStream(message.getEntityData());
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        int updateCount = message.getUpdatedEntries();
        int entityIndex = -1;

        int cmd;
        DTClass cls;
        int serial;
        Object[] state;
        Entity entity;

        System.out.format("(%s) %s at server tick %s, delta %s, baseline %d, update_baseline %s\n",
            ctx.getTick(),
            message.getClass().getSimpleName(),
            serverTick,
            message.getIsDelta() ? "from " + message.getDeltaFrom() : "no",
            message.getBaseline(),
            message.getUpdateBaseline()
        );

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
                    if (indicesToLog.contains(entityIndex))
                        System.out.format("create #%d\n", entityIndex);
                    int clsId = stream.readUBitInt(dtClasses.getClassBits());
                    cls = dtClasses.forClassId(clsId);
                    if (cls == null) {
                        throw new RuntimeException("CLASS " + clsId + " NOT KNOWN!");
                    }
                    serial = stream.readUBitInt(engineType.getSerialBits());
                    if (engineType == EngineType.SOURCE2) {
                        // TODO: there is an extra VarInt encoded here for S2, figure out what it is
                        stream.readVarUInt();
                    }
                    state = Util.clone(getBaseline(ctx, dtClasses, cls.getClassId()));
                    fieldReader.readFields(stream, cls, state, dumpTicks.contains(ctx.getTick()));
                    entity = new Entity(ctx.getEngineType(), entityIndex, serial, cls, true, state);
                    entities[entityIndex] = entity;
                } else {
                    if (indicesToLog.contains(entityIndex))
                        System.out.format("update for #%d\n", entityIndex);
                    if (entityIndex > entities.length || entities[entityIndex] == null) {
                        System.out.format("----------> oops, entity to update not found for idx %d. %d updates remaining. Man the liveboats!\n", entityIndex, updateCount);
                        System.exit(1);
                        return;
                        //throw new RuntimeException("oops, entity to update not found (" + entityIndex + ")");
                    }
                    entity = entities[entityIndex];
                    cls = entity.getDtClass();
                    state = entity.getState();
                    int nChanged = fieldReader.readFields(stream, cls, state, dumpTicks.contains(ctx.getTick()));
                    if (!entity.isActive()) {
                        entity.setActive(true);
                    }
                }
            } else {
                if (indicesToLog.contains(entityIndex))
                    System.out.format("leave%s for #%d\n", (cmd & 2) != 0 ? "+delete" : "", entityIndex);
                entity = entities[entityIndex];
                if (entity != null && entity.isActive()) {
                    entity.setActive(false);
                }
                if ((cmd & 2) != 0) {
                    entities[entityIndex] = null;
                }
            }
        }

        if (message.getIsDelta()) {
            int n = fieldReader.readDeletions(stream, engineType.getIndexBits(), deletions);
            for (int i = 0; i < n; i++) {
                entityIndex = deletions[i];
                entity = entities[entityIndex];
                if (entity != null) {
                    System.out.format("entity at index %s was ACTUALLY found when ordered to delete, tell the press!\n", entityIndex);
                } else {
                    //log.warn("entity at index {} was not found when ordered to delete.", entityIndex);
                }
                entities[entityIndex] = null;
            }
        }
    }

    private Object[] getBaseline(Context ctx, DTClasses dtClasses, int clsId) {
        BaselineEntry be = baselineEntries.get(clsId);
        if (be == null || be.rawBaseline == null) {
            System.out.println("NO BASELINE FOR " + clsId);
            return new Object[2048];
        }
        if (be.baseline == null) {
            DTClass cls = dtClasses.forClassId(clsId);
            BitStream stream = BitStream.createBitStream(be.rawBaseline);
            be.baseline = cls.getEmptyStateArray();
            System.out.print("trying to read baseline for " + clsId);
            fieldReader.readFields(stream, cls, be.baseline, dumpTicks.contains(ctx.getTick()));
            System.out.println("... done");
        }
        return be.baseline;
    }

    public void run(String[] args) throws Exception {
        Runner r = new SimpleRunner(new MappedFileSource(args[0]));
        engineType = r.getEngineType();
        fieldReader = engineType.getNewFieldReader();
        entities = new Entity[1 << engineType.getIndexBits()];
        deletions = new int[1 << engineType.getIndexBits()];
        r.runWith(this);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
