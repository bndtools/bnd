---
layout: bnd
title: bnd – OSGi Bundle Tooling for Java
nav_order: 0
has_children: true
description: "bnd - the Swiss army knife of OSGi. Used for creating and working with OSGi bundles."
permalink: /
---

# bnd – the Swiss army knife of OSGi

bnd is the engine behind many popular OSGi development tools, including Eclipse (Bndtools), Maven, Gradle, and the bnd CLI. Its primary function is generating OSGi metadata by analyzing Java class files.

<div style="text-align:center;margin:1em 0">
<img src="{{ '/img/front-page.png' | prepend:site.baseurl }}" alt="bnd overview" style="max-width:100%">
</div>

---

## Quick Start Examples

bnd makes it easy to add OSGi metadata to JARs, whether wrapping existing libraries or building your own projects.

<div class="bnd-tabs" data-bnd-tabs>
  <div class="bnd-tab-buttons" role="tablist" aria-label="Quick start examples">
    <button class="bnd-tab-button is-active" data-bnd-tab-target="tab-install-cli" role="tab" aria-controls="tab-install-cli">Install bnd CLI</button>
    <button class="bnd-tab-button" data-bnd-tab-target="tab-wrap" role="tab" aria-controls="tab-wrap">bnd CLI JAR wrapping</button>
    <button class="bnd-tab-button" data-bnd-tab-target="tab-cli" role="tab" aria-controls="tab-cli">bnd CLI JAR wrapping advanced</button>
    <button class="bnd-tab-button" data-bnd-tab-target="tab-workspace" role="tab" aria-controls="tab-workspace">bnd CLI Workspace/Projects</button>
    <button class="bnd-tab-button" data-bnd-tab-target="tab-maven" role="tab" aria-controls="tab-maven">Maven</button>
    <button class="bnd-tab-button" data-bnd-tab-target="tab-gradle" role="tab" aria-controls="tab-gradle">Gradle</button>
    <button class="bnd-tab-button" data-bnd-tab-target="tab-eclipse" role="tab" aria-controls="tab-eclipse">Eclipse plugin</button>
  </div>

  <section id="tab-install-cli" class="bnd-tab-panel is-active" role="tabpanel" markdown="1">

```shell
# Install bnd CLI (precondition for the other examples)
curl -Lk -o ~/biz.aQute.bnd.jar \
  https://bndtools.jfrog.io/artifactory/update-latest/biz/aQute/bnd/biz.aQute.bnd/{{ site.data.bnd_version.baseline_version }}/biz.aQute.bnd-{{ site.data.bnd_version.baseline_version }}.jar

# create alias for easy use via 'bnd'
alias bnd='java -jar ~/biz.aQute.bnd.jar'

# display bnd version to verify installation
bnd version
```

This installs the bnd CLI and makes the `bnd` command available in your shell.

