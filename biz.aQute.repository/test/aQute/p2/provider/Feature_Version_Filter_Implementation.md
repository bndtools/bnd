# Feature Version Filter Implementation

## Overview

Implemented proper version range filtering for Eclipse P2 feature requirements, converting Eclipse match rules to OSGi version filter expressions.

## Date

February 17, 2026

## Implementation Details

### Changes to Feature.java

Modified `toResource()` method to properly handle version and match attributes from `<requires><import>` elements in feature.xml files.

#### Added Methods

1. **`buildRequirementFilter(String identity, String version, String match, boolean isFeature)`**
   - Builds complete LDAP filter for a requirement
   - Handles identity, type (for features), and version constraints
   - Returns filter string like: `(&(osgi.identity=id)(type=...)(version>=x.y.z))`

2. **`buildVersionFilter(String version, String match)`**
   - Converts Eclipse match rules to OSGi version range expressions
   - Implements all four Eclipse match semantics
   - Returns version filter fragment

### Eclipse Match Rules Implementation

#### 1. Perfect Match
- **Eclipse Semantic**: Exact version match
- **OSGi Filter**: `(version=x.y.z.qualifier)`
- **Example**: `version="1.2.3.qualifier"` → `(version=1.2.3.qualifier)`

#### 2. Equivalent Match  
- **Eclipse Semantic**: Same major.minor.micro, any qualifier
- **OSGi Filter**: `(&(version>=x.y.z)(!(version>=x.y.(z+1))))`
- **Example**: `version="1.2.3"` → `(&(version>=1.2.3)(!(version>=1.2.4)))`
- **Range**: `[1.2.3, 1.2.4)`

#### 3. Compatible Match
- **Eclipse Semantic**: Same major.minor, micro >= specified
- **OSGi Filter**: `(&(version>=x.y.z)(!(version>=x.(y+1).0)))`
- **Example**: `version="1.2.0"` → `(&(version>=1.2.0)(!(version>=1.3.0)))`
- **Range**: `[1.2.0, 1.3.0)`

#### 4. GreaterOrEqual Match
- **Eclipse Semantic**: Version >= specified (default)
- **OSGi Filter**: `(version>=x.y.z)`
- **Example**: `version="1.0.0"` → `(version>=1.0.0)`
- **Range**: `[1.0.0, ∞)`

### Filter Examples

#### Plugin Requirements

```xml
<!-- Perfect match -->
<import plugin="org.eclipse.core.runtime" version="3.29.0" match="perfect"/>
```
→ `(&(osgi.identity=org.eclipse.core.runtime)(version=3.29.0))`

```xml
<!-- Compatible match (default) -->
<import plugin="org.eclipse.equinox.common" version="3.18.0" match="compatible"/>
```
→ `(&(osgi.identity=org.eclipse.equinox.common)(&(version>=3.18.0)(!(version>=3.19.0))))`

```xml
<!-- No version -->
<import plugin="org.eclipse.osgi"/>
```
→ `(osgi.identity=org.eclipse.osgi)`

#### Feature Requirements

```xml
<!-- Feature with version -->
<import feature="org.eclipse.rcp" version="4.38.0" match="compatible"/>
```
→ `(&(osgi.identity=org.eclipse.rcp)(type=org.eclipse.update.feature)(&(version>=4.38.0)(!(version>=4.39.0))))`

```xml
<!-- Feature without version -->
<import feature="org.eclipse.platform"/>
```
→ `(&(osgi.identity=org.eclipse.platform)(type=org.eclipse.update.feature))`

### Edge Cases Handled

1. **Version 0.0.0**: Treated as no version constraint (simple identity filter)
2. **Complex Qualifiers**: Preserved exactly in perfect match mode
3. **Empty Match**: Defaults to `greaterOrEqual`
4. **Null Version**: No version constraint added
5. **Parse Errors**: Falls back to greaterOrEqual with raw version string

## Test Coverage

