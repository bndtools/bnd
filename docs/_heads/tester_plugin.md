---
layout: default
title: Tester-Plugin
class: Header
summary: |
   It points to a class that must extend the aQute.bnd.build.ProjectTester class. This class is loaded in the bnd environment and not in the target environment. This ProjectTester plugin then gets a chance to configure the launcher as it sees fit. It can get properties from the project and set these in the Project Launcher so they can be picked up in the target environment.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Tester-Plugin= a.b.c.MyTester`

- Pattern: `.*`

