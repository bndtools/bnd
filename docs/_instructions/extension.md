---
layout: default
title: -extension
class: Project
summary: |
   A plugin that is loaded to its url, downloaded and then provides a header used instantiate the plugin.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Pattern: `.*`

<!-- Manual content from: ext/extension.md --><br /><br />
	


# -extension

The `-extension` instruction allows you to specify a plugin that is loaded from a URL. The plugin provides a header used to instantiate the extension. This is useful for extending bnd's functionality with custom plugins or features that are not included by default.

Example:

```
-extension: com.example.MyExtension;version=1.0.0
```

The extension will be downloaded and loaded as part of the build process.


TODO Needs review - AI Generated content
