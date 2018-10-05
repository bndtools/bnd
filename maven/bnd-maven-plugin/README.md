# bnd-maven-plugin

This is a Maven plugin for invoking Bnd.

The plugin hooks into the `process-classes` phase and generates resources such as:

* `META-INF/MANIFEST.MF`
* Declarative Services metadata under `OSGI-INF`
* Metatype Service files under `OSGI-INF/metatype`
* things added using the `-includeresource` instruction
* etc...

All of the above resources will be generated into the project build output directory, e.g. `target/classes`, to be subsequently picked up and included in the JAR file by the default `maven-jar-plugin`.

The plugin only provides one goal `bnd-process`. It is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to phase `process-classes`)

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

Bnd instructions may be declared in a bnd file or in this plugin's configuration in the pom. The default bnd file is named `bnd.bnd` in the base directory of the project. This can be configured to specify an alternate path which can be absolute or relative to the base directory of the project. In the following example, the `project.bnd` file in the `bnd` folder of the project will be used.

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <configuration>
        <bndfile>bnd/project.bnd</bndfile>
    </configuration>
</plugin>
```

It is also supported to specify the Bnd instructions embedded in the pom file. This is *not* recommended but can be useful when the parent project is a repository based pom. Bnd instructions in the pom are not used if the project has a bnd file.

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

The contents of `${project.build.outputDirectory}` is always included as input to the plugin (*i.e. placed in the bnd builder's classpath*).

Optionally, the plugin adds the entire content of `${project.build.outputDirectory}` to the bundle content (but no other packages from the build path). This behavior is **enabled** by default. (*See `includeClassesDir` configuration parameter*).

For further usage information, see the integration test projects under the included
`src/test/resources/integration-test/test` directory.

## Configuration Parameters

|Configuration Parameter | Description |
| ---                   | ---         |
|`bndfile`              | File path to a bnd file containing bnd instructions for this project. The file path can be either absolute or relative to the project directory. _Defaults to `bnd.bnd`_.|
|`bnd`                  | Bnd instructions for this project specified directly in the pom file. This is generally be done using a {@code <![CDATA[]]>} section. If the projects has a `bndfile` configuration property or a file in the default location `bnd.bnd`, then this configuration element is ignored. |
|`manifestPath`         | Specify the path to a manifest file to use. _Defaults to `${project.build.outputDirectory}/META-INF/MANIFEST.MF`._|
|`classesDir`           | The directory where the `maven-compiler-plugin` places its output. _Defaults to `${project.build.outputDirectory}`._|
|`includeClassesDir` | Include the entire contents of `classesDir` in the bundle. *Defaults to `true`*. |
|`outputDir`            | The directory where the `bnd-maven-plugin` will extract it's contents. _Defaults to `${project.build.outputDirectory}`._|
|`warOutputDir`            | The directory where the `bnd-maven-plugin` will extract it's contents when packaging is `war`. _Defaults to `${project.build.directory}/${project.build.finalName}`._|
|`skip`                 | Skip the project. _Defaults to `false`._ Override with property `bnd.skip`.|

## Bnd Instruction Inheritance

This plugin supports a hybrid configuration model where Bnd instructions can come from a bnd file or configuration in the project pom. Inheritance of configuration from parent projects is also supported for this hybrid configuration model. At each project level in the project hierarchy, the configuration can come from a bnd file in the project or from the configuration in the pom with the former taking precedence. This plugin merges the configurations from the parent project with the configuration from the current project. If a parent project does not define a configuration for this plugin, then the configuration, if any, from the `pluginManagement` section for this plugin is used as the configuration from the parent project. This configuration contribution from the `pluginManagement` section for this plugin is evaluated in the context of the current project.

## IMPORTANT NOTE

The `maven-jar-plugin` will NOT currently use the data from the generated `MANIFEST.MF` file when using its default configuration. We anticipate a [patch][1] to the JAR plugin that will do this. In the meantime it is necessary to configure the `maven-jar-plugin` plugin as follows:

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

## Usage Scenarios

The plugin has 3 distinct usage scenarios:

1. The common case is that very little configuration is required; inputs and outputs are based on defaults. Bnd performs it's analysis and enhances the jar with OSGi metadata obtained through introspection of classes, resources, dependencies, and [OSGi bundle annotations](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle):

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
   </plugin>
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

2. The second case, let's call it the _filtering_ case, is a more advanced usage emulating the behavior found in the Bnd _Workspace_ model where instructions are used to include (i.e. *filter*) contents in the jar.

   **Note** that controlling the actual contents of the jar is handled through configuration of the `maven-jar-plugin`:

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
       <configuration>
           <includeClassesDir>false</includeClassesDir>
           <bnd><![CDATA[
               Private-Package: foo.*
           ]]></bnd>
       </configuration>
   </plugin>
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-jar-plugin</artifactId>
       <configuration>
           <archive>
               <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
           </archive>
           <includes>
               <include>META-INF/*</include>
               <include>OSGI-INF/*</include>
               <include>OSGI-OPT/*</include>
               <include>foo/*</include>
           </includes>
       </configuration>
   </plugin>
   ```

3. The third case is when the project packaging is `war`, for instance when the plugin is paired with the `maven-war-plugin` instead of the `maven-jar-plugin`. The output of the plugin re-directed to the default assembly directory of the `maven-war-plugin`, which is `${project.build.directory}/${project.build.finalName}`:

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
       <configuration>
           <bnd><![CDATA[
               Web-ContextPath: /${project.build.finalName}
           ]]></bnd>
       </configuration>
   </plugin>
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-war-plugin</artifactId>
       <configuration>
           <archive>
               <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
           </archive>
       </configuration>
   </plugin>
   ```

   **Note**: When the project has packaging mode `war` the instruction `-wab:` is automatically enabled and the `Bundle-ClassPath` is properly treated such that any dependency libraries are added following the heuristics defined for the inclusion of dependencies of the `maven-war-plugin`. However, if the `-wablib:` instruction is used, then this behavior is disabled and left to that instruction.


[1]: https://issues.apache.org/jira/browse/MJAR-193
