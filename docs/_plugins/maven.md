---
title: Maven Bnd Repository Plugin
layout: bnd
summary: A plugin to use and release to Maven repositories
parent: Plugins
---
The Maven Bnd Repository / MavenBndRepository plugin provides a full interface to the Maven local repository in `~/.m2/repository` and remote repositories like [Nexus] or [Artifactory]. And it provides of course full access to Maven Central. It implements the standard bnd Repository Plugin and can provide an OSGi Repository for resolving.

### Maven Central

To access Maven Central use the following configuration:

	-plugin.central = \
		aQute.bnd.repository.maven.provider.MavenBndRepository; \
			releaseUrl=https://repo.maven.apache.org/maven2/; \
			index=${.}/central.maven; \
			name="Central"

You can add `Group:Artifact:Version` coordinates in the `central.maven` file. The file can contain comments, empty lines, and can use macros per line. That is, you cannot create a macro with a load of GAV's.

#### Release to Maven Central

The recommended approach is the standalone upload scripts described in [Sonatype Central Portal Publishing](/chapters/325-sonatype-central-portal.html).

### Use of .m2 Local Repository

To use your local Maven repository (`~/.m2/repository`) you can define the following plugin:

	-plugin.local = \
		aQute.bnd.repository.maven.provider.MavenBndRepository; \
			index=${.}/local.maven; \
			name="Local"

### Artifactory or Nexus Repository

To use a remote release repository based on Nexus or Artifactory you can define the following plugin:

	-plugin.release = \
		aQute.bnd.repository.maven.provider.MavenBndRepository; \
			releaseUrl=http://localhost:8081/nexus/content/repositories/releases/    ; \
			snapshotUrl=http://localhost:8081/nexus/content/repositories/snapshots/   ; \
			index=${.}/release.maven; \
			name="Release"

If you use a remote repository then you must configure the credentials. This is described in [-connection-settings]. Placing the following XML in  `~/.bnd/settings.xml` will provide you with the default Nexus credentials:

	<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
	                          http://maven.apache.org/xsd/settings-1.0.0.xsd">
		<servers>
			<server>
				<id>http://localhost:8081</id>
				<username>admin</username>
				<password>admin123</password>
			</server>
		</servers>
	</settings>

Notice that the id must match the scheme, the host, and the port if not the default port for the scheme.

## Plugin Configuration

The class name of the plugin is `aQute.bnd.repository.maven.provider.MavenBndRepository`. It can take the following configuration properties:

