---
layout: default
class: Project
title: -runtimeout DURATION
summary:  
---

The `-runtimeout` instruction allows you to specify a timeout duration for running or testing your project. The value should be a duration (such as `30 SECONDS`, `5 MINUTES`, etc.), and it controls how long bnd will wait for the process to complete before terminating it.

This is useful for preventing tests or application runs from hanging indefinitely, ensuring that your build or test process completes in a reasonable amount of time.