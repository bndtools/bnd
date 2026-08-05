---
parent: Plugins
layout: default
title: Git Workspace Plugin
summary: Ensures that certain directories have a gitignore 
---

This plugin automatically creates `.gitignore` files in new projects to ensure that generated build artifacts are not committed to version control.

## How It Works

When a new project is created, the Git plugin will:

1. Create a `.gitignore` file in the project root that excludes common build directories
2. Create `.gitignore` files in source directories to preserve empty directory structure in Git

## Default Ignore Patterns

The plugin automatically creates entries to ignore the following directories (though actual names can be customized via project properties):

- `generated/` - Directory for generated build artifacts (default: from `DEFAULT_PROP_TARGET_DIR`, usually "generated")
- `bin/` - Compiled binary output (default: from `DEFAULT_PROP_BIN_DIR`, usually "bin")
- `bin_test/` - Compiled test output (default: from `DEFAULT_PROP_TESTBIN_DIR`, usually "bin_test")

These patterns prevent accidental commits of compiled code and generated resources.

## Preserving Empty Directories

The plugin also creates minimal `.gitignore` files in source directories (as returned by the project's source path and test source configuration). These empty `.gitignore` files ensure that Git preserves the directory structure even if the directories are initially empty, which is important for bnd project initialization.

## Customization

You can customize the directory names that should be ignored by modifying the project properties:

- `DEFAULT_PROP_TARGET_DIR` - Directory for generated artifacts
- `DEFAULT_PROP_BIN_DIR` - Directory for compiled output
- `DEFAULT_PROP_TESTBIN_DIR` - Directory for compiled test output

These properties are defined in `aQute.bnd.osgi.Constants`.

## Usage in build.bnd

The Git plugin is typically enabled automatically in bnd workspaces. If you need to explicitly enable it, add the following to your `cnf/build.bnd`:

```properties
-plugin.Git: \
  aQute.bnd.plugin.git.GitPlugin
```

When you create a new project in the workspace, the plugin will automatically generate appropriate `.gitignore` files.

## Generated .gitignore

The plugin generates a root `.gitignore` in the project with entries similar to:

```
/generated/
/bin/
/bin_test/
```

And also creates empty `.gitignore` files in source directories to preserve the directory structure in Git. This ensures that:

1. Build artifacts are never committed
2. Empty source directories are preserved in the repository
3. New developers have a clean workspace ready for builds

<hr />
TODO Needs review - AI Generated content