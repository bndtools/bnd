---
parent: Plugins
layout: default
title: Ant Workspace Plugin
summary: Ensures that when a new project is created it also has a build.xml for an ant build 
---

This plugin automatically creates a `build.xml` file in newly created projects. This enables Ant-based builds within a bnd workspace.

## How It Works

When a new project is created in the workspace, the Ant plugin will:

1. Look for a custom template at `cnf/ant/project.xml` in the workspace
2. If found, copy the custom template to the new project as `build.xml`
3. If not found, use a default `build.xml` that imports the workspace build configuration

## Default Template

If no custom template is provided, the plugin uses a default build.xml template that:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<project name='project' default='build'>
  <import file='../cnf/build.xml' />
</project>
```

This default template imports the workspace-level Ant configuration from `cnf/build.xml`, allowing individual projects to benefit from centralized build definitions.

## Customization

To use a custom Ant build template:

1. Create a file at `cnf/ant/project.xml` in your workspace
2. This template will be copied to each new project's `build.xml` file
3. You can use project-specific properties and targets as needed

## Usage in build.bnd

The Ant plugin is typically enabled automatically in bnd workspaces. If you need to explicitly enable it or configure it, add the following to your `cnf/build.bnd`:

```properties
-plugin.Ant: \
  aQute.bnd.plugin.ant.AntPlugin
```

When you create a new project in the workspace (either through the bnd CLI or IDE), the plugin will automatically generate the `build.xml` file for that project.

## Example Workspace Setup

To use a custom Ant template:

1. Create your custom template at `cnf/ant/project.xml`:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<project name='${project.name}' default='build' basedir='.'>
  <property name='src' value='src'/>
  <property name='bin' value='bin'/>
  <import file='../cnf/build.xml' />
  
  <target name='compile'>
    <javac srcdir='${src}' destdir='${bin}'/>
  </target>
</project>
```

2. When a new project is created, this template will be used instead of the default.

<hr />
TODO Needs review - AI Generated content