---
title: Sonatype Central Portal Publishing
summary: How to publish OSGi bundles to Maven Central via the Sonatype Central Portal
layout: bnd
parent: Packaging and Distribution
nav_order: 4
---

[Sonatype Central Portal](https://central.sonatype.com/) is the modern publishing gateway for Maven Central. This page describes how to publish OSGi bundles built with bnd to Maven Central using the Sonatype Central Portal.

## Prerequisites

Before publishing to Sonatype Central Portal you need a local Maven repository that holds the release artifacts. Configure the MavenBndRepository plugin with to build such a local repository first:

	-plugin.release = \
	    aQute.bnd.repository.maven.provider.MavenBndRepository; \
	        releaseUrl   = file:///path/to/local/repo; \
	        index        = ${.}/release.maven; \
	        name         = "Local Release"

This is the recommended approach for CI/CD pipelines: all build tools (bnd workspace, Gradle plugins, Maven plugins) publish to a shared local directory first, and a separate upload step pushes the entire directory to Sonatype as a single deployment.

## Authentication

Publishing to Sonatype Central Portal requires a Bearer Token. To generate one, log in to [Sonatype Central Portal](https://central.sonatype.com/), go to **Account → Generate User Token**, and copy the generated token. See the [Sonatype token generation guide](https://central.sonatype.org/publish/generate-portal-token/) for detailed instructions.

Set the token as an environment variable (e.g. `SONATYPE_BEARER`) 

## Standalone Upload Scripts (recommended for CI/CD)

For CI/CD pipelines, the recommended approach is to use the standalone upload scripts. This separates the build step from the Sonatype deployment step and works across mixed build systems.

The workflow is:

1. All builds publish artifacts to a shared local directory (e.g. `dist/bundles`)
2. `sonatype-upload.sh` zips the directory and uploads it to Sonatype as a single deployment
3. `sonatype-status.sh` checks the deployment state and optionally cleans up

### sonatype-upload.sh

```
Usage: sonatype-upload.sh [options] <release-dir>

Upload a local Maven repository folder to Sonatype Central Portal.

Options:
  --publishing-type <AUTOMATIC|USER_MANAGED>  Publishing type (default: USER_MANAGED)
  --name <name>                               Deployment name (default: auto-generated)
  --upload-url <url>                          Release upload endpoint URL

Environment:
  SONATYPE_BEARER   Bearer token for authentication (required)
```

Upload release artifacts with manual publishing (default):

	SONATYPE_BEARER=<token> \
	  ./.github/scripts/sonatype-upload.sh dist/bundles

Upload and automatically publish to Maven Central:

	SONATYPE_BEARER=<token> \
	  ./.github/scripts/sonatype-upload.sh --publishing-type AUTOMATIC dist/bundles

The script:

1. Detects the group ID from `.pom` files inside `<release-dir>`
2. Creates a ZIP bundle from the release directory (using `jar cMf` for git bash compatibility)
3. Uploads via `POST /api/v1/publisher/upload`
4. Stores the deployment ID in `<release-dir>_DEPLOYMENTID.txt` for later status checks

### sonatype-status.sh

```
Usage: sonatype-status.sh [options] <release-dir>

Check Sonatype deployment status and optionally clean up.

Options:
  --status-url <url>   Status API URL (default: Sonatype Central Portal)
  --clean              Remove release-dir after successful verification

Environment:
  SONATYPE_BEARER   Bearer token for authentication (required)
```

Check release deployment status:

	SONATYPE_BEARER=<token> \
	  ./.github/scripts/sonatype-status.sh dist/bundles

Check status and clean up the release directory on success:

	SONATYPE_BEARER=<token> \
	  ./.github/scripts/sonatype-status.sh --clean dist/bundles

The script reads the deployment ID from `<release-dir>_DEPLOYMENTID.txt` (written by `sonatype-upload.sh`) and queries `/api/v1/publisher/status`. Possible deployment states are:

| State | Meaning |
|-------|---------|
| `PUBLISHED` | Successfully published to Maven Central |
| `VALIDATED` | Passed validation; awaiting manual publishing via the [Sonatype web interface](https://central.sonatype.com/publishing) |
| `PENDING` / `VALIDATING` / `PUBLISHING` | In progress; re-run status check later |
| `FAILED` | Deployment failed; inspect the output for details |

## Gradle Integration

To call the Sonatype upload script after a successful Gradle publish, add a task to your `build.gradle`:

	tasks.register('sonatypeUpload', Exec) {
	    dependsOn ':publish'
	    group = 'publishing'
	    description = 'Upload artifacts to Sonatype Central Portal'
	    commandLine './.github/scripts/sonatype-upload.sh', 'dist/bundles'
	    environment 'SONATYPE_BEARER', System.getenv('SONATYPE_BEARER') ?: ''
	}

## Maven Integration

To call the Sonatype upload script after a successful Maven deploy, use the `exec-maven-plugin`:

	<plugin>
	    <groupId>org.codehaus.mojo</groupId>
	    <artifactId>exec-maven-plugin</artifactId>
	    <executions>
	        <execution>
	            <id>sonatype-upload</id>
	            <phase>deploy</phase>
	            <goals><goal>exec</goal></goals>
	            <configuration>
	                <executable>./.github/scripts/sonatype-upload.sh</executable>
	                <arguments>
	                    <argument>dist/bundles</argument>
	                </arguments>
	            </configuration>
	        </execution>
	    </executions>
	</plugin>

[-connection-settings]: /instructions/connection_settings.html
