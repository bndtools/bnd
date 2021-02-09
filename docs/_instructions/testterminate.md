---
layout: default
class: Test
title: -testterminate BOOLEAN
summary: Terminate the remote framework after running the test suites.
---

Under normal conditions a tester executes in a completely new framework started as configured by the bndrun file specified. With that in mind and to speed up reuse it swiftly and deliberately terminates the framework when tests have completed executing.

However, when using the [remote launcher](/chapters/300-launching.html#remote-launching) the framework was externally started so terminating the framework may not be the desired outcome. For example if the intention is to run several batteries of tests against a target runtime that takes time to startup then terminating and then restarting the runtime each time can be costly.

In order to enable multiple tester invocations to use the remote launcher and target the same remote runtime the `-testterminate` instruction should be set to `false` (the default is `true`.)

The following is an example of a basic bndrun which uses this instruction:

```properties
-testterminate: false

-runpath: biz.aQute.remote.launcher;version=latest

-runremote: \
	test;\
		shell=-1;\
		jdb=8000;\
		host=localhost;\
		agent=29998;\
		timeout=30000

# Remember that after each tester execution all run bundles are removed from
# the remote runtime which allows for conveniently returning to a clean state
-runbundles: \
	biz.aQute.tester.test2.junit4,\
	org.apache.servicemix.bundles.junit
```

