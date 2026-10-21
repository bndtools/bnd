---
layout: bnd
title: Architecture (arc42)
parent: Developing bnd and Bndtools
nav_order: 5
has_children: true
---

# bnd and Bndtools Architecture

This documentation describes the architecture of bnd, Bndtools, their Gradle
and Maven plugins, and the supporting projects maintained by the
[bndtools GitHub organization](https://github.com/bndtools). It follows the
[arc42](https://arc42.org/) section structure.

## Scope and status

- **System:** bnd and Bndtools ecosystem
- **Primary repository:** [bndtools/bnd](https://github.com/bndtools/bnd)
- **Snapshot date:** 2026-10-09
- **Branch:** `master`
- **Audience:** contributors, maintainers, integrators, and build-tool authors
- **Evidence:** repository source, build descriptors, project documentation,
  and public GitHub repository metadata

This is an architecture overview, not a replacement for the detailed
[bnd documentation](https://bnd.bndtools.org/) or
[Bndtools documentation](https://bndtools.org/).

## Contents

1. [Introduction and Goals]({{ '/development/architecture/01-introduction-and-goals.html' | relative_url }})
2. [Architecture Constraints]({{ '/development/architecture/02-architecture-constraints.html' | relative_url }})
3. [System Scope and Context]({{ '/development/architecture/03-system-scope-and-context.html' | relative_url }})
4. [Solution Strategy]({{ '/development/architecture/04-solution-strategy.html' | relative_url }})
5. [Building Block View]({{ '/development/architecture/05-building-block-view.html' | relative_url }})
6. [Runtime View]({{ '/development/architecture/06-runtime-view.html' | relative_url }})
7. [Deployment View]({{ '/development/architecture/07-deployment-view.html' | relative_url }})
8. [Cross-cutting Concepts]({{ '/development/architecture/08-crosscutting-concepts.html' | relative_url }})
9. [Architecture Decisions]({{ '/development/architecture/09-architecture-decisions.html' | relative_url }})
10. [Quality Requirements]({{ '/development/architecture/10-quality-requirements.html' | relative_url }})
11. [Risks and Technical Debt]({{ '/development/architecture/11-risks-and-technical-debt.html' | relative_url }})
12. [Glossary]({{ '/development/architecture/12-glossary.html' | relative_url }})

## Primary evidence

- [Repository overview](https://github.com/bndtools/bnd/blob/master/README.md)
- [bnd CLI overview](https://github.com/bndtools/bnd/blob/master/biz.aQute.bnd/README.md)
- [Gradle plugins](https://github.com/bndtools/bnd/blob/master/gradle-plugins/README.md)
- [Maven plugins](https://github.com/bndtools/bnd/blob/master/maven-plugins/README.md)
- [bnd introduction]({{ '/chapters/110-introduction.html' | relative_url }})
- [bnd concepts]({{ '/chapters/130-concepts.html' | relative_url }})
- [Launching]({{ '/chapters/300-launching.html' | relative_url }})
- [Testing]({{ '/chapters/310-testing.html' | relative_url }})
- [Development notes]({{ '/development/development-notes.html' | relative_url }})
- [Contribution guide](https://github.com/bndtools/bnd/blob/master/CONTRIBUTING.md)

## Diagram notation

Diagrams use Mermaid. Solid lines represent runtime or build-time use.
Dashed lines represent publication, provisioning, or optional integration.

## Viewing this documentation locally

The architecture is part of the Jekyll documentation site. Its Markdown pages
and Mermaid diagrams also render on GitHub and in compatible editor previews.

### Option 1: VS Code Markdown preview

1. Open any file in this folder.
2. Press `Ctrl+Shift+V` (`Cmd+Shift+V` on macOS), or use
   **Markdown: Open Preview to the Side** from the Command Palette.

VS Code renders the Markdown and the relative links between sections. The
built-in preview does not render Mermaid, so install the
[Markdown Preview Mermaid Support](https://marketplace.visualstudio.com/items?itemName=bierner.markdown-mermaid)
extension (`bierner.markdown-mermaid`) to see the diagrams:

```bash
code --install-extension bierner.markdown-mermaid
```

### Option 2: Local website with navigation and search

Follow [Documentation development]({{ '/development/documentation.html' | relative_url }})
to run the Jekyll site. Open `/development/architecture/` on the local server.
Just-the-Docs provides navigation, search, and Mermaid rendering. Mermaid is
loaded from a CDN, so browser diagram rendering requires network access.

### Option 3: GitHub rendering

GitHub renders both the Markdown and the Mermaid diagrams directly, so pushing
a branch and browsing this folder on GitHub needs no local setup.

### Notes

- Internal website links use Jekyll Liquid and resolve in the generated site,
  not in GitHub or editor previews. Use the file browser there to navigate sections.
- Release documentation preserves the architecture snapshot from its release
  branch. Repository evidence links above point to `master`.

