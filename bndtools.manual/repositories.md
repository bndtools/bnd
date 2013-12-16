---
title: Bndtools Repositories
description: Using and configuring repositories.
author: Neil Bartlett
---

Bndtools uses *repositories* to supply dependencies to be used at build-time and at runtime. A repository is essentially a collection of bundles, optionally with some kind of index. It may be located anywhere: in the workspace; somewhere else on the local file system; or on a remote server.

Bndtools uses repositories in the following ways:

* To look up bundles specified on the Build Path by Bundle Symbolic Name (BSN) and version;
* To resolve the Run Requirements list;
* To look up bundles specified in the Run Bundles list by BSN and version.

Repositories are implemented as bnd plug-ins, and can be configured by editing the Plugins sections of the workspace configuration file (*Bndtools* menu » *Open main bnd config*).

![](/images/concepts/repositories01.png)

Since repositories are implemented as plug-ins, it is theoretically possible to support almost any kind of repository, by developing a new plug-in type; though of course it is more convenient to use an existing repository plug-in. Bnd and Bndtools support the following repository types out-of-the-box.

Indexed Repositories
====================

Bndtools supports a collection of repositories based on an _index_ file that reports the content of the repository along with the capabilities and requirements of each resource listed. There are multiple available formats for the index:

* OBR, named after the [OSGi Bundle Repository](http://www.osgi.org/Repository/BIndex) pseudo-standard. This format is obsolete but still used by some parts of the OSGi ecosystem.
* R5, named after OSGi Release 5 which introduced a true specification for OSGi Repositories. The format of this index is described in the OSGi Release 5 Enterprise specification, section 132.5.
* Other arbitrary formats may be supported by the addition of a `IRepositoryContentProvider` plugin.

The advantage of using indexed repositories is that they can be used for automatic Resolution in the bndrun editor. There are two basic types of indexed repositories:

Fixed Index Repositories
------------------------

This repository can use an index file which is located anywhere, so long as the location can be addressed in the form of a URL. For example the index can be located on the local filesystem and addressed via a `file:` URL, or it can be located on a remote HTTP(s) server. The locations of the actual resources -- i.e. JAR files -- is specified by URLs embedded in the index file itself, and so they can also be either local or remote. In the case of remote index and/or resources, a local cache is used to avoid repeated downloads and to enable offline builds.

A Fixed Index repository cannot be modified from within bnd or Bndtools.

The following properties are supported:

------------------------------------------------------------------------------------------------------
Name        Description                                   Required?
----------- --------------------------------------------  --------------------------------------------
`name`      Name for the repository.                      No.

`locations` Comma-separated list of index URLs.           No. Default: empty
            **NB:** surround this value with
            single-quotes if it contains more than one
            entry.

`cache`     Local cache directory for remote              No. Default: `${user.home}/.bnd/cache/`
            resources.
------------------------------------------------------------------------------------------------------

It is not necessary to specify the format of the index -- this will be auto-detected so long as the format is one of those supported by the plugin. The index file may optionally be compressed with gzip. 

Local Indexed Repository
------------------------

This repository maintains a local filesystem directory of bundles. The repository is editable from with bnd/Bndtools and the index file is regenerated automatically when bundles are deployed into it.

The following properties are supported:

------------------------------------------------------------------------------------------------------
Name        Description                                   Required?
----------- --------------------------------------------  --------------------------------------------
`name`      Name for the repository.                      No.

`local`     The local filesystem directory.               Yes.

`type`      The type (format) of index to generate. See   No. Default: `R5`
            note 1 below.

`pretty`    Whether to generate the index in pretty-      No. Default: false
            printed format. See note 2 below.

`readonly`  Whether the repository should be read-only,   No. Default: false
            i.e. disabled for editing from Bndtools.

`mode`      Resolution mode: `build`, `runtime` or `any`. No. Default: `any`
            Controls the resolution phase in which this
            repository may be used.

`locations` Comma-separated list of *additional* index    No. Default: empty
            URLs. **NB:** surround this value with
            single-quotes if it contains more than one
            entry.

`cache`     Local cache directory for remote              No. Default: `${local}/.cache`
            resources.
------------------------------------------------------------------------------------------------------

Note 1: The index is generated by default in R5 format. To request alternative format(s), specify a list of format names separated by the "|" (pipe) character.
For example, to generate both R5 and OBR formats specify `type=R5|OBR`.

Note 2: R5 indexes are generated by default with no whitespace/indenting and gzipped, and the default index file name is `index.xml.gz`. Turning on pretty-printing enables indented, uncompressed output into the file `index.xml`. This setting has no effect on OBR indexes, which are always indented/uncompressed and named `repository.xml`.

File Repository
===============

This type of repository is based on a very simple file system directory structure. It is editable from within Bndtools. **NB:** it does not support indexing, so repositories of this type cannot participate in resolution of Run Requirements.

The following properties are supported:

------------------------------------------------------------------------------------------------------
Name        Description                                   Required?
----------- --------------------------------------------  --------------------------------------------
`name`      Name for the repository.                      No.

`location`  The local filesystem directory.               Yes.

`readonly`  Whether the repository should be read-only,   No. Default: false
            i.e. disabled for editing from Bndtools.
------------------------------------------------------------------------------------------------------

Aether (Maven) Repositories
===========================

This type of repository uses [Eclipse Aether](http://www.eclipse.org/aether/) to work with Maven-style repositories. This includes the public Maven Central repository, and also repositories hosted by repository manager products such as [Nexus](http://www.sonatype.org/nexus) from Sonatype or [Artifactory](http://www.jfrog.com/home/v_artifactory_opensource_overview) from JFrog.

It can also optionally use an index that is provided by the remote repository manager. This feature requires that indexing is enabled and configured in the manager; for example, the Nexus OBR Plugin can be used for this purpose. When an index is provided, this repository type be used to resolve Run Requirements.

The following properties are supported:

------------------------------------------------------------------------------------------------------
Name        Description                                   Required?
----------- --------------------------------------------  --------------------------------------------
`name`      Name for the repository.                      No. Default: "AetherRepository"

`url`       Main URL of the remote repository.            Yes.

`username`  Username for authentication with              No, but certain operations (e.g. deploy)
            repository manager.                           may fail if the manager requires
                                                          authentication.

`password`  Password for authentication with              No.
            repository manager.

`indexUrl`  The URL of the index file generated by the    No. Default: main URL + `-obr/.meta/obr.xml`.
            repository manager.

`cache`     Local cache directory for remote              No. Default: `${user.home}/.bnd/cache/`
            resources.
------------------------------------------------------------------------------------------------------

## Limitations ##

The functionality provided by Aether (and Maven in general) is quite limited. If an index is not available then the following limitations apply:

* **No Browsing**: there is no ability to list or browse the contents of the repository. When viewed in the Repositories view in Bndtools, the repository will appear to be empty. It is still possible to use artifacts from the repository but you must know the exact Group ID and Artifact ID and specify them in the format `<groupId>:<artifactId>`.

* **No Resolving**: the repository cannot participate in Run Requirement resolution.

Maven Repositories (Old Style)
==============================

**NB. The repository types in this section are deprecated. Please use the Aether Repository type instead. The following documentation is retained for reference purposes only.**

## Maven Local ##

This type of repository is used to access bundles in the Maven local repository, usually found in `$HOME/.m2/repository`. Note that this plug-in accesses the Maven repository directly and does not building with Maven.

------------------------------------------------------------------------------------------------------
Name        Description                                   Required?
----------- --------------------------------------------  --------------------------------------------
`name`      Name for the repository.                      No.

`root`      The location of the Maven repository.         No. Default: `$HOME/.m2/repository`
------------------------------------------------------------------------------------------------------

Note that if you use the [Bundle Plugin for Maven](http://felix.apache.org/site/apache-felix-maven-bundle-plugin-bnd.html) then you can also use the [OBR](#obr-repository) repository type, since the Bundle Plugin generates an OBR index file whenever `maven install` is executed. For example:

    aQute.bnd.deployer.repository.FixedIndexedRepo;\
            locations='file:${user.home}/.m2/repository/repository.xml';\
            name='Maven Repo'


## Maven Remote ##

This type of repository can be used to access bundles in a remote Maven repository, including Maven Central or any Nexus repository. **NB:** this repository type is not browseable; if you attempt to view the contents of the repository using the Repositories View in Bndtools, it will appear to be empty. However it will be possible to reference JARs from the repository in your `-buildpath` or `-runbundles` if the group ID and artefact ID is known.

For example to reference a JAR with group ID `org.osgi` and artefact ID `osgi_R4_core`, use the following syntax:

	-buildpath: org.osgi+osgi_R4_core

------------------------------------------------------------------------------------------------------
Name            Description                                      Required?
--------------- -----------------------------------------------  -------------------------------------
`repositories`  Comma-separated list of Maven repository URLs.   No. Default: empty 
                **NB:** surround this value with
                single-quotes if it contains more than one
                entry.
------------------------------------------------------------------------------------------------------

