package skadistats.clarity.examples.s2effectdispatch;

import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.stringtables.OnStringTableCreated;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.wire.shared.s2.proto.S2TempEntities.CMsgEffectData;
import skadistats.clarity.wire.shared.s2.proto.S2TempEntities.CMsgTEEffectDispatch;

/**
 * Provides the {@link OnEffectDispatch} event for Source 2 replays.
 *
 * <p>In S2, the engine sends temp-entity effects through one polymorphic
 * message ({@code CMsgTEEffectDispatch}) carrying a {@code CMsgEffectData}
 * payload. The dispatch handler is selected by the {@code effectname}
 * uint32 field, which is an index into the {@code EffectDispatch} string
 * table — itself shipped in the replay's stringtables packet.</p>
 *
 * <p>This processor caches the {@code EffectDispatch} table when it is
 * created and resolves every incoming dispatch to its handler name
 * before raising the event. The 64-bit {@code effectindex} hash is left
 * untouched — consumers who need the specific particle resource id read
 * it directly off the {@link CMsgEffectData}.</p>
 *
 * <p>This is engine-scoped implicitly: {@code CMsgTEEffectDispatch} only
 * exists in S2 replays, so the {@code @OnMessage} handler is silently
 * inactive on S1 replays.</p>
 */
@Provides({ OnEffectDispatch.class })
@UsesStringTable("EffectDispatch")
public class EffectDispatches {

    private StringTable dispatchTable;
    private Event<OnEffectDispatch> evDispatch;

    @Initializer(OnEffectDispatch.class)
    public void initOnDispatch(final Context ctx, final EventListener<OnEffectDispatch> el) {
        evDispatch = ctx.createEvent(OnEffectDispatch.class, String.class, CMsgEffectData.class);
    }

    @OnStringTableCreated
    public void onStringTableCreated(int tableNum, StringTable table) {
        if ("EffectDispatch".equals(table.getName())) {
            dispatchTable = table;
        }
    }

    @OnMessage(CMsgTEEffectDispatch.class)
    public void onEffectDispatch(CMsgTEEffectDispatch m) {
        if (evDispatch == null || !m.hasEffectdata()) return;
        var data = m.getEffectdata();
        String kind = null;
        if (dispatchTable != null && data.hasEffectname()) {
            int idx = data.getEffectname();
            if (dispatchTable.hasIndex(idx)) {
                kind = dispatchTable.getNameByIndex(idx);
            }
        }
        evDispatch.raise(kind, data);
    }
}
