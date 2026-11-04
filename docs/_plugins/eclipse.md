---
parent: Plugins
layout: default
title: Eclipse Plugin
summary: Will add .project and .classpath files to newly created projects 
---

This plugin automatically creates Eclipse project configuration files when a new project is created in the workspace. These files are essential for importing bnd projects into the Eclipse IDE.

## How It Works

When a new project is created, the Eclipse plugin will:

1. Create a `.project` file (Eclipse project descriptor)
2. Create a `.classpath` file (Eclipse classpath configuration)

If either file already exists, the plugin will not overwrite it.

## Custom Templates

The plugin looks for custom templates in the workspace configuration:

- Custom `.project` template: `cnf/eclipse/project.tmpl`
- Custom `.classpath` template: `cnf/eclipse/classpath.tmpl`

If custom templates exist, they will be used. Otherwise, default templates are used.

## Template Processing

Before writing the template files to disk, they are processed by the project's variable replacer. This allows you to use project variables within your custom templates.

For example, in your template, you can use:
- `${project.name}` - the project name
- `${basedir}` - the base directory
- Any other project properties defined in the project's `bnd.bnd` file

## Initialization

The Eclipse plugin also runs during workspace initialization, ensuring that:
- The workspace `cnf` project has the required `.project` and `.classpath` files
- All existing projects in the workspace get updated Eclipse files if they're missing

## Usage in build.bnd

The Eclipse plugin is typically enabled automatically in bnd workspaces. If you need to explicitly enable it, add the following to your `cnf/build.bnd`:

```properties
-plugin.Eclipse: \
  aQute.bnd.plugin.eclipse.EclipsePlugin
```

## Example Workspace Setup

To create custom Eclipse templates:

1. Create a custom `.project` template at `cnf/eclipse/project.tmpl`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
  <name>${project.name}</name>
  <comment>Custom Eclipse project for ${project.name}</comment>
  <projects>
  </projects>
  <buildSpec>
    <buildCommand>
      <name>org.eclipse.jdt.core.javabuilder</name>
      <arguments>
      </arguments>
    </buildCommand>
  </buildSpec>
  <natures>
    <nature>org.eclipse.jdt.core.javanature</nature>
  </natures>
</projectDescription>
```

2. Create a custom `.classpath` template at `cnf/eclipse/classpath.tmpl`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
  <classpathentry kind="src" path="src"/>
  <classpathentry kind="output" path="bin"/>
  <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
</classpath>
```

These templates will be used for all new projects created in the workspace.

<hr />
TODO Needs review - AI Generated content