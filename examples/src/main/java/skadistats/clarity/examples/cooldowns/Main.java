package skadistats.clarity.examples.cooldowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

@Example(name = "cooldowns", description = "Track ability cooldown events (start, reset, end)", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @Insert
    private Context ctx;

    @OnAbilityCooldownStart
    public void onCooldownStart(Entity ability, Entity owner, Float endTime) {
        System.out.printf("%06d: %s on %s cooldown started, ends at %.2f%n",
                ctx.getTick(),
                ability.getDtClass().getDtName(),
                owner != null ? owner.getDtClass().getDtName() : "(unknown)",
                endTime);
    }

    @OnAbilityCooldownReset
    public void onCooldownReset(Entity ability, Entity owner) {
        System.out.printf("%06d: %s on %s cooldown reset%n",
                ctx.getTick(),
                ability.getDtClass().getDtName(),
                owner != null ? owner.getDtClass().getDtName() : "(unknown)");
    }

    @OnAbilityCooldownEnd
    public void onCooldownEnd(Entity ability, Entity owner) {
        System.out.printf("%06d: %s on %s cooldown ended%n",
                ctx.getTick(),
                ability.getDtClass().getDtName(),
                owner != null ? owner.getDtClass().getDtName() : "(unknown)");
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        } finally {
            long tMatch = System.currentTimeMillis() - tStart;
            log.info("total time taken: {}s", (tMatch) / 1000.0);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
