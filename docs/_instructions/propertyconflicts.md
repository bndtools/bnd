---
layout: bnd
title: -propertyconflicts off | warning | error
class: Processor
summary: |
   Control diagnostics for duplicate and shadowed property definitions.
parent: Instruction Reference
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder.
---

- Example: `-propertyconflicts: error`

- Values: `off,warning,error`

- Pattern: `off|warning|error`

<!-- Manual content from: ext/propertyconflicts.md --><br /><br />

This instruction controls property-conflict diagnostics in bnd builds and Bndtools.
It does not change property values or include precedence.

* Unset: diagnostics are disabled, unless `-pedantic: true` or the processor's
  pedantic switch is enabled. In pedantic mode conflicts are warnings.
* `off`: disable these diagnostics, including in pedantic mode.
* `warning`: report conflicts without failing the build.
* `error`: report conflicts as build errors, independently of pedantic mode.

For example, set this in a project's `bnd.bnd`, or in `cnf/build.bnd` to inherit
the policy across the workspace:

```properties
-propertyconflicts: error
```

The effective policy is evaluated after properties and includes have been loaded;
the instruction may appear after the conflicting definitions. Existing `-failok`
and `-fixupmessages` controls still apply. An invalid policy value is an error.

## Conflicts

Within one properties file, repeated decoded keys are conflicts. This applies to
ordinary properties and instructions alike, including repeated suffixed keys.
The last definition still wins. Continuation lines and comments are parsed using
the regular bnd properties parser.

Across included files, competing definitions of a merged-property key (whether
a plain stem such as `-runvm` or `-runrequires`, or an identical suffixed key
such as `-runblacklist.win32`) are conflicts. Identical values also count.
Distinct suffixed keys do not conflict. Defaults included with `~` and properties
renamed through extension loading are not considered competing definitions.
Normal parent/child property inheritance is not a conflict.

Use unique suffixed keys only for properties which support merging:

```properties
-runvm.application: -Dapplication=true
-runvm.logging: -Dlogging=true
```

Renaming adds previously shadowed values and can change their order. Ordinary
properties such as `Bundle-SymbolicName` do not support this repair; remove or
edit the unwanted definition instead. Bndtools offers merge repairs only for
merge-capable properties.

## Compatibility

For plain merged-property stems, this policy replaces the previous unconditional
`[Include Override]` warning. Those overrides are now silent by default.
Existing override warnings for other properties are unchanged.
Core duplicate diagnostics now use the `[Property Conflict]` prefix and include
source locations. Message-matching `-fixupmessages` rules may need updating.
