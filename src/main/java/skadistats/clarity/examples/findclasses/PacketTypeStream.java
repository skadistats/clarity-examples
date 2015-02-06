package skadistats.clarity.examples.findclasses;

import com.dota2.proto.Demo.CDemoPacket;
import com.dota2.proto.Demo.CDemoSendTables;
import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.EDemoCommands;
import com.dota2.proto.Networkbasetypes.CSVCMsg_UserMessage;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.parser.PacketTypes;
import skadistats.clarity.parser.Profile;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class PacketTypeStream implements Closeable {

    private enum State {
        TOP, EMBED
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InputStream is; // source stream
    private final CodedInputStream ms; // main stream
    private CodedInputStream es = null; // stream for embedded packet
    private int fileInfoOffset;
    private int n = -1;
    private int tick = 0;
    private boolean full = false;
    private State state = State.TOP;

    public PacketTypeStream(InputStream is) {
    	this.is = is;
    	this.ms = CodedInputStream.newInstance(is);
    }
    
    public void bootstrap() throws IOException {
        ms.setSizeLimit(Integer.MAX_VALUE);
        String header = new String(ms.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
        fileInfoOffset = ms.readFixed32();
    }
    
    public Class<? extends GeneratedMessage> read() throws IOException {
        while (!ms.isAtEnd()) {
            switch (state) {
                case TOP:
                    int kind = ms.readRawVarint32();
                    boolean isCompressed = (kind & EDemoCommands.DEM_IsCompressed_VALUE) == EDemoCommands.DEM_IsCompressed_VALUE;
                    kind &= ~EDemoCommands.DEM_IsCompressed_VALUE;
                    tick = ms.readRawVarint32();
                    int size = ms.readRawVarint32();
                    Class<? extends GeneratedMessage> topClazz = PacketTypes.DEMO.get(kind);
                    if (topClazz == null) {
                        log.warn("unknown top level message of kind {}", kind);
                        ms.skipRawBytes(size);
                        continue;
                    }
                    if (topClazz == CDemoPacket.class || topClazz == CDemoSendTables.class) {
                        byte[] data = ms.readRawBytes(size);
                        if (isCompressed) {
                            if (Snappy.isValidCompressedBuffer(data)) {
                                data = Snappy.uncompress(data);
                            } else {
                                throw new IOException("according to snappy, the compressed packet is not valid!");
                            }
                        }
                        GeneratedMessage message = PacketTypes.parse(topClazz, data);
                        if (message instanceof CDemoPacket) {
                            es = CodedInputStream.newInstance(((CDemoPacket) message).getData().toByteArray());
                            state = State.EMBED;
                            continue;
                        } else if (message instanceof CDemoSendTables) {
                            es = CodedInputStream.newInstance(((CDemoSendTables) message).getData().toByteArray());
                            state = State.EMBED;
                            continue;
                        }
                    }
                    ms.skipRawBytes(size);
                    return topClazz;

                case EMBED:
                    if (es.isAtEnd()) {
                        es = null;
                        state = State.TOP;
                        continue;
                    }
                    int subKind = es.readRawVarint32();
                    int subSize = es.readRawVarint32();
                    Class<? extends GeneratedMessage> subClazz = PacketTypes.EMBED.get(subKind);
                    if (subClazz == null) {
                        log.warn("unknown embedded message of kind {}", subKind);
                        es.skipRawBytes(subSize);
                        continue;
                    }
                    if (subClazz == CSVCMsg_UserMessage.class) {
                        byte[] subData = es.readRawBytes(subSize);
                        GeneratedMessage subMessage = PacketTypes.parse(subClazz, subData);
                        CSVCMsg_UserMessage userMessage = (CSVCMsg_UserMessage) subMessage;
                        Class<? extends GeneratedMessage> umClazz = PacketTypes.USERMSG.get(userMessage.getMsgType());
                        if (umClazz == null) {
                            log.warn("unknown usermessage of kind {}", userMessage.getMsgType());
                            continue;
                        }
                        return umClazz;
                    }
                    es.skipRawBytes(subSize);
                    return subClazz;
            }
        }
        return null;
    }

	@Override
	public void close() throws IOException {
		is.close();
	}

}
