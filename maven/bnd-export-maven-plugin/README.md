# bnd-export-maven-plugin

A plugin to optionally resolve and export bndrun files.

Point the plugin to a bndrun file in the same project. It will optionally resolve the project and export
a runnable jar into the targetDir. If not specified targetDir defaults to the project build directory.

```
            <plugin>
                <groupId>biz.aQute.bnd</groupId>
                <artifactId>bnd-export-maven-plugin</artifactId>
                <version>${bnd.version}</version>
                <configuration>
                    <bndruns>
                        <bndrun>mylaunch.bndrun</bndrun>
                    </bndruns>
                    <targetDir>.</targetDir>
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

