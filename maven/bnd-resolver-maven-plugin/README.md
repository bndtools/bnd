# bnd-resolver-maven-plugin

The `bnd-resolver-maven-plugin` is a bnd based plugin to resolve bundles from bndrun files.

## What does the `bnd-resolver-maven-plugin` do?

Point the plugin to one or more bndrun files in the same project. It will resolve the -runbundles value.

```
    <plugin>
        <groupId>biz.aQute.bnd</groupId>
        <artifactId>bnd-resolver-maven-plugin</artifactId>
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
                    <goal>resolve</goal>
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

## Executing the resolve operation

Since the resolve operation is not associated with any maven build phase, it must in invoked manually.

Here's an example invocation:
```
mvn bnd-resolver:resolve
```

## Configuration Properties

|Configuration Property | Description |
| ---                   | ---         |
|`bndruns`              | Can contain `bndrun` child elements naming a bndrun file to resolv. You can also specify `include` and `exclude` child elements using Ant-style globs to specify bndrun files. These are relative to the `${project.basedir}` directory. _Defaults to `<include>*.bndrun</include>`._|
|`failOnChanges`        | Whether to fail the build if any change in the resolved `-runbundles` is discovered. _Defaults to `true`._|
|`bundles`              | This is the collection of files to use for locating bundles during the bndrun resolution. Can contain `bundle` child elements specifying the path to a bundle. These can be absolute paths. You can also specify `include` and `exclude` child elements using Ant-style globs to specify bundles. These are relative to the `${project.basedir}` directory. _Defaults to dependencies in the `compile` and `runtime`, plus the current artifact (if any and `useMavenDependencies` is `true`)._|
|`useMavenDependencies` | If `true`, adds the project's compile and runtime dependencies to the collection of files to use for locating bundles during the bndrun resolution. _Defaults to `true`._|
|`reportOptional`       | If `true`, resolution failure reports will include optional requirements. _Defaults to `true`._|
