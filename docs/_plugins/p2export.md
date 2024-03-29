---
title: P2 Exporter
layout: default
class: Project
summary: An exporter to export features from a bnd build
---

The Bnd Export plugin is a powerful tool that enables the export of a p2 repository. This manual will guide you through the process of using the plugin effectively. 

The plugin is activated by adding it to the `-plugin` clauses of the `build.bnd`. It takes no configuration. In the Bndtools, there is a context menu to help.

    -plugin \
        aQute.p2.export.P2Exporter

If this plugin is present in your workspace, a project can define an _export_ instruction. The export instruction
looks as follows:

    -export \
        release.bndrun; \
            type=p2; \
            name=MyReleaseRepo.jar

With this instruction, the plugin exports a p2 repository named "MyReleaseRepo.jar" containing the specified features, plugins, and metadata. You can also include additional requirements and customize various aspects of the repository using Bnd's capabilities. The key is the master release bndrun file, the specified type is for a p2 repository. 

In the `release.bndrun` file you can specify the following aspects:

A list of features that needs to be included in this release under the macro `features`:

    features        = \
            bndtools.main.feature.bndrun, \
            bndtools.m2e.feature.bndrun, \
            bndtools.pde.feature.bndrun
The list must point to the paths of bndrun files. Each bndrun file will contain specific information for the feature. The bndrun files inherit from the `release.bndrun` file and then from the workspace.

The update URL and label can also be specified with macros:

    update          =   https://bndtools.jfrog.io/bndtools/update-latest
    update.label    =   Bndtools Update Site

Each feature can participate in a number of categories. The category definitions look as follows:

    -categories \
        bndtools; \
            label = Bndtools; \
            description = Bndtools, the incredible IDE for OSGi

This can be repeated by adding more clauses. A feature's bndrun file must set the Bundle-Category that is then matched in this list fo the label and description.

And last but not least, general header files.

    Bundle-Version  7.0.0
    Bundle-License:  \
        ASL-2.0;\
            description="This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0";\
            link="https://opensource.org/licenses/Apache-2.0", \
        EPL-2.0;\
            description="This program and the accompanying materials are made available under the terms of the Eclipse License, Version 2.0";\
            link="https://opensource.org/licenses/EPL-2.0"
            
    
    Bundle-DocURL:          https://bnd.bndtools.org/
    Bundle-Vendor           bnd
    Bundle-Copyright:       Copyright bndtools


These headers are inherited in the features and can be overridden.

## Feature Definitions

Please note that the features you specify must be valid bndrun/feature files. The bndrun files inherit from the Workspace. It is therefore possible to set shared values in the workspace and use them in the bndrun files. For example, you can set the `Bundle-Version` in the workspace and use it in all the feature bndrun files.

Although the features are not bundles, the exporter treats them as such. This means that most of the OSGi headers used for bundles can also be used for features. For example, you can set the `Bundle-License` header in the manifest file of a feature to specify licensing information for that feature.

The following headers can be used in the feature files.

* `Bundle-Name` – Human readable name of the feature
* `Bundle-SymbolicName` – Used for the feature id. This is default the same as the name of the feature/bndrun file.
* `Bundle-Description` – A human readable description of the feature
* `Bundle-DocUrl` – A URL to the documentation of the feature
* `Bundle-Copyright` – The copyright
* `Bundle-License` – The licenses
* `Bundle-Category` – The category of the feature
* `Bundle-Vendor` – The vendor will be used as the provider of the feature

The format of these headers is exactly as outlined by their OSGi specification.

Features can require bundles and other features. 

Bundles will come from the `-runbundles` instruction. This instruction can be managed with the resolver but it can also be set manually. Standard rules apply. The workspace is consulted to find the bundles and their versions. The bundles will be the highest available version in the repository.

    -runbundles \
        biz.aQute.bnd.annotation;version=5.0.0,\
        org.apache.felix.gogo.runtime;version=1.1.0

Features can also require other features. Features, and theoretically other requirements can be added using the Require-Capability header. For this reason, a number of pseudo namespaces are created to make it more readable.

* `feature` – Specifies a feature
*  p2 namespace – P2 namespaces are used to specify requirements for the feature. For example, `osgi.bundle` specifies a bundle requirement and `java.package` specifies a package requirement. This way, any P2 eclipse namespace can be added.

