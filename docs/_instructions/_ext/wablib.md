---
layout: default
class: Builder
title: -wablib FILE ( ',' FILE )*
summary: Specify the libraries that must be included in a Web Archive Bundle (WAB) or WAR.
---

The `-wablib` instruction specifies additional libraries (JAR files) that should be included in a Web Archive Bundle (WAB) or WAR. These libraries are added to the `WEB-INF/lib` directory of the resulting archive, making them available to the web application at runtime.

This instruction is useful when you need to package extra dependencies with your web bundle, ensuring that all required libraries are present in the deployed artifact.

	