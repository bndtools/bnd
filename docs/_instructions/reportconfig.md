---
layout: default
class: Workspace & Project
title: -reportconfig plugin-def ( ',' plugin-def )*  
summary: Configure a the content of report.
---

The purpose of the `-reportconfig` instruction is to configure the content of the reports exported with the `-exportreport` instruction.

When a report is generated, a set of plugins is used to extract a specific piece of data from the source (for example, the information contain in metatypes from a bundle source). Those plugins are generally designed to not require a configuration and are silently ignored if they do not find any data, thus, this instruction should rarely be used.

Additional plugins can be declared and configured with the [-plugin.*](./plugin.html) instruction and will be available for all your reports, however the `-reportconfig` instruction gives more control on the  plugins that should be used when generating a specific report. This instruction diverges from the `-plugins.*` instruction as you can declare named configuration, for example `-reportconfig.api-bundle:...` will have the name `api-bundle` that you can then use for a specific report `-exportreport:file.json;configName=api-bundle`. In addition, this instruction allows to declare plugins with a short name instead of the canonical name of the plugin class (`importFile` instead of `biz.aQute.bnd.reporter.plugins.entries.any.ImportResourcePlugin`).

*See [-exportreport](./exportreport.html) instruction documentation.*

## Syntax

    -reportconfig.xxx  ::= plugin-def ( ',' plugin-def ) *
    plugin-def         ::= plugin | 'clearDefaults'
    plugin             ::= ( qname | plugin-name ) ( ';' parameters ) *
    plugin-name        ::= extended

where *xxx* is the name of the configuration.

## Example

One use case is when you want a specific resource from a bundle in the report but where there is no plugins to extract it. For this you can use the `importJarFile` plugin which need the path to the resource inside the bundle and will import it into the report.

For example, if you need blueprint data:

**bnb.bnd**

    -reportconfig.blueprint: importJarFile;path=OSGI-INF/blueprint/component.xml
    -exportreport: metadata.json;configName=blueprint

## Clean configuration

When you set a `-reportconfig.xxx` instruction, a list of default plugins will be added to the specified list. If you do not want the default plugins you can use the special property `clearDefaults`:

**bnb.bnd**

    -reportconfig.blueprint: \
       importJarFile;path=OSGI-INF/blueprint/component.xml, \
       clearDefaults

## Plugins

This section describes the available plugins in Bnd, additional plugins may be provided by a specific build tool.

All plugins have the `entryName` property which can be set to override the name under which data will be aggregated into the reports (this corresponds to a tag name when serialised in `XML`):

     -reportconfig.bundle: metatypes;entryName=serviceConfigs

### Any Entry

This plugin allows the user to define an arbitrary entry.

* Short name: `anyEntry`
* Properties:
   * `key`: the name under which the value will be available in the report.
   * `value`: the value of the entry.
* Default Plugin: no

```
-reportconfig.api-bundle:anyEntry;key=bundleType;value=api
```

### Import File

This plugin allows to add a local or remote file to the report. The type of the file can be: `properties`, `manifest`, `XML` and `JSON`.

* Short name: `importFile`
* Properties:
   * `url`: URL to the file.
   * `type`: The type of the file. (optional)
* Default Plugin: no

```
-reportconfig.bundle:importFile;url=http://<...>/myFile.json
```

### Import Jar File

This plugin allows to add a file contains in a bundle to the report. The type of the file can be: `properties`, `manifest`, `XML` and `JSON`.

* Short name: `importJarFile`
* Properties:
   * `path`: Path from the root of the Jar.
   * `type`: The type of the file. (optional)
* Default Plugin: no

```
-reportconfig.bundle:
```
### CommonInfo

Add some common data to the report. If data are extracted from the workspace, the following properties will be read in the build.bnd file: `ws-name`, `ws-description`, `ws-version`, `ws-icons`, `ws-docURL`, `ws-updateLocation`, `ws-licenses`, `ws-developers`, `ws-scm`, `ws-copyright`, `ws-vendor`, `ws-contactAddress`; otherwise, the corresponding headers will be read. If data are extracted from a project, the above properties will be read in the bnd.bnd file but with a `p-` prefix, such as `p-contactAddress`.

