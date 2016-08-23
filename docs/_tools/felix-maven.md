---
title: Apache Felix Maven Bundle Plugin
layout: default
summary: The Apache Felix Maven Bundle Plugin.
version: 2.4
---

One set of developers comes from the Maven world and is generally happy with the “Maven Way”; they do not want it to change significantly. We feel that these users are already well served by the Apache Felix Maven Bundle Plugin, m2eclipse and even other IDEs such as NetBeans.  We call the above a “Maven first” approach. bndlib supports this approach with the Apache Felix Maven Bundle plugin. This plugin is maintained by the Apache Felix project the plugin is [well documented there][1]. This document shamelessly copies some of this information.

The Apache Felix Maven Bundle plugin uses bndlib only to create a manifest; it does not support the bndlib's workspace and project model. This means that not all instructions and macros are relevant. 

The Apache Felix Maven Bundle plugin maps the bndlib instructions to XML elements in the POM in the configuration part of the plugin, bndlib is then called to create the manifest for the JAR. Before the Apache Felix Maven Bundle Plugin calls bndlib, it sets up the class path and provides a number of defaults that differ from the standard bndlib defaults. These changes will be discussed later.

## Simple Example

Rather than going straight to a detailed list of plugin features, we will first look at a simple example of how to use the plugin to give an immediate flavor. A detailed "how to" will follow. Assume that we have a simple bundle project that has a pubic API package an several implementation packages, such as:

	com.acme.prime.speaker.api
	com.acme.prime.speaker.provider
	com.acme.prime.speaker.provider.mac
	com.acme.prime.speaker.provider.unix
	com.acme.prime.speaker.provider.windows

If we also assume that we have a bundle activator in one of the implementation packages, then the `<plugins>` section of the POM file for this bundle project would look like this:

	  <plugin>
	    <groupId>org.apache.felix</groupId>
	    <artifactId>maven-bundle-plugin</artifactId>
	    <extensions>true</extensions>
	    <configuration>
	      <instructions>
	        <Export-Package>com.acme.prime.speaker.api</Export-Package>
	        <Private-Package>com.acme.prime.speaker.provider.*</Private-Package>
	        <_dsannotations>*</_dsannotations>
	      </instructions>
	    </configuration>
	  </plugin>

## Instructions, Macros & Name Mangling

It should be clear that the Apache Felix Maven Bundle plugin uses the XML tag names in the configuration for the headers and instructions. Since the name of most instructions start with a minus sign ('-'), which is not allowed in XML, the first minus sign is replaced with an underscore ('_'). Most instructions and macros can be used except for the  project/workspace instructions since these concepts are available from Maven. Such project instructions and macros are indicated on their reference pages.

As always in bndlib, any element starting with an upper case us copied to the manifest, all other elements are internal to bnd; they are available as macros and might indicate instructions to bnd (which mostly start with a minus sign).

You can use macros but you must be careful to avoid conflicting with the macros used in the POM that also use the `${...}` pattern; it is therefore necessary for bnd to use one of its alternative patterns, for example `$[...]`. The maven POM macros are fortunately already expanded before bndlib sees them. 

Since all elements become part of the bndlib properties you can actually also set macros for later use:

      <instructions>
        <opt>resolution:=optional</opt>
        <Import-Package>org.slf4j;$[opt], *</Import-Package>
      </instructions>

## Embed-Dependency

The Apache Felix Maven Bundle plugin supports a special instruction <Embed-Dependency> that will automatically include maven transitive dependencies in the JAR and place these JARs on the Bundle-ClassPath. This feature is explained in [Embedding Dependencies][2]. 

>  This is a convenient feature that is not so wise to use since it makes the actual bundle depending on many aspects that are not under direct control. A bundle is a component and should reflect an implementation of a public API; this model requires that you think about what goes in there and what does not go in there. Adding transitive dependencies inside this bundle tends to create very complex systems that destroy the benefits of OSGi.

## Defaults

