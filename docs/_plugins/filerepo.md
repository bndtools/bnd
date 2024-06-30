---
title: FileRepo Plugin
layout: default
summary: Provides a bnd repository stored on the local file system  
---

This type of repository is based on a very simple file system directory structure. It is editable from within Bndtools. **NB:** it does not support indexing, so repositories of this type cannot participate in resolution of Run Requirements.

The following properties are supported:

| Name    | Description                                  | Required?                                   |  
|---------|----------------------------------------------|---------------------------------------------|
|`name`   |  Name for the repository.                    | No.                                         |
|`location`  | The local filesystem directory.           |   Yes. |
|`readonly`  |Whether the repository should be read-only,|  No. Default: false |
|            |i.e. disabled for editing from Bndtools.| |
| `tags`           | Comma separated list of tags. (e.g. resolve, baseline, release) Use the &lt;&lt;EMPTY&gt;&gt; placeholder for no tags. The `resolve` tag is picked up by the [-runrepos](/instructions/runrepos.html) instruction.| No