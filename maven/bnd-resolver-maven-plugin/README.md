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

## Executing the resolve operation

Since the resolve operation is not associated with any maven build phase, it must in invoked manually.

Here's an example invocation:
```
mvn bnd-resolver:resolve
```

## Configuration Properties

|Configuration Property | Description |
| ---                   | ---         |
|`bndruns`              | Contains at least one `bndrun` child element, each element naming a bndrun file defining a runtime and tests to execute against it.|
|`failOnChanges`        | Whether to fail the build if any change in the resolved `-runbundles` is discovered. _Defaults to `true`._|

