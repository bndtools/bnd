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

## Configuration Properties

|Configuration Property | Description |
| ---                   | ---         |
|`bndruns`              | Contains at least one `bndrun` child element, each element naming a bndrun file defining a runtime and tests to execute against it.|
|`targetDir`            | The director into which to export the result. _Defaults to `${project.build.directory}`._|
|`resolve`              | Whether to resolve the `-runbundles` required for a valid runtime. _Defaults to `false`._|
|`failOnChanges`        | Whether to fail the build if any change in the resolved `-runbundles` is discovered. _Defaults to `true`._|
|`bundlesOnly`          | Instead of creating an executable jar place runbundles into `targetDir`. _Defaults to `false`._|
