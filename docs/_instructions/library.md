---
layout: default
title: -library library ( ',' library )*
class: Workspace or Project
summary: |
   Apply a bnd library to the workspace, project, or bndrun file
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-library: foo;version=1.2.3`

- Pattern: `.*`

<!-- Manual content from: ext/library.md --><br /><br />

The main reason for the `-library` instruction is to let you package and share *bnd-related configuration* in a self-contained way so it can be reused across workspaces, projects, or run descriptions.

Let's use some examples for explanation.

## Example 1) Common bnd Configuration Across Multiple Projects

### Scenario

You have many projects that all need the same baseline OSGi or bnd settings—like default macros, a specific build plugin, or a certain analyzer configuration (e.g., “treat warnings as errors,” “export packages in a particular way,” etc.).

### How a Library Helps

1. Put these shared bnd instructions (i.e., the macros and plugin setups) in `project.bnd` (or `workspace.bnd`/`bndrun.bnd`) inside a “library bundle” in your repo.  
2. In each project that wants these settings, just add:
   ```
   -library myconfig
   ```
3. All the macros and plugin instructions from that library will be included.  

This avoids copying the same lines into each `bnd.bnd`. Instead, every project references the same library, ensuring consistency.


## Example 2) Including Pre-Configured Tooling/Plugins

### Scenario
You have a plugin (e.g., for code generation) that always needs a certain set of properties to work properly—maybe a specific source folder or some environment variables.

### How a Library Helps
1. Provide a bundle that *contains* the plugin (or references it) plus a `bnd` file with the plugin’s configuration.  
2. Other projects include that library:
   ```
   -library codegen
   ```
3. Now each project automatically has the plugin in its build plus the right plugin settings (class paths, macros, etc.).  

This is particularly nice if you have multiple plugins or complex plugin settings that you do not want to replicate.


## Creating a library

A _library_ is stored in a _bundle_. A bundle can contain multiple libraries that are each described by a
capability in the `bnd.library` name space. This capability looks like:

    Provide-Capability: \
        bnd.library; \
            bnd.library     = foo; \
            version         = 1.2.3; \
            path           = lib/foo 

The following attributes are defined for the `bnd.library` capabilities:

* `bnd.library` – The name of the library. Names should be kept simple but the names are shared between all libraries. 
* `version` – The version of the library.
* `path` – A path to a directory in the bundle. The contents of this directory are the root of the library. This root
  is copied to the workspace cache.

The root of the library should contain the bnd files to be included. The defaults (`workspace.bnd`, `project.bnd`, and 
`bndrun.bnd`) should only be there if it makes sense to include the library in that type.



## Using a library

A library can provide additional _named_ functionality to a workspace, a project, or a bndrun file. It does this by
including a `bnd` file in the setup that originates from a bundle in the repositories. This included bnd file
can refer and use any binary resources in this bundle.   Bundles can contain multiple libraries.

The `-library` instruction can apply a _library extension_ from the repositories to a workspace or project. A library
extension is a resource that is stored in one of the repositories. When the `-library` instruction is used, the corresponding resource
will be expanded in the workspace's cache and one or more files from this area are read as include files containing
bnd properties.

    -library    ::=  library ( ',' library )*
    library     ::=  '-'? NAME ( ';' parameter ) *        

For example, the _library_ `foo` is included in a resource in the repository. We can apply this library in the 
`build.bnd`, `bnd.bnd`, or `*.bndrun` file. 

    -library  foo

The following `parameter` attributes are architected:

* `version` – The version range of the capability. If no version is specified, 0 is used. If a version is specified, then
  this is the lowest acceptable version. The runtime will select the highest matching version. The `version` can also be:
  * `file` – the library name is a path to a directory or JAR file. Since this lacks the 'where' of the library in the
    directory or JAR, this can be set with the `where` attribute in the same clause.
* `include` – The name of the include file or directory, relative to the root directory of the _library_. If a directory 
  is targeted, all `*.bnd` files will be read. The default include depends on where the library is included. In:
   * The workspace (`build.bnd`) – `workspace.bnd` 
   * A project (`bnd.bnd`) – `project.bnd`
   * A bndrun spec (`*.bndrun`) – `bndrun.bnd` 

Some remarks:

* If the library name starts with a `-`, there will be no error if the capability cannot be found.
* You can include a library multiple times with different include files/directories
* The `${.}` macro refers to the cached root directory of the library.

## Repositories & Ordering

Since libraries are stored in repositories but can also provide new repositories it is important to 
understand the ordering.

Only libraries included in the workspace can contribute repositories. The workspace will first read the `cnf/ext`
directory bnd files and then `build.bnd`. Any repositories defined in these bnd files can be used for libraries.
The workspace will include the libraries based on these repositories. However, after _all_ libraries have
been included, all plugins are reset and this will reset the repositories. Any repository plugins defined
in a library will then become available.

Projects and bndrun files can include libraries but they cannot define any new repositories. 

NOTE: what about standalone bndruns? 
