---
layout: default
title: -tester REPO-SPEC
class: Project
summary: |
   Species the tester (bundle) that is supposed to test the code. The default is biz.aQute.tester
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-tester=biz.aQute.tester;version=latest`

- Pattern: `.*`

<!-- Manual content from: ext/tester.md --><br /><br />

The `-tester` instruction defines what bundle is used to setup testing. This bundle must have a Tester-Plugin that will setup the test environment.

By default the, `-tester` is the bundle `biz.aQute.tester`. This bundle will instruct bnd to add this bundle to the `-runbundles`, sets the appropriate properties, and then launches. It will then run whatever tests are configured.

## Older Versions

For a long time bnd had biz.aQute.junit as the tester. This launcher added itself to the `-runpath` and then executed the tests from there. Unfortunately this required that the tester actually exported the JUnit packages. This caused constraints between JUnit and bnd that was not good because JUnit itself is not directly a shining example of software engineering :-(

If you want to be backward compatible with the older model, set:

	-tester: biz.aQute.junit
