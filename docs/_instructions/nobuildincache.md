---
layout: default
title: -nobuildincache BOOLEAN
class: Builder
summary: |
   Do not use a build in cache for the launcher and JUnit.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-nobuildincache=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/nobuildincache.md --><br /><br />

The `-nobuildincache` instruction controls whether a build cache is used for the launcher and JUnit. When set to `true`, bnd will not use a build cache, which can be useful for troubleshooting or when you want to ensure that all builds are performed from scratch without relying on cached files.

By default, the build cache is enabled to improve performance by reusing previously built artifacts. Disabling the cache may slow down builds but can help avoid issues related to stale or corrupted cache data.

TODO Needs review - AI Generated content
