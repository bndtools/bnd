# How Required Features Are Stored in the P2 Index

## Overview

Eclipse features can reference other features and plugins through three mechanisms, and all of these are stored as **OSGi requirements** in the P2 repository index (`index.xml.gz`). The requirements use LDAP filter syntax in the `osgi.identity` namespace.

## Three Types of Feature Dependencies

### 1. Plugin References (`<plugin>` elements in feature.xml)

**Source in feature.xml:**
```xml
<plugin
    id="org.eclipse.core.runtime"
    version="3.34.100.v20251111-1421"
    unpack="false"/>
```

**Stored as OSGi Requirement:**
```
Namespace: osgi.identity
Filter: (&(osgi.identity=org.eclipse.core.runtime)(version=3.34.100.v20251111-1421))
```

**Characteristics:**
- No `type` attribute in the filter (distinguishes from features)
- Version constraint included in filter
- One requirement per plugin

### 2. Included Features (`<includes>` elements in feature.xml)

**Source in feature.xml:**
```xml
<includes
    id="org.eclipse.equinox.core.feature"
    version="1.15.700.v20251126-0427"
    optional="false"/>
```

**Stored as OSGi Requirement:**
```
Namespace: osgi.identity
Filter: (&(osgi.identity=org.eclipse.equinox.core.feature)(type=org.eclipse.update.feature)(version=1.15.700.v20251126-0427))
Resolution: optional (if optional="true")
```

**Characteristics:**
- **MUST include `type=org.eclipse.update.feature`** in the filter
- Version constraint included in filter
- Optional includes have `resolution:=optional` directive
- One requirement per included feature

### 3. Required Features/Plugins (`<requires><import>` elements in feature.xml)

**Source in feature.xml:**
```xml
<requires>
    <import feature="org.eclipse.rcp" version="4.38.0" match="greaterOrEqual"/>
    <import plugin="org.eclipse.ui" version="3.200.0" match="greaterOrEqual"/>
</requires>
```

**Stored as OSGi Requirements:**

For feature imports:
```
Namespace: osgi.identity
Filter: (&(osgi.identity=org.eclipse.rcp)(type=org.eclipse.update.feature))
```

For plugin imports:
```
Namespace: osgi.identity
Filter: (osgi.identity=org.eclipse.ui)
```

**Characteristics:**
- Feature imports include `type=org.eclipse.update.feature`
- Plugin imports do NOT include type attribute
- Version and match attributes may not be strictly enforced in filter
- One requirement per import

## Real Example from ECF Repository

### Feature: org.eclipse.ecf.remoteservice.sdk.bndtools.feature

**Identity Capability:**
```
Namespace: osgi.identity
Attributes:
  osgi.identity = org.eclipse.ecf.remoteservice.sdk.bndtools.feature
  type = org.eclipse.update.feature
  version = 3.16.5.v20250914-0333
  label = ECF Remote Services SDK for Bndtools
  provider-name = Eclipse.org - ECF
```

**Requirements (17 total):**

Plugin requirement example:
```
Namespace: osgi.identity
Filter: (&(osgi.identity=org.eclipse.ecf.discovery.ui)(version=3.1.0.v20241230-2144))
```

Included feature requirement example:
```
Namespace: osgi.identity
Filter: (&(osgi.identity=org.eclipse.ecf.core.feature)(type=org.eclipse.update.feature))
```

## Implementation Details

### Feature.toResource() Method

The `Feature` class in `aQute.p2.provider.Feature` creates OSGi Resource objects:

1. **Creates Identity Capability:**
   ```java
   CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
   identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, id);
   identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, "org.eclipse.update.feature");
   identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
   ```

2. **Creates Requirements for Plugins:**
   ```java
   for (Plugin plugin : plugins) {
       CapReqBuilder req = new CapReqBuilder("osgi.identity");
       req.addDirective("filter", 
           String.format("(&(osgi.identity=%s)(version=%s))", plugin.id, plugin.version));
       rb.addRequirement(req);
   }
   ```

