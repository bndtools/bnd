---
layout: default
class: Processor
title: -pom BOOLEAN | PROPERTIES
summary: Generate a maven pom in the JAR
---

The `-pom` instruction can generate a pom derived from the manifest and store it in the
bundle. The groupId can be specified by the `groupid` key which defaults to the value of
the `-groupid` instruction. If neither the `groupid` key or the `-groupid` instruction
are specified, the groupId will be derived from the Bundle Symbolic Name by using
everything until the last '.' (bsn prefix) and the artifactId will be everything from the
last '.' to the end (bsn suffix).

The following properties are supported:

|Key              |Default          |Description                         |
|`groupid`        |`-groupid`       |The groupId to use. Will default to bsn prefix if no groupid is set.|
|`artifactid`     |bundle symbolic name|The artifactId to use. Will default to bsn suffix if no groupid is set.|
|`version`        |bundle version   |The version to use.                 |
|`where`          |`META-INF/maven/<groupid>/<artifactid>/pom.xml`|The location of the pom.xml file. Will default to `pom.xml` if no groupid is set.|

The `-pom` instruction can use any macro but the `${@bsn}` and `${@version}` macros
refer to the current JAR being built. 

The `-pom` instruction will also attempt to convert the following headers to their POM counterpart:

* `Bundle-Description`
* `Bundle-DocUrl`
* `Bundle-Vendor`  – If the  value ends with a HTTP or HTTPS url then this URL is used as the organization URL and the name with be the part without the URL. Otherwise the whole value is used as the value for the organization name.
* `Bundle-License`
* `Bundle-Developers` – This is an unofficial header. The key must be the email. It consist of the following parameters:

|Parameters          |Default       |Description|
|`email`             |              |Email address (mandatory)|
|`id`                |email         |A developer id (defaults to email)|
|`name`              |              |Name of the developer|
|`organization`      |              |Name of the organization|
|`organizationUrl`   |              |URL of the organization|
|`roles`             |              |Roles of the developer (comma separated)|
|`timezone`          |              |Three letter time zone|

    Bundle-Developers: \
      Peter.Kriens@aQute.biz; \
        name="Peter Kriens"; \
        organization=aQute; \
        roles="programmer,gopher"
	 
* `Bundle-SCM` – This is an unofficial header. The key must be the It consists of the following parameters:

|Parameters             |Default       |Description|
|`connection`           |              |Read only connection|
|`developerConnection`  |              |Developer connection|
|`url`                  |              |The URL for a web front end to your SCM system.|

    Bundle-SCM: \
        url=https://github.com/bndtools, \
        connection=scm:git:https://github.com/bndtools/bnd, \
        developerConnection=scm:git:git@github.com/bndtools/bnd

### Example

The following example bnd file:

	Bundle-SymbolicName: com.example.foo
	Bundle-Version: 1.2.3.qualifier
	-pom: true

Generates the following pom in `pom.xml`:

    <project xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="" 
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.2.3.qualifier</version>
                <name>com.example.foo</name>
    </project>

You can override the different parts of the Maven coordinates:

	Bundle-SymbolicName:          com.example.foo
	Bundle-Version:               1.2.3.qualifier
	Bundle-Developers:            Peter.Kriens@aQute.biz; \
	  name="Peter Kriens"; \
	  organization=aQute; \
	  roles="programmer,gopher"
	Bundle-SCM: url=https://github.com/bndtools, \
	  connection=scm:git:https://github.com/bndtools/bnd, \
	  developerConnection=scm:git:git@github.com/bndtools/bnd
	-pom: groupid=com.example, \
	  where=META-INF/maven/pom.xml, \
	  version=${versionmask;==;${Bundle-Version}}
	
Generates the following pom in `META-INF/maven/pom.xml`:

    <project xmlns="http://maven.apache.org/POM/4.0.0" 
        xmlns:xsi=""
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>com.example.foo</artifactId>
                <version>1.2</version>
                <name>com.example.foo</name>
                <scm>
                    <url>https://github.com/bndtools</url>
                    <connection>scm:git:https://github.com/bndtools/bnd</connection>
                    <developerConnection>scm:git:git@github.com/bndtools/bnd</developerConnection>
                </scm>
                <developers>
                    <developer>
                        <id>Peter.Kriens@aQute.biz</id>
                        <name>Peter Kriens</name>
                        <organization>aQute</organization>
                        <roles>
                            <role>programmer</role>
                            <role>gopher</role>
                        </roles>
                        <email>Peter.Kriens@aQute.biz</email>
                    </developer>
                </developers>
    </project>

