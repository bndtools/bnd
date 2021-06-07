---
title: P2 Repository
layout: default
summary: A plugin to use P2 repositories 
---

This is a read only Repository, that enables bnd to get dependencies from a P2 Repository. As bnd does not know the concept of Eclipse Features or Directory shaped bundles, it will not recognize such artifacts.

As P2 does not support all the necessary OSGi metadata, bnd will download the whole content of the repository, so it can analyze it and build its own index. So be cautious, when referencing large repositories. 

## Plugin Configuration

The class name of the plugin is `aQute.bnd.repository.p2.provider.P2Repository`. 

It can take the following configuration properties:

| Property          | Type      | Default    | Description     |
| ----------------- | --------- | ---------- | --------------- |
| `name`           | `NAME`    | p2 + `url` | The name of the repository. |
| `url`            | `URI`     |            | The URL to either the P2 repository (a directory) or an Eclipse target platform definition file. |
| `location`       | `STRING`  |            | The location to store the _index_ file and where bundles will be downloaded to. |

## Example

```
-plugin.p2: \
 	aQute.bnd.repository.p2.provider.P2Repository; \
 	url = http://download.eclipse.org/modeling/emf/emf/builds/release/2.21; \
 	name = EMF
```



