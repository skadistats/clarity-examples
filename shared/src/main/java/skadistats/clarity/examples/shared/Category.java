package skadistats.clarity.examples.shared;

/**
 * Group of an {@link Example} in {@link ExampleLauncher}; corresponds to a project area.
 */
public enum Category {
    /** Teaching examples ({@code examples/} subproject). */
    DOCS,
    /** Issue reproducers ({@code repro/} subproject). */
    REPRO,
    /** Maintainer diagnostic tools ({@code dev/} subproject). */
    DEV,
    /** Benchmarks; not used by any example in this repository at present. */
    BENCH
}
