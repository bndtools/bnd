---
layout: default
title: xmlrepodiff
summary: |
   Shows the differences between two XML resource repositories
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   xmlrepodiff [options]  <newer XML resource repository> <older XML resource repository>

#### Options: 
- `[ -e --expandfilter ]` Expand 'filter' directives
- `[ -i --ignore <string> ]` Ignore elements from the comparison result (Format: type=name,..) e.g. RESOURCE_ID=org.apache.felix.scr#com.company.runtime.*,CAPABILITY=bnd.workspace.project#osgi.wiring.package:javax.xml.*,ATTRIBUTE=bundle-symbolic-name:system.bundle,REQUIREMENT=osgi.wiring.package:org.xml.*
- `[ -s --showall ]` Display all (changed and unchanged both)

<!-- Manual content from: ext/xmlrepodiff.md --><br /><br />


## Examples

    MAJOR      REPOSITORY <repository>
	 MAJOR      RESOURCE_ID org.apache.felix.configadmin
	  MAJOR      CAPABILITIES <capabilities>
	   MAJOR      CAPABILITY bnd.maven:org.apache.felix:org.apache.felix.configadmin
	    REMOVED    ATTRIBUTE  maven-version:1.9.10
	    ADDED      ATTRIBUTE  maven-version:1.9.22
	   REMOVED    CAPABILITY osgi.content:77B03B938E796C0512D9AD89ACF287CCE09C14A159CA05B6CB74DDA17E7AB3FA
	    REMOVED    ATTRIBUTE  mime:application/vnd.osgi.bundle
	    REMOVED    ATTRIBUTE  size:155630
	    REMOVED    ATTRIBUTE  url:file:/Users/amit/.m2/repository/org/apache/felix/org.apache.felix.configadmin/1.9.10/org.apache.felix.configadmin-1.9.10.jar
	   ADDED      CAPABILITY osgi.content:B349E16D60DA66B6DA70AB3E056677A9D6A0B8953DF84ECD63B10AA5EF3C5865
	    ADDED      ATTRIBUTE  mime:application/vnd.osgi.bundle
	    ADDED      ATTRIBUTE  size:168301
	    ADDED      ATTRIBUTE  url:file:/Users/amit/.m2/repository/org/apache/felix/org.apache.felix.configadmin/1.9.22/org.apache.felix.configadmin-1.9.22.jar
	   MAJOR      CAPABILITY osgi.identity:org.apache.felix.configadmin
	    REMOVED    ATTRIBUTE  version:1.9.10
	    ADDED      ATTRIBUTE  version:1.9.22
	   MAJOR      CAPABILITY osgi.wiring.bundle:org.apache.felix.configadmin
	    REMOVED    ATTRIBUTE  bundle-version:1.9.10
	    ADDED      ATTRIBUTE  bundle-version:1.9.22
	   MAJOR      CAPABILITY osgi.wiring.host:org.apache.felix.configadmin
	    REMOVED    ATTRIBUTE  bundle-version:1.9.10
	    ADDED      ATTRIBUTE  bundle-version:1.9.22
	   MAJOR      CAPABILITY osgi.wiring.package:org.apache.felix.cm
	    REMOVED    ATTRIBUTE  bundle-version:1.9.10
	    ADDED      ATTRIBUTE  bundle-version:1.9.22
	   MAJOR      CAPABILITY osgi.wiring.package:org.apache.felix.cm.file
	    REMOVED    ATTRIBUTE  bundle-version:1.9.10
	    ADDED      ATTRIBUTE  bundle-version:1.9.22
	   MAJOR      CAPABILITY osgi.wiring.package:org.osgi.service.cm
	    REMOVED    ATTRIBUTE  bundle-version:1.9.10
	    ADDED      ATTRIBUTE  bundle-version:1.9.22
	  REMOVED    VERSION    1.9.10
	  ADDED      VERSION    1.9.22
	 MAJOR      RESOURCE_ID org.apache.felix.eventadmin
	  MAJOR      REQUIREMENTS <requirements>
	   MAJOR      REQUIREMENT osgi.ee:JavaSE
	    REMOVED    DIRECTIVE  filter:(&(osgi.ee=JavaSE)(version=1.7))
	    ADDED      DIRECTIVE  filter:(&(osgi.ee=JavaSE)(version=1.8))
	  MAJOR      CAPABILITIES <capabilities>
	   MAJOR      CAPABILITY bnd.maven:org.apache.felix:org.apache.felix.eventadmin
	    REMOVED    ATTRIBUTE  maven-version:1.5.0
	    ADDED      ATTRIBUTE  maven-version:1.6.2
	   ADDED      CAPABILITY osgi.content:445A90F6E31CDE9635C474CEA286273481D2E6EE293B52D8FC42ED8E927B5604
	    ADDED      ATTRIBUTE  mime:application/vnd.osgi.bundle
	    ADDED      ATTRIBUTE  size:83611
	    ADDED      ATTRIBUTE  url:file:/Users/amit/.m2/repository/org/apache/felix/org.apache.felix.eventadmin/1.6.2/org.apache.felix.eventadmin-1.6.2.jar
	   REMOVED    CAPABILITY osgi.content:A433A9020E1EAD82494AA6611E8A644F88733BD0278F349D6BEA3B2E448DDD71
	    REMOVED    ATTRIBUTE  mime:application/vnd.osgi.bundle
	    REMOVED    ATTRIBUTE  size:81529
	    REMOVED    ATTRIBUTE  url:file:/Users/amit/.m2/repository/org/apache/felix/org.apache.felix.eventadmin/1.5.0/org.apache.felix.eventadmin-1.5.0.jar
	   MAJOR      CAPABILITY osgi.identity:org.apache.felix.eventadmin
	    REMOVED    ATTRIBUTE  version:1.5.0
	    ADDED      ATTRIBUTE  version:1.6.2
	   MAJOR      CAPABILITY osgi.wiring.bundle:org.apache.felix.eventadmin
	    REMOVED    ATTRIBUTE  bundle-version:1.5.0
	    ADDED      ATTRIBUTE  bundle-version:1.6.2
	   MAJOR      CAPABILITY osgi.wiring.host:org.apache.felix.eventadmin
	    REMOVED    ATTRIBUTE  bundle-version:1.5.0
	    ADDED      ATTRIBUTE  bundle-version:1.6.2
	   MAJOR      CAPABILITY osgi.wiring.package:org.osgi.service.event
	    REMOVED    ATTRIBUTE  bundle-version:1.5.0
	    ADDED      ATTRIBUTE  bundle-version:1.6.2
	  REMOVED    VERSION    1.5.0
	  ADDED      VERSION    1.6.2
	 REMOVED    RESOURCE_ID org.osgi.util.promise
	  REMOVED    REQUIREMENTS <requirements>
	   REMOVED    REQUIREMENT osgi.ee:JavaSE
	    REMOVED    DIRECTIVE  filter:(&(osgi.ee=JavaSE)(version=1.7))
	   REMOVED    REQUIREMENT osgi.wiring.package:org.osgi.util.function
	    REMOVED    DIRECTIVE  filter:(&(osgi.wiring.package=org.osgi.util.function)(version>=1.1.0)(!(version>=2.0.0)))
	  REMOVED    CAPABILITIES <capabilities>
	   REMOVED    CAPABILITY bnd.maven:org.osgi:org.osgi.util.promise
	    REMOVED    ATTRIBUTE  maven-classifier:
	    REMOVED    ATTRIBUTE  maven-extension:jar
	    REMOVED    ATTRIBUTE  maven-repository:Runtime
	    REMOVED    ATTRIBUTE  maven-version:1.1.1
	   REMOVED    CAPABILITY osgi.content:4F85BECCD281CC1A4E735BD266A0DD3DB11651D3D0DDE001E6BFA55DBDFDEE83
	    REMOVED    ATTRIBUTE  mime:application/vnd.osgi.bundle
	    REMOVED    ATTRIBUTE  size:75587
	    REMOVED    ATTRIBUTE  url:file:/Users/amit/.m2/repository/org/osgi/org.osgi.util.promise/1.1.1/org.osgi.util.promise-1.1.1.jar
	   REMOVED    CAPABILITY osgi.identity:org.osgi.util.promise
	    REMOVED    ATTRIBUTE  copyright:Copyright (c) OSGi Alliance (2000, 2018). All Rights Reserved.
	    REMOVED    ATTRIBUTE  description:OSGi Companion Code for org.osgi.util.promise Version 1.1.1
	    REMOVED    ATTRIBUTE  documentation:https://www.osgi.org/
	    REMOVED    ATTRIBUTE  license:Apache-2.0; link="http://www.apache.org/licenses/LICENSE-2.0"; description="Apache License, Version 2.0"
	    REMOVED    ATTRIBUTE  type:osgi.bundle
	    REMOVED    ATTRIBUTE  version:1.1.1.201810101357
	   REMOVED    CAPABILITY osgi.wiring.bundle:org.osgi.util.promise
	    REMOVED    ATTRIBUTE  bundle-version:1.1.1.201810101357
	   REMOVED    CAPABILITY osgi.wiring.host:org.osgi.util.promise
	    REMOVED    ATTRIBUTE  bundle-version:1.1.1.201810101357
	   REMOVED    CAPABILITY osgi.wiring.package:org.osgi.util.promise
	    REMOVED    ATTRIBUTE  bnd.hashes:-1923478059
	    REMOVED    ATTRIBUTE  bundle-symbolic-name:org.osgi.util.promise
	    REMOVED    ATTRIBUTE  bundle-version:1.1.1.201810101357
	    REMOVED    ATTRIBUTE  version:1.1.1
	    REMOVED    DIRECTIVE  uses:org.osgi.util.function
	  REMOVED    VERSION    1.1.1.201810101357
	 REMOVED    RESOURCE_ID org.osgi.util.pushstream
	  REMOVED    REQUIREMENTS <requirements>
	   REMOVED    REQUIREMENT osgi.ee:JavaSE/compact1
	    REMOVED    DIRECTIVE  filter:(&(osgi.ee=JavaSE/compact1)(version=1.8))
	   REMOVED    REQUIREMENT osgi.wiring.package:org.osgi.util.function
	    REMOVED    DIRECTIVE  filter:(&(osgi.wiring.package=org.osgi.util.function)(version>=1.1.0)(!(version>=2.0.0)))
	   REMOVED    REQUIREMENT osgi.wiring.package:org.osgi.util.promise
	    REMOVED    DIRECTIVE  filter:(&(osgi.wiring.package=org.osgi.util.promise)(version>=1.1.0)(!(version>=2.0.0)))
	  REMOVED    CAPABILITIES <capabilities>
	   REMOVED    CAPABILITY bnd.maven:org.osgi:org.osgi.util.pushstream
	    REMOVED    ATTRIBUTE  maven-classifier:
	    REMOVED    ATTRIBUTE  maven-extension:jar
	    REMOVED    ATTRIBUTE  maven-repository:Runtime
	    REMOVED    ATTRIBUTE  maven-version:1.0.1
	   REMOVED    CAPABILITY osgi.content:1E0C9D435A107444A4461788E62BDDC94715E444AFDBC54417593ECA4BB50CE2
	    REMOVED    ATTRIBUTE  mime:application/vnd.osgi.bundle
	    REMOVED    ATTRIBUTE  size:132226
	    REMOVED    ATTRIBUTE  url:file:/Users/amit/.m2/repository/org/osgi/org.osgi.util.pushstream/1.0.1/org.osgi.util.pushstream-1.0.1.jar
	   REMOVED    CAPABILITY osgi.identity:org.osgi.util.pushstream
	    REMOVED    ATTRIBUTE  copyright:Copyright (c) OSGi Alliance (2000, 2018). All Rights Reserved.
	    REMOVED    ATTRIBUTE  description:OSGi Companion Code for org.osgi.util.pushstream Version 1.0.1
	    REMOVED    ATTRIBUTE  documentation:https://www.osgi.org/
	    REMOVED    ATTRIBUTE  license:Apache-2.0; link="http://www.apache.org/licenses/LICENSE-2.0"; description="Apache License, Version 2.0"
	    REMOVED    ATTRIBUTE  type:osgi.bundle
	    REMOVED    ATTRIBUTE  version:1.0.1.201810101357
	   REMOVED    CAPABILITY osgi.wiring.bundle:org.osgi.util.pushstream
	    REMOVED    ATTRIBUTE  bundle-version:1.0.1.201810101357
	   REMOVED    CAPABILITY osgi.wiring.host:org.osgi.util.pushstream
	    REMOVED    ATTRIBUTE  bundle-version:1.0.1.201810101357
	   REMOVED    CAPABILITY osgi.wiring.package:org.osgi.util.pushstream
	    REMOVED    ATTRIBUTE  bnd.hashes:-1923478059
	    REMOVED    ATTRIBUTE  bundle-symbolic-name:org.osgi.util.pushstream
	    REMOVED    ATTRIBUTE  bundle-version:1.0.1.201810101357
	    REMOVED    ATTRIBUTE  version:1.0.1
	    REMOVED    DIRECTIVE  uses:org.osgi.util.function,org.osgi.util.promise
	  REMOVED    VERSION    1.0.1.201810101357
