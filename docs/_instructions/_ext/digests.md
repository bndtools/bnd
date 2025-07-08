---
layout: default
class: Project
title: -digests DIGEST ( ',' DIGEST ) * 
summary: Set the digest algorithms to use
---

The `-digests` instruction allows you to specify which digest (checksum) algorithms should be used when generating JAR files. You can provide a comma-separated list of algorithms, such as `SHA-1`, `MD-5`, or others supported by your environment.

By default, if no value is specified, both `SHA-1` and `MD-5` are used. Setting this instruction ensures that the generated JAR includes the specified digests, which can be useful for verifying file integrity or meeting repository requirements.




