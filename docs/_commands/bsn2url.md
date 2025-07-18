---
layout: default
title: bsn2url
summary: |
   From a set of bsns, create a list of urls if found in the repo
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   bsn2url [options]  ...


#### Options: 
- `[ -p --project <string> ]` Identify another project

<!-- Manual content from: ext/bsn2url.md --><br /><br />
## bsn2url Command

The `bsn2url` command in bnd is used to generate a list of URLs for bundles based on their bundle symbolic names (bsns) and versions. This is useful for creating lists of bundle locations for deployment, analysis, or documentation purposes.

The command reads a list of bsns (and optional version ranges) from input files, looks up the available versions in the configured repositories, and outputs the corresponding URLs for each matching bundle.

**Example:**

Suppose you have a file `bundles.txt` containing:
```
com.example.foo;version='[1.0,2.0)'
com.example.bar
```

You can generate a list of URLs for these bundles with:
```
bnd bsn2url bundles.txt
```

This will print the URLs for the matching bundles and versions, making it easy to retrieve or reference them in other tools or scripts.


