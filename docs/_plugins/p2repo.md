---
title: P2 Repository
layout: bnd
summary: A plugin to use P2 repositories 
parent: Plugins
---
This is a read only Repository, that enables bnd to get dependencies from a P2 Repository / P2Repository. Eclipse features are indexed as well, see [Eclipse Features](#eclipse-features). As bnd does not know the concept of Directory shaped bundles, it will not recognize such artifacts.

As P2 does not support all the necessary OSGi metadata, bnd will download the whole content of the repository, so it can analyze it and build its own index. So be cautious, when referencing large repositories. 

## Plugin Configuration

The class name of the plugin is `aQute.bnd.repository.p2.provider.P2Repository`. 

It can take the following configuration properties:

| Property          | Type      | Default    | Description     |
| ----------------- | --------- | ---------- | --------------- |
| `name`           | `NAME`    | p2 + `url` | The name of the repository. |
| `url`            | `URI`     |            | The URL to either the P2 repository (a directory) or an Eclipse target platform definition file. |
| `location`       | `STRING`  | `cnf/cache/p2-<name>/` | The location to store the index. A value ending in `/` is a directory and stores the index as `index.xml.gz`; an existing directory is also treated as a directory. Otherwise, the value specifies the complete index filename. A directory at a location ending in `.xml` or `.xml.gz`, created by an earlier bnd version, is replaced by the index file if it only holds a generated index and bundle links. Unless `cache` is set, bundle links or copies are stored in the index directory. |
| `cache`          | `STRING`  | Index directory | Optional directory for bundle links or copies, independent of the index location. Applies to both directory and filename locations. Relative paths are resolved against the workspace root; absolute paths are also supported. |
| `tags`           | `STRING`|  | Comma separated list of tags. (e.g. resolve, baseline, release) Use a placeholder like &lt;&lt;EMPTY&gt;&gt; to exclude the repo from resolution. The `resolve` tag is picked up by the [-runrepos](/instructions/runrepos.html) instruction.|

## Example

```
-plugin.p2: \
 	aQute.bnd.repository.p2.provider.P2Repository; \
 	url = https://download.eclipse.org/modeling/emf/emf/builds/release/2.21; \
 	name = EMF
```

To select the index filename explicitly:

```
-plugin.p2: \
	aQute.bnd.repository.p2.provider.P2Repository; \
	url = https://download.eclipse.org/modeling/emf/emf/builds/release/2.21; \
	name = EMF; \
	location = cnf/cache/emf-index.xml.gz
```

To keep an explicitly named index separate from the bundle cache:

```
-plugin.p2: \
	aQute.bnd.repository.p2.provider.P2Repository; \
	url = https://download.eclipse.org/modeling/emf/emf/builds/release/2.21; \
	name = EMF; \
	location = cnf/ext/emf-index.xml.gz; \
	cache = cnf/cache/p2-EMF
```

Omitting `cache` preserves the existing behavior. Setting it does not move or delete
previously cached files, or change the shared HTTP download cache (normally
`~/.bnd/urlcache`).

## Eclipse Features

Eclipse features are stored in the generated index with identity type
`org.eclipse.update.feature`. Bndtools displays them both through the P2 plugin
and through an OSGi Repository configured with that index.

The **Show Eclipse Features** toolbar toggle in the Repositories view controls
feature visibility in the normal tree and advanced search results. Features are
shown by default, and the selection is remembered across restarts. Hiding them
does not change repository contents, build paths, or resolution.

Feature versions can be expanded into included features, required features and
bundles, and included bundles. Selecting a feature or member displays its indexed
capabilities and requirements in the resolution view. The hierarchy is read from
index metadata without downloading feature JARs. Older indexes without
`bnd.relation` metadata must be refreshed through the P2 plugin to distinguish
members from dependencies.

## Tagging

This plugin supports Tagging via the `tags` configuration property. See [Tagging of repository plugins](/plugins/#tagging-of-repository-plugins) for more details.