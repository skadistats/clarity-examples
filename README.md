# clarity-examples

This project contains example code for the [clarity replay parser](https://github.com/skadistats/clarity).

# General

All provided examples can be build with Maven. The build process yields an "uber-jar", that is a jar 
containing all the dependencies, which can be called from the command line easily without having to 
set a correct classpath. 

## Logging

Clarity uses the logback-library for logging. You can enable logging for certain packets by changing 
`src/main/resources/logback.xml`. Changing the log-level to *debug* will output parsed data for almost all
handlers, while putting the level to *trace* will output the raw content of the protobuf messages a
handler is assigned to. 

## Profiles

Often times, you only need a subset of the data available in the replay. Not processing the rest will result
in faster execution times. So, when loading up a replay, you have to tell clarity what part of the replay 
data you are interested in. For example, if you are interested in chat messages and entities, you would
initialize your iterator like this:

	DemoInputStreamIterator iter = Clarity.iteratorForFile(fileName, Profile.ENTITIES, Profile.CHAT_EVENTS); 

Please take a look at `skadistats.clarity.parser.Profile.java` to see what
profiles are available. You can also create a custom profile by copying and adapting the code found there.

# Examples

## Simple Replay Iteration

This is the most basic way to invoke a complete parsing run over a replay.
Please notice that it will just load the replay, and handle each packet once, but not 
output anything. If you want to have output, please adjust the log-level or add some
code.

```Java
public class Main {
    public static void main(String[] args) throws Exception {
    	// Match is a container for all the data clarity provides.
        Match match = new Match();
        // set up an iterator for reading all packets from the file
        DemoInputStreamIterator iter = Clarity.iteratorForFile(args[0], Profile.ALL);
        while (iter.hasNext()) {
	        // read the next Peek from the iterator
            Peek p = iter.next();
	        // and apply it to the match, changing it's state
            p.apply(match);
            // now, it's your turn to do something with match here.
        }
    }
}
```

You can find this example under `skadistats.clarity.examples.simple.Main.java`.
After building it from the project root with

	mvn -P simple package
	
you can run it with

	java -jar target/simple.jar replay.dem
	
## Retrieving the game info

For retrieving the basic game information (players, picks, bans, who won, basically all the stuff
you see on dotabuff for a single match), you do not need to iterate the complete replay. You can
retrieve that info with the following code

```Java
public class Main {
    public static void main(String[] args) throws Exception {
        CDemoFileInfo info = Clarity.infoForFile(args[0]);
        System.out.println(info);
    }
}
```

You can find this example under `skadistats.clarity.examples.info.Main.java`.
After building it from the project root with

	mvn -P info package
	
you can run it with

	java -jar target/info.jar replay.dem


## Send table inspection

Dota2 is a game made with the Source engine from Valve. Source manages a set of networked entities
which exist on the server and are propagated to the client. A lot of stuff you see in a dota match is a networked entity,
for example the heros, creeps and buildings, but also statistical information about the game, like
the current game time, scoreboard, etc. You can find some information about networked entities in the 
[Valve Developer Community Wiki](https://developer.valvesoftware.com/wiki/Networking_Entities).

Since the Dota client is constantly changing and improving, there is no fixed format for what data (properties) these
entities contain. To be able to replay a replay recorded on an old client on a newer version, the replay 
contains definitions of exactly what entities with what properties it contains. These definitions are
called send tables.

This example shows the format of the entity data in a certain replay.

You can find it under `skadistats.clarity.examples.dtinspector.Main.java`.
After building it from the project root with

	mvn -P dtinspector package
	
you can run it with

	java -jar target/dtinspector.jar replay.dem
	
and it will open a window which lets you explore the send tables in an interactive manner.


