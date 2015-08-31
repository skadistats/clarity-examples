package skadistats.clarity.examples.particles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.s1.ParticleAttachmentType;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

@UsesEntities
@UsesStringTable("ParticleEffectNames")
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnMessage(DotaUserMessages.CDOTAUserMsg_ParticleManager.class)
    public void onMessage(Context ctx, DotaUserMessages.CDOTAUserMsg_ParticleManager message) {
        switch(message.getType()) {
            case DOTA_PARTICLE_MANAGER_EVENT_CREATE:
                logCreate(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE:
                logUpdate(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_FORWARD:
                logUnhanded(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_ORIENTATION:
                logUpdateOrientation(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_FALLBACK:
                logUnhanded(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_ENT:
                logUpdateEnt(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_OFFSET:
                logUnhanded(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_DESTROY:
                logDestroy(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_DESTROY_INVOLVING:
                logUnhanded(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_RELEASE:
                logRelease(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_LATENCY:
                logUnhanded(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_SHOULD_DRAW:
                logUnhanded(message, ctx);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_FROZEN:
                logUnhanded(message, ctx);
                break;
        }

    }

    private void logCreate(DotaUserMessages.CDOTAUserMsg_ParticleManager message, Context ctx) {
        int entityHandle = message.getCreateParticle().getEntityHandle();
//        int entityIndex = Handle.indexForHandle(entityHandle);
//        int entitySerial = Handle.serialForHandle(entityHandle);
        Entity parent = ctx.getProcessor(Entities.class).getByHandle(entityHandle);
        String name = ctx.getProcessor(StringTables.class).forName("ParticleEffectNames").getNameByIndex((int)message.getCreateParticle().getParticleNameIndex());
        log.info("{} {} [index={}, entity={}({}), effect={}, attach={}]",
            ctx.getTick(),
            "PARTICLE_CREATE",
            message.getIndex(),
            entityHandle,
            parent == null ? "NOT_FOUND" : parent.getDtClass().getDtName(),
            name == null ? "NOT_FOUND" : name,
            message.getCreateParticle().getAttachType()
        );
        //log.info(message.toString());
    }

    private void logUpdate(DotaUserMessages.CDOTAUserMsg_ParticleManager message, Context ctx) {
        log.info("{} {} [index={}, controlPoint={}, position=[{}, {}, {}]]",
            ctx.getTick(),
            "PARTICLE_UPDATE",
            message.getIndex(),
            message.getUpdateParticle().getControlPoint(),
            message.getUpdateParticle().getPosition().getX(),
            message.getUpdateParticle().getPosition().getY(),
            message.getUpdateParticle().getPosition().getZ()
        );
        //log.info(message.toString());
    }

    private void logUpdateOrientation(DotaUserMessages.CDOTAUserMsg_ParticleManager message, Context ctx) {
        log.info("{} {} [index={}, controlPoint={}, forward=[{}, {}, {}], right=[{}, {}, {}], up=[{}, {}, {}]]",
            ctx.getTick(),
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

    private void logUpdateEnt(DotaUserMessages.CDOTAUserMsg_ParticleManager message, Context ctx) {
        int entityHandle = message.getUpdateParticleEnt().getEntityHandle();
        Entity parent = ctx.getProcessor(Entities.class).getByHandle(entityHandle);
        log.info("{} {} [index={}, entity={}({}), controlPoint={}, attachmentType={}, attachment={}, includeWearables={}]",
            ctx.getTick(),
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

    private void logDestroy(DotaUserMessages.CDOTAUserMsg_ParticleManager message, Context ctx) {
        log.info("{} {} [index={}, immediately={}]",
            ctx.getTick(),
            "PARTICLE_DESTROY",
            message.getIndex(),
            message.getDestroyParticle().getDestroyImmediately()
        );
        //log.info(message.toString());
    }

    private void logRelease(DotaUserMessages.CDOTAUserMsg_ParticleManager message, Context ctx) {
        log.info("{} {} [index={}]",
            ctx.getTick(),
            "PARTICLE_RELEASE",
            message.getIndex()
        );
        //log.info(message.toString());
    }

    private void logUnhanded(DotaUserMessages.CDOTAUserMsg_ParticleManager message, Context ctx) {
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