These pseudo namespaces use the attributes `name` and `version` to specify the name and version _range_ of the requirement. They do not require a filter since p2 lacks this concept.

The following example shows how to specify a feature requirement:

    Require-Capability: \
        feature; \
            name= com.example.my.first.feature; \
            version='[1.1.0,1.1.0]'

If no version is specified in a feature, it will default to the version of the requiring feature. This means that if you specify a feature requirement without a version, the version of the requiring feature will be used as the version requirement for the required feature.

## Categories

Features can be grouped in categories.A feature can be a member in a category by specifying the `Bundle-Category` header. The value of this header is the identity of the category. For example, the following header specifies that the feature belongs to the category "bndtools" and "java":

    Bundle-Category: bndtools, java

The release bndrun file provide the details of the category. The following example shows how to specify the details of the categories can be specified with the `-categories` instruction in the project's bnd.bnd file:

    -categories \
        bndtools; \
            label=Bndtools; \
            description=Bndtools is a set of tools for OSGi development,\
        java; \
            label=Java ©; \
            description=The Java language

## Output

The output will be a self contained p2 repository contained in a JAR file. it contains the following:

* `content.jar` – This is the metadata of the repository
* `artifacts.jar` – A list of all the artifacts in the repository and rules how to map an id to a file in the p2 archive.
* `plugins/` – A directory with all the bundles in the repository
* `features/` – A directory with all the features in the repository

The repository can be used as a drop-in replacement for the Eclipse p2 repository. It can be used to update an Eclipse installation or to install a new Eclipse installation.

## Sample Setup

In the workspace's build.bnd file:

    -plugin \
        aQute.p2.export.P2Exporter

    Bundle-Version: 1.0.0
    Bundle-Vendor: Example
    Bundle-License: Apache-2.0
    Bundle-Copyright: Public domain


In project X's bnd.bnd file:

    -export \
        release.bndrun; \
            name=myrepo.jar; \
            features="a.bndrun,b.bndrun"

    -categories \
        java; \
            label=Java ©; \
            description=The Java language

In the a.bndrun file:

    Bundle-Name: A
    Bundle-SymbolicName: com.example.a
    Bundle-Description: A feature called A!
    Bundle-Category: java
    Bundle-DocUrl: https://example.com/a

    Require-Capability: \
        feature; \
            name= com.example.b

    -runbundles: \
        com.example.util, \
        com.example.a

In the b.bndrun file:

    Bundle-Name: B
    Bundle-SymbolicName: com.example.b
    Bundle-Description: A feature called B!
    Bundle-Category: java
    Bundle-DocUrl: https://example.com/b

    -runbundles: \
        com.example.util, \
        com.example.util2
        com.example.b

This will generate a p2 archive called myrepo.jar with:

* `content.jar`
  * `content.xml` – The metadata of the repository
    * repository metadata with name X, version 1.0.0, and provider Example
    * for each feature, an jar IU and group IU
    * for each bundle, a bundle IU
* `artifacts.jar`
  * `artifacts.xml` – A list of all the artifacts in the repository and rules how to map an id to a file in the p2 archive.
* `plugins/`
    * `com.example.util-1.0.0.jar`
    * `com.example.util2-1.0.0.jar` 
    * `com.example.a-1.0.0.jar`
    * `com.example.b-1.0.0.jar`
* `features/`
  * `a-1.0.0.jar`
     * `feature.xml` – The feature metadata for feature a
  * `b-1.0.0.jar`
    * `feature.xml` – The feature metadata for feature b

## Notes

P2 is a surprisingly complex beast for what it tries to achieve. The P2Exporter plugin makes an attempt to map the much cleaner OSGi model to the P2 model. Sadly, P2 has the attitude that the file formats are not standardized and that the only way to get the right format is to use the P2 API. The authors found it hard to understand the concept of backward compatibility. Once a format is out there, it must be supported in a backward compatible way forever, severely restricting the ability to change the data format. Which means that most of the data formats have been reverse enginered from existing repos. This is not a good situation. Where the OSGi provides a solid foundation with well defined namespaces, the P2 model seems a lot more ad-hoc.

To minimize compatibility problems for this bnd plugin, the code generates the metadata in almost exactly the way it was found that p2 generated the metadata for bndtools. Writing this code raised lots of questions but there was nobody to ask as far as I could see. Any feedback would be highly appreciated.