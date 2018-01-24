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

The plugin only provides one goal `bnd-process` which is doing that. 
It is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to phase `process-classes`)

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>

    <executions>
        <execution>
            <goals>
                <goal>bnd-process</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

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

## Configuration Parameters

|Configuration Parameter | Description |
| ---                   | ---         |
|`bndfile`              | File path to a bnd file containing bnd instructions for this project. The file path can be either absolute or relative to the project directory. _Defaults to `bnd.bnd`_.|
|`bnd`                  | Bnd instructions for this project specified directly in the pom file. This is generally be done using a {@code <![CDATA[]]>} section. If the projects has a `bndfile` configuration property or a file in the default location `bnd.bnd`, then this configuration element is ignored. |
|`targetDir`            | The director into which to export the result. _Defaults to `${project.build.directory}`._|
|`sourceDir`            | Specify an alternative source directory. _Defaults to `${project.build.sourceDirectory}`._|
|`resources`            | Specify an alternative resources directory. _Defaults to `${project.build.resources}`._|
|`classesDir`           | Specify an alternative classes directory. _Defaults to `${project.build.outputDirectory}`._|
|`manifestPath`         | Specify the path to a manifest file to use. _Defaults to `${project.build.outputDirectory}/META-INF/MANIFEST.MF`._|
|`skip`                 | Skip the index process altogether. _Defaults to `false`._ Override with property `bnd.skip`.|

## Bnd Instruction Inheritance

This plugin supports a hybrid configuration model where Bnd instructions can come from a bnd file or configuration
in the project pom. Inheritance of configuration from parent projects is also supported for this hybrid configuration model.
At each project level in the project hierarchy, the configuration can come from a bnd file in the project or
from the configuration in the pom with the former taking precedence. This plugin merges the configurations from the parent project with the
configuration from the current project. If a parent project does not define a configuration for this plugin, then
the configuration, if any, from the `pluginManagement` section for this plugin is used as the configuration from
the parent project. This configuration contribution from the `pluginManagement` section for this plugin
is evaluated in the context of the current project.

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
        <archive>
            <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
        </archive>
    </configuration>
</plugin>
```

[1]: https://issues.apache.org/jira/browse/MJAR-193
