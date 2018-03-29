# bnd-testing-maven-plugin

The `bnd-testing-maven-plugin` is a bnd based plugin to run integration tests.

## What does the `bnd-testing-maven-plugin` do?

Point the plugin at one or more bndrun files in the same project. It will execute tests against the
runtime defined in the bndrun file.

The bndrun file must contain bundles that have the `Test-Cases` header set to class names that
contain the JUnit tests.

Here is an example configuration:
```
    <plugin>
        <groupId>biz.aQute.bnd</groupId>
        <artifactId>bnd-testing-maven-plugin</artifactId>
        <version>${bnd.version}</version>
        <configuration>
            <failOnChanges>false</failOnChanges>
            <bndruns>
                <bndrun>mytest.bndrun</bndrun>
            </bndruns>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>testing</goal>
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

|Configuration Property          | Description |
| ---                            | ---         |
|`bndruns`                       | Contains at least one `bndrun` child element, each element naming a bndrun file defining a runtime and tests to execute against it.|
|`resolve`                       | Whether to resolve the `-runbundles` required for a valid runtime. _Defaults to `false`._|
|`failOnChanges`                 | Whether to fail the build if any change in the resolved `-runbundles` is discovered. _Defaults to `true`._|
|`reportsDir`                    | The output directory for test reports. A subdirectory of `${bndrun}` will be created for each bndrun file supplied. _Defaults to `${project.build.directory}/test-reports`._|
|`cwd`                           | The current working directory of the test process. A subdirectory of `${bndrun}` will be created for each bndrun file supplied. _Defaults to `${project.build.directory}/test`._|
|`skipTests` OR `maven.test.skip`| Does not execute any tests. Used from the command line via `-D`. _Defaults to `false`._|
|`testingSelect`                 | A file path to a test file, overrides anything else. _Defaults to `${testing.select}`._ Override with property `testing.select`.|
|`testing`                       | A glob expression that is matched against the file name of the listed bndrun files. _Defaults to `${testing}`._ Override with property `testing`.|
|`test`                          | A comma separated list of the fully qualified names of test classes to run. If not set, or empty, then all the test classes listed in the `Test-Classes` manifest header are run. Override with property `test`.|
|`bundles`                       | This is the collection of files to use for locating bundles during the bndrun resolution. Paths are relative to `${project.basedir}` by default. Absolute paths are allowed. _Defaults to dependencies in the `compile` and `runtime`, plus the current artifact (if any)._|
|`useMavenDependencies`          | If `true`, adds the project's compile and runtime dependencies to the collection of files to use for locating bundles during the bndrun resolution. _Defaults to `true`._|
|`reportOptional`                | If `true`, resolution failure reports (see `resolve`) will include optional requirements. _Defaults to `true`._|
