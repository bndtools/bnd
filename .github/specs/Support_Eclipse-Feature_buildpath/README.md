# Support for Eclipse Features on -buildpath

## Overview

This spec describes the support for referencing Eclipse features in the bnd `-buildpath` instruction (and all other container paths), implemented for issue [#7322](https://github.com/bndtools/bnd/issues/7322). It builds on top of the p2 feature indexing introduced with [PR #7124](https://github.com/bndtools/bnd/pull/7124) (see `PR7124_Support-Eclipse-Features_p2-repos.md`) and the resolver support from [#7279](https://github.com/bndtools/bnd/issues/7279)/[#7296](https://github.com/bndtools/bnd/pull/7296).

**Canonical clause syntax:**

```properties
-buildpath: \
    org.eclipse.e4.rcp;version='4.40.0.v20260516-1214';type=org.eclipse.update.feature
```

A feature is a **container of bundles**: on a path it expands to its member bundles — the `<plugin>` references of its `feature.xml` and, recursively, the members of its `<includes>` referenced features. `<requires>` imports are dependencies, not members, and are **not** expanded.

## The Identity Problem

OSGi uses bsn+version as the unique identifier of a bundle. This is **not sufficient** for Eclipse features: a feature and a bundle may share the same id and version in a p2 repository (e.g. `org.eclipse.emf.ecore` exists both as bundle and as feature). All lookups therefore additionally honor the identity type:

```java
(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, "org.eclipse.update.feature")
```

| Aspect | Bundle entry | Feature entry |
|---|---|---|
| Clause | `bsn;version=V` | `id;version='V';type=org.eclipse.update.feature` |
| Version listing | `RepositoryPlugin.versions(bsn)` | `Repository.findProviders` with `(&(osgi.identity=id)(type=org.eclipse.update.feature))` |
| Fetch | `RepositoryPlugin.get(bsn, v, attrs)` | same — the `type` attribute is part of the `attrs` map and honored by `P2Indexer.get()` |
| Container | `Container.TYPE.REPO` | `Container.TYPE.FEATURE`, expands via `getMembers()` |
| Workspace projects | may shadow repo versions | never consulted (projects deliver bundles, not features) |

## Design Decisions

1. **Syntax**: the `type=org.eclipse.update.feature` attribute marks a clause as a feature reference. It matches the `-runrequires` alias vocabulary (`bnd.identity;id=…;type=…`, see `CapReqBuilder.unalias`) and flows through the existing `attrs` parameter of `RepositoryPlugin.get()` unchanged. The previously generated `feature:` bsn prefix and `feature=true` attribute are **not** supported and no longer emitted by the UI.
2. **Single parser**: `aQute.p2.provider.Feature` (from PR #7124) remains the only feature.xml parser. bndlib consumes the feature exclusively through the **repository index** using standard OSGi APIs (`Repository.findProviders`, `Resource.getRequirements`). No new bnd service interface is introduced (a `FeatureProvider` interface was already rejected in [#7133](https://github.com/bndtools/bnd/pull/7133)).
3. **Expansion scope**: `<plugin>` and `<includes>` members only, `<requires>` ignored, include recursion with cycle guard.
4. **Platform filter**: plugin/include entries carrying `os`/`ws`/`arch` attributes are skipped when they do not match the running platform.
5. **Member versions**: the version pinned in feature.xml is resolved exactly first; when absent from the repositories, the highest available version is used and a warning is issued.
6. **All paths**: `-buildpath`, `-testpath`, `-runpath` and `-runbundles` share `Project.getBundles()`, so feature clauses work on all of them.

## Index Enrichment (biz.aQute.repository)

### Requirement provenance attributes

`Feature.toResource()` stores every feature.xml reference as an `osgi.identity` requirement (PR #7124). Plugin references and `<requires><import plugin=…>` imports were structurally indistinguishable, and the parsed `os/ws/arch` data was dropped. The requirements are now enriched with **additive attributes** — the filter and directives stay byte-identical, so resolver behavior and existing indexes remain valid:

```java
// <plugin> reference
req.addAttribute(RELATION_ATTRIBUTE, RELATION_PLUGIN);   // bnd.relation=plugin
req.addAttribute("id", plugin.id);
addVersionAttribute(req, plugin.version);                // exact Version, if valid
req.addAttribute("os", plugin.os);                       // only when present
req.addAttribute("ws", plugin.ws);
req.addAttribute("arch", plugin.arch);
req.addAttribute("fragment", "true");                    // only when fragment

// <includes> reference
req.addAttribute(RELATION_ATTRIBUTE, RELATION_INCLUDE);  // bnd.relation=include
req.addAttribute("id", include.id);
req.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, "org.eclipse.update.feature");
addVersionAttribute(req, include.version);

// <requires><import> reference
req.addAttribute(RELATION_ATTRIBUTE, RELATION_REQUIRE);  // bnd.relation=require
```

The constants are owned by bndlib (`aQute.bnd.osgi.resource.ResourceUtils`) and re-exported by `Feature` for repository-module consumers:

| Constant | Value |
|---|---|
| `ResourceUtils.TYPE_ECLIPSE_FEATURE` | `org.eclipse.update.feature` |
| `ResourceUtils.FEATURE_RELATION_ATTRIBUTE` | `bnd.relation` |
| `ResourceUtils.FEATURE_RELATION_PLUGIN` | `plugin` |
| `ResourceUtils.FEATURE_RELATION_INCLUDE` | `include` |
| `ResourceUtils.FEATURE_RELATION_REQUIRE` | `require` |

The attributes use the same `id`/`version`/`type` vocabulary as the `bnd.identity` requirement alias, so consumers need **no LDAP filter parsing**.

### Index migration (self-healing)

Cached `index.xml.gz` files written before this change lack the provenance markers. `P2Indexer.readRepository()` detects such stale indexes and re-indexes once:

```java
private boolean requiresReindex(List<Resource> resources) {
    for (Resource resource : resources) {
        if (!ResourceUtils.hasType(resource, ECLIPSE_FEATURE_CAPABILITY))
            continue;
        List<Requirement> requirements = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        if (requirements.isEmpty())
            continue;
        boolean marked = requirements.stream()
            .anyMatch(req -> req.getAttributes().containsKey(Feature.RELATION_ATTRIBUTE));
        if (!marked)
            return true; // stale cache, re-index
    }
    return false;
}
```

### Cache link collision

`P2Indexer.get()` linked every artifact as `<bsn>-<version>.jar`. A feature sharing bsn+version with a bundle would overwrite the bundle in the cache (and vice versa). Feature links now use a distinct suffix: `<bsn>-<version>.feature.jar`.

## Core Implementation (biz.aQute.bndlib)

### Container.TYPE.FEATURE

`Container.TYPE` gained the `FEATURE` constant. Feature containers behave like `LIBRARY` containers everywhere a container can expand:

- `Container.getMembers()` — expansion entry point, delegates to `Project.getFeatureMembers(this, visited)` with a cycle guard set
- `Container.contributeFiles()` / `Container.flatten()` — treat `FEATURE` like `LIBRARY`
- `RepoCollector` (`${repo}` macro) and `SubsystemExporter` — expand members

`Project.toContainer()` creates a `FEATURE` container when the clause attrs carry `type=org.eclipse.update.feature`. Because `Project.getBundles()` flattens `found.getMembers()` into the result, downstream consumers (classpath, launcher, Eclipse classpath container) only ever see the expanded member containers — the feature container itself never surfaces on a path.

### Type-aware version lookup

`Project.getBundle()`'s range-search path used `RepositoryPlugin.versions(bsn)`, which only lists bundles (features are intentionally filtered out by `P2Indexer.versions()` since PR #7124 bugfixes). For typed lookups the versions are collected via the OSGi Repository API instead:

```java
private SortedSet<Version> versionsOf(RepositoryPlugin plugin, String bsn, String requestedType) throws Exception {
    if (requestedType == null)
        return plugin.versions(bsn);                    // unchanged bundle path
    SortedSet<Version> versions = new TreeSet<>();
    if (!(plugin instanceof Repository repo))
        return versions;                                // repo cannot answer typed queries
    Requirement identity = identityRequirement(bsn, requestedType, null);
    // (&(osgi.identity=<bsn>)(type=<requestedType>))
    ...collect CAPABILITY_VERSION_ATTRIBUTE of matching identity capabilities...
}
```

The workspace-project version merge is skipped for typed lookups — a workspace project with the same name must not shadow a feature. The `EXACT` strategy path needs no change: `plugin.get(bsn, version, attrs)` already receives the `type` attribute and `P2Indexer.findResource()` filters by it.

### Member expansion

`Project.getFeatureMembers(Container feature, Set<String> visitedFeatures)`:

1. Cycle guard on `id:version` (warning on repeated occurrence, empty result)
2. Locate the feature `Resource` via `findFeatureResource(bsn, version)` — first repository implementing `org.osgi.service.repository.Repository` that answers `(&(osgi.identity=…)(type=org.eclipse.update.feature)(version=…))`
3. Iterate `resource.getRequirements("osgi.identity")`:
   - no `bnd.relation` attribute → skipped (counted; a fully unmarked requirement set produces a "refresh the repository" warning for pre-enrichment indexes of non-p2 origin)
   - `bnd.relation=require` → ignored (dependency, not member)
   - platform mismatch (`os`/`ws`/`arch` vs `EclipsePlatform.CURRENT`) → skipped
   - `bnd.relation=plugin` → resolve bundle: exact version first, fallback `Strategy.HIGHEST` with warning
   - `bnd.relation=include` → resolve nested feature (attrs carry `type=`), recurse via `member.getMembers(visitedFeatures)`
   - `resolution:=optional` members that cannot be resolved are skipped silently
4. Members are de-duplicated; unresolvable mandatory members surface as `ERROR` containers which `doPath()` reports with header/context, exactly like ordinary path entries

### Platform matching

`EclipsePlatform` (package-private) maps `os.name`/`os.arch` to the Eclipse coordinates (`win32|linux|macosx…` / `win32|gtk|cocoa` / `x86_64|aarch64|x86|…`) and matches the comma-separated filter lists of feature.xml; absent filters match any platform.

## Bndtools UI (bndtools.core)

| Component | Change |
|---|---|
| `RepositoryBundleSelectionPart` (shared by **Build Path** and **Run Bundles** panes) | Drag & drop of a `RepositoryFeature` emits the canonical clause via `RepositoryBundleUtils.convertRepoFeature()`; the old `feature:`+`feature=true` generation is gone. `handleAdd()` matches existing entries by name **and** type, so a feature no longer replaces a bundle of the same name |
| `RepositoryBundleUtils.convertRepoFeature()` | Single conversion helper: `id;version='V';type=org.eclipse.update.feature` |
| `SelectionDragAdapter` | Text drag (e.g. into the Source tab) produces the canonical clause instead of `feature:id:version` |
| `VersionedClauseLabelProvider` | Rows with `type=org.eclipse.update.feature` show the feature icon |
| `RepoBundleSelectionWizardPage` ("Add Bundle" dialog) | `RepositoryFeature`/`FeatureVersionNode` selections are convertible; the selected-set is keyed by name+type so features and bundles with equal names coexist |

Example result of both the wizard and DND flows (note that bnd quotes the attribute value when serializing):

```properties
-buildpath: test.feature;version='1.0.0';type='org.eclipse.update.feature'
```

## SWTBot UI Test Infrastructure (bndtools.core.test)

The repository previously had no SWTBot support; the existing harness runs a deliberately **non-rendered** workbench (`NullContextPresentationEngine`). The following infrastructure was added:

| Piece | Purpose |
|---|---|
| `launch.rendered.bnd` → `bndtools.core.test.launch.rendered` | `RenderedLauncher` bridges the bnd launcher main thread to the Eclipse application **without** installing the null presentation engine → real SWT widgets |
| `swtbot.tests.bnd` → `bndtools.core.test.swtbot.tests` | SWTBot test bundle (`bndtools.core.test.ui.swtbot` + a copy of the harness utils); excluded from the headless test bundle |
| `test.swtbot.shared.bndrun` | Overrides `-runrequires` (note the required `~` include: `-include: ~test.shared.bndrun`), swapping headless launch/tests for rendered launch/SWTBot tests |
| `test.swtbot.{win32.x86_64,gtk.linux.x86_64,cocoa.macosx.aarch64}.bndrun` | Platform launches with frozen `-runbundles` |
| `resources/workspaces/ui/swtbot/features/` | Template workspace: cnf configures a **local file: p2 repository** (`fixture-p2`: `p2.index`, `artifacts.xml`, two member bundles, `test.feature_1.0.0.jar`) — offline end-to-end through the real P2 indexing and enrichment |

SWTBot 4.1.0 resolves from the already-pinned `Eclipse-4_30-2023-12` pobr index — **no additional repository** in cnf.

Test classes:

- `FeatureBuildPathWizardSwtbotTest` — Build page → "Add Bundle" → select feature below "Feature Fixture" → Add/Finish → asserts the canonical clause in bnd.bnd
- `FeatureBuildPathDndSwtbotTest` — drags the feature from the Repositories view onto the Build Path table → asserts the canonical clause and the absence of `feature:`/`feature=true`

Run:

```bash
./gradlew :bndtools.core.test:testrun.test.swtbot.win32.x86_64      # Windows
./gradlew :bndtools.core.test:testrun.test.swtbot.gtk.linux.x86_64  # Linux (needs a display / xvfb-run)
```

Note: the runs are opt-in Gradle tasks (auto-registered per bndrun) and not part of `check`; enabling them on Linux CI requires re-activating `xvfb-run` in the cibuild workflow matrix.

## Tests

| Test | Module | Coverage |
|---|---|---|
| `test.FeatureBuildpathTest` | biz.aQute.bndlib.tests | Expansion incl. nested include, platform filter (`os=qnx` skipped), cycle termination, exact→highest fallback with warning, optional include skip, missing feature ERROR container. Uses a P2-mimicking fake repo (type-dispatching `get()`, bundles-only `versions()`, `findProviders`) |
| `FeatureParserTest.testMemberRelationAttributes` | biz.aQute.repository | Enrichment contract: `bnd.relation`, `id`/`version`/`type`, `os/ws/arch/fragment`, unchanged filters/directives, `resolution:=optional` |
| `FeatureBuildPathWizardSwtbotTest`, `FeatureBuildPathDndSwtbotTest` | bndtools.core.test | Rendered-workbench UI flows (see above) |
| Existing PR #7124 suites (`FeatureRequirementsInIndexTest`, `FeatureVersionFilterTest`, `FeaturePropertiesTest`, `Eclipse431FeatureParsingTest`, …) | biz.aQute.repository | Unchanged and green — enrichment is strictly additive |

```bash
./gradlew :biz.aQute.bndlib.tests:test --tests "test.FeatureBuildpathTest"
./gradlew :biz.aQute.repository:test --tests "aQute.p2.provider.FeatureParserTest"
```

## API Compatibility

All changes are additive; no existing signature changed.

| Package | Version | Additions |
|---|---|---|
| `aQute.bnd.build` | 4.7.1 → 4.8.0 | `Container.TYPE.FEATURE` |
| `aQute.bnd.osgi.resource` | 5.1.0 → 5.2.0 | `TYPE_ECLIPSE_FEATURE`, `FEATURE_RELATION_*` constants |
| `aQute.p2.provider` | 1.0.0 → 1.1.0 | `Feature.RELATION_*` constants |

`RepositoryPlugin` is untouched: the `type` attribute travels through the pre-existing `attrs`/`properties` maps, and typed version listing uses the optional `org.osgi.service.repository.Repository` capability of a repository plugin.

## Known Limitations

- Wildcard clauses (`bsn*`) do not support the `type` attribute — features must be referenced by exact id.
- `version=hash` lookups are not type-aware.
- Repositories that do not implement the OSGi `Repository` interface cannot answer typed lookups; feature clauses resolve only against repositories that index features (currently `P2Repository` and any OSGi-XML-indexed repository containing enriched feature resources).
- Feature expansion contributes member bundles only; the feature JAR itself never appears on a path.

## Summary

Eclipse features are now first-class citizens on all bnd container paths. The critical invariant, carried through every layer from clause parsing over repository lookup to the Bndtools UI:

- **Features** are identified by id+version **plus** `type=org.eclipse.update.feature`
- **Bundles** remain identified by bsn+version alone

The feature's member information is read from the (enriched) repository index that PR #7124 already builds — one parser, one source of truth, no new service interfaces, full backward compatibility.