3. **Creates Requirements for Included Features:**
   ```java
   for (Includes include : includes) {
       CapReqBuilder req = new CapReqBuilder("osgi.identity");
       String filter = String.format(
           "(&(osgi.identity=%s)(type=org.eclipse.update.feature)(version=%s))", 
           include.id, include.version);
       req.addDirective("filter", filter);
       if (include.optional) {
           req.addDirective("resolution", "optional");
       }
       rb.addRequirement(req);
   }
   ```

4. **Creates Requirements for Required Features/Plugins:**
   ```java
   for (Requires requirement : requires) {
       CapReqBuilder req = new CapReqBuilder("osgi.identity");
       if (requirement.plugin != null) {
           req.addDirective("filter", 
               String.format("(osgi.identity=%s)", requirement.plugin));
       } else if (requirement.feature != null) {
           req.addDirective("filter",
               String.format("(&(osgi.identity=%s)(type=org.eclipse.update.feature))", 
                   requirement.feature));
       }
       rb.addRequirement(req);
   }
   ```

## Index Storage Format

The requirements are stored in the `index.xml.gz` file in OSGi Repository XML format:

```xml
<resource>
  <capability namespace="osgi.identity">
    <attribute name="osgi.identity" value="org.eclipse.ecf.core.feature"/>
    <attribute name="type" value="org.eclipse.update.feature"/>
    <attribute name="version" type="Version" value="1.7.0.v20250304-2338"/>
    <attribute name="label" value="Eclipse Communication Framework (ECF) Core"/>
  </capability>
  
  <requirement namespace="osgi.identity">
    <directive name="filter" value="(&amp;(osgi.identity=org.eclipse.core.runtime)(version=3.34.0))"/>
  </requirement>
  
  <requirement namespace="osgi.identity">
    <directive name="filter" value="(&amp;(osgi.identity=org.eclipse.equinox.feature)(type=org.eclipse.update.feature))"/>
  </requirement>
</resource>
```

## Key Differences Between Requirement Types

| Type | Filter Pattern | Type Attribute? | Use Case |
|------|---------------|-----------------|----------|
| Plugin | `(osgi.identity=plugin.id)` | ❌ No | Direct plugin dependencies |
| Included Feature | `(&(osgi.identity=feature.id)(type=org.eclipse.update.feature))` | ✅ Yes | Feature composition |
| Required Feature | `(&(osgi.identity=feature.id)(type=org.eclipse.update.feature))` | ✅ Yes | Feature dependencies |
| Required Plugin | `(osgi.identity=plugin.id)` | ❌ No | Plugin dependencies |

## Resolution Process

The P2 resolver uses these requirements to:

1. **Find all dependencies** of a feature recursively
2. **Resolve feature hierarchies** by following `type=org.eclipse.update.feature` requirements
3. **Determine which bundles are needed** by following plugin requirements
4. **Handle optional dependencies** through the `resolution:=optional` directive
5. **Apply version constraints** from the filter expressions

## Testing

The `FeatureRequirementsInIndexTest` demonstrates:
- How requirements are created from feature.xml
- How they're stored in the OSGi Resource
- How they're persisted in the index.xml.gz
- How they can be retrieved and inspected

Run the test:
```bash
./gradlew :biz.aQute.repository:test --tests "aQute.bnd.repository.p2.provider.FeatureRequirementsInIndexTest"
```

## Summary

**Required features are stored as OSGi requirements** in the `osgi.identity` namespace with LDAP filter directives. The critical distinction is:

- **Features** ALWAYS include `type=org.eclipse.update.feature` in the filter
- **Plugins** NEVER include a type attribute

This allows the P2 resolver to distinguish between feature-to-feature dependencies and feature-to-bundle dependencies, enabling proper resolution of the entire dependency graph.
