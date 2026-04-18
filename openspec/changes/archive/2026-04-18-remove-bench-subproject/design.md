## Context

The `bench/` subproject and `src/jmh/` JMH harness were created when clarity-examples was the home for all benchmarking work. That work has since been extracted into a dedicated `clarity-bench` repository (`/home/spheenik/projects/clarity/clarity-bench`) with proper JMH infrastructure, multi-version support, and a managed replay corpus. The examples-repo copies are now dead weight.

Additionally, CLAUDE.md documentation across three projects (`clarity-examples`, `clarity`, `clarity-bench`) is inconsistent: it either references the removed code or omits the new home entirely.

## Goals / Non-Goals

**Goals:**
- Remove `bench/` subproject and `src/jmh/` from `clarity-examples`
- Remove JMH plugin wiring from root build files
- Leave `clarity-examples` scoped to teaching code, reproducers, and maintainer tools
- Bring all three CLAUDE.md files into alignment with the current reality

**Non-Goals:**
- Migrating benchmark content to `clarity-bench` (already done)
- Changing any code in `clarity-bench`
- Modifying the `add-entity-bindings-cs2-state` change — its task 6 (cs2statebench) must be updated separately to remove the bench/ reference

## Decisions

**Delete, don't archive.** The three bench examples (`entitybaseline`, `eventdispatchbench`, `propertychangebench`) have direct equivalents in `clarity-bench` (`ParseBench`, `DispatchBench`, `PropertyChangeBench`). No content is lost; no archival is needed.

**Root `build.gradle.kts` cleanup scope.** The JMH plugin (`me.champeau.jmh`) and its dependency block exist solely for `src/jmh/`. Once `src/jmh/` is deleted, remove the plugin, the JMH dependency lines, and the `"bench"` entry in `verifyExampleNames`. Leave the rest of the root build file untouched.

**`clarity-bench/CLAUDE.md` is a new file.** The project has a thorough README already; CLAUDE.md should be a shorter maintainer-focused companion: how to run, how to publish a candidate, key flags, replay corpus policy. Avoids duplicating the README.

## Risks / Trade-offs

- `add-entity-bindings-cs2-state` tasks.md references `bench/` (task 6, task 5.4, task 7.2/7.3). Those tasks are not yet implemented, so removing the subproject now means those task descriptions will be wrong. That change's tasks.md should be updated as a follow-up — it is out of scope here to avoid coupling two unrelated changes.
- No runtime risk: `bench/` has no consumers in the project graph.
