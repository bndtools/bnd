# bnd-run-maven-plugin

The `bnd-run-maven-plugin` is a bnd based plugin to run a framework from bndrun files.

## What does the `bnd-run-maven-plugin` do?

Point the plugin to one bndrun file in the same project. The bndrun file should be resolved (using the `bnd-resolver-maven-plugin`). This plugin will launch it using `bnd`'s project launching mechanism.

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

## Multiple `bndrun` Files per project

The `bnd-run-maven-plugin` supports multiple `bndrun` files per project when a unique execution is created for each one allowing each run invocation to be executed by id using the command pattern `bnd-run:run@<execution-id>`. 

**Note:** It is highly recommended to give every `bnd-run-maven-plugin` execution a unique `id`, even if there is only one in the project. See [Live Coding](#live-coding).

```xml
<plugin>
  <groupId>biz.aQute.bnd</groupId>
  <artifactId>bnd-run-maven-plugin</artifactId>
  <!-- shared configuration -->
  <configuration>
    <includeDependencyManagement>true</includeDependencyManagement>
  </configuration>
  <executions>
    <execution>
      <id>foo</id>
      <configuration>
        <bndrun>foo.bndrun</bndrun>
      </configuration>
    </execution>
    <execution>
      <id>bar</id>
      <configuration>
        <bndrun>bar.bndrun</bndrun>
      </configuration>
    </execution>
  </executions>
</plugin>
```

For example, executing a run for `bar.bndrun` as configured above  would be:

```bash
mvn bnd-run:run@bar
```

## Live Coding

To enable live coding the maven `package` phase must be called in conjunction with the run invocation. This causes the artifacts discovered for satisfying the `bndrun` file's `-runbundles` instruction to be those found in the `target` directory (as opposed to locally _installed_ artifacts). The run execution must remain active in the foreground during development.

Once the run is executing the bnd launcher takes care to reload modified artifacts. Therefore; **any tooling** which cause the project `target` artifacts to be rebuilt automatically can participate in live coding.

### Live Coding With Eclipse

When using [bndtools M2E Eclipse plugin](https://bndtools.org/installation.html) on a maven project configured to use the `bnd-maven-plugin` the jar plugin is invoked with each code change, automatically regenerating the `target` artifact. Therefore; no further configuration is required to use live coding.

### Live Coding With Intellij IDEA

Live coding can be enabled with **Intellij IDEA** using maven goal configurations as described in [this blog post](https://blog.io7m.com/2019/05/16/instant-code-reloading.xhtml).

### Multi-module Live Coding

A common maven pattern is having a multi-module structure; referred to as a reactor project. In this model a POM file contains a `<modules>` element listing a number of child modules (a.k.a. projects) which are built at once, in succession. In order to use this model with the `bnd-run-maven-plugin` with the goal of having each child module participate in live coding (so that each one is automatically reloaded):

1. the `bnd-run-maven-plugin` must be configured for every project such that invoking the plugin execution from the reactor does not result in errors (the plugin silently ignores projects not having bndrun files configured)

2. `bnd-run-maven-plugin` executions **must** have execution ids that are unique within the whole reactor so that any one can be invoked directly as described in [Multiple `bndrun` Files per project](#multiple-bndrun-files-per-project)

3. the `bnd-run:run` invocation must be run from the reactor along with the `package` phase as described in [Live Coding](#live-coding)

The resulting invocation from the reactor, where one of the modules is configured with a `bnd-run-maven-plugin` execution with id `bar`, should then be:

```bash
mvn package bnd-run:run@bar
```

## Bndrun Details Inferred from Maven

The `-runee` and `-runrequires` values can be inferred from the maven project as follows:

  * `-runee`, if omitted from the bndrun file, will be inferred from the `<target>` configuration of `maven-compiler-plugin`
  * `-runrequires`, if omitted from the bndrun file, will be inferred from the project's `artifactId` and applied as `osgi.identity;filter:='(osgi.identity=<artifactId>)'`, if the project packaging is `jar` or `war` and the project has the `bnd-maven-plugin`

## Implicit Repository

An *implicit repository* containing the project artifact and project dependencies (as defined through the configuration of `bundles`, `scopes`, `useMavenDependencies` and `includeDependencyManagement`) is created and added when this plugin is executed.

## Configuration Properties

|Configuration Property       | Description |
| ---                         | ---         |
|`bndrun`                     | A bndrun file to run. The file is relative to the `${project.basedir}` directory. Override with property `bnd.run.file`. |
|`bundles`                    | A collection of files to include in the *implicit repository*. Can contain `bundle` child elements specifying the path to a bundle. These can be absolute paths. You can also specify `include` and `exclude` child elements using Ant-style globs to specify bundles. These are relative to the `${project.basedir}` directory. _Defaults to dependencies in the scopes specified by the `scopes` property, plus the current artifact (if any and `useMavenDependencies` is `true`)._|
|`useMavenDependencies`       | If `true`, adds the project dependencies subject to `scopes` to the collection of files to include in the *implicit repository*. _Defaults to `true`._|
|`scopes`                     | Specify from which scopes to collect dependencies. _Defaults to `compile, runtime`._ Override with property `bnd.run.scopes`.|
|`includeDependencyManagement`| Include `<dependencyManagement>` subject to `scopes` when collecting files to include in the *implicit repository*. _Defaults to `false`._ Override with property `bnd.run.include.dependency.management`.|