To use this plugin, very little information is required by bndlib. As part of the Maven integration, the plugin tries to set reasonable defaults based on the POM for various instructions.

Default Headers:

 	 Bundle-SymbolicName: <groupId>.<artifactId>
	 Bundle-Name:         project.getName();
	 Bundle-Version:      normalized pom <version>
	 Import-Package:      *
	 Export-Package:      all packages except packages with impl/internal in their name
	 Bundle-Description:  project.getDescription()
	 Bundle-License:      project.getLicenses())
	 Bundle-Vendor:       project.getOrganization().getName();
	 Bundle-DocURL:       project.getOrganization().getUrl()
	 Include-Resource:    src/main/resources

Special Macros:

	${bundle-symbolicname}
	${local-packages}

### Bundle Symbolic Name

The <Bundle-SymbolicName> is computed using the shared Maven2OsgiConverter component, which uses the following algorithm:

* Get the symbolic name as groupId + "." + artifactId, with the following exceptions:
	* If artifact.getFile is not null and the jar contains a OSGi Manifest with Bundle-SymbolicName property then that value is returned
	* If groupId has only one section (no dots) and artifact.getFile is not null then the first package name with classes is returned. eg. commons-logging:commons-logging -> org.apache.commons.logging
	* If artifactId is equal to last section of groupId then groupId is returned. eg. org.apache.maven:maven -> org.apache.maven
	* if artifactId starts with last section of groupId that portion is removed. eg. org.apache.maven:maven-core -> org.apache.maven.core

The computed symbolic name is also stored in the $[maven-symbolicname] property in case you want to add attributes or directives to it.

### Export Package

Export-Package is assumed to be the set of packages in your local Java sources, excluding the default package '.' and any packages containing 'impl' or 'internal'. (before version 2 of the bundleplugin it was based on the symbolic name)

>  From a bndlib perspective, this is the wrong default. An exported package is an expensive thing to have and the philosophy of bndlib is to make these expensive choices explicit, this is the reason the bndlib default is to make nothing exported, especially because good bundles should have no or very few exports.


## Detailed "How To"

To use the maven-bundle-plugin, you first need to add the plugin and some appropriate plugin configuration to your bundle project's POM. Below is an example of a simple OSGi bundle POM for Maven2:

	<project>
	  <modelVersion>4.0.0</modelVersion>
	  <groupId>my-osgi-bundles</groupId>
	  <artifactId>examplebundle</artifactId>
	  <packaging>bundle</packaging>    <!-- (1) -->
	  <version>1.0</version>
	  <name>Example Bundle</name>
	  <dependencies>
	    <dependency>
	      <groupId>org.apache.felix</groupId>
	      <artifactId>org.osgi.core</artifactId>
	      <version>1.0.0</version>
	    </dependency>
	  </dependencies>
	  <build>
	    <plugins>
	      <plugin>    <!-- (2) START -->
	        <groupId>org.apache.felix</groupId>
	        <artifactId>maven-bundle-plugin</artifactId>
	        <extensions>true</extensions>
	        <configuration>
	          <instructions>
	            <Export-Package>com.my.company.api</Export-Package>
	            <Private-Package>com.my.company.*</Private-Package>
	            <Bundle-Activator>com.my.company.Activator</Bundle-Activator>
	          </instructions>
	        </configuration>
	      </plugin>    <!-- (2) END -->
	    </plugins>
	  </build>
	</project>

There are two main things to note: (1) the <packaging> specifier must be `bundle` and (2) the plugin and configuration must be specified (the configuration section is where you will issue instructions to the plugin).

## Real-World Example

