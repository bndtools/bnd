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
| `max.stale` | `integer` | one year | Bnd has it's own download cache. `max.stale` configures for how many _seconds_ the downloaded index file stays in the internal download cache. Use _-1_ to always check if there is a newer file on the server. |
| `poll.time`      | `integer` | 5 seconds | Number of seconds between checks for polls on the `index` file. If the value is negative or the workspace is in batch/CI mode, then no polling takes place. |
| `tags`           | `STRING`|  | Comma separated list of tags. (e.g. resolve, baseline, release) Use the &lt;&lt;EMPTY&gt;&gt; The `resolve` tag is picked up by the [-runrepos](/instructions/runrepos.html) instruction.

## Example

To set up the `OSGi Repository` use:

		aQute.bnd.repository.osgi.OSGiRepository;\
			locations=https://devel.data-in-motion.biz/repository/gecko/release/geckoREST/index.xml;\
			max.stale=-1;\
			poll.time=86400;\
			name=GeckoJaxRsWhiteboard;\
			cache=${build}/cache/GeckoREST,\

