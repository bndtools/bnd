---
layout: default
title: bootstrap
summary: |
   Interactive gogo shell
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   bootstrap  ...


<!-- Manual content from: ext/bootstrap.md --><br /><br />
## Bootstrap Command

The `bootstrap` command in bnd is used to start a local OSGi framework using the workspace's configuration. This command looks for the `bnd.bnd` file in the workspace's `cnf` directory and uses it to launch the framework locally.

If the `bnd.bnd` file is not found, the command will report an error and exit. Otherwise, it will initialize the framework and run it in the local environment, making it easy to test or develop OSGi applications directly from your workspace setup.

**Example:**

To start a local framework using the workspace configuration:

```
bnd bootstrap
```

This will launch the OSGi framework as defined by your workspace's `bnd.bnd` file.