| Property         | Type  | Default | Description |
|------------------|-------|---------|-------------|
| `releaseUrl`     | `URI` |         | Comma separated list of URLs to the repositories of released artifacts.|
| `snapshotUrl`    | `URI` |         | Comma separated list of URLs to the repositories of snapshot artifacts.|
| `stagingUrl`    | `URI` |         | A single URL to the repositories staging repository. This is required, e.g. for a release to maven central, which usually goes through a staging repository.|
| `sonatypeMode`   | enum `none` `manual` `autopublish` | `none` | Controls how artifacts are published to Maven Central via the Sonatype Central Portal. `none`: standard Maven repository behavior; `manual`: upload for validation but requires manual publishing approval; `autopublish`: automatically publish after validation. Requires Bearer Token authentication via [-connection-settings]. |
| `local`          | `PATH`| `~/.m2/repository` | The file path to the local Maven repository.  |
|                  |       |                    | If specified, it should use forward slashes. If the directory does not exist, the plugin will attempt to create it.|
|                  |       |         | The default can be overridden with the `maven.repo.local` System property.|
| `readOnly`       | `true`|`false` | `false` | If set to _truthy_ then this repository is read only.|
| `name`           | `NAME`| `Maven` | The name of the repository.|
| `index`          | `PATH`| `cnf/<name>.mvn` | The path to the _index_ file. The index file is a list of Maven _coordinates_ (text with one GAV per line or pom.xml).|
| `checksumFile`   | `PATH`| `<index-dir>/<index-filename>.checksums` | The path to the trusted checksum file relative to `build.bnd`. If not specified, defaults to a `.checksums` sidecar file next to the index file. See [Trusted Checksum Verification](#trusted-checksum-verification).|
| `checksumRecord`   | `boolean` | `false` | When set to `true`, the repository automatically generates (records) the trusted checksum file during initialization. Checksums are computed for all artifacts currently listed in the index file and written to the default sidecar location (`<index>.checksums`) or the path specified by `checksumFile`. Non-fatal errors during recording are logged but do not prevent the repository from initializing. |
| `tags`           | `STRING`|  | Comma separated list of tags. (e.g. resolve, baseline, release) Use a placeholder like &lt;&lt;EMPTY&gt;&gt; to exclude the repo from resolution. The `resolve` tag is picked up by the [-runrepos](/instructions/runrepos.html) instruction.|
| `source`         | `STRING`| `org.osgi:org.osgi.service.log:1.3.0 org.osgi:org.osgi.service.log:1.2.0` | A space, comma, semicolon, or newline separated GAV string. |
| `noupdateOnRelease` | `true|false` | `false` | If set to _truthy_ then this repository will not update the `index` when a non-snapshot artifact is released.|
| `poll.time`      | `integer` | 5 seconds | Number of seconds between checks for changes to the `index` file. If the value is negative or the workspace is in batch/CI mode, then no polling takes place.|
| `multi`          | `NAME`|        | Comma separated list of extensions to be searched for indexing containing bundles. For example, a zip file could comprise further bundles. Hence, this zip artifact can be referenced in this plugin for indexing the internal JARs. |

If no `releaseUrl` nor a `snapshotUrl` are specified then the repository is _local only_.

For finding archives, both URLs are used. For releasing, only the first or the `stagingUrl` is used.

## Index file

The `index` file specifies a view on the remote repository, it _scopes_ it. Since we use the bnd repositories to resolve against, it is impossible to resolve against the world. The index file falls under source control, it is stored in the source control management system. This guarantees that at any time the project is checked out it has the same views on its repository. This is paramount to prevent breaking builds due to changes in repositories.
The index file supports two formats:

- a) text file with one GAV per line or
- b) Maven _pom.xml_ content (note that not the full maven pom.xml features are supported. Mainly the `<dependency>` entries are relevant).

Note on auto-detection of index format: If bnd detects xml it assumes `pom.xml`, otherwise the text-file format is assumed.

Alternative, the GAV's can be specified in the file where the repository is defined with the  `source` configuration property. This is a string separated by either whitespace, commas, semicolons, or any combination thereof.

Both the index file and the source configuration can be replaced by macros. The only difference is that the source can use macros for more than one GAV while the indexFile is processed per line and that line must deliver at most single GAV.

### Coordinates & Terminology of Text file

The index file contains a list of _coordinates_. A coordinate specifies an _archive_ in a Maven _revison_. An archive is a ZIP, POM, JAR, or any other type of file. In Maven, these files are addressed within a revision with an _extension_ and a _classifier_. The extension indicates the type of the file and the classifier is a modifier that makes the name unique in the project. A _revision_ is the combination of a _program_ and a _version_, where the program is the combination of _groupId_ and _artifactId_.

