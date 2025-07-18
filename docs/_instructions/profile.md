---
layout: default
title: -profile KEY
class: Builder
summary: |
   Sets a prefix that is used when a variable is not found, it is then re-searched under "[<[profile]>]<[key]>".
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-profile: "[<[profile]>]<[key]>`

- Pattern: `.*`

<!-- Manual content from: ext/profile.md --><br /><br />

The `-profile` instruction sets a prefix (profile key) that is used when a variable is not found in the current context. If a variable is missing, bnd will re-search for it using the pattern `[<profile>]<key>`, where `<profile>` is the value of the `-profile` instruction and `<key>` is the variable name.

This allows you to define profile-specific values for variables, making it easier to manage different build or runtime configurations within the same project. If no profile is set or the variable is not found under the profile, bnd will fall back to the default value.


<hr />
TODO Needs review - AI Generated content
