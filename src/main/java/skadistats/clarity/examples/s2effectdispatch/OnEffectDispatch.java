package skadistats.clarity.examples.s2effectdispatch;

import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.wire.shared.s2.proto.S2TempEntities.CMsgEffectData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Raised for every {@code CMsgTEEffectDispatch} sent by the Source 2 engine,
 * with the {@code effectname} uint32 already resolved against the
 * {@code EffectDispatch} string table to a human-readable handler name
 * (e.g. {@code ParticleEffect}, {@code csblood}, {@code Impact}).
 *
 * <p>Handler signature:</p>
 * <pre>
 * &#64;OnEffectDispatch
 * public void onEffectDispatch(String effectKind, CMsgEffectData data) { ... }
 * </pre>
 *
 * <p>{@code effectKind} is {@code null} if the {@code EffectDispatch} string
 * table is missing or the index is out of range. The full
 * {@link CMsgEffectData} (with {@code origin}, {@code angles}, {@code entity},
 * {@code effectindex} hash, etc.) is passed as the second argument.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { String.class, CMsgEffectData.class })
public @interface OnEffectDispatch {
}
