---
parent: Plugins
layout: default
title: Java Persistence Architecture Plugin
summary: Analyses JPA persistence.xml files and adds any discovered class to the imported packages.  
---

This plugin analyzes Java Persistence Architecture (JPA) configuration files and automatically adds discovered classes to the bundle's imported packages. This ensures that all JPA entities and related classes are properly included in the package imports.

## How It Works

The JPA plugin processes `persistence.xml` files found in the `META-INF` directory of JAR files during the build analysis phase. It:

1. Locates all `persistence.xml` resources in the `META-INF` directory
2. Parses the JPA persistence configuration to identify entity classes and other related classes
3. Automatically adds these classes to the bundle's package imports

## Processing Method

The plugin uses XSLT (XSL Transformations) to parse the XML configuration. This approach:

- Reliably extracts class references from the XML structure
- Handles complex persistence configurations
- Ensures consistent processing across different persistence.xml formats

## Imported Classes

The plugin discovers and imports:

- Entity classes defined in the persistence.xml
- Entity listeners
- Converter classes
- Any other classes referenced in the persistence configuration

This automatic import discovery prevents the need to manually declare package imports for JPA-related classes, reducing configuration errors and improving build reliability.

## Integration

The JPA component integrates with bnd's analyzer plugin architecture, running automatically during the build analysis phase when JPA resources are detected. No additional configuration is required beyond including the plugin in your build configuration.

## Usage in build.bnd

The JPA plugin is typically enabled by default in bnd. If you need to explicitly enable or reference it, add the following to your `cnf/build.bnd`:

```properties
-plugin.JPA: \
  aQute.lib.spring.JPAComponent
```

The plugin runs automatically during the build analysis phase when processing bundles that contain JPA persistence.xml files.

## Example JPA Configuration

Suppose you have a bundle with the following `META-INF/persistence.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence" version="2.2">
  <persistence-unit name="default">
    <class>com.example.entity.User</class>
    <class>com.example.entity.Order</class>
    <class>com.example.listener.AuditListener</class>
  </persistence-unit>
</persistence>
```

The JPA plugin will automatically add the following packages to your bundle's imports:

- `com.example.entity`
- `com.example.listener`

This ensures that all entity and listener classes are properly declared as package imports without manual configuration.

<hr />
TODO Needs review - AI Generated content