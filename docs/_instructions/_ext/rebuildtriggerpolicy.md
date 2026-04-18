---
layout: default
class: Project
title: -rebuildtriggerpolicy (always|api)
summary: Controls when a rebuilt bundle is treated as changed for downstream builds.
---

# -rebuildtriggerpolicy

## Intention

When a bundle is rebuilt in a bnd workspace, downstream projects check whether they need to rebuild by inspecting the `lastModified` timestamp of the dependency's output JAR. By default (`always`), every rebuild updates the JAR's timestamp, and all downstream projects recompile even if nothing relevant to them actually changed.

The `api` policy eliminates unnecessary cascade rebuilds by preserving the output JAR's timestamp when the rebuild did not produce a meaningfully different artifact. Two levels of comparison are performed in order:

1. **Content digest** — A stable, timeless hash of the entire JAR content (excludes timestamps embedded in entries). If the content is byte-identical to the previous build, the timestamp is preserved immediately. This covers cases such as a comment-only source change where the compiler produces identical bytecode.

2. **API digest** — A hash of the _exported API surface_: the public and protected types, methods, and fields in all packages listed in `Export-Package`. If the content changed (e.g. a method body was modified) but the exported API is identical, the timestamp is still preserved. Downstream projects that only consume the exported API will not see any change and therefore skip rebuilding.

When the timestamp is preserved, downstream projects' staleness checks find that the dependency is not newer than their own output, so no cascade rebuild is triggered — not even the recursive `isStale()` check that runs before any timestamp comparison.

The two digest sidecar files (`.digest` and `.api-digest`, stored next to the output JAR) are created on the first build with this policy and updated on every subsequent build. They are intentionally **not** created when the default `always` policy is active, to avoid polluting existing projects.

> **Note:** The `api` policy only considers _exported_ packages. Changes to private packages or internal implementation classes are intentionally invisible to this check and will preserve the timestamp, because downstream bundles cannot legally depend on them.

## Values

| Value | Description |
|-------|-------------|
| `always` | _(default)_ Every rebuild updates the JAR timestamp. All downstream projects will be considered stale and rebuild. |
| `api`    | Preserves the JAR timestamp when the rebuilt content is byte-identical **or** when the exported API surface is unchanged. Downstream projects only rebuild when the API actually changes. |

## Sidecar files

When the `api` policy is active, bnd stores two small sidecar files next to each output JAR (e.g. `myproject.jar`):

| File | Contains |
|------|----------|
| `myproject.jar.digest` | Hex-encoded SHA-1 of the entire JAR content (timeless) |
| `myproject.jar.api-digest` | Hex-encoded SHA-1 of the exported API surface |

These files are used to compare builds across sessions. They can safely be added to `.gitignore` or your VCS ignore list because they are always regenerated on the next build.

## Examples

### Enable API-level optimization globally (workspace `cnf/build.bnd`)

```properties
# Preserve JAR timestamps when only non-API implementation details change.
# Avoids rebuilding all downstream projects after minor internal edits.
-rebuildtriggerpolicy: api
```

### Enable only for Eclipse; use default (always) for Gradle and other builds

Bndtools in Eclipse typically runs incremental builds on every save. Enabling the `api` policy there prevents a change in one project from cascading through all dependents when nothing in the exported API changed. Regular Gradle CI builds, which build everything from scratch, do not need the optimization and can keep the safe default.

```properties
# In cnf/build.bnd or bnd.bnd:
-rebuildtriggerpolicy: ${if;${driver;eclipse};api;always}
```

The `${driver;eclipse}` macro evaluates to `true` when the build is driven by the Bndtools Eclipse builder, and to an empty string otherwise. The `${if;...;api;always}` macro selects `api` for Eclipse and `always` for everything else (Gradle, Maven, bnd CLI, etc.).

### Enable in a single project only

Place the instruction in the project's `bnd.bnd` file to scope the optimization to that project only:

```properties
# myproject/bnd.bnd
-rebuildtriggerpolicy: api
```

### Force all downstream rebuilds unconditionally (explicit default)

```properties
-rebuildtriggerpolicy: always
```

## Interaction with `-dependson`

The `api` policy works through the existing timestamp-based staleness infrastructure. When a dependency's JAR timestamp is preserved, a downstream project's `isStale()` check — which first recurses into each dependency and then compares timestamps — never sees a newer file and therefore does not rebuild. No additional configuration is required on the consuming project side.

## See Also

* [`-dependson`](dependson.html) — Declares explicit project build-order dependencies.
* [`-builderignore`](builderignore.html) — Excludes directories from the Eclipse/Gradle incremental builder.
* [`${driver}`](../macros/driver.html) — Macro that returns the current build driver (`eclipse`, `gradle`, `bnd`, …).
