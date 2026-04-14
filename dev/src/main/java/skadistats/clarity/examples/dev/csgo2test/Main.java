package skadistats.clarity.examples.dev.csgo2test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.Clarity;
import skadistats.clarity.model.csgo.PlayerInfoType;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.OnPlayerInfo;
import skadistats.clarity.processor.stringtables.PlayerInfo;
import skadistats.clarity.source.MappedFileSource;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private static class Replay {
        private final String type;
        private final String file;

        public Replay(String type, String file) {
            this.type = type;
            this.file = file;
        }

        public String getType() {
            return type;
        }

        public String getFile() {
            return file;
        }
    }

    private static final Replay DOTA_S1 = new Replay("DOTA_S1", "/home/spheenik/projects/replays/dota/s1/normal/271145478.dem");
    private static final Replay DOTA_S2 = new Replay("DOTA_S2", "/home/spheenik/projects/replays/dota/s2/297/7048355297.dem");
    private static final Replay CSGO_S1 = new Replay("CSGO_S1", "/home/spheenik/projects/replays/csgo/s1/issue-271/astralis-vs-godsent-m1-nuke.dem");
    private static final Replay CSGO_S2 = new Replay("CSGO_S2", "/home/spheenik/projects/replays/csgo/s2/prelaunch/bayes_demo.dem");


    private static final Replay B_1 = new Replay("CSGO_S2", "/home/spheenik/projects/replays/csgo/s2/bayes_042024/ta_esea_open-league-s49-eu_lilmix-roundsgg-240420241800game1.dem");
    private static final Replay B_2 = new Replay("CSGO_S2", "/home/spheenik/projects/replays/csgo/s2/bayes_042024/ta_relog_res-cs2-regional-europe-3-2024_bleed-dms-260420241400game1.dem");
    private static final Replay B_3 = new Replay("CSGO_S2", "/home/spheenik/projects/replays/csgo/s2/bayes_042024/ta_relog_res-cs2-regional-europe-3-2024_gaimin-gladiators-alternate-attax-260420241100game1a.dem");
    private static final Replay B_4 = new Replay("CSGO_S2", "/home/spheenik/projects/replays/csgo/s2/bayes_042024/ta_relog_res-cs2-regional-europe-3-2024_gaimin-gladiators-alternate-attax-260420241100game1b.dem");
    private static final Replay B_5 = new Replay("CSGO_S2", "/home/spheenik/projects/replays/csgo/s2/bayes_042024/ta_relog_res-cs2-regional-europe-3-2024_gaimin-gladiators-alternate-attax-260420241100game1c.dem");


    public static void main(String[] args) throws Exception {
        //runDump(CSGO_S1);
        runEntities(B_3);
        //runGameEvents(DOTA_S1, DOTA_S2, CSGO_S1, CSGO_S2);
        //runSpawnGroups(DOTA_S1, DOTA_S2, CSGO_S1, CSGO_S2);
        //runResources(DOTA_S2, CSGO_S2);
        //runGameEvents(CSGO_S2);
        //runStringTables(CSGO_S1, CSGO_S2);
        //runPlayerInfo(CSGO_S1, CSGO_S2);
        //runInfo(data);
    }

    private static void runInfo(Replay... replays) {
        for (Replay replay : replays) {
            System.out.println("INFO " + replay.getType());
            try {
                System.out.println(Clarity.infoForFile(replay.getFile()));
            } catch (Exception e) {
                log.error("Info not found", e);
            }
        }
    }

    private static void runDump(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("DUMP " + replay.getType());
            new skadistats.clarity.examples.dev.dump.Main().run(replay.getFile(), false, false);
        }
    }

    private static void runParticles(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("PARTICLES " + replay.getType());
            new skadistats.clarity.examples.particles.Main().run(new String[]{replay.getFile()});
        }
    }

    private static void runEntities(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("ENTITIES " + replay.getType());
            new skadistats.clarity.examples.dev.entityrun.Main().run(new String[]{replay.getFile()});
        }
    }

    private static void runStringTables(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("STRING TABLES " + replay.getType());
            new skadistats.clarity.examples.dev.stringtabledump.Main().runSeek(new String[]{replay.getFile()});
        }
    }

    private static void runGameEvents(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("GAME_EVENTS " + replay.getType());
            new skadistats.clarity.examples.gameevent.Main().run(new String[]{replay.getFile()});
        }
    }

    private static void runSpawnGroups(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("SPAWN GROUPS " + replay.getType());
            new skadistats.clarity.examples.spawngroups.Main().run(new String[]{replay.getFile()});
        }
    }

    private static void runResources(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("RESOURCES " + replay.getType());
            new skadistats.clarity.examples.resources.Main().run(new String[]{replay.getFile()});
        }
    }


    private static void runPlayerInfo(Replay... replays) throws Exception {
        for (Replay replay : replays) {
            System.out.println("PLAYERINFO " + replay.getType());
            runCustom(replay.getFile(), new Object() {
                @OnPlayerInfo
                protected void onPlayerInfo(int n, PlayerInfoType playerInfo) {
                    System.out.println(playerInfo);
                }
            });
        }
    }

    private static void runCustom(String replay, Object... processors) throws Exception {
        long tStart = System.currentTimeMillis();
        try (MappedFileSource s = new MappedFileSource(replay)) {
            new SimpleRunner(s).runWith(processors);
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

}
