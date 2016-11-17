# Clarity-examples

This project contains example code for the [clarity replay parser](https://github.com/skadistats/clarity).

## Introduction

Clarity 2 uses an event based approach to replay analysis. To use it, you have to supply one or more
processors to clarity. A processor is a simple POJO, that you enrich with annotations, which tell
clarity what kind of data you want to receive.
 
This simple yet fully working example prints all messages from all chat (Source 2):

```java
public class AllChatProcessor {
    @OnMessage(S2UserMessages.CUserMessageSayText2.class)
    public void onMessage(Context ctx, S2UserMessages.CUserMessageSayText2 message) {
        System.out.format("%s: %s\n", message.getParam1(), message.getParam2());
    }
    public static void main(String[] args) throws Exception {
        // 1) create an input source from the replay
        Source source = new MappedFileSource("replay.dem");
        // 2) create a simple runner that will read the replay once
        SimpleRunner runner = new SimpleRunner(source);
        // 3) create an instance of your processor
        AllChatProcessor processor = new AllChatProcessor();
        // 4) and hand it over to the runner
        runner.runWith(processor);
    }
}
```

The main method does the following:

1. Creates a source from the replay file. In this case, a MappedFileSource is used, which needs a locally available file
   to work. This is the fastest implementation, but there is also an InputStreamSource, which lets you create a source 
   from any InputStream you can come up with.
2. Create a runner with your source. The runner is what drives the replay analysis. The SimpleRunner we use in this case
   will simply run over the whole replay once. There is also a more sophisticated ControllableRunner, which uses a separate
   thread for doing the work, and which allows seeking back and forth in the replay. 
3. Create an instance of your processor. Please note that you are not limited to only using one processor, and clarity itself
   contains a lot of processors that might take part in the run if what you requested requires it.
4. This starts the processing run. Your annotated method onMessage() will be called back whenever clarity finds an 
   allchat-message in the replay.

### Building the examples

All provided examples can be build with Maven. The build process yields an "uber-jar", that is a jar 
containing all the dependencies, which can be called from the command line easily without having to 
set a correct classpath. 

### Logging

Clarity uses the logback-library for logging. You can enable logging for certain packages by changing 
`src/main/resources/logback.xml`.  

## Examples

### Watching the data in real time

[Clarity Analyzer](https://github.com/spheenik/clarity-analyzer) is nifty little JavaFX Application 
that lets you see all the entity data in the replay in real time.
 
![Clarity Analyzer](https://raw.githubusercontent.com/spheenik/clarity-analyzer/master/screenshot.png)

### Showing the combat log

This example *almost* replicates what is shown on the combat log from the game.
It has problems with finding out if some modifier applied to a unit is a buff or a debuff, 
and it doesn't know how to convert the technical hero names to plain english... but otherwise it has it all :)

You can find it under [skadistats.clarity.examples.combatlog.Main.java](https://github.com/skadistats/clarity-examples/blob/master/src/main/java/skadistats/clarity/examples/combatlog/Main.java).
After building it from the project root with

	mvn -P combatlog package
	
you can run it with

	java -jar target/combatlog.one-jar.jar replay.dem

### Show stats at the end of the game

This example shows how to use the PlayerResource entity as well as the ControllableRunner.
It outputs the score table at the end of the match. For getting to the result as fast as possible, it does not 
run the complete replay, but instead uses the ControllableRunner to directly seek to the last tick in the replay.

You can find it under [skadistats.clarity.examples.matchend.Main.java](https://github.com/skadistats/clarity-examples/blob/master/src/main/java/skadistats/clarity/examples/matchend/Main.java).
After building it from the project root with

	mvn -P matchend package
	
you can run it with

	java -jar target/matchend.one-jar.jar replay.dem

### Tracking spawns / deaths

This example shows how to write a processor that provides events related to the lifestate of an entity.
The processor provides 3 new events (`@OnEntitySpawned`, `@OnEntityDying` and `@OnEntityDied`) and an associated
main class that uses them.

You can find the processor under [skadistats.clarity.examples.lifestate.SpawnsAndDeaths.java](https://github.com/skadistats/clarity-examples/blob/master/src/main/java/skadistats/clarity/examples/lifestate/SpawnsAndDeaths.java),
and the class that uses it under [skadistats.clarity.examples.lifestate.Main.java](https://github.com/skadistats/clarity-examples/blob/master/src/main/java/skadistats/clarity/examples/lifestate/Main.java). 

After building it from the project root with

	mvn -P lifestate package
	
you can run it with

	java -jar target/lifestate.one-jar.jar replay.dem

### Retrieving basic game info

For retrieving the basic game information (players, picks, bans, who won), 
you do not need to iterate the replay. You can retrieve that info with the following code

```Java
public class Main {
    public static void main(String[] args) throws Exception {
        CDemoFileInfo info = Clarity.infoForFile(args[0]);
        System.out.println(info);
    }
}
```

You can find this example under [skadistats.clarity.examples.info.Main.java](https://github.com/skadistats/clarity-examples/blob/master/src/main/java/skadistats/clarity/examples/info/Main.java).
After building it from the project root with

	mvn -P info package
	
you can run it with

	java -jar target/info.one-jar.jar replay.dem


### Send table inspection (Source 1)

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

You can find it under [skadistats.clarity.examples.dtinspector.Main.java](https://github.com/skadistats/clarity-examples/blob/master/src/main/java/skadistats/clarity/examples/dtinspector/Main.java).
After building it from the project root with

	mvn -P dtinspector package
	
you can run it with

	java -jar target/dtinspector.one-jar.jar replay.dem
	
and it will open a window which lets you explore the send tables in an interactive manner.


## Under the hood

### Events / Providers

Clarity is driven by a small annotation driven event system. Clarity provides basic events, like @OnMessage, 
which is used to get a callback whenever a message of a certain type is found in the replay.

If you want, you can subscribe to those events directly, for example to create a dump of the replay.

But those events can also be used to listen for certain data in the replay and refine the raw data into more
sophisticated events. One example from Clarity is the GameEvents processor, which listens for raw messages of type
CSVCMsg_GameEventList and CSVCMsg_GameEvent and transforms their content into an easier to use form:

```java
@Provides(OnGameEvent.class) // 1. register as a provider for @OnGameEvent
public class GameEvents {
    @OnMessage(NetMessages.CSVCMsg_GameEventList.class)
    public void onGameEventList(Context ctx, NetMessages.CSVCMsg_GameEventList message) {
        // 2. process the incoming message, and create GameEventDescriptors from it  
    }
    @OnMessage(NetworkBaseTypes.CSVCMsg_GameEvent.class)
    public void onGameEvent(Context ctx, NetworkBaseTypes.CSVCMsg_GameEvent message) {
        // 3. use the GameEventDescriptors from 2, to create a single GameEvent
        GameEvent e = new GameEvent(descriptor);
        // 4. raise @OnGameEvent
        ctx.createEvent(OnGameEvent.class, GameEvent.class).raise(e);
    }
```

1. the @Provides-annotation tells clarity that this processor is able to supply @OnGameEvent events.
   So whenever some processor gets added to the run that listens for this event, Clarity will make sure an instance
   of GameEvents is also part of the run to supply those events.
2. The exact structure of GameEvents in this replay is encoded in a CSVCMsg_GameEventList message. 
3. Create a single GameEvent, by using the descriptors created in 2.
4. fire the @OnGameEvent event, passing the created GameEvent as parameter.

Another example for creating your own event provider is [a provider for spawn / death events](https://github.com/skadistats/clarity-examples/blob/master/src/main/java/skadistats/clarity/examples/lifestate/SpawnsAndDeaths.java).

### Context

The first parameter on any event listener called by Clarity is a Context object.
You can use it to do useful stuff:

```java
public class Context {
    // 1. get a reference to another processor also taking part in the run
    public <T> T getProcessor(Class<T> processorClass) {}
    // 2. query the current tick
    public int getTick() {}
    // 3. query the engine type the replay was recorded with
    public EngineType getEngineType() {}
    // 4. query the build number the replay was recorded with (Source 2 only)
    public int getBuildNumber() {}
    // 5. raise an event yourself
    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes) {}
}
```

1. Often times you need a reference to another processor. You could get a reference to the mentioned GameEvents processor
   by calling `ctx.getProcessor(GameEvents.class)`.
2. returns the current tick
3. returns the type of engine (Source 1 or 2) the replay was recorded with.
4. if the replay was recorded with Source 2, this will give you the build number of the server that recorded the replay
5. this function can be used to create events yourself. 

