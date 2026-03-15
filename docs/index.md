---
layout: bnd
title: Home
nav_order: 0
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

### Install the bnd CLI

```shell
curl -Lk -o ~/biz.aQute.bnd.jar \
    https://bndtools.jfrog.io/artifactory/update-latest/biz/aQute/bnd/biz.aQute.bnd/7.2.1/biz.aQute.bnd-7.2.1.jar

alias bnd='java -jar ~/biz.aQute.bnd.jar'
bnd version
```

See [bnd CLI installation](/chapters/120-install.html#command-line) for more options.

### Wrap a JAR into an OSGi bundle

```shell
curl -L -o activation.jar \
    https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar
bnd wrap activation.jar
```

See [bnd wrap command](/commands/wrap.html) for details.

### Maven

```xml
<plugin>
  <groupId>biz.aQute.bnd</groupId>
  <artifactId>bnd-maven-plugin</artifactId>
  <version>7.1.0</version>
  <executions>
    <execution>
      <goals><goal>bnd-process</goal></goals>
    </execution>
  </executions>
</plugin>
```

See [Maven plugins](https://github.com/bndtools/bnd/tree/master/maven-plugins#maven-plugins) for full documentation.

### Gradle

```groovy
plugins {
  id "biz.aQute.bnd.builder" version "7.1.0"
}

jar {
  bundle {
    bnd '-exportcontents: com.acme.api.*'
  }
}
```

See [Gradle plugins](https://github.com/bndtools/bnd/tree/master/gradle-plugins#gradle-plugins) for full documentation.

### Create a bnd workspace

```shell
bnd add workspace -f demo-webapp -f osgi myworkspace
cd myworkspace
bnd build
bnd run launch.bndrun
```

See the [Workspace and Projects Tour](/chapters/123-tour-workspace.html) to learn more.

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
