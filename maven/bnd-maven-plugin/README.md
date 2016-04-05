# bnd-maven-plugin

This is a Maven plugin for invoking Bnd.

The plugin hooks into the process-classes phase and generates artifacts such as:

* META-INF/MANIFEST.MF
* Declarative Services metadata under OSGI-INF
* Metatype Service files
* etc...

All of the above artifacts will be generated into the project build output
directory, i.e. `target/classes`, to be subsequently picked up and included
in the JAR file by the default `maven-jar-plugin`.

All Bnd instructions must be declared in a bnd file. By default, this is `bnd.bnd`
in the base directory of the project. This can be configured to specify an alternate
path which can be absolute or relative to the base directory of the project.
In the following example, the `project.bnd` file in the `bnd` folder of the project
will be used.

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <configuration>
        <bndfile>bnd/project.bnd</bndfile>
    </configuration>
</plugin>
```

It is also supported to specify the Bnd instructions embedded in the pom file. This
is not recommended but can be useful when the parent project is a repository based
pom. Bnd instructions in the pom are not used if the project has a bnd file.

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <configuration>
        <bnd><![CDATA[
-exportcontents:\
 org.example.api,\
 org.example.types
-sources: true
]]></bnd>
    </configuration>
</plugin>
```

The plugin adds the entire content of the project from
`${project.build.outputDirectory}` (but no other packages from 
the build path) to the bundle content.

NB: If there are no Bnd instructions for the project then the bundle will contain only private
packages: no Bundle-Activator, no Export-Package header, etc.  Therefore
although it will be valid, the bundle may not be *useful*.

For further usage information, see the integration test projects under the included
`src/test/resources/integration-test/test` directory.


## IMPORTANT NOTE

The `maven-jar-plugin` will NOT currently use the data from the generated 
MANIFEST.MF file when using its default configuration. We anticipate a [patch][1] 
to the JAR plugin that will do this.
In the meantime it is necessary to configure the plugin as follows:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <useDefaultManifestFile>true</useDefaultManifestFile>
    </configuration>
</plugin>
```

[1]: https://issues.apache.org/jira/browse/MJAR-193
