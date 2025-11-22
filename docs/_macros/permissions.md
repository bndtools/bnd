---
layout: default
class: Builder
title: permissions (';' ( 'packages' | 'admin' | 'permissions' ) )+
summary: Generate OSGi permission declarations for the bundle
---

## Summary

The `permissions` macro generates OSGi permission declarations in the format required for the OSGi permissions resource (OSGI-INF/permissions.perm). It can generate package permissions and admin permissions.

## Syntax

```
${permissions[;<type>...]}
```

## Parameters

- `type` (optional) - One or more permission types:
  - `packages` - Generate PackagePermission for imports and exports
  - `admin` - Generate AdminPermission
  - `all` - Generate all permission types
  - `permissions` - No-op marker

## Behavior

- Generates permission declarations in OSGi format
- **packages**: Creates import and export PackagePermissions for all bundle packages (except java.* packages)
- **admin**: Adds AdminPermission
- **all**: Equivalent to both packages and admin
- Returns formatted permission strings

## Examples

Generate package permissions:
```
${permissions;packages}
# Returns:
# (org.osgi.framework.PackagePermission "com.example.api" "import")
# (org.osgi.framework.PackagePermission "com.example.impl" "export")
```

Generate admin permissions:
```
${permissions;admin}
# Returns: (org.osgi.framework.AdminPermission)
```

Generate all permissions:
```
${permissions;all}
```

Use in permissions file:
```
# In OSGI-INF/permissions.perm
${permissions;packages}
${permissions;admin}
```

## Use Cases

- Generating OSGi permissions resource files
- Security policy configuration
- Java security manager setup
- Signed bundle permissions
- OSGi security specifications
- Fine-grained access control

## Notes

- Follows OSGi permissions specification format
- PackagePermissions exclude java.* packages
- Use in OSGI-INF/permissions.perm resource
- Part of OSGi security framework
- Useful for signed bundles requiring explicit permissions


<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
