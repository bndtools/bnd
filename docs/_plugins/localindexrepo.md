---
title: Local Index Repo
layout: default
summary: A Plugin for locally indexed repositories 
---

Bnd stores information about bundles it knows in an index, following the [OSGi Repository Service Specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.repository.html). The `LocalIndexedRepo` can be used as a repository to release bundles in or where bundles can be added manually (in bndtools via drag and drop). 

## Configuration

The class name of the plugin is `aQute.bnd.deployer.repository.LocalIndexRepo`. It can take the following configuration properties:

Merging the information from both tables into one, we get the following comprehensive table:

| Property    | Type      | Default           | Description                                                                                                                                                                              | Required?                                    |
|-------------|-----------|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `name`      | `NAME`    |                   | The name of the repository.                                                                                                                                                              | No                                           |
| `local`     | `STRING`  |                   | The directory the index and added bundles will be stored in                                                                                                                              | Yes                                          |
| `locations` | `STRING`  | empty                  | The location to store the _index_ file. Comma-separated list of *additional* index URLs. **NB:** surround this value with single-quotes if it contains more than one entry.             | No.                            |
| `readonly`  | `BOOLEAN` | `false`           | Blocks write access to the repository. Whether the repository should be read-only, i.e., disabled for editing from Bndtools.                                                             | No.                            |
| `overwrite` | `BOOLEAN` | `false`           | Enable overwrite of existing Bundles. By default, Bundles with the same Versions will not be added again.                                                                               |                                              |
| `onlydirs`  | `BOOLEAN` | `false`           | A comma-separated list of directories relative to the `local` property to be whitelisted for this repo (used when the index is created).                                                |                                              |
| `cache`    | `STRING`  | `${local}/.cache` |   Local cache directory for remote resources.                                                                                                                                                                                       | No.                |
| `pretty`    | `BOOLEAN` | `false`           | Whether to generate the index in printed format. See _Note 2_ below.                                                                                                                                                                                         | No.                            |
| `phase`     | `STRING`  |                   | Controls the resolution phase in which this repository may be used: `build`, `runtime` or `any`                                                                                                                                                                                        |                                              |
| `timeout`   | `integer` |                   | If there is a cached file, then just use it if the cached file is within the `timeout` period OR `online` is `false`.                                                                                                                                                                                        |                                              |
| `online`    | `BOOLEAN` | `true`           | Specifies if this repository is online. If `false` then cached resources are used.                                                                                                                                                                                     |                                              |
| `type`      | `STRING`  | `R5`              | The type (format) of index to generate. See _Note 1_ below.                                                                                                                                | No.                            |
| `tags`           | `STRING`|  | Comma separated list of tags. (e.g. resolve, baseline, release) Use the &lt;&lt;EMPTY&gt;&gt; The `resolve` tag is picked up by the [-runrepos](/instructions/runrepos.html) instruction. | No

**Note 1**: The index is generated by default in R5 format. To request alternative format(s), specify a list of format names separated by the "|" (pipe) character.
For example, to generate both R5 and OBR formats specify `type=R5|OBR`.

**Note 2**: R5 indexes are generated by default with no whitespace/indenting and gzipped, and the default index file name is `index.xml.gz`. Turning on pretty-printing enables indented, uncompressed output into the file `index.xml`. This setting has no effect on OBR indexes, which are always indented/uncompressed and named `repository.xml`.


## Example

```
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Release; \
		pretty = true; \
		local = ${build}/release
```