* Short name: `commonInfo`
* Default Plugin: yes

### Manifest

Add the OSGI headers to the report.

* Short name: `manifest`
* Default Plugin: yes


### Components

Add a list of the declarative services.

* Short name: `components`
* Default Plugin: yes


### Metatypes

Add a list of the metatypes.

* Short name: `metatypes`
* Default Plugin: yes

### File Name

Add the file name or the folder name in which the source is backed up.

* Short name: `fileName`
* Default Plugin: yes

### Bundles

Add a list of bundle data (for example, the bundles built by a project).

* Short name: `bundles`
* Properties:
   * `useConfig`: The configuration name used when generating the data. (optional)
   * `excludes`: A list of bundle symbolic names to exclude. (optional)
* Default Plugin: yes

```
-reportconfig.project:bundles;useConfig=api-bundle;excludes="com.domain.product.provider"
```

### Code Snippets

Add a list of code snippet extracted from source files in a project. By default, source files containing code snippets must be located in a `<root test source dir>/examples` folder and its sub directories (eg; `src/test/java/examples/**`). The actual supported file types are: `java`.

Code snippets can either be a single sample code with a title and a description or have multiple steps.

* Short name: `codeSnippets`
* Properties:
   * `path`: The directory path from which code snippets will be looked up. (optional)
* Default Plugin: yes

#### Java Code Snippets

You can either export a full type or a method. For the snippet to be extracted you must add a comment block to the type or the method. The comment must contain the `${snippet }` tag with the following optional properties:

* `title` (String): A title for the code snippet.
* `description` (String): A short description of the code snippet.
* `includeDeclaration` (Boolean): If true, the type declaration (or the method declaration) will be include, otherwise only its content. Default is true.
* `includeImports` (Boolean): If true, the imports declration will be include. Default is true.
* `groupName` (String): A user defined group name. If set, it will turn the actual snippet into a snippet with multiple steps, only the title and the description considered then and                the snippet children must reference its `groupName` in their `parentGroup` property.
* `parentGroup` (String): The parent group name. If set, it will make the actual snippet a step in the referenced snippet group.
* `id` (String): A user defined id of the snippet. By default, the Id is the type name or the method name, optionally appended by a counting starting from 1 if not unique (eg; MyType, MyType1, etc).

Here is an example illustrating a code snippet with steps:

```java
/**
 * ${snippet title=How to Print Something in Java, description="In this example we will show how to
 * print a string to the console.", groupName=print} 
 */
public class PrintExample {

  /**
   * ${snippet title=Print No New Line, description="Here, we print without a new line at the end.",
   * parentGroup=print, includeDeclaration=false} 
   */
  public void printNoNewLine() {
    System.out.print("Hello");
  }

  /**
   * ${snippet title=Print With New Line, description="Here, we print with a new line at the end.",
   * parentGroup=print, includeDeclaration=false} 
   */
  public void printWithNewLine() {
    System.out.println("Hello");
  }
}
```

Which will result in the following `json` object:

```json
{ 
   "id": "PrintExample",
   "title":"How to Print Something in Java",
   "description":"In this example we will show how to print a string to the console.",
   "steps":[
      {
         "id": "printNoNewLine",
         "title":"Print No New Line",
         "description":"Here, we print without a new line at the end.",
         "programmingLangague":"java",
         "codeSnippet":"System.out.print(\"Hello\");"
      },
      {
         "id": "printWithNewLine",
         "title":"Print With New Line",
         "description":"Here, we print with a new line at the end.",
         "programmingLangague":"java",
         "codeSnippet":"System.out.println(\"Hello\");"
      }
   ]
}
```

### Projects

Add a list of project data (for example, the projects built by the workspace).

* Short name: `projects`
* Properties:
   * `useConfig`: The configuration name used when generating the data. (optional)
   * `excludes`: A list of project names to exclude.
* Default Plugin: yes

```
-reportconfig.ws:projects;useConfig=bnd-proj;excludes="maven-index"
```

### Maven Coordinate

Add the maven coordinate of the bundle (extracted from the pom.properties file).

* Short name: `mavenCoordinate`
* Properties: no
* Default Plugin: yes
