---
parent: Plugins
layout: default
title: Spring Plugin
summary: Analyzes spring files and adds any discovered classes to the imported packages. 
---

This plugin analyzes Spring Framework XML configuration files and automatically adds discovered classes to the bundle's imported packages. This ensures that all Spring-related classes referenced in your configuration are properly declared as package imports.

## How It Works

The Spring plugin processes Spring XML configuration files found in the `META-INF/spring/` directory of JAR files during the build analysis phase. It:

1. Locates all XML files in `META-INF/spring/` directory
2. Parses the Spring configuration using XSLT transformation
3. Extracts fully qualified class names from the configuration
4. Automatically adds discovered classes to the bundle's package imports

## Spring Class Discovery

The plugin discovers and imports classes from:

- Spring bean class definitions (`<bean class="..."/>`)
- Factory classes (`<bean factory-class="..."/>`)
- Event listener classes
- Spring AOP aspects
- Custom namespace handlers and their referenced classes
- Any other class references in the Spring XML configuration

## Processing Method

The plugin uses XSLT (XSL Transformations) with the `extract.xsl` stylesheet to reliably extract class references from the XML structure. This approach:

- Handles complex Spring XML schemas
- Ensures consistent class extraction across different Spring versions
- Properly validates that extracted class names are valid Java identifiers

## Package Extraction

From discovered class references, the plugin extracts the package names and adds them to the bundle's referred packages. For example:

- `com.example.MySpringBean` → imports package `com.example`
- `org.springframework.context.ApplicationContext` → imports package `org.springframework.context`

This automatic import discovery eliminates the need to manually declare package imports for all Spring-related classes, reducing configuration errors and improving build reliability.

## Integration

The Spring component integrates with bnd's analyzer plugin architecture, running automatically during the build analysis phase when Spring XML resources are detected. No additional configuration is required.

## Usage in build.bnd

The Spring plugin is typically enabled by default in bnd. If you need to explicitly enable or reference it, add the following to your `cnf/build.bnd`:

```properties
-plugin.Spring: \
  aQute.lib.spring.SpringComponent
```

The plugin runs automatically during the build analysis phase when processing bundles that contain Spring configuration files.

## Example Spring Configuration

Suppose you have a bundle with Spring configuration at `META-INF/spring/context.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">
  
  <bean id="userService" class="com.example.service.UserServiceImpl"/>
  <bean id="userDao" class="com.example.repository.UserRepository"/>
  <bean id="authListener" class="com.example.listener.AuthenticationEventListener"/>
</beans>
```

The Spring plugin will automatically add the following packages to your bundle's imports:

- `com.example.service`
- `com.example.repository`
- `com.example.listener`

This ensures that all Spring-related classes are properly declared as package imports without manual configuration.

<hr />
TODO Needs review - AI Generated content