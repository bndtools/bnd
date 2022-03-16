# bnd-maven-plugin

This is a Maven plugin for invoking Bnd. This plugin contains the following goals:

- [`jar`](#jar-goal)
- [`bnd-process`](#bnd-process-goal)
- [`test-jar`](#test-jar-goal)
- [`bnd-process-tests`](#bnd-process-tests-goal)

## `jar` goal

The `jar` goal binds to the `package` phase and generates resources such as:

* `META-INF/MANIFEST.MF`
* Declarative Services metadata under `OSGI-INF`
* Metatype Service files under `OSGI-INF/metatype`
* things added using the `-includeresource` instruction
* etc...

All of the above resources will be generated into the final artifact.

When using this goal the `bnd-maven-plugin` must be configured with `<extensions>true</extensions>` in order to coordinate with the `maven-(jar|war)-plugin`.

The `jar` goal is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to phase `package`)

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>jar</id>
            <goals>
                <goal>jar</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

  **Note**: This goal **replaces** _matching_ executions of the `maven-(jar|war)-plugin` (more on [_matching executions_](#matching-executions)).

### Configuration Parameters

|Configuration Parameter | Description |
| ---                   | ---         |
|`bndfile`              | File path to a bnd file containing bnd instructions for this project. The file path can be either absolute or relative to the project directory. _Defaults to `bnd.bnd`_.|
|`bnd`                  | Bnd instructions for this project specified directly in the pom file. This is generally be done using a `<![CDATA[  ]]>` section. If the projects has a `bndfile` configuration property or a file in the default location `bnd.bnd`, then this configuration element is ignored. |
|`classifier`          | A string added to the artifact indicating a supplemental artifact produced by the project. If no value is provided it indicates the main artifact produced by the project. _Defaults to no value_.||`manifestPath`         | Specify the path to a manifest file to use. _Defaults to `${project.build.outputDirectory}/META-INF/MANIFEST.MF`._|
|`classesDir`           | The directory where the `maven-compiler-plugin` places its output. _Defaults to `${project.build.outputDirectory}`._|
|`includeClassesDir` | Include the entire contents of `classesDir` in the bundle. *Defaults to `true`*. |
|`outputDir`            | The directory where the `bnd-maven-plugin` will place the jar when packaging. _Defaults to `${project.build.directory}`._|
|`warOutputDir`            | The directory where the `bnd-maven-plugin` will place the war when packaging is `war`. _Defaults to `${project.build.directory}`._|
|`packagingTypes`                | The list of maven packaging types for which the plugin will execute. *Defaults to `jar,war`*. Override with property `bnd.packagingTypes`. |
|`skip`                 | Skip the project. _Defaults to `false`._ Override with property `bnd.skip`.|
|`skipIfEmpty`         | Skip processing if `includeClassesDir` is `true` and the `${project.build.outputDirectory}` is empty. _Defaults to `false`._ Override with property `bnd.skipIfEmpty`.|

**No additional packaging plugins are necessary when using the `jar` goal.**

#### Matching executions

The `bnd-maven-plugin` does not blindly remove all `maven-(jar|war)-plugin` found in the project, only those whose executions match by `goal`, `packaging` and `classifier`. Therefore it is possible to have both plugins operating within the same project provided they do not overlap. (And remember that no `classifier` means the main artifact.)

## `bnd-process` goal

The `bnd-process` goal binds to the `process-classes` phase and generates resources such as:

* `META-INF/MANIFEST.MF`
* Declarative Services metadata under `OSGI-INF`
* Metatype Service files under `OSGI-INF/metatype`
* things added using the `-includeresource` instruction
* etc...

All of the above resources will be generated into the project build output directory, e.g. `target/classes`, to be subsequently picked up and included in the JAR file by the default `maven-jar-plugin`.

The `bnd-process` is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to phase `process-classes`)

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>bnd-process</id>
            <goals>
                <goal>bnd-process</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Configuration Parameters

|Configuration Parameter | Description |
| ---                   | ---         |
|`bndfile`              | File path to a bnd file containing bnd instructions for this project. The file path can be either absolute or relative to the project directory. _Defaults to `bnd.bnd`_.|
|`bnd`                  | Bnd instructions for this project specified directly in the pom file. This is generally be done using a `<![CDATA[  ]]>` section. If the projects has a `bndfile` configuration property or a file in the default location `bnd.bnd`, then this configuration element is ignored. |
|`manifestPath`         | Specify the path to a manifest file to use. _Defaults to `${project.build.outputDirectory}/META-INF/MANIFEST.MF`._|
|`classesDir`           | The directory where the `maven-compiler-plugin` places its output. _Defaults to `${project.build.outputDirectory}`._|
|`includeClassesDir` | Include the entire contents of `classesDir` in the bundle. *Defaults to `true`*. |
|`outputDir`            | The directory where the `bnd-maven-plugin` will extract it's contents. _Defaults to `${project.build.outputDirectory}`._|
|`warOutputDir`            | The directory where the `bnd-maven-plugin` will extract it's contents when packaging is `war`. _Defaults to `${project.build.directory}/${project.build.finalName}`._|
|`packagingTypes`                | The list of maven packaging types for which the plugin will execute. *Defaults to `jar,war`*. Override with property `bnd.packagingTypes`. |
|`skip`                 | Skip the project. _Defaults to `false`._ Override with property `bnd.skip`.|
|`skipIfEmpty`         | Skip processing if `includeClassesDir` is `true` and the `${project.build.outputDirectory}` is empty. _Defaults to `false`._ Override with property `bnd.skipIfEmpty`.|

### IMPORTANT NOTE about Maven JAR|WAR Plugin

When using the `bnd-process` goal it is important to take the following into consideration. The `maven-(jar|war)-plugin` will NOT currently use the data from the generated `MANIFEST.MF` file when using its default configuration.It is therefore necessary to configure the `maven-(jar|war)-plugin` as follows:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
        </archive>
    </configuration>
</plugin>
```

## Aspects Common to all goals

Bnd instructions may be declared in a bnd file or in this plugin's configuration in the pom. The default bnd file is named `bnd.bnd` in the base directory of the project. This can be configured to specify an alternate path which can be absolute or relative to the base directory of the project. In the following example, the `project.bnd` file in the `bnd` folder of the project will be used.

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <configuration>
        <bndfile>bnd/project.bnd</bndfile>
    </configuration>
</plugin>
```

It is also supported to specify the Bnd instructions embedded in the pom file. This is not the preferred option but can be useful in many scenarios. Bnd instructions in the pom are not used if the project has a bnd file.

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <configuration>
        <bnd><![CDATA[
            -exportcontents:\
                org.example.api,\
                org.example.types
            -sources: true
        ]]></bnd>
    </configuration>
</plugin>
```

  **Note**: Deep indentation of the `<bnd>` content to match the xml indentation functions perfectly well.

The contents of `${project.build.outputDirectory}` is always included as input to the plugin (*i.e. placed in the bnd builder's classpath*).

Optionally, the plugin adds the entire content of `${project.build.outputDirectory}` to the bundle content (but no other packages from the build path). This behavior is **enabled** by default. (*See `includeClassesDir` configuration parameter*).

For further usage information, see the integration test projects under the included
`src/test/resources/integration-test/test` directory.

### Default Bundle Headers

The plugin will by default set some OSGi bundle headers derived from [pom elements](https://maven.apache.org/pom.html) (if not overwritten with explicit bnd instructions).

|OSGi Header | Derived from POM Element |
| --- | --- |
| `Bundle-SymbolicName` | `artifactId` |
| `Bundle-Name` | `name` |
| `Bundle-Version` | `version` |
| `Bundle-Description` | `description` |
| `Bundle-Vendor` | `organisation.name` |
| `Bundle-License` | `licenses` |
| `Bundle-SCM` | `scm` |
| `Bundle-Developers` | `developers` (child element `id` must be set on each developer) |
| `Bundle-DocURL` | `url` |

### Reproducible Builds

If the Maven project property `project.build.outputTimestamp` is set, indicating [reproducible builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html), this plugin will automatically use the following Bnd instructions, if not otherwise configured.

To support reproducible builds, the following Bnd instructions need to be configured:

```properties
-noextraheaders: true
-snapshot: SNAPSHOT
```

The `-noextraheaders: true` instruction will prevent Bnd from adding extra manifest headers whose values depend upon the build environment. The `-snapshot: SNAPSHOT` instruction will prevent Bnd from replacing the version qualifier `SNAPSHOT` in the `Bundle-Version` manifest header with the build time stamp. The latter instruction only makes a difference for snapshot builds since release builds do not have the version qualifier `SNAPSHOT`.

### Bnd Instruction Inheritance

This plugin supports a hybrid configuration model where Bnd instructions can come from a bnd file or configuration in the project pom. Inheritance of configuration from parent projects is also supported for this hybrid configuration model. At each project level in the project hierarchy, the configuration can come from a bnd file in the project or from the configuration in the pom with the former taking precedence. This plugin merges the configurations from the parent project with the configuration from the current project. If a parent project does not define a configuration for this plugin, then the configuration, if any, from the `pluginManagement` section for this plugin is used as the configuration from the parent project. This configuration contribution from the `pluginManagement` section for this plugin is evaluated in the context of the current project.

### Usage Scenarios

The plugin has 6 distinct usage scenarios broken into two groups.

#### Group 1: Scenarios based on the `jar` goal

Given executions using the `jar` goal we have the following 3 cases:

1. The common case where very little configuration is required; inputs and outputs are based on defaults. Bnd performs it's analysis and enhances the jar with OSGi metadata obtained through introspection of classes, resources, dependencies, and [OSGi bundle annotations](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle):

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
   </plugin>
   ```

2. The _filtering_ case, is a more advanced usage emulating the behavior found in the Bnd _Workspace_ model where instructions are used to include (i.e. *filter*) contents in the jar.

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
       <configuration>
           <includeClassesDir>false</includeClassesDir>
           <bnd><![CDATA[
               -includepackage: foo.*
           ]]></bnd>
       </configuration>
   </plugin>
   ```

3. The third case is when the project packaging is `war` and so a WAR is produced:

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
       <configuration>
           <bnd><![CDATA[
               Web-ContextPath: /${project.build.finalName}
           ]]></bnd>
       </configuration>
   </plugin>
   ```

   **Note**: When the project has packaging mode `war` the instruction `-wab:` is automatically enabled and the `Bundle-ClassPath` is properly treated such that any dependency libraries are added following the heuristics defined for the inclusion of dependencies of the `maven-war-plugin`. However, if the `-wablib:` instruction is used, then this behavior is disabled and left to that instruction.

#### Group 2: Scenarios based on the `bnd-process` goal

Given executions using the `bnd-process` goal we have the following 3 cases:

1. The common case is that very little configuration is required; inputs and outputs are based on defaults. Bnd performs it's analysis and enhances the jar with OSGi metadata obtained through introspection of classes, resources, dependencies, and [OSGi bundle annotations](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle):

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
   </plugin>
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-jar-plugin</artifactId>
       <configuration>
           <archive>
               <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
           </archive>
       </configuration>
   </plugin>
   ```

2. The second case, let's call it the _filtering_ case, is a more advanced usage emulating the behavior found in the Bnd _Workspace_ model where instructions are used to include (i.e. *filter*) contents in the jar.

   **Note** that controlling the actual contents of the jar is handled through configuration of the `maven-jar-plugin`:

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
       <configuration>
           <includeClassesDir>false</includeClassesDir>
           <bnd><![CDATA[
               -includepackage: foo.*
           ]]></bnd>
       </configuration>
   </plugin>
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-jar-plugin</artifactId>
       <configuration>
           <archive>
               <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
           </archive>
           <includes>
               <include>META-INF/*</include>
               <include>OSGI-INF/*</include>
               <include>OSGI-OPT/*</include>
               <include>foo/*</include>
           </includes>
       </configuration>
   </plugin>
   ```

3. The third case is when the project packaging is `war`, for instance when the plugin is paired with the `maven-war-plugin` instead of the `maven-jar-plugin`. The output of the plugin re-directed to the default assembly directory of the `maven-war-plugin`, which is `${project.build.directory}/${project.build.finalName}`:

   ```xml
   <plugin>
       <groupId>biz.aQute.bnd</groupId>
       <artifactId>bnd-maven-plugin</artifactId>
       <configuration>
           <bnd><![CDATA[
               Web-ContextPath: /${project.build.finalName}
           ]]></bnd>
       </configuration>
   </plugin>
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-war-plugin</artifactId>
       <configuration>
           <archive>
               <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
           </archive>
       </configuration>
   </plugin>
   ```

   **Note**: When the project has packaging mode `war` the instruction `-wab:` is automatically enabled and the `Bundle-ClassPath` is properly treated such that any dependency libraries are added following the heuristics defined for the inclusion of dependencies of the `maven-war-plugin`. However, if the `-wablib:` instruction is used, then this behavior is disabled and left to that instruction.

## `test-jar` goal

The `test-jar` goal binds to the `package` phase and behaves in the identical fashion as the `jar` goal except that the intention is to generate an artifact from test code.

When using this goal the `bnd-maven-plugin` must be configured with `<extensions>true</extensions>` in order to coordinate with the `maven-(jar|war)-plugin`.

The `test-jar` goal is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to phase `package`)

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>test-jar</id>
            <goals>
                <goal>test-jar</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

  **Note**: This goal **replaces** _matching_ executions of the `maven-(jar|war)-plugin` (more on [_matching executions_](#matching-executions)).

### Configuration Parameters

| Configuration Parameter | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| `bndfile`               | File path to a bnd file containing bnd instructions for this project. The file path can be either absolute or relative to the project directory. _Defaults to `bnd.bnd`_. |
| `bnd`                   | Bnd instructions for this project specified directly in the pom file. This is generally be done using a `<![CDATA[  ]]>` section. If the projects has a `bndfile` configuration property or a file in the default location `bnd.bnd`, then this configuration element is ignored. |
|`classifier`          | A string added to the artifact indicating a supplemental artifact produced by the project. _Defaults to `tests`_.|
| `includeClassesDir`     | Include the entire contents of `classesDir` in the bundle. *Defaults to `true`*. |
| `skip`                  | Skip the goal. _Defaults to `false`._ Override with property `bnd-tests.skip` or `maven.test.skip`. |
| `artifactFragment`      | If true, make the tests artifact a fragment using `${project.artifactId}` as the `Fragment-Host` header and setting the `Bundle-SymbolicName` of the tests artifact to `${project.artifactId}-tests`. *Defaults to `false`*. |
| `testCases`             | Specify the filter that will determine which classes to identify as test cases. *Defaults to `junit5`*. See [Test Cases](#test-cases). |
|`skipIfEmpty`         | Skip processing if `includeClassesDir` is `true` and the `${project.build.testOutputDirectory}` is empty. _Defaults to `false`._ Override with property `bnd.skipIfEmpty`.|

Some details are predefined for simplicity:
- `${project.build.testSourceDirectory}` is used as a the source directory
- `${project.build.testResources}` is used as the resources directory
- `${project.build.testOutputDirectory}` is used as the source of input for bnd to analyse
- `${project.build.testOutputDirectory}/META-INF/MANIFEST.MF` is used as the manifest path

**No additional packaging plugins are necessary when using the `test-jar` goal.**

## `bnd-process-tests` goal

The `bnd-process-tests` goal binds to the `process-test-classes` phase and behaves in the identical fashion as the `bnd-process` goal except that the intention is to generate a bundle from test code output to `target/test-classes`.

The `bnd-process-tests` is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to phase `process-test-classes`)

```xml
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>bnd-process-tests</id>
            <goals>
                <goal>bnd-process-tests</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Configuration Parameters

| Configuration Parameter | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| `bndfile`               | File path to a bnd file containing bnd instructions for this project. The file path can be either absolute or relative to the project directory. _Defaults to `bnd.bnd`_. |
| `bnd`                   | Bnd instructions for this project specified directly in the pom file. This is generally be done using a `<![CDATA[  ]]>` section. If the projects has a `bndfile` configuration property or a file in the default location `bnd.bnd`, then this configuration element is ignored. |
| `includeClassesDir`     | Include the entire contents of `classesDir` in the bundle. *Defaults to `true`*. |
| `skip`                  | Skip the goal. _Defaults to `false`._ Override with property `bnd-tests.skip` or `maven.test.skip`. |
| `artifactFragment`      | If true, make the tests artifact a fragment using `${project.artifactId}` as the `Fragment-Host` header and setting the `Bundle-SymbolicName` of the tests artifact to `${project.artifactId}-tests`. *Defaults to `false`*. |
| `testCases`             | Specify the filter that will determine which classes to identify as test cases. *Defaults to `junit5`*. See [Test Cases](#test-cases). |
|`skipIfEmpty`         | Skip processing if `includeClassesDir` is `true` and the `${project.build.testOutputDirectory}` is empty. _Defaults to `false`._ Override with property `bnd.skipIfEmpty`.|

Some details are predefined for simplicity:
- `${project.build.testSourceDirectory}` is used as a the source directory
- `${project.build.testResources}` is used as the resources directory
- `${project.build.testOutputDirectory}` is used as the source of input for bnd to analyse
- `${project.build.testOutputDirectory}/META-INF/MANIFEST.MF` is used as the manifest path

### IMPORTANT NOTE about Maven JAR Plugin

When using the `bnd-process-tests` goal it is important to take the following into consideration. The `maven-jar-plugin` provides the goal `test-jar` for building a jar from a project's test classes. It is bound to the `package` phase but has no default execution and so one must be configured. Like the `jar` goal it will NOT currently use the data from the generated `MANIFEST.MF` file when using its default configuration so it is necessary to configure it as follows:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <executions>
        <execution>
            <id>test-jar</id>
            <goals>
                <goal>test-jar</goal>
            </goals>
            <configuration>
                <archive>
                    <manifestFile>${project.build.testOutputDirectory}/META-INF/MANIFEST.MF</manifestFile>
                </archive>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Aspects Common to both test goals

### Test Cases

Bnd's integration testing uses the manifest header `Test-Cases` to identify classes within a bundle that are test cases. This eliminates the need for runtime class scanning. The `bnd-maven-plugin` simplifies this configuration by creating several predefined specifications of test cases that use bnd's `classes` macro:
- **`junit3`** - represents the filter `${classes;EXTENDS;junit.framework.TestCase;CONCRETE}`.
- **`junit4`** - represents the filter `${classes;HIERARCHY_ANNOTATED;org.junit.Test;CONCRETE}`.
- **`junit5`** - represents the filter `${classes;HIERARCHY_INDIRECTLY_ANNOTATED;org.junit.platform.commons.annotation.Testable;CONCRETE}`.
- **`all`** - represents all the JUnit filters: `junit3`, `junit4`, and `junit5`.
- **`testng`** - represents the filter `${classes;HIERARCHY_ANNOTATED;org.testng.annotations.Test;CONCRETE}`. Note: A JUnit Platform engine for TestNG, or other means to run TestNG tests, must be in the test execution runtime.
- **`useTestCasesHeader`** - indicates that the the `Test-Cases` header in the bnd configuration should be used instead. The build will fail if this value is set and there is no `Test-Cases` header in the bnd configuration.
