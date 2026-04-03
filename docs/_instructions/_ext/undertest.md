---
layout: default
class: Project
title: -undertest true 
summary:  Will be set by the project when it builds a JAR in test mode, intended to be used by plugins.
---

The `-undertest` instruction is set by the project when it builds a JAR in test mode. It is intended to be used by plugins or build logic to indicate that the current build is for testing purposes. When this instruction is present, certain behaviors or configurations (such as including test packages) may be enabled automatically.

When building with `-undertest`, the build process skips dependency checking, so it is important to ensure that any dependent projects are built first. The instruction also causes the project to include test-specific packages (such as those specified by `-testpackages`) in the build, making them available for testing. This helps automate and streamline the test build process, ensuring that test-specific settings are applied only when needed and that the resulting JAR is suitable for test execution.

In summary, `-undertest` signals that the current build is for testing, enables test package inclusion, and skips dependency checks to speed up the test build workflow.

