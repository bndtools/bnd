---
layout: default
class: Project
title: -cdiannotations SELECTORS
summary: Selects the packages that need processing for CDI annotations. 
---

The `-cdiannotations` instruction tells **bnd** which bundle classes, if any, to search for OSGI CDI Integration (or plain CDI) annotations. **bnd** will then process those classes into requirements and capabilities.

The value of this instruction is a comma delimited list of globs matching packages or classes by fully qualified name.

The default value of this instruction is `*`, which means that by default **bnd** will process all bundle classes looking for OSGI CDI Integration (or plain CDI) annotations.

##### discover

Each glob may specify the `discover` attribute which determines the bean discovery mode to apply to matches.

The following `discover` modes are supported:

| `discover`                    | Bean Discovery Mode                                          |
| ----------------------------- | ------------------------------------------------------------ |
| `all`                         | include all classes in the bundle as CDI beans               |
| `annotated`                   | include classes annotated with [bean defining annotations](http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#bean_defining_annotations) as CDI beans |
| `annotated_by_bean` (default) | include classes annotated with `org.osgi.service.cdi.annotations.Bean` or classes in packages annotated with `org.osgi.service.cdi.annotations.Beans` as CDI beans. *This is the **default** mode when `discover` is not specified.* |
| `none`                        | do not include any classes in the bundle as CDI beans        |

```properties
-cdiannotations: *;discover=all
```

##### noservicecapabilities

Each glob may specify the `noservicecapabilities` attribute which indicates that no service capabilities will be added for matches.

```properties
-cdiannotations: *;discover=annotated;noservicecapabilities=true
```


##### noservicerequirements

Each glob may specify the `noservicerequirements` attribute which indicates that no service requirements will be added for matches.
```properties
-cdiannotations: *;noservicerequirements=true
```

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/cdi/CDIAnnotations.java)