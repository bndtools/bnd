---
layout: default
title: -deploy
class: Project
summary: |
   Deploy the current project to a repository through Deploy plugins (e.g. MavenDeploy plugin)
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-deploy=mavenrepo`

- Values: `${repos}`

- Pattern: `.*`

<!-- Manual content from: ext/deploy.md --><br /><br />

The `-deploy` instruction is used to deploy the current project to a repository using deploy plugins, such as the MavenDeploy plugin. When this instruction is set, bnd will attempt to deploy the project's build outputs to the specified repositories.

You must specify which repositories to deploy to using the appropriate configuration. If no repositories are set, deployment will not occur and a warning will be issued. This instruction is typically used in conjunction with plugins that handle the actual deployment process.

TODO Needs review - AI Generated content
