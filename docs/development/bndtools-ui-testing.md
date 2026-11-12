---
layout: bnd
title: bndtools UI Testing
parent: Developing bnd and Bndtools
nav_order: 7
---

# bndtools UI Testing

SWTBot tests exercise real SWT widgets in an Eclipse workbench: repository
views, Bnd editor pages, selection dialogs, drag and drop, context menus, and
the workspace wizard. Use them when changing Bndtools UI behavior that a
plain JUnit test cannot verify.

This inventory was checked against
[master `7068f88ec847662ed382c93ff8c6cca6e69c6e88`](https://github.com/bndtools/bnd/tree/7068f88ec847662ed382c93ff8c6cca6e69c6e88)
on **2026-11-12**. There are **35 test methods in six SWTBot suite classes**:
31 functional tests and four opt-in performance tests. A separate workspace
wizard class has one SWTBot widget/screenshot test and three filter-logic tests.
The inventory below includes all of them, not only classes named `SwtbotTest`.

## Prerequisites and test isolation

- Use Java 17 or newer and a bnd/Bndtools development checkout. See the
  [contribution guide](https://github.com/bndtools/bnd/blob/master/CONTRIBUTING.md)
  for importing and building the workspace.
- Use an interactive desktop with the native SWT libraries for your OS and
  architecture. Linux needs a graphical display and GTK; a CI virtual display
  must provide these too. A headless Java process alone is not sufficient.
- Build the local snapshot bundles before launching from the CLI. In Eclipse,
  let the imported bnd workspace finish building first.
- Dependency resolution needs the configured Eclipse and
  [SWTBot P2 repositories](https://github.com/bndtools/bnd/blob/master/cnf/ext/swtbot.bnd)
  unless dependencies are already cached. The Maven context-menu suite also
  requires Maven Central at test time. The workspace wizard uses its configured
  template catalog.
- Run only one UI test launch at a time. Do not interact with its workbench or
  clipboard while tests are running.

The six suites live in
[`bndtools.core.test/src/bndtools/core/test/ui/swtbot`](https://github.com/bndtools/bnd/tree/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot).
They are packaged in
[`bndtools.core.test.swtbot.tests`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/swtbot.tests.bnd),
separate from the ordinary integration-test bundle.
[`test.swtbot.shared.bndrun`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/test.swtbot.shared.bndrun)
uses the rendered launcher, `bndtools.core.test.launch.rendered`, instead of the
ordinary launcher's `NullContextPresentationEngine`. The JUnit Platform tester
runs on a separate thread so SWT can keep processing UI events.

[`@WorkbenchTest`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/utils/WorkbenchTest.java)
serializes access to the test workspace.
Its [extension and importer](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/utils/WorkspaceImporter.java)
delete existing projects in the **launched test workspace**, then import each
class's fixture from
[`resources/workspaces/ui/swtbot`](https://github.com/bndtools/bnd/tree/master/bndtools.core.test/resources/workspaces/ui/swtbot).
The supplied launches use `bndtools.core.test/generated/workspace`.
**Never point `osgi.instance.area` at your development workspace or a workspace
containing personal projects.** Concurrent launches also conflict on this directory.

[`AbstractFeatureSwtbotTest`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot/AbstractFeatureSwtbotTest.java)
is shared infrastructure, not another suite. It sets a 30,000 ms default widget
timeout and a 20 ms playback delay, closes Welcome if present, opens the fixture
project's Bnd editor, and saves/closes editors after each test. Tests wait for
asynchronous repository updates rather than assuming widgets are immediately ready.

## Execute directly

### From Eclipse/Bndtools

In the imported `bndtools.core.test` project, right-click the matching launch
file and choose **Run As > Bnd OSGi Test Launcher**. For breakpoints, use
**Debug As > Bnd OSGi Test Launcher**. These tests need their OSGi runtime;
**Run As > JUnit Test** on the source class is not an equivalent launch.

| Platform | Functional launch |
|---|---|
| Windows x86-64 | [test.swtbot.win32.x86_64.bndrun](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/test.swtbot.win32.x86_64.bndrun) |
| Linux x86-64 | [test.swtbot.gtk.linux.x86_64.bndrun](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/test.swtbot.gtk.linux.x86_64.bndrun) |
| macOS Apple Silicon | [test.swtbot.cocoa.macosx.aarch64.bndrun](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/test.swtbot.cocoa.macosx.aarch64.bndrun) |

There is no supplied SWTBot launch for Intel macOS or other architectures.
Do not substitute a launch with incompatible native bundles. The supplied
macOS launch already sets `-XstartOnFirstThread`.

For one functional class or method, create a local `.bndrun` alongside these
files, include the matching platform launch, and add a tester filter:

```properties
-include: ~test.swtbot.win32.x86_64.bndrun
-runproperties.selection: \
    tester.names=bndtools.core.test.ui.swtbot.RepositoryFilterSwtbotTest
```

For a method, use
`bndtools.core.test.ui.swtbot.RepositoryFilterSwtbotTest:repositoriesViewFilters`.
Keep such personal launch files out of your contribution unless they are
intended as shared configurations. Do not apply this early-discovery filter to
the performance launch; see [performance runs](#performance-runs).

### From the command line

Run these **PowerShell commands from the repository root**. Build the test
project, its dependencies, and the CLI without starting integration tests:

```powershell
.\gradlew.bat :bndtools.core.test:build :biz.aQute.bnd:build -x test -x testOSGi
```

Then run the functional Windows suite:

```powershell
java -jar .\biz.aQute.bnd\generated\biz.aQute.bnd.jar runtests --dir bndtools.core.test --workspace .. --reportdir generated\swtbot-reports test.swtbot.win32.x86_64.bndrun
```

Run one functional class:

```powershell
java -jar .\biz.aQute.bnd\generated\biz.aQute.bnd.jar runtests --dir bndtools.core.test --workspace .. --reportdir generated\swtbot-reports --tests bndtools.core.test.ui.swtbot.RepositoryFilterSwtbotTest test.swtbot.win32.x86_64.bndrun
```

Run one method:

```powershell
java -jar .\biz.aQute.bnd\generated\biz.aQute.bnd.jar runtests --dir bndtools.core.test --workspace .. --reportdir generated\swtbot-reports --tests bndtools.core.test.ui.swtbot.FeatureBuildPathWizardSwtbotTest:addFeatureViaWizard_writesCanonicalClause test.swtbot.win32.x86_64.bndrun
```

Replace the class and method with any functional entry below. All six suite
classes share the package prefix `bndtools.core.test.ui.swtbot`.
The CLI method selector uses **`Class:method`**, not Gradle's `Class.method`.
`--dir` selects the launch-file directory; `--workspace ..` selects the
repository's bnd workspace relative to that directory.

On Linux or macOS, use the matching launch from the platform table:

```bash
./gradlew :bndtools.core.test:build :biz.aQute.bnd:build -x test -x testOSGi
java -jar biz.aQute.bnd/generated/biz.aQute.bnd.jar runtests \
  --dir bndtools.core.test --workspace .. \
  --reportdir generated/swtbot-reports test.swtbot.gtk.linux.x86_64.bndrun
```

For Apple Silicon, replace the final filename with
`test.swtbot.cocoa.macosx.aarch64.bndrun`.

The standard
[`:bndtools.core.test:testOSGi` task](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/build.gradle)
selects the ordinary platform integration launch, **not the SWTBot suite**.
It is also disabled for local Windows runs unless `CI=true`. Neither a skipped
task nor `gradlew test --tests ...` proves that these rendered UI tests ran.
Use the explicit SWTBot launch above instead.

## Functional test inventory

### Eclipse features on the build path

Both classes use the local `features` fixture: `test.feature` version `1.0.0`
with member bundles `feature.member.a` and `feature.member.b`.

| Class / method | What it verifies |
|---|---|
| [FeatureBuildPathWizardSwtbotTest](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot/FeatureBuildPathWizardSwtbotTest.java): `addFeatureViaWizard_writesCanonicalClause` | Opens Build > Add Bundle, selects the feature, clicks Add and Finish, then saves. The file contains `test.feature`, version `1.0.0`, and `type=org.eclipse.update.feature` (quoted forms are accepted). |
| [FeatureBuildPathDndSwtbotTest](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot/FeatureBuildPathDndSwtbotTest.java): `dragFeatureToBuildPath_writesCanonicalClause` | Drags the feature from Repositories to the Build Path table and saves. The file contains the feature identity and canonical type, not legacy `feature:` or `feature=true` forms. This method does not separately assert the saved version. |
| `FeatureBuildPathDndSwtbotTest`: `featureVisibilityAndResolution_workForP2AndOSGi` | Exercises both the P2 fixture and an OSGi repository backed by its generated index. Feature version children are populated and selection exposes `test.feature` capabilities in the Resolution view. Hiding features removes them from both repositories while member bundles remain; showing features restores them. |

The drag-and-drop test targets **Build Path**, not Run Bundles. Sharing a drop
handler with another editor section is not direct test coverage of that section.
The visibility test checks displayed capabilities, not a complete resolve-and-run.

### Repository filtering

[`RepositoryFilterSwtbotTest`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot/RepositoryFilterSwtbotTest.java)
uses the local `filter` fixture with five bundles. Typing `rcp` or `RCP` must
show exactly `com.acme.RCPTools`, `org.example.rcp.core`, and
`org.example.rcp.ui`. The UI must expand the repository itself; the test does
not manually expand it. Loading/failure placeholder rows must disappear.

| Method | Surface and additional assertions |
|---|---|
| `projectBuildPathWizardFilters` | Build > Add Bundle. The root starts collapsed; changing the filter to `util` shows only `org.example.util`; clearing restores the collapsed state. |
| `addBundleRequirementWizardFilters` | Run > Add Bundle Requirement. The same matching, filter-change, and expansion-restoration assertions as the build-path wizard. |
| `runPageBrowseReposFilters` | Run page's Browse Repos tree. Checks lowercase and uppercase matching; does not test clearing the filter. |
| `repositoriesViewFilters` | Repositories view. Checks both cases, changes to `util`, then clears. A second applied filter must not replace the original collapsed expansion state. |

### Maven repository context menus

[`MavenRepositoryContextMenuSwtbotTest`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot/MavenRepositoryContextMenuSwtbotTest.java)
has **24 methods**. The `maven` fixture provides a normal Maven repository and
a strict-checksum repository for `org.osgi:org.osgi.util.function:1.1.0`.
The strict fixture trusts the binary artifact but has no trusted source-JAR
checksum. The entire class is skipped by a JUnit assumption when Maven Central
is unreachable; a skipped run is not a successful coverage check.

Most methods check menu labels or enabled entries, **not execution of every
action**. Clipboard and cache methods explicitly execute the actions described.
Updates and source availability depend on Maven Central.

| Method | Assertion |
|---|---|
| `repositoryOffersOpenDefinition` | Repository menu contains Open definition. |
| `repositoryOffersCopyInfo` | Repository clipboard submenu contains Copy info. |
| `repositoryOffersCopyBundlesWithSelfImports` | Clipboard submenu offers bundles with substitution packages (self-imports). |
| `repositoryOffersUpdateRevisions` | Update Revisions offers higher MICRO, MINOR, and MAJOR revisions. |
| `repositoryOffersDryRunUpdateRevisions` | All three revision levels offer dry-run-to-clipboard entries. |
| `repositoryOffersConvert` | Convert submenu contains Text <--> pom.xml. |
| `repositoryOffersCreateTrustedChecksumsFile` | Repository menu offers trusted-checksum-file creation. |
| `bundleOffersCopyInfo` | Bundle clipboard submenu contains Copy info. |
| `bundleOffersDeleteAllFromIndex` | Bundle menu contains Delete All from Index. |
| `revisionOffersRefreshCache` | Refresh Cache (re-download) exists and is enabled. |
| `revisionOffersClearFromCache` | Clear from Cache exists and is enabled. |
| `revisionOffersDeleteFromIndex` | Delete from Index exists and is enabled. |
| `revisionOffersAddCompileDependencies` | Add Compile Dependencies exists and is enabled. |
| `revisionOffersAddRuntimeDependencies` | Add Runtime Dependencies exists and is enabled. |
| `revisionOffersUpdateToNewerRevision` | Menu offers an update or an `Update [...]` explanation of unavailability. |
| `revisionOffersAddSources` | Add Sources exists and is enabled. |
| `revisionOffersCopyBsnAndVersion` | Clicking Copy bsn+version writes text beginning with the expected BSN and version. |
| `revisionOffersCopyInfo` | Clicking Copy info writes text containing the BSN. |
| `revisionOffersCopyGav` | Clicking Copy GAV writes the expected group/artifact and version. |
| `revisionOffersCopyCompileDependencies` | Clipboard submenu contains the existing label `Copy Compile Dependecies to clipboard`. |
| `clearFromCacheRemovesCachedArtifact` | Waits for the artifact, clicks Clear from Cache, and checks that the cached JAR disappears. |
| `refreshCacheDownloadsArtifactAgain` | Clicks Refresh Cache (re-download) and waits for the cached JAR to exist. |
| `strictRepositoryRevisionKeepsItsActions` | Opens the strict revision menu twice. Probing sources, including a cached source JAR without a trusted checksum, must not remove revision actions. |
| `strictRepositoryRevisionKeepsItsClipboardEntries` | The strict repository still offers Copy bsn+version, Copy info, and Copy GAV. |

## Performance runs

The four performance methods have `@Tag("performance")` and require the exact
system property `bndtools.test.performance=true`. Functional launches leave it
unset, so these tests are skipped. Merely selecting a performance class does
not enable it.

On Windows, launch
[`test.swtbot.perf.win32.x86_64.bndrun`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/test.swtbot.perf.win32.x86_64.bndrun)
from Eclipse, or run:

```powershell
java -jar .\biz.aQute.bnd\generated\biz.aQute.bnd.jar runtests --dir bndtools.core.test --workspace .. --reportdir generated\swtbot-perf-reports test.swtbot.perf.win32.x86_64.bndrun
```

This runs functional tests **and both performance classes**. Keep the launch
unfiltered: its configuration explicitly warns that running performance tests
first with `tester.names` can race Bndtools core activation. Do not add `--tests`
to this command as a shortcut. For Linux/macOS, create a local launch including
the corresponding functional platform file and add:

```properties
-runproperties.perf: \
    bndtools.test.performance=true
```

### Repository selection and Run page responsiveness

[`RepoBundleSelectionPerfSwtbotTest`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot/RepoBundleSelectionPerfSwtbotTest.java)
uses the `perf` fixture and generates an OSGi XML index of **5,000 bundles**.
The default filter `bundle.0123` matches exactly **10 bundles**; no bundle JAR
downloads are needed for that synthetic index.

| Method | Measured behavior |
|---|---|
| `openAndFilter` | Opens Build > Add Bundle, waits for collapsed repository roots, enters the filter per keystroke, checks exact matching children, clears back to collapsed, and reopens/refilters to check cached contents. Logs cached reopen/filter timing but does not impose a separate limit on it. |
| `addBundleRequirementOpensAndFiltersAsynchronously` | Opens a requirement-phase repository-selection wizard directly, measures shell opening and background root loading, then checks matching filtered children. It does not test the Run toolbar entry point; the functional suite does. |
| `runPageRepositoryBrowserLoadsAndFiltersAsynchronously` | Opens Run, checks immediate typed feedback and background Browse Repos filtering. Requires at least the expected identity-match row count, checks a sample of up to 20 rows for matches and no status placeholders, and requires an identity match in that sample. Unlike the dialog tests, it does not inspect every row for exact equality. |

### Workspace template filtering

[`WorkspaceTemplateFilterPerfSwtbotTest`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/ui/swtbot/WorkspaceTemplateFilterPerfSwtbotTest.java):
`filtersWorkspaceTemplates` opens New Workspace, waits for templates, chooses
a substring matching a nonempty proper subset of the loaded catalog, and
checks the exact filtered row count and case-insensitive matching in columns
1 through 3. It measures filter settling, not initial catalog download or
workspace creation. There is no fixed template-count fixture.

### Default limits and overrides

All limits below are **milliseconds**, accepted inclusively (`<=`). The tests
print dataset sizes, queries, match counts, and measured timings to the console.

| System property | Default | Applies to |
|---|---|---|
| `bndtools.test.performance.open.ms` | 5,000 | Dialog opening and Run page activation |
| `bndtools.test.performance.load.ms` | 20,000 | Background repository-root/content loading |
| `bndtools.test.performance.filter.ms` | 5,000 | Dialog filtering and clearing the Build Path filter |
| `bndtools.test.performance.run.filter.ms` | 8,000 | Total Run page filtering time |
| `bndtools.test.performance.template.filter.ms` | 5,000 | Workspace template filtering |

Run page typed feedback also has a fixed **500 ms** limit. It is not controlled
by the properties above. To compare machines or investigate a regression,
set overrides in the **launched runtime**, not only the parent CLI JVM:

```properties
-runproperties.performance-limits: \
    bndtools.test.performance.open.ms=5000,\
    bndtools.test.performance.filter.ms=5000
```

Record overrides with results; raising a limit is not evidence of meeting the
default responsiveness target.

### Real Eclipse SimRel dataset

The repository performance class can instead load a local OSGi XML index,
including `.xml.gz`, using `BNDTOOLS_PERF_INDEX` or the system property
`bndtools.test.performance.index` (which takes precedence).
For the supplied
[`test.swtbot.perf.simrel.win32.x86_64.bndrun`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/test.swtbot.perf.simrel.win32.x86_64.bndrun),
clone [bndtools/workspace-templates](https://github.com/bndtools/workspace-templates),
then set an absolute index path:

```powershell
$env:BNDTOOLS_PERF_INDEX = 'C:\git\workspace-templates\eclipse\eclipse_4.41_simrel_2026-09\cnf\ext\eclipse_4.41_simrel_2026-09_p2.index.xml.gz'
java -jar .\biz.aQute.bnd\generated\biz.aQute.bnd.jar runtests --dir bndtools.core.test --workspace .. --reportdir generated\swtbot-simrel-reports test.swtbot.perf.simrel.win32.x86_64.bndrun
```

That launch requires an external index and uses filter `HA`; it must not silently
fall back to synthetic data. Expected bundle/feature matches are computed from
the index, not assumed to be 10. A custom external index defaults to filter `ha`
unless `bndtools.test.performance.filter` overrides it, and must have both
matching and nonmatching bundles. The external index changes the repository
dataset, not the workspace template catalog. When launching from Eclipse,
set the environment variable in the IDE's launch environment.

## Additional workspace wizard tests

[`NewWorkspaceWizardTest`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/src/bndtools/core/test/wizards/newworkspace/NewWorkspaceWizardTest.java)
is in package `bndtools.core.test.wizards.newworkspace`, **outside** the SWTBot
suite bundle. Its four methods are:

| Method | What it verifies |
|---|---|
| `plainTextGlobMatchesSubstring` | Plain `eclipse` matches Eclipse Workspace case-insensitively but not workspace alone. |
| `wildcardGlobMatchesExpectedText` | `*eclipse?` requires one character after eclipse. |
| `regexCharactersRemainLiteral` | Regex characters in `a[b].c` stay literal rather than becoming regex operators. |
| `wizardTableShowsVisibleIndexNumbers` | Opens a wizard shell, uses SWTBot to check nonempty rows, positive index-column width, row data, numeric visible index, and nonblank name. Captures the shell to `new-workspace-wizard.png` and checks that the PNG is nonempty. |

Use its dedicated
[`test.wizard.win32.x86_64.bndrun`](https://github.com/bndtools/bnd/blob/master/bndtools.core.test/test.wizard.win32.x86_64.bndrun)
with **Bnd OSGi Test Launcher**, or:

```powershell
java -jar .\biz.aQute.bnd\generated\biz.aQute.bnd.jar runtests --dir bndtools.core.test --workspace .. --reportdir generated\wizard-reports test.wizard.win32.x86_64.bndrun
```

This launch selects the class from the ordinary `bndtools.core.test.tests`
bundle and uses its ordinary launcher. The test creates its own visible shell;
it does not require a rendered workbench as the six SWTBot suites do.
To execute just the widget test, add
`--tests bndtools.core.test.wizards.newworkspace.NewWorkspaceWizardTest:wizardTableShowsVisibleIndexNumbers`
before the launch filename. There is no dedicated wizard launch for other
platforms; use a local equivalent including the ordinary platform test launch
and the same `tester.names` filter.

## Results and troubleshooting

- Eclipse displays results in the JUnit view and launch console.
- The CLI `runtests` command writes XML reports plus `summary.xml` in the
  selected report directory under `bndtools.core.test`. **It replaces that
  report directory on each run**; use only a dedicated test-output directory.
  Check executed/skipped counts as well as failures.
- The wizard screenshot is normally
  `bndtools.core.test/screenshots/new-workspace-wizard.png` for local generated
  bundles; the test derives the directory from the test bundle's installed
  location. The six suites do not automatically capture failure screenshots.
- A missing widget can mean the wrong launch (ordinary instead of rendered),
  an incompatible SWT platform, an unfinished build, or repository loading
  failure. Inspect the launch console and the test workspace's
  `generated/workspace/.metadata/.log` before increasing timeouts.
- If Windows reports locked test-workspace files, stop the specific previous
  test launch and close its workbench before retrying. Do not run overlapping
  tests or delete another worktree's workspace.
- If a local snapshot or external bundle is missing, rebuild the affected
  projects and resolve the matching platform `.bndrun` in Bndtools. The CLI
  equivalent is `bnd resolve resolve -W -b <file.bndrun>`. Inspect any resulting
  run-bundle changes before committing them.

See also [Development notes]({{ '/development/development-notes.html' | relative_url }})
for JUnit patterns and [Quality requirements]({{ '/development/architecture/10-quality-requirements.html' | relative_url }})
for the broader architecture acceptance criteria.
