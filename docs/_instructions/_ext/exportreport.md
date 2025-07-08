---
layout: default
class: Workspace & Project
title: -exportreport report-def ( ',' report-def )* 
summary: Configure a list of reports to be exported.
---

The purpose of the `-exportreport` instruction is to configure a list of reports of the current state of the workspace and/or its projects, which can then be exported by the build tool. The primary usage is to automate the documentation of projects. An introduction to this feature can be found [here](../chapters/395-generating-documentation.html).

*See [-exportreport](../commands/exportreport.html) command documentation to export the reports.*

## Syntax

    -exportreport    ::= report-def ( ',' report-def ) *
    report-def       ::= path ( ';' option ) *
    option           ::= scope | template | templateType | parameters | locale | configName
    scope            ::= 'scope' '=' ( 'workspace' | 'project' | EXTENSION )
    template         ::= 'template' '=' ( path | url )
    templateType     ::= 'templateType' '=' ( 'xslt' | 'twig' | EXTENSION )
    parameters       ::= 'parameters' '=' '"' parameter ( ',' parameter )* '"'
    locale           ::= 'locale' '=' <language> ( '-' <country> ( '-' <variant> )? )?
    configName       ::= 'configName' '=' extended


*This is a merged instruction.*

## Example

The most simple configuration is to generate a file which will contain all the OSGI headers, metatypes declarations, declarative services, ... of a bundle built by a project:

**bnb.bnd**

    -exportreport: metadata.json

an `XML` file can also be generated

    -exportreport: metadata.xml

## Advanced usage

### Targeting the data source

The `scope` attribute allows to define all the reports in a common place and to target the source of the extracted data. In Bnd, the two possible values are *workspace* and *project* but other tools (such as Maven) could define their own scopes. 

For example, the following configuration will generate a report at the workspace root, which will aggregate data of all the projects, and one report per project:

**build.bnd**

    -exportreport: \
       ${workspace}/jekyll_metadata.json;scope=workspace, \
       metadata.json;scope=project

### Transformation

The `template` attribute allows to specify a *path* or an *URL* to a template file. In Bnd, two template types are accepted: *XSLT* and *TWIG*.

For example, a template can be used to generate a markdown file:

**bnd.bnd**

    -exportreport: readme.md;template=/home/me/templates/readme.twig


If the extension file is missing, the `templateType` attribute can be used to indicate the template type:

**bnd.bnd**

    -exportreport: webpage.html;template=http:<...>/f57ge56a;templateType=xslt

If the template file needs to be parametrized, the `parameters` attribute can be used to provide a list of parameters and their values:

**bnd.bnd**

    -exportreport: \
       webpage.html; \
         template=<...>/template.xslt; \
         parameters="oneKey=<path to other template>,otherKey=api-bundle"

> If a file with the same name as the exported report but with a template file extension is found in the same folder as the exported report, this file will be used to transform the report instead of the optionally defined `template` attribute. This allows to quickly customize a report without redefining an inherited instruction.

### Internationalisation

The `locale` attribute can be used to extract the data for a specific locale. For example, if a bundle defines some metatype description in French:


**bnd.bnd**

    -exportreport: report.json;locale=fr-FR


### Customization of the Report Content

In some case, it may be necessary to control what data should be present in the report, for example if you use external plugins to contribute to the extraction and aggregation phase. For this you can use the `-reportconfig` instruction to create named configuration of reports.

With the `configName` attribute you can reference the configuration name that will be used to extract and aggregate the data of the report.

*See the [-reportconfig](./reportconfig.html) instruction documentation for more information.*

### Case of Sub-Projects

Reports are generated from workspaces and projects. However, in case of sub-projects, it may be necessary to generate a file for each built bundle and to know what bundle must be process by the template engine. For this, you can define a paramater in your template file and provide a different value for each bundle, for example:

**build.bnd**

    -exportreport: \
       readme.md;template=<...>/readme.twig;scope=project

**bnd.bnd**

    -exportreport.sub: \
       mybundle.api.md; \
         template=<...>/readme.twig; \
         parameters="currentBundle=mybundle.api", \
       mybundle.provider.md; \
         template=<...>/readme.twig; \
         parameters="currentBundle=mybundle.provider"

For this specific multi-project, this will generate three files:

* readme.md
* mybundle.api.md
* mybundle.provider.md
