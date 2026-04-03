---
layout: default
class: Header
title: Bnd-LastModified  LONG
summary: Timestamp from bnd, aggregated last modified time of its resources 
---

# Bnd-LastModified

The `Bnd-LastModified` header is automatically added by bnd to the bundle manifest. It contains a timestamp (in milliseconds since the epoch) that represents the aggregated last modified time of all resources included in the bundle. This value is useful for tracking when the bundle was last built or updated, and can help with cache invalidation or deployment automation.

The timestamp is generated at build time and reflects the most recent modification among all files and resources that are part of the bundle. This ensures that any change to the bundle's contents will result in a new, updated timestamp in the manifest.

This header is set by bnd and should not be manually modified. It is primarily intended for tooling and automation purposes.


<hr />
TODO Needs review - AI Generated content