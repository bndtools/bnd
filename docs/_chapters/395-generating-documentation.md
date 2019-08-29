---
order: 395
title: Generating Documentation
summary: Discusses how to generate documentation of OSGi projects.
layout: default
author: Clément Delgrange
---

Bnd can help you to maintain your projects' documentation up-to-date. 
In a best effort, Bnd can automatically update your documentation according to changes in your programs.
Whenever it is not possible, you still have an easy way to edit the documentation manually. 

The process is in two steps: first, Bnd will analyze your projects and your bundles to 
collect and aggragate relevant data into an intermediate format (`json` or `xml`). Then, 
it will process the data with a template file (`xslt` or `twig`) to generate the documentation.

Relevant data are the OSGi headers, the configuration of your services, code snippets that you write 
with the help of your IDE, your Gogo commands, etc. Once they are collected you have three ways to use them:

1. You can export the intermediate data into a file. You can then use the exported file with any external 
tool (such as *Jekyll* to generate a static site for example).
2. You can define your own documentation files and provide your own templates, Bnd can then directly export the final files.
3. You can use built-in documentation templates. In this latter case, you have no mandatory configurations to provide and updating
the documentation is done by executing a simple command.

This feature is available for the Bnd Workspace Model and Maven projects respectivly using Bnd CLI and `bnd-reporter-maven-plugin`. We will use
Bnd CLI as an example in the next sections, you can directly look at the documentation in the [Github repository](https://github.com/bndtools/bnd/blob/master/maven/bnd-reporter-maven-plugin/README.md) for the corresponding feature with Maven.

> Note: It is necessary that the workspace is completely built before generating the documentation files to take into account the latest changes.

## Generating custom documentation

Custom documentations are configured with the [-exportreport](../instructions/exportreport.html) instruction. 
This instruction defines a list of reports which can then be exported with the [exportreport export](../commands/exportreport.html) subcommand.

For example, executing `bnd exportreport export` on a project with the following configuration:

**bnd.bnd**

    -exportreport: metadata.json, info.html;template=http://.../mytemplate.xslt

will export two files: `metadata.json` (intermediate data) and `info.html` (final documentation file). 

## Generating built-in documentation

### Readme files

With the [exportreport readme](../commands/exportreport.html) subcommand you can generate a set of README files.
If this command is applied on a workspace, a `readme.md` file will be generated into the workspace folder as well as one
`readme.md` file for each project. If a project builds multiple bundles, additional `readme.<bsn>.md` will be generated for each
bundle built.

#### Manual edition

In case you want to customize the README files with your own text, you have to create a `readme.twig` file next to the `readme.md` file that you wish to customize.

The simplest way to proceed is to copy-paste the following snippet into this file:

```
{% raw %}{% embed 'default:readme.twig' %}{% endraw %}
{% raw %}{% block beforeTitle %}{% endraw %}
Write your own markdown text here.
{% raw %}{% endblock %}{% endraw %}
{% raw %}{% endembed %}{% endraw %}
```
Inside an `embed` tag one can only specify block tags which will override the parent template included. 
Here, the parent template is the readme template file built-in into Bnd available at the `default:readme.twig` URL (see the file [here](https://raw.githubusercontent.com/bndtools/bnd/master/biz.aQute.bnd.reporter/src/biz/aQute/bnd/reporter/plugins/transformer/templates/readme.twig)). This file defines a set of blocks that you can override with your own text. 
In the above snippet, we override the `beforeTitle` block. You can add multiple blocks depending on where you want to insert your text, here is a list of available blocks:

* `beforeTitle`, `afterTitle`
* `beforeOverview`, `afterOverview`
* `beforeLinks`, `afterLinks`
* `beforeCoordinates`, `afterCoordinates`
* `beforeArtifacts`, `afterArtifacts`
* `beforeCodeUsage`, `afterCodeUsage`
* `beforeComponents`, `afterComponents`
* `beforeDevelopers`, `afterDevelopers`
* `beforeLicenses`, `afterLicenses`
* `beforeCopyright`, `afterCopyright`
* `beforeVendor`, `afterVendor`

`before*` blocks are shown between the title of a section and the generated content. `after*` blocks are shown at the end of a section, after the generated content. 

#### Parameters

| Name | Description | Default value |
|--- |--- |---|
|`iconUrl` |URL to an icon that will be shown in front of the title | "" |