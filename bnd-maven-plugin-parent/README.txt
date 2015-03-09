bnd-maven-plugin
================

This is a Maven plugin for invoking bnd.

The plugin hooks into the process-classes phase and generates artifacts such as:

* META-INF/MANIFEST.MF
* Declarative Services metadata under OSGI-INF
* Metatype Service files
* etc...

All of the above artifacts will be generated into the project build output
directory, i.e. `target/classes`, to be subsequently picked up and included
in the JAR file by the default `maven-jar-plugin`.

All bnd instructions must be declared in a bnd.bnd file at the root of the
project. The plugin adds the following implicit instruction, which means that
the entire content of the project (but no other packages from the build path)
will be treated as bundle content and analyzed for imported packages:

    -includeresource: ${project.build.outputDirectory} # i.e target/classes

NB: If the bnd.bnd file is absent then the bundle will contain only private
packages, no Bundle-Activator, no Service-Component header, etc.  Therefore
although it will be valid, the bundle would not be *useful*.

For further usage information, see the example projects under the included
`examples` directory.


IMPORTANT NOTE:

The `maven-jar-plugin` will NOT currently use the generated MANIFEST.MF. We
anticipate a patch to the JAR plugin that will do this; in the meantime it is
necessary to configure the plugin as follows:

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
            <archive>
                <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
            </archive>
        </configuration>
    </plugin>


