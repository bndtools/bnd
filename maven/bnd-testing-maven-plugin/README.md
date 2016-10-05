# bnd-testing-maven-plugin

A plugin to run integration tests.

Point the plugin to a bndrun file in the same project. It will optionally resolve the project and test
a runnable jar into the targetDir. If not specified targetDir defaults to the project build directory.

The bndrun file must contain bundles that have the Test-Cases header set to class names that
contain the JUnit tests.

 
```
            <plugin>
                <groupId>biz.aQute.bnd</groupId>
                <artifactId>bnd-testing-maven-plugin</artifactId>
                <version>${bnd.version}</version>
                <configuration>
                    <bndruns>
                        <bndrun>mytest.bndrun</bndrun>
                    </bndruns>
                    <targetDir>.</targetDir>
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

You can use the following system properties on the command line:

	testing.select     A file path to a test file, overrides anything else
	testing            A glob expression that is matched against the file name of the listed bndrun files
