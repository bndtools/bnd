---
title: Local Index Repo
layout: default
summary: A Plugin for locally indexed repositories 
---

Bnd stores information about bundles it knows in an index, following the [OSGi Repository Service Specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.repository.html). The `LocalIndexedRepo` can be used as a repository to release bundles in or where bundles can be added manually (in bndtools via drag and drop). 

## Configuration

The class name of the plugin is `aQute.bnd.deployer.repository.LocalIndexRepo`. It can take the following configuration properties:

| Property    | Type      | Default | Description                                                  |
| ----------- | --------- | ------- | ------------------------------------------------------------ |
| `name`      | `NAME     |         | The name of the repository.                                  |
| `local`     | `STRING`  |         | The directory the index and added bundles will be stored in  |
| `locations` | `STRING`  |         | The location to store the _index_ file.                      |
| `readonly`  | `BOOLEAN` | `false` | Blocks write access to the repository                        |
| `overwrite` | `BOOLEAN` | `false` | Enable overwrite of existing Bundles. By default Bundles with the same Versions will not be added again |
| `onlydirs`  | `BOOLEAN` | `false` | A comma separated list of directories relative to the `local`property to be whitelisted for this repo (used when the index is created) |
| `.cache`    | `STRING`  |         |                                                              |
| `pretty`    | `BOOLEAN` | `false` |                                                              |
| `phase`     | `STRING`  |         |                                                              |
| `timeout`   | `integer` |         |                                                              |
| `online`    | `BOOLEAN` | `false` |                                                              |
| `type`      | `STRING`  |         |                                                              |

## Example

```
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Release; \
		pretty = true; \
		local = ${build}/release
```

