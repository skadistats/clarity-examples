package skadistats.clarity.examples.shared;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an example's {@code Main} class so that {@link ExampleLauncher} can list and start it.
 * Classes carrying this annotation are indexed at compile time (ClassIndex) and must have a
 * {@code public static void main(String[])}.
 * <p>Every {@code Main} in {@code examples/}, {@code repro/} and {@code dev/} carries it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@IndexAnnotated
public @interface Example {
    /** Unique launcher identifier; first argument of {@link ExampleLauncher}. Conventionally the example's directory name. */
    String name();
    /** One-line description shown in the launcher and in the example list. */
    String description();
    /** Group under which the example is listed. */
    Category category();
}