Created comprehensive test suite: `FeatureVersionFilterTest.java`

### Test Methods

1. **`testVersionMatchRules()`**
   - Tests all four match rules with plugins
   - Tests feature requirements with and without versions
   - Verifies default behavior when match not specified
   - **Status**: ✅ PASSED

2. **`testVersionFilterEdgeCases()`**
   - Tests version 0.0.0 handling
   - Tests complex qualifiers
   - Tests large version numbers
   - Tests empty match attribute
   - **Status**: ✅ PASSED

### Test Results

```
Total requirements: 8
✅ Perfect match: (&(osgi.identity=plugin.perfect)(version=1.2.3.qualifier))
✅ Equivalent match: (&(osgi.identity=plugin.equivalent)(&(version>=1.2.3)(!(version>=1.2.4))))
✅ Compatible match: (&(osgi.identity=plugin.compatible)(&(version>=1.2.0)(!(version>=1.3.0))))
✅ GreaterOrEqual match: (&(osgi.identity=plugin.greaterOrEqual)(version>=1.0.0))
✅ Default (no match): (&(osgi.identity=plugin.default)(version>=2.0.0))
✅ No version: (osgi.identity=plugin.noversion)
✅ Feature with compatible: (&(osgi.identity=feature.compatible)(type=org.eclipse.update.feature)(&(version>=1.5.0)(!(version>=1.6.0))))
✅ Feature without version: (&(osgi.identity=feature.noversion)(type=org.eclipse.update.feature))
```

### Edge Case Results

```
✅ Version 0.0.0: (osgi.identity=plugin.zero)
✅ Complex qualifier: (&(osgi.identity=plugin.qualifier)(version=1.2.3.v20251201-1234))
✅ Large version: (&(osgi.identity=plugin.large)(&(version>=99.99.99)(!(version>=99.100.0))))
✅ Empty match: (&(osgi.identity=plugin.emptymatch)(version>=1.0.0))
```

## Backward Compatibility

✅ **Fully backward compatible**
- Features without version/match attributes work as before
- Simple identity filters for plugins without versions
- Type attribute added only for features
- Version constraints only added when version is present and not "0.0.0"

## Integration

The implementation is integrated into the P2 indexing workflow:

1. P2Indexer extracts features from P2 repository
2. Feature parser reads feature.xml from feature JARs
3. `toResource()` creates OSGi Resource with requirements
4. **NEW**: Version and match attributes properly converted to OSGi filters
5. Requirements stored in index with correct LDAP filter syntax

## Benefits

1. **Correct Dependency Resolution**: OSGi resolver can now properly evaluate version constraints from Eclipse features
2. **Match Rule Semantics**: All Eclipse match rules (perfect, equivalent, compatible, greaterOrEqual) correctly translated
3. **Better Compatibility**: Proper handling of Eclipse P2 semantics in bnd tooling
4. **Comprehensive Testing**: Edge cases and all match rules covered by tests

## Files Modified

- `biz.aQute.repository/src/aQute/p2/provider/Feature.java`
  - Modified `toResource()` method
  - Added `buildRequirementFilter()` method
  - Added `buildVersionFilter()` method

## Files Created

- `biz.aQute.repository/test/aQute/p2/provider/FeatureVersionFilterTest.java`
  - Comprehensive test suite for version filter generation
  - Tests all match rules and edge cases

## Related Documentation

- [Feature_Requirements_in_P2_Index.md](Feature_Requirements_in_P2_Index.md) - How requirements are stored
- [Eclipse438_P2_Feature_Verification.md](Eclipse438_P2_Feature_Verification.md) - Eclipse 4.38 verification results
- Eclipse P2 Match Rules: https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/misc/update_manager_compatibility.html

## Notes

- The implementation follows OSGi semantic versioning principles
- LDAP filter syntax conforms to RFC 4515
- Version class from bnd.osgi used for proper version parsing
- Error handling ensures fallback to greaterOrEqual if parsing fails
