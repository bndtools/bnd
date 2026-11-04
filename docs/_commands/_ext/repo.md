
## deps — license/IP analysis with the Eclipse Dash License Tool

The `deps` sub-command exports the Maven coordinates (GAVs) of **all artifacts in
the workspace's Maven repositories** as a flat, newline-separated list. Macros in
the repository index files (e.g. `${junit.version}` in `cnf/ext/central.mvn`) are
resolved through the workspace, so the output reflects the actual, curated set of
third-party dependencies the workspace pulls in.

The output format is exactly what the
[Eclipse Dash License Tool](https://github.com/eclipse-dash/dash-licenses)
(`dash-licenses`) consumes, which makes it the bridge between the bnd workspace
model and Eclipse IP / license vetting.

### Examples

Write the GAV list of every Maven repository to a file:

```
bnd repo deps -o deps.txt
```

Feed it to the Dash License Tool to produce a `DEPENDENCIES` summary that marks
each artifact as `approved` or `restricted`:

```
bnd repo deps -o deps.txt
java -jar org.eclipse.dash.licenses-<version>.jar deps.txt -summary DEPENDENCIES
```

Pipe directly via stdin (`-` means stdin to the Dash tool):

```
bnd repo deps | java -jar org.eclipse.dash.licenses-<version>.jar -
```

Restrict to a single repository (glob on the repository name) and drop your own
bundles from the report:

```
bnd repo deps --from "Maven Central" -e "biz.aQute.bnd:*" -e "org.bndtools:*"
```

Emit ClearlyDefined ids instead of Maven GAVs:

```
bnd repo deps --clearlydefined
```

### Running it from Gradle

There is no dedicated Gradle task for the command (neither the bnd build nor the
bnd Gradle plugin exposes arbitrary `bnd` CLI sub-commands as tasks), but it is
trivial to wrap in a `JavaExec` task:

```gradle
tasks.register("ipDashDeps", JavaExec) {
    group = "verification"
    description = "Export workspace Maven GAVs for the Eclipse Dash License Tool"
    classpath = files("…/biz.aQute.bnd.jar")   // the bnd executable jar
    mainClass = "aQute.bnd.main.bnd"
    args = ["repo", "-w", projectDir.toString(), "deps", "-e", "biz.aQute.bnd:*", "-o", "${projectDir}/deps.txt"]
}
```

Notes:

- In a workspace that uses the **bnd Gradle plugin**, bnd is already on the
  buildscript classpath, so instead of a hard-coded `files(...)` path resolve it as
  a dependency — e.g. a dedicated `configurations { bndcli }` with
  `dependencies { bndcli "biz.aQute.bnd:biz.aQute.bnd:<version>" }` and
  `classpath = configurations.bndcli`.
- `--workspace` (`-w`) is a parent `repo` option, so it comes *before* `deps` in
  `args`.
- To get a one-shot `./gradlew licenseCheck`, add a second `JavaExec`/`Exec` task
  that depends on `ipDashDeps` and runs
  `org.eclipse.dash.licenses-<version>.jar deps.txt -summary DEPENDENCIES`.

### Notes

- Only repositories backed by `MavenBndRepository` are exported; OSGi/P2
  repositories are skipped because they have no Maven coordinates.
- Source, javadoc and pom variants collapse into a single entry per artifact, and
  the list is de-duplicated and sorted.
- This command only *produces* the dependency list; the actual license vetting is
  performed by `dash-licenses`. See its README for `-review`/`-token`/`-project`
  options to automatically open Eclipse IP review requests.
