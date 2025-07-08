---
layout: default
class: Macro
title: Bnd-AddXmlToTest RESOURCE ( ',' RESOURCE )
summary: Add XML resources from the tested bundle to the output of a test report.
---

# Bnd-AddXmlToTest

The `Bnd-AddXmlToTest` header allows you to include additional XML resources from the tested bundle in the output of a test report. This can be useful for adding custom metadata, configuration, or other relevant XML files to your test results.

## How It Works

When a test is executed, the test framework inspects the `Bnd-AddXmlToTest` header in the bundle under test. If present, it treats the value as a comma- or space-separated list of resource paths. For each specified resource:

- The framework attempts to locate the resource within the bundle.
- If the resource exists and is an XML file, it is added to the test report output.
- If the resource cannot be found, an error entry is added to the report indicating the missing resource.

In addition to including these resources, the test report also contains metadata about the test environment, such as:
- The hostname where the test ran
- The symbolic name and location of the tested bundle
- The timestamp and framework version
- System properties at the time of the test
- Information about all bundles in the framework, including their state, version, and symbolic name

This mechanism provides a flexible way to enrich your test reports with bundle-specific XML data, making it easier to diagnose issues or provide additional context for test results.

## Example

To use this feature, add the following header to your bundle’s manifest:

```
Bnd-AddXmlToTest: config.xml, extra-info.xml
```

When the tests run, both `config.xml` and `extra-info.xml` (if present in the bundle) will be included in the test report output.

---
