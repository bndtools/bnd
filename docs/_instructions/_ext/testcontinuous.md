---
layout: default
class: Test
title: -testcontinuous BOOLEAN 
summary: Do not exit after running the test suites but keep watching the bundles and rerun the test cases if the bundle is updated.
---

The `-testcontinuous` instruction enables continuous testing for your project. When set to `true`, bnd will not exit after running the test suites. Instead, it will keep watching the bundles and automatically rerun the test cases if any bundle is updated.

This is useful for development workflows where you want immediate feedback on changes, as tests will be re-executed whenever relevant code is modified.


---
TODO Needs review - AI Generated content