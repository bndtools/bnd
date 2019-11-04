# bnd-export-maven-plugin

The `bnd-export-maven-plugin` is a bnd based plugin to export bndrun files.

## What does the `bnd-export-maven-plugin` do?

Point the plugin to a bndrun file in the same project. It will export a runnable jar.

```
    <plugin>
        <groupId>biz.aQute.bnd</groupId>
        <artifactId>bnd-export-maven-plugin</artifactId>
        <version>${bnd.version}</version>
        <configuration>
            <failOnChanges>false</failOnChanges>
            <bndruns>
                <bndrun>mylaunch.bndrun</bndrun>
            </bndruns>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>export</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
```

Here's an example setting the `bundles` used for resolution.

```
    ...
    <configuration>
        ...
        <bundles>
            <bundle>bundles/org.apache.felix.eventadmin-1.4.8.jar</bundle>
            <bundle>bundles/org.apache.felix.framework-5.4.0.jar</bundle>
        </bundles>
    </configuration>
    ...
```

## Bndrun Details Inferred from Maven

The `-runee` and `-runrequires` values can be inferred from the maven project as follows:

  * `-runee`, if omitted from the bndrun file, will be inferred from the `<target>` configuration of `maven-compiler-plugin`
  * `-runrequires`, if omitted from the bndrun file, will be inferred from the project's `artifactId` and applied as `osgi.identity;filter:='(osgi.identity=<artifactId>)'`, if the project packaging is `jar` or `war` and the project has the `bnd-maven-plugin`

## Implicit Repository

An *implicit repository* containing the project artifact and project dependencies (as defined through the configuration of `bundles`, `scopes`, `useMavenDependencies` and `includeDependencyManagement`) is created and added when this plugin is executed.

## Configuration Properties

|Configuration Property       | Description |
| ---                         | ---         |
|`bndruns`                    | Can contain `bndrun` child elements naming a bndrun file to export. You can also specify `include` and `exclude` child elements using Ant-style globs to specify bndrun files. These are relative to the `${project.basedir}` directory. _Defaults to `<include>*.bndrun</include>`._|
|`targetDir`                  | The director into which to export the result. _Defaults to `${project.build.directory}`._|
|`resolve`                    | Whether to resolve the `-runbundles` required for a valid runtime. _Defaults to `false`._|
|`failOnChanges`              | Whether to fail the build if any change in the resolved `-runbundles` is discovered. _Defaults to `true`._|
|`exporter`                   | The name of the exporter plugin to use. Bnd has two built-in exporter plugins. `bnd.executablejar` exports an executable jar and `bnd.runbundles` exports the `-runbundles` files. _Default to `bnd.executablejar`._|
|`bundles`                    | A collection of files to include in the *implicit repository*. Can contain `bundle` child elements specifying the path to a bundle. These can be absolute paths. You can also specify `include` and `exclude` child elements using Ant-style globs to specify bundles. These are relative to the `${project.basedir}` directory. _Defaults to dependencies in the scopes specified by the `scopes` property, plus the current artifact (if any and `useMavenDependencies` is `true`)._ |
|`useMavenDependencies`       | If `true`, adds the project dependencies subject to `scopes` to the collection of files to include in the *implicit repository*. _Defaults to `true`._|
|`attach`                     | If `true` then if the exported generates a jar file, the jar file will be attached as an output of the current artifact. _Defaults to `true`._|
|`reportOptional`             | If `true`, resolution failure reports (see `resolve`) will include optional requirements. _Defaults to `true`._|
|`scopes`                     | Specify from which scopes to collect dependencies. _Defaults to `compile, runtime`._ Override with property `bnd.export.scopes`.|
|`includeDependencyManagement`| Include `<dependencyManagement>` subject to `scopes` when collecting files to include in the *implicit repository*. _Defaults to `false`._ Override with property `bnd.export.include.dependency.management`.|
|`skip`                       | Skip the project. _Defaults to `false`._ Override with property `bnd.export.skip`.|
