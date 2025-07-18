---
layout: default
class: Builder
title: -nobuildincache BOOLEAN   
summary:  Do not use a build in cache for the launcher and JUnit. 
---

The `-nobuildincache` instruction controls whether a build cache is used for the launcher and JUnit. When set to `true`, bnd will not use a build cache, which can be useful for troubleshooting or when you want to ensure that all builds are performed from scratch without relying on cached files.

By default, the build cache is enabled to improve performance by reusing previously built artifacts. Disabling the cache may slow down builds but can help avoid issues related to stale or corrupted cache data.

<hr />
TODO Needs review - AI Generated content