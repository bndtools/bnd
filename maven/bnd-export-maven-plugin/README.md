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

## Configuration Properties

|Configuration Property | Description |
| ---                   | ---         |
|`bndruns`              | Contains at least one `bndrun` child element, each element naming a bndrun file defining a runtime and tests to execute against it.|
|`targetDir`            | The director into which to export the result. _Defaults to `${project.build.directory}`._|
|`resolve`              | Whether to resolve the `-runbundles` required for a valid runtime. _Defaults to `false`._|
|`failOnChanges`        | Whether to fail the build if any change in the resolved `-runbundles` is discovered. _Defaults to `true`._|
|`exporter`          | The name of the exporter plugin to use. Bnd has two built-in exporter plugins. `bnd.executablejar` exports an executable jar and `bnd.runbundles` exports the -runbundles files. _Default to `bnd.executablejar`._|
|`bundles`              | This is the collection of files to use for locating bundles during the bndrun resolution. Paths are relative to `${project.basedir}` by default. Absolute paths are allowed. _Defaults to dependencies in the `compile` and `runtime`, plus the current artifact (if any)._|
|`useMavenDependencies` | If `true`, adds the project's compile and runtime dependencies to the collection of files to use for locating bundles during the bndrun resolution. _Defaults to `true`._|
|`attach` | If `true` then if the exported generates a jar file, the jar file will be attached as an output of the current artifact. _Defaults to `true`._|
|`reportOptional`       | If `true`, resolution failure reports (see `resolve`) will include optional requirements. _Defaults to `true`._|
