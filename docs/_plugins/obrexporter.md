---
title: OBR Index Exporter Plugin
layout: default
summary: Exports OBR Index XML from a specific Bndrun file
---

The OBR repository file is an XML-based representation of bundle meta-data.

The `biz.aQute.resolve.obr.plugin.ObrExporter` plugin allows you to export the OBR index XML file from a bndrun definition. It provides a new export type:

    biz.aQute.resolve.obr.plugin.ObrExporter

The export function has the following arguments:

* `outputdir` – The output directory where the OBR index XML file will be generated. If not set, the XML file will be generated in the configured target directory by default.
* `name` – The name of the generated OBR index XML file. By default, the name will be set to the name of the bndrun file.
* `excludesystem` – Flag to include the system bundle capabilities in the generated OBR index XML file. By default, the system bundle capabilities are excluded; that is, the framework bundle capabilities will not be part of the generated OBR XML file. 

## Example

To enable the OBR Exporter plugin, add a plugin in the workspace `build.bnd` file. (There is a context
menu entry for this.)

    -plugins \
        ...., \
        biz.aQute.resolve.obr.plugin.ObrExporter

The following snippet will export the OBR Index XML:

```
-export: \
	my-custom.bndrun; \
		type               =    bnd.obr.exporter; \
		name               =    sample.xml; \
		excludesystem      =    true
```

This will export the metadata (requirements and capabilities) of the bundles (including the framework bundle) specified in `-runbundles` section of the bndrun file to OBR Index XML file `sample.xml`.