Consider this more real-world example using Felix' Log Service implementation. The Log Service project is comprised of a single package: org.apache.felix.log.impl. It has a dependency on the core OSGi interfaces as well as a dependency on the compendium OSGi interfaces for the specific log service interfaces. The following is its POM file:

	<project>
	  <modelVersion>4.0.0</modelVersion>
	  <groupId>org.apache.felix</groupId>
	  <artifactId>org.apache.felix.log</artifactId>
	  <packaging>bundle</packaging>
	  <name>Apache Felix Log Service</name>
	  <version>0.8.0-SNAPSHOT</version>
	  <description>
	    This bundle provides an implementation of the OSGi R4 Log service.
	  </description>
	  <dependencies>
	    <dependency>
	      <groupId>${pom.groupId}</groupId>
	      <artifactId>org.osgi.core</artifactId>
	      <version>0.8.0-incubator</version>
	    </dependency>
	    <dependency>
	      <groupId>${pom.groupId}</groupId>
	      <artifactId>org.osgi.compendium</artifactId>
	      <version>0.9.0-incubator-SNAPSHOT</version>
	    </dependency>
	  </dependencies>
	  <build>
	    <plugins>
	      <plugin>
	        <groupId>org.apache.felix</groupId>
	        <artifactId>maven-bundle-plugin</artifactId>
	        <extensions>true</extensions>
	        <configuration>
	          <instructions>
	            <Export-Package>org.osgi.service.log</Export-Package>
	            <Private-Package>org.apache.felix.log.impl</Private-Package>
	            <Bundle-SymbolicName>${pom.artifactId}</Bundle-SymbolicName>
	            <Bundle-Activator>${pom.artifactId}.impl.Activator</Bundle-Activator>
	            <Export-Service>org.osgi.service.log.LogService,org.osgi.service.log.LogReaderService</Export-Service>
	          </instructions>
	        </configuration>
	      </plugin>
	    </plugins>
	  </build>
	</project>

This will create a manifest of:

	Manifest-Version: 1
	Bundle-License: http://www.apache.org/licenses/LICENSE-2.0.txt
	Bundle-Activator: org.apache.felix.log.impl.Activator
	Import-Package: org.osgi.framework;version=1.3, org.osgi.service.log;v
	 ersion=1.3
	Include-Resource: src/main/resources
	Export-Package: org.osgi.service.log;uses:=org.osgi.framework;version=
	 1.3
	Bundle-Version: 0.8.0.SNAPSHOT
	Bundle-Name: Apache Felix Log Service
	Bundle-Description: This bundle provides an implementation of the OSGi
	  R4 Log service.
	Private-Package: org.apache.felix.log.impl
	Bundle-ManifestVersion: 2
	Export-Service: org.osgi.service.log.LogService,org.osgi.service.log.L
	 ogReaderService
	Bundle-SymbolicName: org.apache.felix.log
	The resulting bundle JAR file has the following content (notice how the LICENSE and NOTICE files were automatically copied from the src/main/resources/ directory of the project):

If you want to keep your project packaging type (for example "jar") but would like to add OSGi metadata
you can use the manifest goal to generate a bundle manifest. The maven-jar-plugin can then be used to
add this manifest to the final artifact. For example:

	<plugin>
	  <artifactId>maven-jar-plugin</artifactId>
	  <configuration>
	    <archive>  
	      <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
	    </archive> 
	  </configuration>
	</plugin>  
	<plugin>   
	  <groupId>org.apache.felix</groupId>
	  <artifactId>maven-bundle-plugin</artifactId>
	  <executions>
	    <execution>
	      <id>bundle-manifest</id>
	      <phase>process-classes</phase>
	      <goals>    
	        <goal>manifest</goal>
	      </goals>   
	    </execution>
	  </executions>
	</plugin>

If you want to use packaging types other than "jar" and "bundle" then you also need to enable support
for them in the bundleplugin configuration, for example if you want to use the plugin with WAR files:

	<plugin>
	  <groupId>org.apache.felix</groupId>
	  <artifactId>maven-bundle-plugin</artifactId>
	  <executions>
	    <execution>
	      <id>bundle-manifest</id>
	      <phase>process-classes</phase>
	      <goals>
	        <goal>manifest</goal>
	      </goals>
	    </execution>
	  </executions>
	  <configuration>
	    <supportedProjectTypes>
	      <supportedProjectType>jar</supportedProjectType>
	      <supportedProjectType>bundle</supportedProjectType>
	      <supportedProjectType>war</supportedProjectType>
	    </supportedProjectTypes>
	    <instructions>
	      <!-- ...etc... -->
	    </instructions>
	  </configuration>
	</plugin>

