---
layout: default
class: Project
title: -extension 
summary: A plugin that is loaded to its url, downloaded and then provides a header used instantiate the plugin. 
---
	


# -extension

The `-extension` instruction allows you to specify a plugin that is loaded from a URL. The plugin provides a header used to instantiate the extension. This is useful for extending bnd's functionality with custom plugins or features that are not included by default.

Example:

```
-extension: com.example.MyExtension;version=1.0.0
```

The extension will be downloaded and loaded as part of the build process.


<hr />
TODO Needs review - AI Generated content