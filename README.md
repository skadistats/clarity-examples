# Clarity-examples (2.0 beta)

This project contains example code for the [clarity replay parser](https://github.com/skadistats/clarity).

# Changes in version 2:

### Processors

Clarity 2 now uses an event based approach to replay analysis. To use it, you have to supply one or more
processors to clarity, which declare what data they are interested in via annotations:

This simple processor that prints all messages from all chat:
```java
public class AllChatProcessor {
    @OnMessage(Usermessages.CUserMsg_SayText2.class)
    public void onMessage(Context ctx, Usermessages.CUserMsg_SayText2.class message) {
        log.info(message.getText());
    }
    public static void main(String[] args) throws Exception {
        new Runner().runWith(new FileInputStream(args[0]), new AllChatProcessor());
    }
}
```
Runner is the class that bootstraps the processing run. You need to supply it with one or more instances of processors
you want to take part in the run. It will then find out what events you requested and instantiate other (built-in) processors that
provide the events you requested.

To illustrate, this is a stripped down example of how clarity uses the
@OnMessage event to listen for occurrences of CSVCMsg_GameEventList and CSVCMsg_GameEvent and uses them to 
supply a new event @OnGameEvent:
 
```java
@Provides(OnGameEvent.class)
public class GameEvents {
    private final Map<Integer, GameEventDescriptor> byId = new TreeMap<>();
    @OnMessage(Netmessages.CSVCMsg_GameEventList.class)
    public void onGameEventList(Context ctx, Netmessages.CSVCMsg_GameEventList message) {
        // some code here to fill the Map "byId"  
    }
    @OnMessage(Networkbasetypes.CSVCMsg_GameEvent.class)
    public void onGameEvent(Context ctx, Networkbasetypes.CSVCMsg_GameEvent message) {
        GameEventDescriptor desc = byId.get(message.getEventid());
        GameEvent e = new GameEvent(desc);
        // some more code to fill the GameEvent
        ctx.createEvent(OnGameEvent.class, GameEvent.class).raise(e);
    }
```

Notice the @Provides annotation: It tells clarity that an instance of this class has to be part of a run where another
processor is annotated with @OnGameEvent. If you write a processor that emits custom events, this is how you make them known 
to clarity.

### Context

By decree of the gods the first parameter on any event listener in clarity shall be the Context.
At the moment, it doesn't contain much functionality, but is likely to grow into part of what Match was in clarity 1.
```java
public class Context {
    public <T> T getProcessor(Class<T> processorClass) {}
    public int getTick() {}
    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes) {}
}
```

With getProcessor() you can get a reference to a processor taking part in the run. For example, you could write a processor
```java
public class UselessProcessor {
    @OnTickStart
    @UsesStringTable("ActiveModifiers");
    public void onMessage(Context ctx) {
        StringTables stringTables = ctx.getProcessor(StringTables.class);
        StringTable table = stringTables.forName("ActiveModifiers");
        if (table != null) {
            // if we knew what, we could do something with the table here...
        }
    }
    public static void main(String[] args) throws Exception {
        new Runner().runWith(new FileInputStream(args[0]), new UselessProcessor());
    }
}
```

# Examples

### Building

All provided examples can be build with Maven. The build process yields an "uber-jar", that is a jar 
containing all the dependencies, which can be called from the command line easily without having to 
set a correct classpath. 

### Logging

Clarity uses the logback-library for logging. You can enable logging for certain packets by changing 
`src/main/resources/logback.xml`. Changing the log-level to *debug* will output parsed data for almost all
handlers, while putting the level to *trace* will output the raw content of the protobuf messages a
handler is assigned to. 

## Showing the combat log

It *almost* replicates what is shown on the combat log from the game.
It still has problems with finding out if some modifier applied to a unit is a buff or a debuff, 
and it doesn't know how to convert the technical hero names to plain english... but otherwise it has it all :)

You can find it under [skadistats.clarity.examples.combatlog.Main.java](https://github.com/skadistats/clarity-examples/blob/2.0-dev/src/main/java/skadistats/clarity/examples/combatlog/Main.java).
After building it from the project root with

	mvn -P combatlog package
	
you can run it with

	java -jar target/combatlog.one-jar.jar replay.dem

## Show stats at the end of the game

This example shows how to use the PlayerResource entity.
It outputs the score table at the end of the game, almost as complete as dotabuff.
It could be improved since it iterates over the complete replay to get to the end of the game,
which takes a while.
You can find it under [skadistats.clarity.examples.matchend.Main.java](https://github.com/skadistats/clarity-examples/blob/2.0-dev/src/main/java/skadistats/clarity/examples/matchend/Main.java).
After building it from the project root with

	mvn -P matchend package
	
you can run it with

	java -jar target/matchend.one-jar.jar replay.dem

## Retrieving the game info

For retrieving the basic game information (players, picks, bans, who won), 
you do not need to iterate the complete replay. You can retrieve that info with the following code

```Java
public class Main {
    public static void main(String[] args) throws Exception {
        CDemoFileInfo info = Clarity.infoForFile(args[0]);
        System.out.println(info);
    }
}
```

You can find this example under [skadistats.clarity.examples.info.Main.java](https://github.com/skadistats/clarity-examples/blob/2.0-dev/src/main/java/skadistats/clarity/examples/info/Main.java).
After building it from the project root with

	mvn -P info package
	
you can run it with

	java -jar target/info.one-jar.jar replay.dem


## Send table inspection

Dota 2 is a game made with the Source engine from Valve. Source manages a set of networked entities
which exist on the server and are propagated to the client. A lot of stuff you see in a dota match is a networked entity,
for example the heros, creeps and buildings, but also statistical information about the game, like
the current game time, scoreboard, etc. You can find some information about networked entities in the 
[Valve Developer Community Wiki](https://developer.valvesoftware.com/wiki/Networking_Entities).

Since the Dota client is constantly changing and improving, there is no fixed format for what data (properties) these
entities contain. To be able to replay a replay recorded on an old client on a newer version, the replay 
contains definitions of exactly what entities with what properties it contains. These definitions are
called send tables.

This example shows the format of the entity data in a certain replay.

You can find it under [skadistats.clarity.examples.dtinspector.Main.java](https://github.com/skadistats/clarity-examples/blob/2.0-dev/src/main/java/skadistats/clarity/examples/dtinspector/Main.java).
After building it from the project root with

	mvn -P dtinspector package
	
you can run it with

	java -jar target/dtinspector.one-jar.jar replay.dem
	
and it will open a window which lets you explore the send tables in an interactive manner.


