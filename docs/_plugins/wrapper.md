---
title: Repository Wrapper Plugin 
layout: default
summary: A plugin that provides an OSGi Capability Repository on top of bnd repositories 
---

The Repository Wrapper plugin supports bnd Repository Plugin plugins that implement the `InfoRepository` interface. These repositories do not have to create an OSGi Repository with resources but only have to maintain a SHA-1 for each entry they hold. The Wrapper then uses the SHA-1 to maintain a cache of fully indexed resources. There is only one wrapper necessary, it automatically finds all  `InfoRepository` interfaces.

The name of the plugin is `aQute.bnd.deployer.repository.wrapper.Plugin`

It has the following configuration properties:

| Property                  | Type          | Default  | Description |
|-------------------------------------------|----------|-------------|
| `location`                | `PATH`        |          | A path to a directory to be used as cache. |
|                           |               |          | This cache must be unique for the workspace. There is no default,| 
|                           |               |          | the parameter must be set.|
| `reindex`                 | `true|false`  | `false`  | If the resources need to be reindexed at start up |

## Example

	-plugin.wrapper = \
        aQute.bnd.deployer.repository.wrapper.Plugin; \
            location            =	"${build}/cache/wrapper"; \
            reindex				=	true, \