For an archive, the extension has a default of `jar` and the classifier is by default not set (either `null` or empty). The syntax for the coordinates is therefore:

	group ':' artifact ( ':' extension ( ':' classifier )? )? ':' version ( '-SNAPSHOT` )?

Valid coordinates are:

	group:artifact:1.0-SNAPSHOT
	commons-cli:commons-cli:jar:sources:1.3.1
	commons-lang:commons-lang:2.6
	commons-logging:commons-logging:1.2
	org.osgi:osgi.core:6.0.0
	org.osgi:osgi.annotation:6.0.1

The file can contain comments (start the line with `#`), empty lines and also macros. The domain of the macros is the Workspace. A comment may also be placed after the GAV.

    # This is a comment
    ${osgi}:${osgi}.service.log:1.4.0

### Coordinates & Terminology of pom.xml

An example `pom.xml` index file which is supported by MavenBndRepository looks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>bnd.index</groupId>
  <artifactId>central</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <dependencies>
      <dependency>
        <groupId>org.osgi</groupId>
        <artifactId>org.osgi.framework</artifactId>
        <version>1.10.0</version>
      </dependency>
      <dependency>
        <groupId>org.osgi</groupId>
        <artifactId>org.osgi.framework</artifactId>
        <version>1.8.0</version>
      </dependency>
    </dependencies>
</project>
```

As stated earlier: Not all maven `pom.xml` features are supported. Mainly the `<dependency>` entries are relevant. The parser is very simple and `pom.xml` is just meant to be an alternative format for the text file.

One advantage of using the `pom.xml` format over the flat text file is that `pom.xml` is understood by more tooling. For example Dependabot can automatically update `pom.xml` files, but not the flat text file.


## Local Repository

Maven supports a local repository in `~/.m2/repository`. All repositories will install through a local repository. The default is the same repository as Maven but this can be overridden with the `local` configuration property.

It is possible to define a Maven Bnd Repository without a `releaseUrl` or `snapshotUrl`. In that case only access to the local repository is provided. Trying to release remotely will be an error for such a repository.

The [-buildrepo] instruction can be pointed to such a local repository; it will then store a JAR in the local repository after every build. This can be useful if there are Maven projects that consume the output of a bnd workspace.

## Releasing

In bnd, releasing is done by moving the JARs into a special release repository after they've been approved. That is, the location of a JAR defines if it is released, there is no need to reflect the release status in the version.

In Maven, the release status is modeled in the version. To support the  staging model, versions can end in `-SNAPSHOT`. Snapshots are treated very differently in the release process. The most important difference is that snapshot versions can overwrite previous versions.

In the release cycle, a JAR is `put` to  _release_ repository. The project of the released JAR is provided as _context_, this means it can inherit from the project and workspace. This plugin will read the [-maven-release] instruction from the context. This can result in generating source and javadoc jars. By default, a local only release only installs the actual binary.

To properly release the revision we need to know Maven specific information that is normally specified in the Maven _pom_. The bnd build can construct a pom using the [-pom] instruction. This instruction creates a pom in the JAR at `META-INF/group/artifact/pom.xml`. This plugin requires such a pom to be present. If it is not present, the [-maven-release] instruction must provide a PATH to an alternative pom. If no pom can be found then this is aborts the release.

In general, the plugin will first _install_ the JARs and its constituents into the `local` revision directory, using the Maven naming rules. If this is successful, it uploads the files to the remote repository using the remote naming rules. It will then update the `maven-metadata` to reflect the new release.

In Maven, revisions that end in `-SNAPSHOT` are treated special in many places. In bnd, we support this model with the [-snapshot] and [-pom] instructions. If a `snapshotUrl` is specified, then versions that end in `SNAPSHOT` are released to that URL.

## Authentication

The Maven Bnd Repository uses the bnd Http Client. See the [-connection-settings] instruction for how to set the proxy and authentication information.

## Tagging

This plugin supports Tagging via the `tags` configuration property. See [Tagging of repository plugins](/plugins/#tagging-of-repository-plugins) for more details.

## Trusted Checksum Verification

The Maven Bnd Repository supports trusted checksum verification to protect against tampered or corrupted artifacts. When a checksum file is present, every artifact fetched from the repository is validated against the expected checksum before use. If the checksum does not match, the local file is deleted and an exception is thrown.

### The .checksums File

The checksum file is a plain-text sidecar file that lists the expected checksum for each artifact coordinate in the index. By convention, bnd looks for a file with the same name as the index file plus the `.checksums` extension. For example, if your index is `cnf/central.maven`, bnd will automatically look for `cnf/central.maven.checksums`.

You can override this location with the checksumFile configuration property:

```
-plugin.central = \
	aQute.bnd.repository.maven.provider.MavenBndRepository; \
		releaseUrl=https://repo.maven.apache.org/maven2/; \
		index=${.}/central.maven; \
		checksumFile=${.}/trusted/central.checksums; \
		name="Central"
```

If neither a default nor an explicit checksum file exists, checksum verification is silently skipped and the repository operates normally.

### File Format

Each line in the `.checksums` file specifies one artifact coordinate and its expected checksum in the form:

```
<GAV>=<hashType>:<hexDigest>
```

where `<GAV>` uses the same coordinate syntax as the index file and `<hashType>` is one of `sha1`, `sha-1`, `sha256`, `sha-256`, `sha512`, `sha-512`, or `md5`.

Lines starting with # are treated as comments. Empty lines are ignored.

**Example central.maven.checksums:**

```
# Trusted checksums for central.maven
commons-cli:commons-cli:1.0=sha1:6dac9733315224fc562f6268df58e92d65fd0137
commons-cli:commons-cli:1.2=sha1:2bf96b7aa8b611c177d329452af1dc933e14501c
org.osgi:osgi.core:6.0.0=sha256:a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2
```

### Generating the Checksums File

The `checksumRecord` configuration option enables automatic generation of the trusted checksum file during repository initialization. This is useful for CI/CD pipelines and build automation where you want to "bootstrap" or audit the checksums without manual intervention.

**Configuration:**

```
-plugin.central =
	aQute.bnd.repository.maven.provider.MavenBndRepository;
	releaseUrl=https://repo.maven.apache.org/maven2/;
	index=${.}/central.maven;
	checksumRecord=true;
	name="Central"
```

**Workflow:**

1. Repository initialization runs (during workspace startup or build)
2. Index file is loaded and all artifacts are discovered
3. If `checksumRecord=true`, bnd iterates through each artifact
4. Computes a checksum (SHA-1 by default) for each locally available artifact
5. Writes the checksums to the `.checksums` sidecar file (or custom location)
6. Repository becomes ready for use

**Use Cases:**

- **Local development:** Enable `checksumRecord=true` locally when you need to generate or update the checksum file. After building, review the changes to `index.checksums` and commit to version control if satisfied.
- **Controlled checksum updates:** Use an environment variable like `CHECKSUMS_RECORD` to selectively enable recording only when intentional (e.g., `checksumRecord=${if;${env;CHECKSUMS_RECORD};false}`). Leave disabled in regular CI builds to prevent unintended modifications to the checksum file.
- **Onboarding new artifacts:** When adding new dependencies to the index, enable recording on a local build to bootstrap the initial checksum file.

**Notes:**

- Recording happens **after** repository initialization completes, ensuring all artifacts are available
- Only artifacts that have been downloaded to the local repository can have their checksums recorded
- Remote-only artifacts (not yet downloaded) are skipped during recording
- The generated file uses SHA-1 by default (matching the format created by the IDE's "Create Trusted Checksums file" action)
- If the checksum file already exists, it will be **overwritten** with the current state
- **Best practice:** Leave `checksumRecord=false` in standard CI builds. Enable it only when you intend to update and review the checksum file before committing.
- To use recorded checksums for verification on subsequent builds, ensure `checksumFile` is configured or the default `.checksums` sidecar is in place

### Generating checksums file in Bndtools

Additionally the Bndtools Eclipse Plugin provides a context menu action on the repository to generate the checksum file automatically. Right-click the repository entry and choose "Create Trusted Checksums file". This computes a `sha1` checksum for every artifact currently in the index and writes the result to the default sidecar location (`<index>.checksums`).

After generating the file, review it and commit it to version control alongside your index file. This ensures that everyone on your team (and CI) uses the same trusted checksums.

### Expected Behavior

|                              Situation                             |                               Behavior                              |
|:------------------------------------------------------------------:|:-------------------------------------------------------------------:|
| No checksum file exists                                            | Default: No verification is performed; artifacts are used as-is.                  |
| `.checksums` file exists but has no entry for an artifact              | That artifact is not verified; it is used as-is.                    |
| Checksum file exists and artifact matches the expected hash        | Artifact is used normally.                                          |
| Checksum file exists but  artifact does not match the expected hash | The local file is deleted and a `TrustedChecksumException` is thrown. The artifact appears as if it does not exist / could not be downloaded and thus will not resolve. Removing the artifact entry from the checksum file and a rebuild should re-download the artifact |

This design means you can incrementally add entries to the checksum file: only artifacts explicitly listed are verified. Artifacts not listed in the file are passed through without verification.


## IDEs

The repository view in the IDE will show detailed information when you hover the mouse over the the repository entry, the program entry, or the revision entry.

You can add new entries by:

* Editing the `index` file. The repository will be updated immediately
* Using the menus on the revision or program entries to delete entries
* Dropping a URL to a POM. This pom will be parsed and then its coordinates are added. POMs from `search.maven.org` are also supported.
* Using the menus to add all runtime or compile time dependencies of another entry.

[Nexus]: https://www.sonatype.com/products/repository-pro
[Artifactory]: https://www.jfrog.com/open-source/
[-maven-release]: /instructions/maven_release.html
[-snapshot]: /instructions/snapshot.html
[-pom]: /instructions/pom.html
[-connection-settings]: /instructions/connection_settings.html
[-buildrepo]: /instructions/buildrepo.html
