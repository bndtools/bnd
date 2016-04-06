# bnd-indexer-maven-plugin

The `bnd-indexer-maven-plugin` is a bnd based plugin that creates OSGi 
indexes of bundles. Read about repositories (http://bndtools.org/repositories.html) and workspaces (http://bnd.bndtools.org/chapters/130-concepts.html#the-workspace) 

## What does the `bnd-indexer-maven-plugin` do?

This plugin creates an OSGi repository index (suitable for use in resolving 
and provisioning) from a set of maven project dependencies. 

The project dependencies are referenced in the remote repositories where 
they are deployed (i.e. they are not republished in a single artifact as 
they would be when using the maven-assembly-plugin).

The output is an XML file, and a parallel gzipped version of the XML. When
deployed to a remote repository these can be used to efficiently host an
OSGi repository system.

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
                    <goal>index</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    
### Running the `bnd-indexer-maven-plugin`

The only goal of the `bnd-indexer-maven-plugin` is `index` which generates the
OSGi index output files. By default the `bnd-indexer-maven-plugin` binds to the 
package phase of your build.

The outputs of the `bnd-indexer-maven-plugin` are `target/index.xml` and
`target/index.xml.gz`. 

### Configuring the `bnd-indexer-maven-plugin`

The `bnd-indexer-maven-plugin` may be configured in several ways

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
