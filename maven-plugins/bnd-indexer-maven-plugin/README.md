# bnd-indexer-maven-plugin

The `bnd-indexer-maven-plugin` is a bnd based plugin that creates OSGi 
indexes of bundles.

## What does the `bnd-indexer-maven-plugin` do?

This plugin creates an OSGi repository index (suitable for use in resolving 
and provisioning).

The plugin has two goals. `index` and `local-index`.

### The `index` goal

The index goal creates an OSGi repository index from a set of maven project 
dependencies. 

The project dependencies are referenced in the remote repositories where 
they are deployed (i.e. they are not republished in a single artifact as 
they would be when using the maven-assembly-plugin).

The output is an XML file, and a parallel gzipped version of the XML. When
deployed to a remote repository these can be used to efficiently host an
OSGi repository system.

### The `local-index` goal

The `local-index` goal is used to create an OSGi repository index from a
folder containing bundles. The repository index URIs can be set relative
to the bundle location.

The `local-index` goal can be usefully combined with the `copy-dependencies`
goal of the `maven-dependency-plugin`, and then the whole output released
using the `maven-assembly-plugin`. 

Indexes produced using the `local-index` goal trade file size for the
convenience of accessing all files at a single location, and should 
therfore be used with care.

## How do I use the `bnd-indexer-maven-plugin` in my project?

Normally you will create a module specifically to build your index file.
The easiest way do this is to create a simple module with a `pom` packaging
type. Including the bnd-indexer-maven-plugin in your module is very easy:

    <plugin>
        <groupId>biz.aQute.bnd</groupId>
        <artifactId>bnd-indexer-maven-plugin</artifactId>
        <version>${bnd.version}</version>
        <configuration>...</configuration>
        <executions>
            <execution>
                <id>index</id>
                <goals>
                    <goal>...</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    
### Running the `bnd-indexer-maven-plugin`

First you must select the type of index that you want to create:

 * Indexes with remote references using `index`
 * Indexes using local references using `local-index`
 
#### Common configuration for the plugin

The output index file name can be changed very easily:

    <configuration>
        <outputFile>${project.build.directory}/custom.xml</outputFile>
    </configuration>

The name attribute of the repository index can also be changed:

    <configuration>
        <indexName>my-index</indexName>
    </configuration>

The gzip version of the output artifact can also be disabled if desired:    
    
    <configuration>
        <includeGzip>false</includeGzip>
    </configuration>					

The plugin execution can be skipped if desired

    <configuration>
        <skip>false</skip>
    </configuration>

## Building indexes with `index`

The `index` goal of the `bnd-indexer-maven-plugin` generates OSGi index 
output files which are attached as deployable artifacts with the classifier
`osgi-index`. By default this goal binds to the 
`package` phase of your build.

The default outputs of the `bnd-indexer-maven-plugin` are `target/index.xml` 
and `target/index.xml.gz`. 

### Configuring the `bnd-indexer-maven-plugin` `index` goal

The `index` goal has a number of useful configuration parameters.

#### Allowing local file locations

By default the `bnd-indexer-maven-plugin` will fail the build if it cannot 
determine a remote repository from which a dependency can be downloaded.
Sometimes (for example during local development) some dependencies are not
yet deployed to a remote repository, even as a snapshot, and this should
not fail the build. It is also sometimes the case that local URLs are
specifically required for local testing.

The `bnd-indexer-maven-plugin` can be configured to use local URLs by 
setting the localURLs property to one of three values:

* `FORBIDDEN` - The default value. This will cause the build to fail if
no remote URL can be found
* `ALLOWED` - Where a remote URL can be found it will be use. If one cannot
be found then the local URL will be used instead
* `REQUIRED` - No remote URLs will be used. All URLs in the index will be
local.

Setting the localURLs property is very easy:

    <configuration>
        <localURLs>ALLOWED</localURLs>
    </configuration>

N.B. If local URLs are used in the index then it *should not be deployed* 
to a remote repository. The local URLs are only valid on the machine that
created them. For this reason it is recommended that CI servers use a 
`FORBIDDEN` policy

##### Using indexes at development time