You'll also need to configure the other plugin to pick up and use the generated manifest, which is written to ${project.build.outputDirectory}/META-INF/MANIFEST.MF by default (unless you choose a different manifestLocation in the maven-bundle-plugin configuration). Continuing with our WAR example:

	<plugin>
	  <groupId>org.apache.maven.plugins</groupId>
	  <artifactId>maven-war-plugin</artifactId>
	  <configuration>
	    <archive>
	      <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
	    </archive>
	  </configuration>
	</plugin>

## Using the Workspace/Project facilities

Though the Apache Felix Maven Bundle plugin is used in a 'maven first' model, it can actually also be used with some of the benefits of using bndlib more direct so that for example bndtools can be used. In this model, the POM includes a bnd file that is also used by bnd(tools). This is far from perfect since it will be necessary to maintain the classpath in two different places: through the transitive dependencies in the POM and in bnd. It will also not provide full fidelity between the maven build and the IDE build since other plugins in maven are ignored. However, it will allow the use of bndtools as the primary IDE, which allows you to develop bundles faster than any other environment.

If you want to use this model, be aware that bnd has a strict disk layout for the workspace. A workspace is a directory with a `cnf` directory and project directory. It is not possible to create arbitrary layouts. For maven, the disk layout should be:

	  ./com.acme.prime/                workspace
	    cnf/                           configuration
	      ext/                         extensions
	        maven.bnd                  contains the maven plugin setup
	      build.bnd                    your shared settings
	    pom.xml                        maven modules POM
	    com.acme.prime.speaker.api/    project directory
	      pom.xml                      your project POM
	      src/                         maven source directory
	        main/
	          java/
	        test/
	          java/
	      target/
	        classes/                   output directory
	        test-classes/              test output

You can include files with the -include instruction:

	  <instructions>
	    <_include>bnd.bnd</_include>
	  </instructions>

Now you can add the instruction in the `bnd.bnd` file:

	Export-Package: com.acme.prime.speaker.api
	Private-Package: com.acme.prime.speaker.provider.*
	-dsannotations: true

The easiest way to setup such a workspace is is by adding the bnd maven plugin:

	$ bnd add workspace com.acme.prime
	$ cd com.acme.prime
	$ bnd add plugin maven
	
The bnd maven plugin will automatically maintain a pom.xml in the workspace containing the workspace projects. 
 
	$ bnd add project com.acme.speaker.api
	$ more pom.xml
	<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	    <modelVersion>4.0.0</modelVersion>
	    
	    <groupId>/Users/aqute/tmp/com.acme.prime</groupId>
	    <artifactId>root</artifactId>
	    <packaging>pom</packaging>
	    
	    <modules>
	    <!-- DO NOT EDIT MANAGED BY BND MAVEN LIFECYCLE PLUGIN -->
	        <module>com.acme.prime.speaker.api</module>
	    </modules>
	</project>

You can edit this file to add more specific information, the bnd maven plugin will only change the marked part.

The maven bnd plugin also changes the file layout to match the default maven file layout. Take a look at `cnf/ext/maven.bnd` for the details.

## Links

* [Secrets of the Apache Felix Bundle Plugin Revealed][2] – Interesting blog about using the bndlib macros inside the plugin to handle semantic versioning

[1]: http://felix.apache.org/site/apache-felix-maven-bundle-plugin-bnd.html
[2]: http://davidvaleri.wordpress.com/2011/04/07/secrets-of-the-felix-bundle-plug-in-macros-revealed/
[3]: http://felix.apache.org/site/apache-felix-maven-bundle-plugin-bnd.html#ApacheFelixMavenBundlePlugin(BND)
