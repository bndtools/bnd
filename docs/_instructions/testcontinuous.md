---
layout: default
title: -testcontinuous BOOLEAN
class: Test
summary: |
   Do not exit after running the test suites but keep watching the bundles and rerun the test cases if the bundle is updated.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-testcontinuous=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/testcontinuous.md --><br /><br />

The `-testcontinuous` instruction enables continuous testing for your project. When set to `true`, bnd will not exit after running the test suites. Instead, it will keep watching the bundles and automatically rerun the test cases if any bundle is updated.

This is useful for development workflows where you want immediate feedback on changes, as tests will be re-executed whenever relevant code is modified.


<hr />
TODO Needs review - AI Generated content
