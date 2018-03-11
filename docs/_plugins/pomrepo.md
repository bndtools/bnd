---
title: Bnd Pom Repository
layout: default
summary: A plugin to use a Maven POM as a repository 
---

A Maven POM can be viewed as the root node in an artifact transitive dependency graph. The Bnd Pom Repository plugin reads this graph and provides the set of artifacts as a bnd repository. The purpose of this plugin is to be able to have a single dependency definition that can be used by Maven projects and bnd projects. 

The pom can be a file on the local file system, a URL, a group, artifact, version (GAV) coordinate, or a query expression on maven central. 

## Use Cases

The use case that triggered the development of the Bnd Pom Repository is OSGi enRoute. The artifacts in the OSGi enRoute effort needed to be shared between the Bndtools workspace and the Maven examples. Expressing the dependencies in a POM allows this. In the OSGi enRoute case the distro (the implementations for the OSGi enRoute API) are maintained in a [POM](https://github.com/osgi/osgi.enroute/blob/next/osgi.enroute.pom.distro/pom.xml). This POM is used to provide the compile time dependency (the OSGi enRoute Base API jar) as well as the runtime dependencies.

## Example

In OSGi enRoute we need the following dependencies in a `pom.xml` file:

    <dependencies>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>osgi.enroute.base.api</artifactId>
            <version>2.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>osgi.enroute.pom.distro</artifactId>
            <version>2.0.0</version>
        </dependency>
    </dependencies>

In a bndrun file in the same directory we can now use this `pom.xml` file as our repository:

    -standalone: true
    -plugin.enroute-distro = \
        aQute.bnd.repository.maven.pom.provider.BndPomRepository; \
            snapshotUrls=https://oss.sonatype.org/content/repositories/osgi/; \
            releaseUrls=https://repo.maven.apache.org/maven2/; \
            pom=${.}/pom.xml; \
            name=enRouteDistroPom

Opening the `Run` tab of the bndrun editor on this file will show you all transitive dependencies.

## Searching

Maven Central supports a [searching facility](http://blog.sonatype.com/2011/06/you-dont-need-a-browser-to-use-maven-central/) based on Solr. For example, you want all the artifacts of a given group id. In that case you could use the following Bnd Pom Repository plugin:

    -standalone: true
    -plugin.query = \
        aQute.bnd.repository.maven.pom.provider.BndPomRepository; \
            releaseUrls=https://repo.maven.apache.org/maven2; \
            query='q=g:%22biz.aQute.bnd%22'; \
            name=Query

The query must return a JSON response.

## Configuration

| Property         | Type  | Default | Description |
|------------------|-------|---------|-------------|
| `releaseUrls`    | `URI...` |      | Comma separated list of URLs to the repositories of released artifacts.| 
| `snapshotUrls`   | `URI...` |      | Comma separated list of URLs to the repositories of snapshot artifacts.|
|                  |       |         | If this is not specified, it falls back to the release repository or just `local` if that is also not specified.|
| `local`          | `PATH`| `~/.m2/repository` | The file path to the local Maven repository.  |
|                  |       |                    | If specified, it should use forward slashes. If the directory does not exist, the plugin will attempt to create it.|
|                  |       |         | The default can be overridden with the `maven.repo.local` System property.|
| `revision`       | `GAV...` |      | A comma separated list of Maven coordinates. The GAV will be searched in the normal way.|
| `pom`            | `URI...` |      | A comma separated list of URLs to POM files.|
| `location`       | `PATH` | `cnf/cache/pom-<name>.xml` | Optional cached index of the parsed POMs. |
| `query`          | `STRING` |      | A Solr query string. This is the part after `?` and must be properly URL encoded|
| `queryUrl`       | `URI` | `http://search.maven.org/solrsearch/select` | Optional URL to the search engine.|
| `name`           | `STRING`|       | Required name of the repo.|
| `transitive`     | `true|false` | `true` | If set to _truthy_ then dependencies are transitive.|
| `poll.time`      | `integer`| 5 minutes | Number of seconds between checks for changes to POM files referenced by `pom` or `revision`. If the value is negative or the workspace is in batch/CI mode, then no polling takes place.|


One, and only one, of the `pom`, `revision`, or `query` configurations can be set. If multiple are set then the first in `[pom, revision, query]` is used and the remainders are ignored.


## Authentication

The Maven Bnd Repository uses the bnd Http Client. See the [-connection-settings] instruction for how to set the proxy and authentication information.

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

## IDEs

The repository view in the IDE will show detailed information when you hover the mouse over the the repository entry, the program entry, or the revision entry. 

## Caveats

* The repository does not use any Maven code to parse the POMs to keep the dependencies to bnd low. It attempts to strictly follow the rules prescribed for POMs concerning properties, inheritance, and ordering. However, there are a number of issues with this approach. 
    * bnd attempts to follow the rules, maven sometimes relaxes its own and other specification rules so you could run into errors where POMs are wrong but still accepted by Maven.
    * It is possible to add dependencies via a plugin. This is not supported for what should be obvious reasons.
* The parser ignores repositories in POMs and restricts the repositories to the ones listes in the `releaseUrls` and `snapshotUrls` configuration parameters. This is for security reasons.
* The parser ignores any restrictions on dependencies beause the intention of these restrictions is to handle unicity of the class path. The primary purpose of the Bnd Pom Repository is to be used in assembling. Since this is not an issue for the resolver we ignore this. You can always override this with the `-runblacklist` instruction.

[-connection-settings]: /instructions/connection-settings
