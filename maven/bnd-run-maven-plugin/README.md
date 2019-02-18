# bnd-run-maven-plugin

The `bnd-run-maven-plugin` is a bnd based plugin to run a framework from bndrun files.

## What does the `bnd-run-maven-plugin` do?

Point the plugin to one bndrun file in the same project. The bndrun file should be resolved. This plugin will launch it using `bnd`'s project launching mechanism.

This plugin can only be invoked directly.

```
mvn bnd-run:run
```

An example configuration.

```
    <plugin>
        <groupId>biz.aQute.bnd</groupId>
        <artifactId>bnd-run-maven-plugin</artifactId>
        <version>${bnd.version}</version>
        <configuration>
            <failOnChanges>false</failOnChanges>
            <includeDependencyManagement>true</includeDependencyManagement>
            <bndrun>mylaunch.bndrun</bndrun>
        </configuration>
    </plugin>
```

Here's an example setting the `bundles` used for execution.

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

|Configuration Property       | Description |
| ---                         | ---         |
|`bndrun`                     | A bndrun file to run. The file is relative to the `${project.basedir}` directory.|
|`bundles`                    | This is the collection of files to use for locating bundles during the bndrun setup. Can contain `bundle` child elements specifying the path to a bundle. These can be absolute paths. You can also specify `include` and `exclude` child elements using Ant-style globs to specify bundles. These are relative to the `${project.basedir}` directory. _Defaults to dependencies in the scopes specified by the `scopes` property, plus the current artifact (if any and `useMavenDependencies` is `true`)._|
|`useMavenDependencies`       | If `true`, adds the project's compile and runtime dependencies to the collection of files to use for locating bundles during the bndrun resolution. _Defaults to `true`._|
|`scopes`                     | Specify from which scopes to collect dependencies. _Defaults to `compile, runtime`._ Override with property `bnd.run.scopes`.|
|`includeDependencyManagement`| Include `<dependencyManagement>` when locating bundles during the bndrun resolution. _Defaults to `false`._ Override with property `bnd.run.include.dependency.management`.|
