---
layout: default
class: Macro
title: env ';' KEY (';' STRING)?
summary: The given environment variable or a default if the environment variable is not defined. The default is an empty string if not specified.
---

The specified key is looked up in `System.env` and returned. If the environment variable specified
by key is not set, then the default string is returned. The default is an empty string if not
specified. 
