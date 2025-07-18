---
layout: default
class: Builder
title: -profile KEY   
summary:  Sets a prefix that is used when a variable is not found, it is then re-searched under "[<[profile]>]<[key]>". 
---

The `-profile` instruction sets a prefix (profile key) that is used when a variable is not found in the current context. If a variable is missing, bnd will re-search for it using the pattern `[<profile>]<key>`, where `<profile>` is the value of the `-profile` instruction and `<key>` is the variable name.

This allows you to define profile-specific values for variables, making it easier to manage different build or runtime configurations within the same project. If no profile is set or the variable is not found under the profile, bnd will fall back to the default value.


<hr />
TODO Needs review - AI Generated content