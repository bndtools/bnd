---
layout: default
title: .bnd
class: Header
summary: |
   Home directory usage (~/.bnd) in bnd.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Pattern: `.*`

### Options 

- `build-deps` Stores build dependencies of bnd from gradle, ant, etc. In general, bnd will be among this. The files in this directory must be fully versioned
  - Example: `~/.bnd/biz.aQute.bnd-2.2.0.jar`

  - Pattern: `.*`


- `settings.json` Contains the settings used by bnd in json format. These settings are maintained by bnd command line (bnd help settings). These settings can be used through macros and can provide passwords, user ids, and platform specific settings. Names starting witha dot (.) are considered protected
  - Example: `{"id":"30...001","map":{".github.secret":"xxxxxx","github.user":"minime","email":"Peter.Kriens@aQute.biz"},"secret":"308...CC56"}`

  - Pattern: `.*`

  - Options: 
    - `email` The user's email address
      - Pattern: `.*`


    - `id` The public key for this machine
      - Pattern: `.*`


    - `secret` The private key for this machine
      - Pattern: `.*`


- `caches/shas` Directory with sha artifacts. The sha is the name of the directory, it contains the artifact with a normal bsn-version.jar name
  - Pattern: `.*`

