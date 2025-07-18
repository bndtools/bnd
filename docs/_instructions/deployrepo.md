---
layout: default
title: -deployrepo
class: Project
summary: |
   Specifies to which repo the project should be deployed.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-deployrepo=cnf`

- Values: `${repos}`

- Pattern: `.*`

<!-- Manual content from: ext/deployrepo.md --><br /><br />


The `-deployrepo` instruction is used to deploy a bundle (JAR file) to a specific repository using deploy plugins, such as the MavenDeploy plugin. You can specify the repository by name, and the bundle will be uploaded to that repository if it supports write operations.

If no repository name is provided, the first writable repository found will be used. If no suitable repository is found, deployment will fail with an error. This instruction is typically used in conjunction with plugins that handle the actual deployment process.

Note: This feature may require additional configuration and plugin support to work as intended.


<hr />
TODO Needs review - AI Generated content
