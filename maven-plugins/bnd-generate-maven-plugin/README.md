# bnd-generate-maven-plugin

The `bnd-generate-maven-plugin` is a bnd based plugin to run bnd generators. 

## What does the `bnd-generate-maven-plugin` do?

It provides a flexible mechanism, that can run system commands or run external generator plugins. A more general description about the use of generators in bnd can be found [here](https://bnd.bndtools.org/instructions/generate.html). A few examples can be found in the [tests](https://github.com/bndtools/bnd/tree/master/maven-plugins/bnd-generate-maven-plugin/src/it) for this plugin.

The Mojo comes with two goals: `generate` and `generate-test`. Both work similar except, that `generate-test` resolves test dependencies as well and will make them available as `-testbuildpath`. The `-buildpath` will be available in both cases and will represent all dependencies Maven knows about for this module.

An example that runs `dir` and pipes it to a file looks like

```
           <plugin>
				<groupId>biz.aQute.bnd</groupId>
				<artifactId>bnd-generate-maven-plugin</artifactId>
				<version>${bnd.version}</version>
				<configuration>
					<steps>
						<step>
							<trigger>src/main/resources/marker.txt</trigger>
							<systemCommand>dir > src/main/java/test.txt</systemCommand>
							<output>src/main/java</output>
							<properties>
								<genmodel>src/main/resources/model/test.genmodel</genmodel>
							</properties>
						</step>
					</steps>
				</configuration>
				<executions>
					<execution>
						<phase>generate-sources</phase>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
```

An example running a code generator twice located in an external artifact.

```
			<plugin>
				<groupId>biz.aQute.bnd</groupId>
				<artifactId>bnd-generate-maven-plugin</artifactId>
				<version>${bnd.version}</version>
				<configuration>
					<externalPlugins>
						<dependency>
							<groupId>org.geckoprojects.emf</groupId>
							<artifactId>org.gecko.emf.osgi.codegen</artifactId>
							<version>4.1.1.202202162308</version>
						</dependency>
					</externalPlugins>
					<steps>
						<step>
							<trigger>src/main/resources/model/test.genmodel</trigger>
							<generateCommand>geckoEMF 1>out.log 2>out.log</generateCommand>
							<output>src/main/java</output>
							<properties>
								<genmodel>src/main/resources/model/test.genmodel</genmodel>
							</properties>
						</step>
						<step>
							<trigger>src/main/resources/model/test.genmodel</trigger>
							<generateCommand>geckoEMF 1>out.log 2>out.log</generateCommand>
							<output>src/main/java2</output>
							<properties>
								<genmodel>src/main/resources/model/test.genmodel</genmodel>
							</properties>
						</step>
					</steps>
				</configuration>
				<executions>
					<execution>
						<phase>generate-sources</phase>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
```

## Configuration Properties

This plugin supports configuration via an bnd file which uses the standard `-generate` instruction as described in the [bnd documentation](https://bnd.bndtools.org/instructions/generate.html). A more maven typical configuration is supported as well via the steps. Please note, that the steps will be translated in in a list of instructions as bnd understands internally as per definition in the documentation. Concretely it will result in the instruction `-generate.maven:` which may be overwritten if the exact same namespace is used in the `bndfile` or `bnd` configuration.

| Configuration Property | Description                                                  |
| ---------------------- | ------------------------------------------------------------ |
|`bndfile`              | File path to a bnd file containing bnd instructions for this project. The file path can be either absolute or relative to the project directory. _Defaults to `bnd.bnd`_.|
|`bnd`                  | Bnd instructions for this project specified directly in the pom file. This is generally be done using a `<![CDATA[  ]]>` section. If the projects has a `bndfile` configuration property or a file in the default location `bnd.bnd`, then this configuration element is ignored. |
| `externalPlugins`      | A list of artefact's containing any of the desired code generators. The Artifact, that contains the required generator will be loaded by bnd in a completely isolated classloader and thus must be an uber jar containing all the classes it requires. |
| `steps`                | A list of generate steps, that will be executed in the given order |
| `step.trigger`         | A trigger is comprised of a ant like fileset expression like `src/main/resources/model/**.mymodel`.  If the file is newer then any file in the output folder, the code will be generated again. _This is required when a step is configured._ |
| `step.output`          | The output folder where the result will be expected. _This is required when a step is configured._ |
| `step.generateCommand` | A generate command conforms to the generate property in the original bnd [instruction](https://bnd.bndtools.org/instructions/generate.html). It can name an external plugin or call a main Class. _Default to `bnd.executablejar`._ |
| `step.systemCommand`   | A System command that will be executed if set.               |
| `step.properties`      | A map of additional properties that the generator to call my need. |
| `skip`                 | Skip the goal. _Defaults to `false`._ Override with property `bnd.generate.skip`. |
