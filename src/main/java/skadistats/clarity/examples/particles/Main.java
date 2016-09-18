package skadistats.clarity.examples.particles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.s1.ParticleAttachmentType;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.OnStringTableCreated;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @Insert
    private Entities entities;
    @Insert
    private StringTables stringTables;
    @Insert
    private Context context;

    private int getTick() {
        return context.getTick();
    }

    @OnMessage(DotaUserMessages.CDOTAUserMsg_ParticleManager.class)
    public void onMessage(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        switch(message.getType()) {
            case DOTA_PARTICLE_MANAGER_EVENT_CREATE:
                logCreate(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE:
                logUpdate(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_FORWARD:
                logUnhanded(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_ORIENTATION:
                logUpdateOrientation(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_FALLBACK:
                logUnhanded(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_ENT:
                logUpdateEnt(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_OFFSET:
                logUnhanded(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_DESTROY:
                logDestroy(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_DESTROY_INVOLVING:
                logUnhanded(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_RELEASE:
                logRelease(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_LATENCY:
                logUnhanded(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_SHOULD_DRAW:
                logUnhanded(message);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_FROZEN:
                logUnhanded(message);
                break;
        }

    }

    private void logCreate(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        int entityHandle = message.getCreateParticle().getEntityHandle();
//        int entityIndex = Handle.indexForHandle(entityHandle);
//        int entitySerial = Handle.serialForHandle(entityHandle);
        Entity parent = entities.getByHandle(entityHandle);
        log.info("{} {} [index={}, entity={}({}), effect={}, attach={}]",
            getTick(),
            "PARTICLE_CREATE",
            message.getIndex(),
            entityHandle,
            parent == null ? "NOT_FOUND" : parent.getDtClass().getDtName(),
            message.getCreateParticle().getParticleNameIndex(),
            message.getCreateParticle().getAttachType()
        );
        //log.info(message.toString());
    }

    private void logUpdate(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        log.info("{} {} [index={}, controlPoint={}, position=[{}, {}, {}]]",
            getTick(),
            "PARTICLE_UPDATE",
            message.getIndex(),
            message.getUpdateParticle().getControlPoint(),
            message.getUpdateParticle().getPosition().getX(),
            message.getUpdateParticle().getPosition().getY(),
            message.getUpdateParticle().getPosition().getZ()
        );
        //log.info(message.toString());
    }

    private void logUpdateOrientation(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        log.info("{} {} [index={}, controlPoint={}, forward=[{}, {}, {}], right=[{}, {}, {}], up=[{}, {}, {}]]",
            getTick(),
            "PARTICLE_UPDATE_ORIENT",
            message.getIndex(),
            message.getUpdateParticleOrient().getControlPoint(),
            message.getUpdateParticleOrient().getForward().getX(),
            message.getUpdateParticleOrient().getForward().getY(),
            message.getUpdateParticleOrient().getForward().getZ(),
            message.getUpdateParticleOrient().getRight().getX(),
            message.getUpdateParticleOrient().getRight().getY(),
            message.getUpdateParticleOrient().getRight().getZ(),
            message.getUpdateParticleOrient().getUp().getX(),
            message.getUpdateParticleOrient().getUp().getY(),
            message.getUpdateParticleOrient().getUp().getZ()
        );
        //log.info(message.toString());
    }

    private void logUpdateEnt(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        int entityHandle = message.getUpdateParticleEnt().getEntityHandle();
        Entity parent = entities.getByHandle(entityHandle);
        log.info("{} {} [index={}, entity={}({}), controlPoint={}, attachmentType={}, attachment={}, includeWearables={}]",
            getTick(),
            "PARTICLE_UPDATE_ENT",
            message.getIndex(),
            entityHandle,
            parent == null ? "NOT_FOUND" : parent.getDtClass().getDtName(),
            message.getUpdateParticleEnt().getControlPoint(),
            ParticleAttachmentType.forId(message.getUpdateParticleEnt().getAttachType()),
            message.getUpdateParticleEnt().getAttachment(),
            message.getUpdateParticleEnt().getIncludeWearables()
        );
        //log.info(message.toString());
    }

    private void logDestroy(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        log.info("{} {} [index={}, immediately={}]",
            getTick(),
            "PARTICLE_DESTROY",
            message.getIndex(),
            message.getDestroyParticle().getDestroyImmediately()
        );
        //log.info(message.toString());
    }

    private void logRelease(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        log.info("{} {} [index={}]",
            getTick(),
            "PARTICLE_RELEASE",
            message.getIndex()
        );
        //log.info(message.toString());
    }

    private void logUnhanded(DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        log.info(message.toString());
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
