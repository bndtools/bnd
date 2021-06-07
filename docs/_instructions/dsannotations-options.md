---
layout: default
class: Project
title: -dsannotations-options SELECTORS
summary: Options for controlling DS annotation processing.
---


The `-dsannotations-options` instruction configures how DS component annotations are processed and what metadata is generated.

```properties
-dsannotations-options: version;minimum=1.2.0;maximum=1.3.0
```

The example above, will restrict the use of OSGi DS annotations to minimum 1.2.0 and maximum 1.3.0 version. The version number denotes that the users are free to use any version equal to or higher than 1.2.0 but less than or equal to 1.3.0, provided that the users have the SCR annotations included in the workspace.

The following options are supported:

|option||
|-|-|
|inherit|use DS annotations found in the class hierarchy of the component class. *WARNING:* Annotations are not inherited from the component's super classes by default. The problem is that super classes from imported packages may be different at runtime than they were at build time. So it is always best to declare your annotations on the actual component class. |
|felixExtensions|enable features proprietary to Apache Felix SCR|
|extender|add the `osgi.extender=osgi.component` requirement to the manifest|
|nocapabilities|do not add `osgi.service` capabilities to the manifest|
|norequirements|do not add `osgi.service` requirements to the manifest|
|version|set the minimum and/or maximum version of the `osgi.extender=osgi.component` requirement added to the manifest|
