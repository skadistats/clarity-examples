package skadistats.clarity.examples.s2dotatempentities;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.dota.common.proto.DOTAUserMessages.CDOTAUserMsg_TE_DestroyProjectile;
import skadistats.clarity.wire.dota.common.proto.DOTAUserMessages.CDOTAUserMsg_TE_DotaBloodImpact;
import skadistats.clarity.wire.dota.common.proto.DOTAUserMessages.CDOTAUserMsg_TE_Projectile;
import skadistats.clarity.wire.dota.common.proto.DOTAUserMessages.CDOTAUserMsg_TE_ProjectileLoc;
import skadistats.clarity.wire.dota.common.proto.DOTAUserMessages.CDOTAUserMsg_TE_UnitAnimation;
import skadistats.clarity.wire.dota.common.proto.DOTAUserMessages.CDOTAUserMsg_TE_UnitAnimationEnd;

import java.util.Map;
import java.util.TreeMap;

/**
 * Demonstrates how to consume Dota-2 specific Source 2 temp-entity messages.
 *
 * <p>Unlike the engine-level {@code S2TempEntities.CMsgTE*} messages
 * (see the {@code s2effectdispatch} example), Dota 2 ships its own set of
 * temp-entity-style notifications inside {@code DotaUserMessages}:</p>
 *
 * <ul>
 *   <li>{@code CDOTAUserMsg_TE_Projectile} — projectile spawned with explicit target entity</li>
 *   <li>{@code CDOTAUserMsg_TE_ProjectileLoc} — projectile spawned aimed at a location</li>
 *   <li>{@code CDOTAUserMsg_TE_DestroyProjectile} — projectile removed early</li>
 *   <li>{@code CDOTAUserMsg_TE_DotaBloodImpact} — hit visualization on an entity</li>
 *   <li>{@code CDOTAUserMsg_TE_UnitAnimation} — animation playback request</li>
 *   <li>{@code CDOTAUserMsg_TE_UnitAnimationEnd} — animation cancel</li>
 * </ul>
 *
 * <p>These are already typed protobuf messages, so no wrapper or string-table
 * resolution is needed — just listen with {@code @OnMessage} and read the
 * fields directly. This example simply counts each kind and prints the
 * first projectile's payload as a sanity check.</p>
 *
 * <p>Only meaningful on Dota 2 Source 2 replays. The handlers will silently
 * not fire on CSGO/CS2/Deadlock replays since these messages do not exist
 * there.</p>
 */
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getName());

    private final Map<String, Long> counts = new TreeMap<>();
    private boolean firstProjectileLogged = false;

    private void count(Message m) {
        counts.merge(m.getClass().getSimpleName(), 1L, Long::sum);
    }

    @OnMessage(CDOTAUserMsg_TE_Projectile.class)
    public void onProjectile(CDOTAUserMsg_TE_Projectile m) {
        count(m);
        if (!firstProjectileLogged) {
            log.info("first projectile: source={}, target={}, ability={}, handle={}, "
                            + "move_speed={}, launch_tick={}, target_loc=({},{},{}), "
                            + "particle_system_handle=0x{}",
                    m.getSource(), m.getTarget(), m.getAbility(), m.getHandle(),
                    m.getMoveSpeed(), m.getLaunchTick(),
                    m.getTargetLoc().getX(), m.getTargetLoc().getY(), m.getTargetLoc().getZ(),
                    Long.toHexString(m.getParticleSystemHandle()));
            firstProjectileLogged = true;
        }
    }

    @OnMessage(CDOTAUserMsg_TE_ProjectileLoc.class)
    public void onProjectileLoc(CDOTAUserMsg_TE_ProjectileLoc m) {
        count(m);
    }

    @OnMessage(CDOTAUserMsg_TE_DestroyProjectile.class)
    public void onDestroyProjectile(CDOTAUserMsg_TE_DestroyProjectile m) {
        count(m);
    }

    @OnMessage(CDOTAUserMsg_TE_DotaBloodImpact.class)
    public void onBloodImpact(CDOTAUserMsg_TE_DotaBloodImpact m) {
        count(m);
    }

    @OnMessage(CDOTAUserMsg_TE_UnitAnimation.class)
    public void onUnitAnimation(CDOTAUserMsg_TE_UnitAnimation m) {
        count(m);
    }

    @OnMessage(CDOTAUserMsg_TE_UnitAnimationEnd.class)
    public void onUnitAnimationEnd(CDOTAUserMsg_TE_UnitAnimationEnd m) {
        count(m);
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;

        log.info("=== dota temp-entity histogram ===");
        long total = 0;
        for (var e : counts.entrySet()) {
            log.info("  {} : {}", e.getKey(), e.getValue());
            total += e.getValue();
        }
        log.info("total: {} dota temp-entity messages across {} kinds", total, counts.size());
        log.info("total time taken: {}s", tMatch / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
