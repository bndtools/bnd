---
title: Bndtools Rebuild Trigger Policy Plugin
layout: bnd
parent: Plugins
nav_order: 15
---

# Rebuild Trigger Policy (Bndtools Eclipse Plugin)

> **Note:** This is an **optional Bndtools Eclipse IDE optimization**, not a core bnd feature. The underlying mechanism is the [JAR Lifecycle Listener](jar-lifecycle.html) plugin interface.

## Problem

In bnd workspaces, downstream projects check whether to rebuild by inspecting their dependency JAR's `lastModified` timestamp. By default, **every rebuild updates this timestamp**, causing all downstream projects to rebuild even when nothing relevant to them changed.

This cascade rebuild problem is particularly painful in incremental build scenarios (IDE, CI) where many small changes accumulate, causing unnecessary full rebuilds.

## Solution

The **rebuild trigger policy** is a Bndtools plugin that preserves JAR timestamps when the rebuild does not produce a meaningfully different artifact. It uses two levels of comparison:

1. **Content digest** – SHA-1 of entire JAR (byte-for-byte identical)
2. **API digest** – SHA-1 of exported API surface (public/protected types, methods, fields in `Export-Package`)

If either matches the previous build, the timestamp is preserved, preventing downstream cascade rebuilds.

## Policies

| Policy | Behavior |
|--------|----------|
| `always` (default) | Every rebuild updates the JAR timestamp. All downstream projects consider the dependency stale and rebuild. |
| `api` | Preserves timestamp when content is byte-identical OR exported API is unchanged. Only consuming projects rebuild when the API actually changes. |

## How It Works

The Bndtools `RebuildTriggerPolicyListener` implements the [JAR Lifecycle Listener](jar-lifecycle.html) interface:

**Before write phase:**
- Compute SHA-1 digest of JAR bytes (timeless, excludes embedded timestamps)
- Compute SHA-1 digest of exported API surface (using `DiffPluginImpl`)
- Store in context for after phase

**After write phase:**
- Load previous digests from sidecar files (`.digest`, `.api-digest`)
- If content digest matches → **preserve timestamp** (fast path, no changes)
- Else if API digest matches → **preserve timestamp** (content changed but API didn't)
- Else → **use new timestamp** (both content and API changed)
- Store new digests for next build

## Sidecar Files

When the `api` policy is active, Bndtools stores two small sidecar files next to each output JAR:

```
myproject.jar
myproject.jar.digest          # SHA-1 of entire JAR content (hex-encoded)
myproject.jar.api-digest      # SHA-1 of exported API surface (hex-encoded)
```

These files are regenerated on every build and should be added to `.gitignore`.

> **Note:** Only _exported_ packages are analyzed. Changes to private packages or internal implementation classes are intentionally invisible (because downstream bundles cannot legally depend on them), so the timestamp is preserved.

## Eclipse Bndtools Configuration

Bndtools Eclipse IDE provides a UI to enable this optimization:

**Preferences → Bndtools → Build → Rebuild Trigger Policy**

Options:
- **"Always rebuild (default)"** – Standard behavior; all changes trigger downstream rebuilds
- **"Optimized (skip if API unchanged)"** – Enable digest-based timestamp preservation

### Current Status

The Bndtools Explorer toolbar displays the current rebuild policy status:

- **"Rebuild: Always"** – Default policy is active
- **"Rebuild: Optimized"** – API-based optimization is enabled

Click the status to open preferences.

## Workspace-Level Configuration

The policy is configured programmatically at the workspace level (not via `build.bnd` properties):

This is typically done by Bndtools before building, based on the user's Eclipse preferences.

## Future Extensibility

The plugin-based design enables other build tools to implement similar optimizations without modifying core bnd:

## Limitations

This pragmatic approach trades perfect accuracy for simplicity:

- ✅ **Works well for** typical Eclipse IDE incremental builds where small changes accumulate
- ❌ **Not recommended for** highly dynamic build environments with complex classpath interdependencies and heavy use of bnd features like `-includeresource` or `-conditionalpackage` which may not always be triggered for rebuild by this plugin. If you find problems, just switch back to the default 'always' policy or do a full workspace clean build
- ❌ **Not recommended for** scenarios requiring perfect timestamp accuracy for external build tools

In practice, this optimization provides substantial benefits by preventing most unnecessary rebuilds while maintaining reliability.

## See Also

- [JAR Lifecycle Listener](jar-lifecycle.html) – Core plugin interface
- [Plugin Overview](00-overview.html)
- [Build Chapter](../chapters/150-build.html) – General build concepts
