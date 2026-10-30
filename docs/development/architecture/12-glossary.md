---
layout: bnd
title: Glossary
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 12
---

# 12. Glossary

| Term | Meaning |
|---|---|
| bnd | Toolchain and libraries for building and working with OSGi bundles |
| Bndtools | Eclipse IDE integration built around bnd |
| bndlib | Main embeddable Java library containing analyzer, builder, processor, and workspace models |
| Bundle | JAR with OSGi metadata identifying imported/exported packages and other capabilities/requirements |
| BSN | Bundle-SymbolicName, stable logical identity of an OSGi bundle |
| Capability | Typed attribute set offered by an OSGi resource |
| Requirement | Filtered need that must be matched by a capability |
| Resource | OSGi model element representing a bundle or other resolvable unit |
| Resolver | Component selecting resources whose capabilities satisfy a requirement graph |
| Workspace | Directory rooted by `cnf`, containing shared configuration, repositories, plugins, and projects |
| Project | Buildable unit in a bnd workspace, normally configured by `bnd.bnd` |
| `bnd.bnd` | Main project instruction file |
| `.bndrun` | Resolve, launch, test, or export configuration |
| `-runrequires` | Desired runtime requirements |
| `-runbundles` | Concrete bundles selected for a runtime |
| Analyzer | bnd component that reads classes and derives package/capability metadata |
| Builder | bnd component that assembles and verifies bundle JARs |
| Baseline | Comparison of current and prior bundle APIs/content to determine semantic version changes |
| Repository plugin | Provider of artifacts/resources to workspace and resolver operations |
| Launcher | Pluggable bridge from a run model to a local or remote OSGi framework |
| Tester | Runtime component that discovers and runs tests inside an OSGi framework |
| p2 | Eclipse provisioning platform and update-site format |
| M2E | Maven integration for Eclipse |
| PDE | Eclipse Plug-in Development Environment |
| DS | OSGi Declarative Services component model |
| GAV | Maven group, artifact, and version coordinate |
| OSGi repository index | XML representation of resources, capabilities, requirements, and artifact locations |
| Gradle builder plugin | `biz.aQute.bnd.builder`, for ordinary Gradle projects |
| Gradle workspace plugin | `biz.aQute.bnd.workspace`, for bnd workspaces |
| Gradle project plugin | `biz.aQute.bnd`, applied to projects in a bnd workspace |

## Further references

- [bnd documentation](https://bnd.bndtools.org/)
- [Bndtools documentation](https://bndtools.org/)
- [OSGi specifications](https://docs.osgi.org/specification/)
- [arc42 template](https://arc42.org/)
- [bndtools organization](https://github.com/bndtools)

[Back to index]({{ '/development/architecture/' | relative_url }})
