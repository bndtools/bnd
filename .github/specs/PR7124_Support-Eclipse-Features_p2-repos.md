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

# Eclipse 4.38 P2 Repository Feature Parsing Verification

## Overview
This document verifies that the `biz.aQute.repository` bundle correctly parses Eclipse P2 repositories, specifically validating feature extraction from the Eclipse 4.38 release repository.

**Repository URL:** https://download.eclipse.org/eclipse/updates/4.38/R-4.38-202512010920/

## Test Results Summary

### ✅ All Tests Passed Successfully

#### Test: Repository Indexing
- **Status:** PASSED
- **Index Size:** 693,864 bytes
- **Total Capabilities:** 1,151
- **Total Resources:** 1,148

#### Test: Feature Extraction
- **Status:** PASSED
- **Total Features Found:** 34
- **Resource Type Distribution:**
  - Features (`org.eclipse.update.feature`): 34
  - Bundles (`osgi.bundle`): 1,031
  - Fragments (`osgi.fragment`): 47
  - Synthetic (`bnd.synthetic`): 36

#### Feature Content Statistics
- Features with plugins: 33/34 (97%)
- Features with included features: 10/34 (29%)
- Features with requirements: 12/34 (35%)

## Sample Features Parsed

### 1. Eclipse Core Runtime Feature
- **ID:** org.eclipse.core.runtime.feature
- **Version:** 1.4.1000.v20251126-0427
- **Label:** Eclipse Core Runtime Infrastructure
- **Provider:** Eclipse Equinox Project
- **Plugins:** 7
- **Includes:** 1 (org.eclipse.equinox.core.feature)
- **Sample Plugins:**
  - org.eclipse.core.runtime v3.34.100.v20251111-1421
  - org.eclipse.core.variables v3.6.700.v20250913-1442
  - org.eclipse.core.contenttype v3.9.800.v20251105-1620

### 2. Eclipse 4 RCP
- **ID:** org.eclipse.e4.rcp
- **Version:** 4.38.0.v20251126-0427
- **Label:** Eclipse 4 Rich Client Platform
- **Provider:** Eclipse.org
- **Plugins:** 88
- **Includes:** 0
- **Sample Plugins:**
  - org.eclipse.e4.core.services v2.5.300.v20251112-1253
  - org.eclipse.e4.ui.workbench.swt v0.17.1000.v20251023-0611
  - org.eclipse.e4.core.commands v1.1.700.v20250916-0927

### 3. Equinox Compendium SDK
- **ID:** org.eclipse.equinox.compendium.sdk
- **Version:** 3.23.800.v20251111-0704
- **Label:** Equinox Compendium SDK
- **Provider:** Eclipse Equinox Project
- **Plugins:** 10
- **Includes:** 0
- **Sample Plugins:**
  - org.eclipse.equinox.app v1.7.500.v20250629-0337
  - org.eclipse.equinox.coordinator v1.5.500.v20250517-0335
  - org.eclipse.equinox.cm v1.6.300.v20250515-0501

## Property Resolution
All 34 features had their properties correctly resolved:
- **Resolved Labels:** 34/34 (100%)
- **Resolved Provider Names:** 34/34 (100%)

This indicates that feature.properties files within feature JARs are being correctly parsed and property references (e.g., `%featureLabel`) are being properly resolved.

## Index Capabilities Verification
- **Total Feature Capabilities in Index:** 37
- Each feature has:
  - Identity namespace capability with type `org.eclipse.update.feature`
  - Proper version attribute
  - Correct identity attribute

### Sample Feature Capabilities
1. **org.eclipse.swt.tools.feature** v3.109.900.v20250925-1621
2. **org.eclipse.equinox.sdk** v3.23.1900.v20251126-0427
3. **org.eclipse.equinox.p2.rcp.feature** v1.4.3000.v20251124-1504

## Specific Feature Retrieval
The test validated that individual features can be retrieved by ID and version:
- **Feature ID:** org.eclipse.core.runtime.feature
- **Version:** 1.4.1000.v20251126-0427
- **Retrieval:** Successful ✅
- **Plugins Count:** 7
- **Includes Count:** 1

## Implementation Details

### Feature Parsing Process
1. **P2 Repository Indexing** - The P2Indexer reads the repository metadata and creates an OSGi repository index
2. **Feature Discovery** - Features are identified by their `org.eclipse.update.feature` type in the identity namespace
3. **Feature JAR Download** - Each feature JAR is downloaded from the `features/` directory
4. **feature.xml Parsing** - The `Feature` class parses the feature.xml file including:
   - Root attributes (id, version, label, provider-name, etc.)
   - Plugin references with versions and optional flags
   - Included feature references
   - Required features and plugins
5. **Property Resolution** - feature.properties is loaded and `%key` references are resolved
6. **Resource Creation** - OSGi Resource objects are created with proper capabilities and requirements

### Key Classes
- **P2Indexer** (`aQute/bnd/repository/p2/provider/P2Indexer.java`)
  - `getFeatures()` - Extracts all features from the indexed repository
  - `getFeature(id, version)` - Retrieves a specific feature
  - `extractFeatureFromResource()` - Downloads and parses feature JARs
  - `parseFeatureFromJar()` - Parses feature.xml from JAR files

- **Feature** (`aQute/p2/provider/Feature.java`)
  - Parses Eclipse feature.xml files
  - Resolves property references from feature.properties
  - Provides access to plugins, includes, and requires
  - Creates OSGi Resource representations

- **P2Repository** (`aQute/bnd/repository/p2/provider/P2Repository.java`)
  - Implements `FeatureProvider` interface
  - Delegates to P2Indexer for feature operations

### Test Coverage
The comprehensive test suite (`Eclipse438FeatureTest`) validates:
- ✅ Repository indexing creates proper index files
- ✅ Features are correctly identified in the index
- ✅ Feature JARs can be downloaded from features/ directory
- ✅ feature.xml files are correctly parsed
- ✅ Plugin references are extracted
- ✅ Included features are identified
- ✅ Property resolution works (feature.properties)
- ✅ OSGi capabilities are created with correct attributes
- ✅ Individual features can be retrieved by ID and version

## Conclusion
The `biz.aQute.repository` bundle successfully parses Eclipse P2 repositories and correctly handles Eclipse features stored in the `features/` directory. The implementation:

✅ Downloads and parses feature JAR files  
✅ Extracts feature.xml content accurately  
✅ Resolves property references from feature.properties  
✅ Creates proper OSGi Resource representations  
✅ Indexes features with correct capabilities  
✅ Supports querying features by ID and version  
✅ Handles complex features with plugins, includes, and requires  

All 34 features from the Eclipse 4.38 repository were successfully parsed and indexed, demonstrating robust feature support for Eclipse P2 repositories.

## Test Execution
```bash
./gradlew :biz.aQute.repository:test --tests "aQute.bnd.repository.p2.provider.Eclipse438FeatureTest"
```

**Result:** BUILD SUCCESSFUL - All 5 tests passed
**Date:** February 17, 2026
