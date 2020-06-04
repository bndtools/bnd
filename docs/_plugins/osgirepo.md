---
title: OSGi Repository
layout: default
summary: A plugin to use OSGi repositories 
---

The OSGi Repository can read index files as specified by the [OSGi Repository Service Specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.repository.html) and is a read only repository.

## Plugin Configuration

The class name of the plugin is `aQute.bnd.repository.osgi.OSGiRepository`. It can take the following configuration properties:

| Property         | Type  | Default | Description |
|------------------|-------|---------|-------------|
| `name`           | `NAME`|  | The name of the repository.|
| `locations`          | `STRING`|  | A Comma separate list of URLs point to an OSGi Resource file.|
| `cache`         | `STRING`| The workspace cache folder | The location, the downloaded bundles are stored. |
| `max.stale` | `integer` | one year | Bnd has it's own download cache. Max stale configures for how long the downloaded index file stays in the internal download cache. |
| `poll.time`      | `integer` | 5 seconds | Number of seconds between checks for polls on the `index` file. If the value is negative or the workspace is in batch/CI mode, then no polling takes place. |

## Example

To set up the `OSGi Repository` use:

		aQute.bnd.repository.osgi.OSGiRepository;\
			locations=https://devel.data-in-motion.biz/public/repository/gecko/release/geckoREST/index.xml;\
			max.stale=-1;\
			poll.time=86400;\
			name=GeckoJaxRsWhiteboard;\
			cache=${build}/cache/GeckoREST,\


## 