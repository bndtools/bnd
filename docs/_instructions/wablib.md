---
layout: default
title: -wablib FILE ( ',' FILE )*
class: Builder
summary: |
   Specify the libraries that must be included in a Web Archive Bundle (WAB) or WAR.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-wablib=lib/a.jar, lib/b.jar`

- Pattern: `.*`

<!-- Manual content from: ext/wablib.md --><br /><br />

The `-wablib` instruction specifies additional libraries (JAR files) that should be included in a Web Archive Bundle (WAB) or WAR. These libraries are added to the `WEB-INF/lib` directory of the resulting archive, making them available to the web application at runtime.

This instruction is useful when you need to package extra dependencies with your web bundle, ensuring that all required libraries are present in the deployed artifact.

	
