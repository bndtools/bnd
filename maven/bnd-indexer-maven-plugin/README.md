# bnd-indexer-maven-plugin
=============================

The `bnd-indexer-maven-plugin` is a bnd based plugin that creates OSGi 
indexes of bundles.

# What does the `bnd-indexer-maven-plugin` do?

This plugin creates an OSGi repository index (suitable for use in resolving 
and provisioning) from a set of maven project dependencies. 

The project dependencies are referenced in the remote repositories where 
they are deployed (i.e. they are not republished in a single artifact as 
they would be when using the maven-assembly-plugin).

The output is an XML file, and a parallel gzipped version of the XML. When
deployed to a remote repository these can be used to efficiently host an
OSGi repository system.

# How do I use the `bnd-indexer-maven-plugin` in my project?

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
    
## Running the `bnd-indexer-maven-plugin`

The only goal of the `bnd-indexer-maven-plugin` is `index` which generates the
OSGi index output files. By default the `bnd-indexer-maven-plugin` binds to the 
package phase of your build.

The outputs of the `bnd-indexer-maven-plugin` are `target/index.xml` and
`target/index.xml.gz`. 

# Configuring the `bnd-indexer-maven-plugin`

The `bnd-indexer-maven-plugin` may be configured in several ways

## Allowing local file locations

By default the `bnd-indexer-maven-plugin` will fail the build if it cannot 
determine a remote repository from which a dependency can be downloaded.
Sometimes (for example during local development) some dependencies are not
yet deployed to a remote repository, even as a snapshot. The 
`bnd-indexer-maven-plugin` can be configured to use these local URLs as
follows:

    <configuration>
        <allowLocal>true</allowLocal>
    </configuration>

N.B. If local URLs are used then the index *should not be deployed* to a 
remote repository. The local URLs will not be valid on another machine. 

## Excluding transitive dependencies

By default the `bnd-indexer-maven-plugin` will include the full transitive
dependency tree of the module in the index that it generates. If this
behaviour is not desired then it can be disabled as follows:

    <configuration>
        <includeTransitive>false</includeTransitive>
    </configuration>
    
## Restricting dependency scopes

By default the `bnd-indexer-maven-plugin` will include dependencies with 
`compile` and `runtime` scope. Different scopes may be selected as follows:

    <configuration>
        <scopes>
            <scope>compile</scope>
            <scope>provided</scope>
            <scope>test</scope>
        </scopes>
    </configuration>