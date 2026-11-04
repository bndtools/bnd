---
layout: bnd
title: repo
summary: |
   Access to the repositories. Provides a number of sub commands to manipulate the repository (see repo help) that provide access to the installed repos for the current project.
parent: bnd CLI Commands
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   repo [options]  <sub-cmd> ...

#### Options: 
- `[ -c --cache ]` Include the cache repository
- `[ -f --filerepo <string>* ]` Add a File Repository
- `[ -m --maven ]` Include the maven repository
- `[ -p --project <string> ]` Specify a project
- `[ -r --release <glob> ]` Override the name of the release repository (-releaserepo)
- `[ -w --workspace <string> ]` Workspace (a standalone bndrun file or a sbdirectory of a workspace (default is the cwd)

## Available sub-commands 
-  `copy` -   
-  `deps` - Export the Maven GAVs of all artifacts in the Maven repositories for license/IP analysis (dash-licenses) 
-  `diff` - Diff jars (or show tree) 
-  `get` - Get an artifact from a repository. 
-  `index` -   
-  `list` - List all artifacts from the current repositories with their versions 
-  `put` - Put an artifact into the repository after it has been verified. 
-  `refresh` - Refresh refreshable repositories 
-  `repos` - List the current repositories 
-  `sync` -   
-  `topom` -   
-  `versions` - Displays a list of versions for a given bsn that can be found in the current repositories. 

### copy 
#### Synopsis: 
	   copy [options]  <source> <dest> <bsn[:version]...>

##### Options: 
- `[ -d --dry ]` Do not really copy but trace the steps
- `[ -f --filter <string;> ]` 
- `[ -F --force ]` 
- `[ -p --project <string> ]` Identify another project
- `[ -q --quiet ]` 
- `[ -s --standalone <string> ]` A stanalone bndrun file

### deps 
Export the Maven coordinates (GAVs) of all artifacts in the Maven repositories as a flat, newline-separated list. Macros in the repository index files (e.g. ${junit.version}) are resolved. The output is directly consumable by the Eclipse Dash License Tool (dash-licenses), for example:
  bnd repo deps -o deps.txt
  java -jar org.eclipse.dash.licenses-<version>.jar deps.txt -summary DEPENDENCIES

#### Synopsis: 
	   deps [options] 

##### Options: 
- `[ -c --clearlydefined ]` Emit ClearlyDefined ids (maven/mavencentral/<group>/<artifact>/<version>) instead of Maven GAVs
- `[ -e --exclude <string;> ]` Exclude artifacts whose 'group:artifact:version' coordinate matches any of the given glob expressions (e.g. '-e biz.aQute.bnd:*' to drop your own bundles). May be repeated.
- `[ -f --from <glob> ]` A glob expression on the repository name; default is all Maven repositories
- `[ -o --output <string> ]` Output file (default: the console)

### diff 
Show the diff tree of a single repo or compare 2  repos. A diff tree is a detailed tree of all aspects of a bundle, including its packages, types, methods, fields, and modifiers.

#### Synopsis: 
	   diff [options]  <newer repo> <[older repo]>

##### Options: 
- `[ -a --added ]` Just additions (no removes)
- `[ -A --all ]` Both add and removes
- `[ -d --diff ]` Formatted like diff
- `[ -f --full ]` Show full diff tree (also wen entries are equal)
- `[ -j --json ]` Serialize to JSON
- `[ -r --remove ]` Just removes (no additions)

### get 
Get an artifact from a repository.

#### Synopsis: 
	   get [options]  <bsn> <[range]>

##### Options: 
- `[ -f --from <instruction> ]` 
- `[ -l --lowest ]` 
- `[ -o --output <string> ]` Where to store the artifact

### index 
#### Synopsis: 
	   index [options]  ...


##### Options: 
- `[ -f --from <instruction> ]` A glob expression on the source repo, default is all repos
- `[ -n --name <string> ]` The name of the output file. If not set will show on the console
- `[ -o --output <string> ]` Output file (will be compressed)
- `[ -q --query <string> ]` Optional search term for the list of bsns (given to the repo)
- `[ -Q --quiet ]` No output

### list 
List all artifacts from the current repositories with their versions

#### Synopsis: 
	   list [options] 

##### Options: 
- `[ -f --from <instruction> ]` A glob expression on the source repo, default is all repos
- `[ -n --noversions ]` Do not list the versions, just the bsns
- `[ -q --query <string> ]` Optional search term for the list of bsns (given to the repo)

### put 
Put an artifact into the repository after it has been verified.

#### Synopsis: 
	   put [options]  <<jar>...>

##### Options: 
- `[ -f --force ]` Put in repository even if verification fails (actually, no verification is done).

### refresh 
Refresh refreshable repositories

#### Synopsis: 
	   refresh [options] 

##### Options: 
- `[ -q --quiet ]` 

### repos 
List the current repositories

#### Synopsis: 
	   repos 

### sync 


#### Synopsis: 
	   sync [options]  ...


##### Options: 
- `[ -d --dest <string> ]` 
- `[ -g --gavs <string;> ]` 
- `[ -s --source <string;> ]` 
- `[ -w --workspace <string> ]` 

### topom 
Create a POM out of a bnd repository

#### Synopsis: 
	   topom [options]  <repo> <name>

##### Options: 
- `[ -d --dependencyManagement ]` Use the dependency management section
- `[ -o --output <string> ]` Output file
- `[ -p --parent <string> ]` The parent of the pom (default none.xml)

### versions 
Displays a sorted set of versions for a given bsn that can be found in the current repositories.

#### Synopsis: 
	   versions  <bsn>

<!-- Manual content from: ext/repo.md --><br /><br />

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
