---
layout: default
class: Project
title: -testsources REGEX ( ',' REGEX )* 
summary:  Specification to find JUnit test cases by traversing the test src directory and looking for java classes. The default is (.*).java.
---

The `-testsources` instruction specifies how bnd should find JUnit test cases by searching the test source directory for Java classes. By default, it looks for all `.java` files, but you can provide a regular expression to customize which files are considered test sources.

This instruction is useful for projects with non-standard test file naming or organization, allowing you to control which files are included as test cases during the build process.

