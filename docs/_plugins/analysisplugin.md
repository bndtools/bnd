---
title: Analysis Plugin
layout: default
summary: A plugin to receive callbacks about analysis decisions
---

The Analysis Plugin provides a way to track and log analysis decisions made by the Analyzer during bundle creation. This is particularly useful for understanding why certain version ranges were chosen for package imports.

## Overview

When building OSGi bundles, bnd analyzes your code and determines appropriate version ranges for imported packages based on various factors:

- Whether a package contains provider types (annotated with `@ProviderType`)
- Explicit directives in the Import-Package or Export-Package headers
- Default version policies configured in your build

The Analysis Plugin allows you to observe these decisions and understand the reasoning behind them.

## Interface

The plugin implements the `aQute.bnd.service.AnalysisPlugin` interface:

```java
public interface AnalysisPlugin extends OrderedPlugin {
    /**
     * Called when the analyzer determines a version range for an import package.
     */
    void reportImportVersion(Analyzer analyzer, PackageRef packageRef, 
                           String version, String reason) throws Exception;

    /**
     * Called when the analyzer makes other analysis decisions.
     */
    default void reportAnalysis(Analyzer analyzer, String category, 
                               String details) throws Exception {
        // Default implementation does nothing
    }
}
```

## Example Implementation

Here's a simple example that logs all import version decisions:

```java
package com.example;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.service.AnalysisPlugin;

public class LoggingAnalysisPlugin implements AnalysisPlugin {
    
    @Override
    public void reportImportVersion(Analyzer analyzer, PackageRef packageRef, 
                                   String version, String reason) throws Exception {
        System.out.printf("Import: %s -> %s (%s)%n", 
                         packageRef.getFQN(), version, reason);
    }
    
    @Override
    public int ordering() {
        return 0; // Default ordering
    }
}
```

## Usage

To use an Analysis Plugin in your build:

### In a bnd.bnd file:

```
-plugin: com.example.LoggingAnalysisPlugin
```

### Programmatically in Java:

```java
try (Builder builder = new Builder()) {
    builder.addClasspath(/* ... */);
    builder.getPlugins().add(new LoggingAnalysisPlugin());
    builder.build();
}
```

## Understanding the Reasons

The `reason` parameter in `reportImportVersion()` typically contains one of the following:

- `"provider type detected"` - The imported package contains provider types, requiring a strict version range
- `"consumer type (default)"` - The imported package is used as a consumer, allowing a more flexible version range
- `"explicit provide directive: true/false"` - An explicit `provide:=true` or `provide:=false` directive was specified
- `"export provide directive: ..."` - The provide directive came from the exporting bundle

## Use Cases

Analysis Plugins are useful for:

1. **Debugging** - Understanding why bnd chose a particular version range
2. **Compliance** - Logging analysis decisions for audit purposes
3. **Optimization** - Identifying opportunities to optimize version policies
4. **Documentation** - Generating reports about bundle dependencies

## Plugin Ordering

Like other bnd plugins, Analysis Plugins support ordering via the `ordering()` method. Lower values are called before higher values. This is useful when you have multiple plugins that need to coordinate or when one plugin's output depends on another's.