You may want to use generated indexes at development time, indexing your 
recently built snapshots and doing local testing. This model works really
well if you set up profiles for your build 
[(see the maven docs for more info)](https://maven.apache.org/guides/introduction/introduction-to-profiles.html).

A recommended setup is as follows:

*POM configuration data*
    
    <!-- Properties configuration -->
    <properties>
        <local.url.policy>REQUIRED</local.url.policy>
    </properties>
    ...
    <!-- Profile configuration -->
    <profiles>
        <profile>
            <id>RunningInCI</id>
            <activation>
                <property>
                    <name>in.ci</name>
                    <value>true</value>
                </property>
            </activation>
            <properties>
                <local.url.policy>FORBIDDEN</local.url.policy>
            </properties>
        </profile>
    </profiles>
    ...
    <!-- Indexer plugin configuration -->
    <configuration>
        <localURLs>${local.url.policy}</localURLs>
    </configuration>
    ...

This configuration means that when a developer runs a build on their local
machine they will generate an index using only local URLs suitable for rapid
deployment and testing. When run on a CI server (specifying the `in.ci`
environment property) then an index with full remote URLs will be created.

#### Excluding transitive dependencies

By default the `bnd-indexer-maven-plugin` will include the full transitive
dependency tree of the module in the index that it generates. If this
behaviour is not desired then it can be disabled as follows:

    <configuration>
        <includeTransitive>false</includeTransitive>
    </configuration>
    
#### Restricting dependency scopes

By default the `bnd-indexer-maven-plugin` will include dependencies with 
`compile` and `runtime` scope. Different scopes may be selected as follows:

    <configuration>
        <scopes>
            <scope>compile</scope>
            <scope>provided</scope>
            <scope>test</scope>
        </scopes>
    </configuration>

#### Avoiding attaching the generated index

By default the OSGi index output files generate by this plugin are attached as deployable 
artifacts with the classifier `osgi-index`. This behaviour is not always desirable, for
example if the index is only used as input to another plugin within the build. Attaching
the generated indexes can be disabled as follows:

    <configuration>
        <attach>false</attach>
    </configuration>


#### Including the current project output as part of the index

A nice application of the `bnd-indexer-maven-plugin` is using it in conjunction with the
`bnd-testing-maven-plugin` for integration testing, which assembles a testable framework
by means of the OSGi resolver from the index of the current project. By default, the
`bnd-indexer-maven-plugin`, which is not typically associated with a project containing
code, does not include the current project output as part of the index but this would
be ideal for integration test projects. To support this scenario, if the project's
packaging is `jar`, the plugin may be configured to include the artifact in the index
as follows:

    <configuration>
        <includeJar>true</includeJar>
    </configuration>

Setting up an integration test project using this approach could be achieved as follows:

*POM configuration data*

    ...
    <packaging>jar</packaging>
    ...
    <!-- Indexer plugin configuration -->
    <configuration>
        <includeJar>true</includeJar>
        <localURLs>REQUIRED</localURLs>
        <attach>false</attach>
    </configuration>
    ...
    <!-- Testing plugin configuration -->
    <configuration>
        <failOnChanges>false</failOnChanges>
        <resolve>true</resolve>
        <bndruns>
            <bndrun>itest.bndrun</bndrun>
        </bndruns>
        <targetDir>.</targetDir>
    </configuration>
    ...

*`itest.bndrun` file data*

    -standalone: target/index.xml
    ...

## Building indexes with `local-index`

The `local-index` goal of the `bnd-indexer-maven-plugin` generates OSGi index 
output files which are *not* attached as deployable artifacts. By default this 
goal binds to the `process-resources` phase of your build.

The default output files are `target/index.xml` and `target/index.xml.gz`. 

### Configuring the `bnd-indexer-maven-plugin` `local-index` goal

The `local-index` goal has a number of useful configuration parameters.

#### Selecting the indexed directory

The `local-index` goal indexes bundles in a local folder. The location
of this folder is passed as configuration.

    <configuration>
        <inputDir>${project.build.directory}/bundles</inputDir>
    </configuration>

#### Including and Excluding paths to be indexed

The `local-index` goal may be configured with ant-style glob expressions
indicating what should be included and what should be excluded from the
index. The default includes is `**/*.jar`.

    <configuration>
        <inputDir>${project.build.directory}/bundles</inputDir>
        <indexFiles>
            <include>**/org.osgi.*.jar</include>
            <exclude>**/*-javadoc.jar</exclude>
            <exclude>**/*-sources.jar</exclude>
        </indexFiles>
    </configuration>

#### Changing relative directory

All of the index URIs stored in the index use relative paths (as long as `absolute` is not set to true)
to reference the bundles in the indexing folder. By default these relative 
paths use the index output location as their base directory, but a
different base directory can be supplied if needed

    <configuration>
        <baseFile>${project.build.directory}/some/folder</baseFile>
    </configuration>

## Configuration Properties

|Configuration Properties for `index` goal | Description |
| ---               | ---         |
|`outputFile`       | The name and location of the resulting index file. _Defaults to `${project.build.directory}/index.xml`._ Override with property `bnd.indexer.output.file`.|
|`localURLs`        | See [Allowing local file locations](#allowing-local-file-locations). _Defaults to `FORBIDDEN`._ Override with property `bnd.indexer.localURLs`.|
|`includeTransitive`| See [Excluding transitive dependencies](#excluding-transitive-dependencies). _Defaults to `true`._ Override with property `bnd.indexer.includeTransitive`.|
|`includeJar`       | See [Including the current project output as part of the index](#including-the-current-project-output-as-part-of-the-index). _Defaults to `false`._ Override with property `bnd.indexer.includeJar`.|
|`addMvnURLs`       | In addition to other resource urls include `mvn` protocol urls as well if found. _Defaults to `false`._ Override with property `bnd.indexer.add.mvn.urls`.|
|`scopes`           | See [Restricting dependency scopes](#restricting-dependency-scopes). _Defaults to `compile,runtime`._ Override with property `bnd.indexer.scopes`.|
|`includeGzip`      | Include a GZIP'd version of the index file adjacent to the non-GZIP'd one. _Defaults to `true`._ Override with property `bnd.indexer.include.gzip`.|
|`skip`             | Skip the index process altogether. _Defaults to `false`._ Override with property `bnd.indexer.skip`.|

|Configuration Properties for `local-index` goal | Description |
| ---               | ---         |
|`inputDir`         | A directory contain the bundles to index. Override with property `bnd.indexer.input.dir`.|
|`indexFiles`       | A set of `include` and `exclude` child elements using Ant-style globs matching paths within the `inputDir` which should be included in the index or excluded from the index. _Defaults to `<include>**/*.jar</include>`._|
|`outputFile`       | The name and location of the resulting index file. _Defaults to `${project.build.directory}/index.xml`._ Override with property `bnd.indexer.output.file`.|
|`baseFile`         | See [Changing relative directory](#changing-relative-directory). Override with property `bnd.indexer.base.file`.|
|`absolute`         | Flag to enable absolute index URIs. Override with property `bnd.indexer.absolute`.|
|`includeGzip`      | Include a GZIP'd version of the index file adjacent to the non-GZIP'd one. _Defaults to `true`._ Override with property `bnd.indexer.include.gzip`.|
|`skip`             | Skip the index process altogether. _Defaults to `false`._ Override with property `bnd.indexer.skip`.|