**Reference:** [bnd CLI installation](/chapters/120-install.html#command-line) | [bnd CLI documentation](/chapters/400-commands.html)
  </section>

  <section id="tab-wrap" class="bnd-tab-panel" role="tabpanel" markdown="1">

```shell
# Quick wrap - automatically generates OSGi metadata
# download an example jar to wrap
curl -L -o activation.jar \
  https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar
# run bnd to wrap as bundle with OSGi metadata
bnd wrap activation.jar
```

The result is a new jar file with OSGi metadata (`META-INF/MANIFEST.MF`).

**Reference:** [bnd wrap command documentation](/commands/wrap.html)
  </section>

  <section id="tab-cli" class="bnd-tab-panel" role="tabpanel" markdown="1">

```shell
# Use a bnd file for more control
# download an example jar to wrap
curl -L -o activation.jar \
  https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar

# a activation.bnd file to specify OSGi metadata
cat > activation.bnd <<EOF
-classpath: activation.jar
Export-Package: *;version=1.1.1
Bundle-Version: 1.1.1
EOF

# Generate the OSGi bundle
bnd activation.bnd
```

The result is a new jar file with OSGi metadata (`META-INF/MANIFEST.MF`).

**Reference:** [bnd CLI advanced JAR wrapping tutorial](/chapters/125-tour-features.html)
  </section>

  <section id="tab-workspace" class="bnd-tab-panel" role="tabpanel" markdown="1">

```shell
# Create a new workspace with templates
bnd add workspace -f demo-webapp -f osgi myworkspace

# Navigate to the workspace
cd myworkspace

# Start a live-coding / hot reload dev server
bnd dev launch.bndrun

# or alternatively just build and run without hot-reloading:
bnd build
bnd run launch.bndrun
```

This creates a complete bnd workspace that works identically across Eclipse, Gradle, Maven, and the command line.

**Learn more:** [Workspace and Projects Tour](/chapters/123-tour-workspace.html)

**Reference:** [bnd add](/commands/add.html) | [bnd build](/commands/build.html) | [bnd dev](/commands/dev.html) | [bnd run](/commands/run.html)
  </section>

  <section id="tab-maven" class="bnd-tab-panel" role="tabpanel" markdown="1">

```xml
<plugin>
  <groupId>biz.aQute.bnd</groupId>
  <artifactId>bnd-maven-plugin</artifactId>
  <version>7.1.0</version>
  <executions>
    <execution>
      <goals>
        <goal>bnd-process</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

This automatically generates OSGi metadata (`META-INF/MANIFEST.MF`) during the Maven build.

**Reference:** [Maven plugin documentation](https://github.com/bndtools/bnd/blob/master/maven-plugins/bnd-maven-plugin/README.md)
  </section>

  <section id="tab-gradle" class="bnd-tab-panel" role="tabpanel" markdown="1">

```groovy
plugins {
  id "biz.aQute.bnd.builder" version "7.1.0"
}

jar {
  bundle {
    bnd '''
-exportcontents: com.acme.api.*
-sources: true
'''
  }
}
```

This automatically generates OSGi metadata (`META-INF/MANIFEST.MF`) during the Gradle build.

**Reference:** [Gradle plugin documentation](https://github.com/bndtools/bnd/tree/master/gradle-plugins#create-a-task-of-the-bundle-type)
  </section>

  <section id="tab-eclipse" class="bnd-tab-panel" role="tabpanel" markdown="1">
The bndtools plugin for Eclipse provides tight integration with the bnd build system. It allows you to create, build, and run OSGi projects directly from within Eclipse.

**Reference:** [Bndtools Plugin documentation](https://bndtools.org/)

**Get started:** [Bndtools Workspace tutorial](https://bndtools.org/workspace.html)
  </section>
</div>

---

## What is bnd?

bnd consists of two major parts:

1. **Manifest generation** – Excellent at creating JARs with OSGi metadata based on instructions and the information in class files. Used by Maven, Gradle, Ant, and other build tools.

2. **Workspace model** – An IDE/build-tool-independent model of a workspace with projects that works identically across Eclipse, Maven, Gradle, and the command line.

> "If you want to teach people a new way of thinking, don't bother trying to teach them. Instead, give them a tool, the use of which will lead to new ways of thinking."
> — R. Buckminster Fuller

---

## How to get started?

The most feature-rich option is the [Bndtools Eclipse plugin](https://bndtools.org/). There are [tutorial videos](https://bndtools.org/workspace.html) and an [OSGi Starter guide](https://bndtools.org/assets/osgi-starter-workspace.pdf) available.

For Maven users, start with the [Maven plugins README](https://github.com/bndtools/bnd/tree/master/maven-plugins#maven-plugins).

For Gradle users, see the [Gradle Plugins README](https://github.com/bndtools/bnd/tree/master/gradle-plugins#gradle-plugins).

The [bnd CLI](/chapters/400-commands.html) is great for exploration — especially the `shell` command.

---

## Table of Contents

{% assign chapters_sorted = site.chapters | where_exp: "item", "item.parent != nil" | sort: "nav_order" %}
{% for chapter in chapters_sorted %}
- [{{ chapter.title }}]({{ chapter.url | prepend: site.baseurl }})
{% endfor %}
