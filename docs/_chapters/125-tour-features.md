---
title: Guided Tour Workspace & Projects
layout: default
---


Since bnd is by design headless, the best way to get start is to use one of the IDEs like [bndtools][1]. They have tutorials and an IDE is a more pleasant place than a command line. So if you just want to learn OSGi, please go away, this chapter is not for you! bndlib relates to OSGi like the ASM byte code manipulation library relates to Java. Sometimes incredibly useful but in general something you do not want to touch, and obviously not the way to learn Java.

Assuming we're now only left with the blue collar workers of our industry: people that need to maintain builds or that must do JAR engineering. First a word of warning, the fact that bndlib provides a function is in no way an advertisement to use that function. bndlib grew up together with the OSGi specifications and has been used to build the Reference Implementations (RI) and Test Compatibility Kits (TCK). Though this has a lot of benefits for you, the disadvantage is that it also has to support all the bad parts of the specifications, and even sometimes must be able to create erroneous situations so we could create test cases. And we also had to handle the situations caused by the mess of non-modular bundles out there.  

So to make it crystal clear: the fact that a function is in bndlib does not mean it is intended to be used. This section contains a whole bunch of things you wish you never had to touch, and if you do OSGi properly, you will only see a tiny fraction of bndlib. That said, when the unprepared JARs hit the OSGi framework, it is nice to have bndlib as backup. 

This tour uses the bnd command line bnd to demonstrate much of the inner details of bndlib. 

bnd became the Swiss army knife to manipulate JARs and gained a lot of function you _should never attempt to use_. However, this chapter will give you an overview of what is in bnd.

bnd has lots of commands to try out aspects of bndlib. The [install section][6] shows how to install bnd.

## Wrapping

A common use of bnd is to wrap a JAR file; there are still too many files out there that have no OSGi metadata.This lack of metadata makes them kind of useless as a bundle since it will be fully isolated from the other bundles. In OSGi, you must explicitly indicate what packages should be imported and what packages should be exported, by default everything is private. Though this can be quite frustrating sometimes, a rather strong argument can be made for the long term benefits of this strategy.

The OSGi metadata is stored in the _manifest_ of the JAR file. The manifest is a like a properties file stored in `META-INF/MANIFEST.MF` in the JAR file. The manifest format is [specified by Sun/Oracle][4]. Though the manifest is a text file and human readable it has a number of quirks that makes it hard to edit for humans. So even though it is text, the intention of the manifest was never to be done by hand, bnd like tools were always the intention. Tools can provide many useful defaults, handle expansions, and verify consistency,

So let's use bnd to wrap the [javax.activation.jar][2] file even though it is already wrapped for us in the [Enterprise Bundle Repository][3]. Let's download it:

	$ mkdir jar
	$ bnd copy https://repo.maven.apache.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar \
		jar/javax.activation-1.1.1.jar
	
bnd provides a convenience function to print out the contents of a JAR. The --uses option shows the packages and what other packages they use. 

	$ bnd print --uses jar/javax.activation-1.1.1.jar 
	[USES]
	com.sun.activation.registries            []
	com.sun.activation.viewers               [javax.activation]
	javax.activation                         [com.sun.activation.registries]

## Simplistic

In the simplistic case our goal is to provide a manifest that makes all the packages in the source JAR available to the other bundles (exports) and that imports anything the classes in the JAR require from the external world (imports). bnd is controlled by a _bnd_ file. This file is a standard Java properties file. In this properties file we can provide headers, instructions, and macros to bnd. In general, a header starts with an upper case, and an instruction with a minus sign ('-'). The format of the headers and instructions is defined by the OSGi specifications. The headers specifically try to follow the structure of an OSGi header. Since exports are defined with the `Export-Package` header and imports are defined by the `Import-Package` header we can create a bnd file that looks very much like the desired manifest, except for the instruction that defines our class path. We could also use the URL directly on the classpath but this would download the JAR everytime we ran bnd. So `javax.activation.bnd` should look like:

	-classpath:		jar/javax.activation-1.1.1.jar
	Export-Package: *

## Properties Format
The casing of the headers is important, even though OSGi headers are case insensitive. That is Export-Package is not the same for bnd as Export-package.

The properties format we use in bnd is darned flexibile. A header is recognized when you start the line with a header name (a word without spaces) and then:

* Any white space (space, tab)
* A  colon (':'), white space around the colon is ignored
* An equal sign ('='), white space around the equal sign is ignored
 
Though lines can be as long as you want, it is better for your brains to keep them short. You can break a line with a backslash ('\\') followed by a newline ('\n'). Be careful, the newline *must* follow the backslash or you will get whatever comes after the backslash in the header. Some examples that all set the variable `foo` to 3:

	foo				= 	3
	foo:3
	foo 3
	foo \
	3

Since bnd files are property files, you cannot repeat a property. Later properties will completely overwrite earlier ones and there is no order between properties.

## Running bnd

We can now wrap the source JAR with the following command:

	$ bnd javax.activation.bnd

bnd has many commands, you can find out more about them with the`bnd help [sub]` command. In this case we rely on bnd detecting a bnd file and doing the right thing. bnd is also prepared to show the manifest of any bundle, so let's take a look at the bundle we just generated:

	$ bnd javax.activation.jar
	[MANIFEST javax.activation]
	Bnd-LastModified                         1407229057278                           
	Bundle-ManifestVersion                   2                                       
	Bundle-Name                              javax.activation                   
	Bundle-SymbolicName                      javax.activation                   
	Bundle-Version                           0                                       
	Created-By                               1.8.0 (Oracle Corporation)              
	Export-Package                           com.sun.activation.registries;version="0.0.0",
	                                         com.sun.activation.viewers;uses:="javax.activation";version="0.0.0",
	                                         javax.activation;version="0.0.0"
	Manifest-Version                         1.0                                     
	Require-Capability                       osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.4))"
	Tool                                     Bnd-2.4.0.201408050759                  

## Output

When we create the JAR, we can also specify the output file. By default, bnd will store JAR under the same name as the bnd file, but then with a `.jar` extension. You can override the default from the command line:

	$ mkdir bundle
	$ bnd do --output bundle javax.activation.bnd
	$ ls bundle
	javax.activation.jar

You can either specify a directory or a file name. However, you can also specify the output in the bnd file itself:

	-output: 		bundle

## The Manifest

It is clear that bnd has been busy. It detected 3 packages (com.sun.activation.registries, com.sun.activation.viewers, and javax.activation) and since we exported all, they appear in the manifest as exports. Observant readers will have noticed that we used _wildcards_ ('*'). The OSGi specifications are not so generous, every package must be explicitly listed so that the manifest contains all the information required to process a bundle. This, again, has many long term macro advantages. It's however bnd's goal in life to provide the convenience in this model and that is the reason that bnd supports wildcards in virtual any situation where they are usable. A wildcard requires a _domain_ to be effective, you can only select something out of a set, its domain. In the case of `Export-Package`, its domain is the _class path_. This class path is set by the `-classpath` instruction. In this case the activation jar.


## Export Package

Currently our package is exported at version 0.0.0. This is not a very useful version, we therefore need to version the exported packages. Why? Well, this is the contract of modularity. To get the benefits of privacy (the privilege to change whenever we feel the need) comes the cost of compatibility for the parts we share. If you export a package, you intend it to be used by others. These others are an uncontrollable bunch that will use your _revision_ (the actual artifact with a given digest, i.e. the instance of the _program_) but will expect updates in the future with new functionality and bug fixes. 

Since a software _program_ is an ephemeral product that changes over time these others do not become dependent on the revision, nope, they become dependent on the _program_. This creates two problems. 

* How do they make sure they do not get linked in runtime to a revision that is older than they they compiled against?
* How do they know if a revision is backward compatible?

The answer to these two questions is given to us by [semantic versioning][5], a prescriptive syntax for versions. Though semantic versions work for bundles, they really shine for packages. The basic idea is that when you compile against a version, this version is recorded in the OSGi metadata as an _range_ on the imported package. The floor of this range is the version in the revision use to compile against, the ceiling indicates the first version that would not be compatible anymore. For example, `[1.2,2)` indicates that any version that is larger or equal than 1.2 and less than 2 is compatible. Therefore, 1.5 would be compatible. Since OSGi supports version ranges on the Import-Package header it will be able to control what revisions get linked and how.

Such a version scheme of course only works if you agree how to use the version syntax, this should explain the 'semantic' in semantic versioning. Therefore, the bnd/OSGi versions have a strictly defined syntax:

* major – For breaking change
* minor – For changes that break the provider of the package but are backward compatible for the consumer of the package. See [semantic versions][5] for more detail.
* micro – Backward compatible changes, e.g. small bug fixes or documentation changes
* qualifier – Identifying the build 

Long story, but it is at the heart of what we're trying to achieve: evolving large systems in a anti-fragile way.

Back to the exported packages. We can add the version to the export-package by _decorating_ the Export-Package header: 
  
	-classpath:		jar/javax.activation-1.1.1.jar
	Export-Package: *;version=1.1.1

Let's take another look:
	
	$ bnd javax.activation.bnd
	$ bnd javax.activation.jar
	[MANIFEST javax.activation]
	...
	Export-Package                           com.sun.activation.registries;version="1.1.1",
	                                         com.sun.activation.viewers;uses:="javax.activation";version="1.1.1",
	                                         javax.activation;version="1.1.1"
	...

Looking at the manifest we can see the _uses_ directive; this directive is the primary reason you should use bnd. To prevent wiring the wrong bundles together, the OSGi Framework must know which packages use what other packages in their API, this list is reflected in the uses constraints. It allows the OSGi Framework to handle multiple versions of the same package in one VM and not create Class Cast Exceptions ... Very powerful but it is impossible to maintain this list by hand and it would be prohibitively expensive in runtime.

## Private Package

An exported package is a _commitment_ to any future users out there. This commitment will restrict your freedom to make changes since users can get pretty nasty if you make something that will not work with their existing software. In contrast, a package that is not exported is private; since no external user can have knowledge about this package we have complete freedom to do whatever we want in a future revision without getting hollering users on the phone.

In our example javax.activation JAR we should therefore not export the `com.sun.*` packages since they are actually private. The Private-Package header has the same syntax as the Export-Package header but will not export the packages, it will keep them hidden in the bundle. Unlike the Export-Package header, it is not necessary to decorate the packages with a version since nobody can depend on them.

	-classpath:		jar/javax.activation-1.1.1.jar
	Export-Package: javax.activation;version=1.1.1
	Private-Package: com.sun.activation.*

	$ bnd javax.activation.bnd
	$ bnd javax.activation.jar
	[MANIFEST javax.activation]
	...
	Export-Package          javax.activation;version="1.1.1"
	Private-Package         com.sun.activation.registries,
	                        com.sun.activation.viewers
	...

The Private-Package has no meaning to OSGi, if you want to not have this header in your manifest then you can also use `-private-package` instead.

## Bundle Version

The `Bundle-Version` header obviously provides the version. Default it will use version 0, so we should override it. We could just add the 1.1.1 version from our source JAR, however, this would make it impossible to to distinguish the different variations we might make over time. A good tool is to use the ${tstamp} macro for the _qualifier_. The qualifier is the last part of the version. A macro provides a textual replacement, bnd has hundreds of macros (including access to Javascript) and you can define your own. The ${tstamp} macro provides a time stamp.

	-classpath:		jar/javax.activation-1.1.1.jar
	Export-Package: javax.activation;version=1.1.1
	Private-Package: com.sun.activation.*
	Bundle-Version:	1.1.1.${tstamp}

## DNRY or the Benefit of Macros

The 'Do Not Repeat Yourself' mantra encodes one of the more important lessons of software engineering. You should always strive to define a 'thing' or a piece of knowledge in only one place. In our little example, we actually repeat ourselves with the version 1.1.1:

* The classpath
* The export package
* The version
* The output

bnd has a macro processor on board that is a life saver if you are addicted to DNRY (we are). This macro processor has access to all properties in the bnd file, including headers and instructions. Therefore `${Bundle-Version}` refers to whatever value the Bundle Version is set to. However, in this case the Bundle Version also contains the time stamp in the qualifier, which we do not need in the other places. That is, we do not want the output file name to contain the qualifier. 

bnd also contains a large number of built-in macros that provide common utilities. The `${versionmask;mask;version}` is for example such a utility for picking out the parts of a version, bumping it, as well as normalizing it. Normalizing (making sure all parts are present, leading zeros removed, etc.) is crucial to keep things workable.

We can try out the macro from the command line, the bnd command has a `macro` sub command:

	$ bnd macro "version;===;1.2.3.q" => 1.2.3
	$ bnd macro "version;+00;1.2.3.q" => 2.0.0
	$ bnd macro "version;+00;0"       => 1.0.0
	$ bnd macro "version;=;1.2.3.q"   => 1

To reuse the Bundle Version in the class path, the output, and the export package:  

	-classpath:		jar/javax.activation-${versionmask;===;${Bundle-Version}}.jar

	Export-Package: javax.activation;version=${versionmask;===;${Bundle-Version}}
	Private-Package: com.sun.activation.*
	Bundle-Version:	1.1.1.${tstamp}
   
This of course looks awkward and hardly DNRY. A better solution is to create an intermediate variable. Variables are always lower case (if they started with an upper case, they would end up in the manifest). Variables can be referred to with the macro syntax of `${}`.

	v:               1.1.1

	-classpath:		 jar/javax.activation-${v}.jar
	-output:		 bundle
	
	Bundle-Version:	 ${v}.${tstamp}
	Export-Package:  javax.activation;version=${v}
	Private-Package: com.sun.activation.*

## Description

Maven Central is quickly moving towards a million revisions organized in 200.000 programs. It should be clear that we need to organize this huge pile of software. One way is to make sure you document your programs appropriately. It is highly recommended to have a short one paragraph description in each bundle. Adding a description to our bundle is easy:
 
	v:               1.1.1

	-classpath:		 jar/javax.activation-${v}.jar
	-output:		 bundle
	
	Bundle-Description: \
		A wrapped version of javax.activation ${v} JAR from Oracle. \
		This bundle is downloaded from maven and wrapped by bnd.
	Bundle-Version:	 ${v}.${tstamp}
	Export-Package:  javax.activation;version=${v}
	Private-Package: com.sun.activation.*

For longer descriptions you can continue on the next line with a backslash ('\') followed by a newline. Unlike the manifest, it does not has to start with a space though indenting the text for the next lines is usually a good practice. You cannot use newlines or other markup in the Bundle-Description.

That said, a good Bundle-Description is a single paragraph with a few short sentences. 

## Include Resources

The Bundle-Description is an excellent way to provide a short description, but what about a longer description with examples of usage, caveats, configuration requirements, etc? It would be nice to include a `readme` file in the bundle that indexing sites could use. You can achieve this effect by including a `readme.md` file in the root of your bundle.

In bnd it is possible to include any resource from anywhere in the file system (actually, any URL as well).

	-includeresource: readme.md 

The resource is of course not always in the proper place, it is therefore possible to make the output name different from the file path:

	-includeresource: readme.md=doc/readme.md

In this case, the readme could benefit from the bnd macro support. For example, it could then contain the actual version of the artifact or use any of the other macros available. Preprocessing can be indicated by enclosing the clause with curly braces ('{' and '}').  

	-includeresource: {readme.md=doc/readme.md}
	
Another option is to encode the text for the readme in the bnd file using the literal option (though this is not such a good idea for any decent readme file since it is hard to use newlines):

	-includeresource: readme.md;literal=${unescape;#JAF\nThis is the Java Activation Framework}

The `-includeresource` instruction is quite powerful, there are many more options to recurse directories, filter, etc. See [-includeresource](../instructions/includeresource.html).

## Import Package

So far, we've ignored the imported packages because the javax.activation JAR only depends on java.*; java.* packages are not imported, the OSGi Framework will always provide access to them. Let's add another JAR, the javax.mail jar that uses javax.activation. This JAR is already a bundle, which is really good. Except for this exercise, so we copy and strip the OSGi metadata 

	$ bnd copy --strip https://repo.maven.apache.org/maven2/com/sun/mail/javax.mail/1.5.2/javax.mail-1.5.2.jar \
	   > jar/javax.mail-1.5.2.jar
	$ bnd print --uses jar/javax.mail-1.5.2.jar 
	[USES]
	com.sun.mail.auth          [javax.crypto, ... ]
	com.sun.mail.handlers      [javax.activation, ... ]
	com.sun.mail.iap           [com.sun.mail.util, ... ]
	com.sun.mail.imap          [com.sun.mail.imap.protocol, ...]
	com.sun.mail.imap.protocol [com.sun.mail.iap, ... ]
	com.sun.mail.pop3          [javax.mail, ... ]
	com.sun.mail.smtp          [com.sun.mail.util, ... ]
	com.sun.mail.util          [javax.mail,  ... ]
	com.sun.mail.util.logging  [javax.mail, ...]
	javax.mail                 [javax.mail.event, ... ]
	javax.mail.event           [javax.mail]
	javax.mail.internet        [javax.mail, ... ]
	javax.mail.search          [javax.mail.internet, javax.mail]
	javax.mail.util            [javax.mail.internet, javax.activation]

The javax.mail bundle leverages the Java Activation Framework (JAF) embedded in our previous javax.activation bundle. One of the most useful features in bnd is that it can analyze the class files to find the package dependencies and turn them into an Import-Package header. If bnd finds the exported packages with a version on the class path then it will automatically use the exported versions to calculate the import version range. That is, if the package is exported as 1.1.1, then bnd will import it in general as `[1.1,2)`.

So lets wrap the javax.mail package, add to `javax.mail.bnd`:

	v:          1.5.2
	
	-classpath: \
		jar/javax.activation-1.1.1.jar, \
		jar/javax.mail-${v}.jar
	
	Bundle-Version: ${v}
	Bundle-Description: \
	  An OSGi wrapped version of the javax.mail library downloaded from maven.
	Export-Package: javax.mail.*
	Private-Package: com.sun.mail.*

bnd contains a special `-i/--impexp` option to print the imports and exports of a bundle. So lets make the bundle and see:
 
	$ bnd javax.mail.bnd
	$ bnd print -i javax.mail.jar
	[IMPEXP]
	Import-Package
	  javax.activation                       
	  javax.crypto                           
	  javax.crypto.spec                      
	  javax.mail.event                       {version=[1.5,2)}
	  javax.mail.search                      {version=[1.5,2)}
	  javax.mail.util                        {version=[1.5,2)}
	  javax.net                              
	  javax.net.ssl                          
	  javax.security.auth.callback           
	  javax.security.auth.x500               
	  javax.security.sasl                    
	  javax.xml.transform                    
	  javax.xml.transform.stream             
	Export-Package
	  javax.mail                             {version=1.5}
	  javax.mail.event                       {version=1.5, imported-as=[1.5,2)}
	  javax.mail.internet                    {version=1.5}
	  javax.mail.search                      {version=1.5, imported-as=[1.5,2)}
	  javax.mail.util                        {version=1.5, imported-as=[1.5,2)}

First, we notice that there are quite a few imports that have no import range. This is not good but unfortunately Java's VM package versions are more or less absent and are clearly not semantically versioned. A correctly setup system ensures that the correct execution environment is used.

However, we also have in this list javax.activation, for this package we do have an export, the problem is that we included the original JAR on the class path and not the bundle we generated.
 
	v:          1.5.2
	
	-classpath: \
		bundle/javax.activation.jar, \
		jar/javax.mail-${v}.jar
	
	Bundle-Version: ${v}
	Bundle-Description: \
	  An OSGi wrapped version of the javax.mail library downloaded from maven.
	Export-Package: javax.mail.*
	Private-Package: com.sun.mail.*

And now build + print:

	$ bnd javax.mail.bnd; bnd print -i javax.mail.jar
	[IMPEXP]
	Import-Package
	  javax.activation                       {version=[1.1,2)}
	  javax.crypto                           
	  ...

Yes! Now the javax.activation package is imported with a range of `[1.1,2)`. You now may wonder why not `[1.1.1,2)`? Well, the reason is that changes in the micro part of the version should not make a difference in API. If you include the micro part in the import range then it turns out that the overall system becomes very volatile, small changes become large quickly. Ignoring the micro part in the import range is like a bit of oil in the engine ... However, if you feel uncomfortable with this lubricant then it is possible to override this.

## Imports

We got the Import-Package header in the manifest but we never specified it. The reason is that bnd has a default Import-Package set to '*'.  There is a very good reason for this, it works almost exactly as it should work. Any time you feel the desire to muck around with the imports you should realize that you're not doing the right thing. That said, it is the unfortunate truth that sometimes doing the least worst thing is the best option ...

Assume we want to provide a version range on some of the imported packages because these packages could also be provided by bundles, they do not have to come from the JVM. For this, we can add the following header that will decorate the javax.net.ssl package:

	Import-Package: javax.net.*;version=1.1, *

You can decorate any package, including packages specified with a wildcard. The domain of the Import-Package is all packaged that are referred to by any class inside the bundle. If there are for example imports 

## Remove Headers

The `removeheaders` header uses a _selector_ expression. A selector expression consists of a number of comma separated clauses. A clause contains a name (which can be wildcarded) and some optional instructions. A selector is always applied to a _domain_, in the case of `-removeheaders` the domain is all the headers in the manifest. The clauses in the selector are applied one by one in their declaration order to the domain. If a clause matches a member of the domain then the member is removed from the set, and the next member is again matched against the first clause. A suffix of ':i' indicates a case insensitive match.

This ordering is important because selector clauses can use negation. When a clause starts with an exclamation mark ('!') then a matching member is further ignored. Therefore, the following selector will never match the Bundle-Name, the first clause already threw any header that starts with `Bundle-` out:

	-removeheaders: !Bundle-*, Bundle-Name:i

The `Require-Capability` header ensures that the runtime has at least a Java 1.4 VM by requiring an OSGi execution environment. bnd calculates this requirement based on the compiler version used to compile the classes in the JAR. We can override this requirement or disable it with the `-noee=true` instruction.

## Includes

We now have 2 files: javax.activation.bnd and javax.mail.bnd. If we would like to add some more descriptive headers like Bundle-Vendor, Bundle-DocURL, and Bundle-Copyright then we would have to add them to both files, another case of DNRY. Since this is quite common, we can use includes.

	-include: common.bnd

In common.bnd we can then place:

	Bundle-Vendor: Oracle (wrapped by bnd)
	Bundle-Copyright: (c) Oracle, All Rights Reserved

These headers are then automatically placed in the manifests of our target bundles.

A common mistake with the `-include` instruction is that it is repeated several times. Since bnd files are property files, only one of them survives. However, the `-include` instruction accepts multiple files/URLs to both property files or manifest files. You can also indicate that it is ok to not have a certain file by prefixing it with a minus sign ('-').

	-include common.bnd, -other.bnd

Always use Unix like paths, using the forward slash ('/') as file separator, also on Windows. It is highly recommended to always use relative paths since any absolute references makes your builds unportable.

## Sub bnd Files

In our progression towards wrapping javax.activation and javax.mail we have created an unmanaged ordering; we can only build javax.mail once the javax.activation is created. For this reason, bnd can also create a control file that steers _sub_ bnd files. The control file can also contain common properties, though these can be overridden by the sub files. Place the following content in javax.bnd:

	Bundle-Vendor:       Oracle (wrapped by bnd)
	Bundle-Copyright:    (c) Oracle, All Rights Reserved
	-output:             bundle
	-sub: \
		javax.activation.bnd, \
		javax.mail.bnd

We can now build the jars with a simple command:

	$ rm bundle/*
	$ bnd javax.bnd
	$ ls bundle
	javax.activation.jar    javax.mail.jar

## Merging

The basic model of bnd is to collect packages from the classpath and assemble them in a bundle. This pull model is quite different from the more common push model in builds where it is harder to include packages from other projects. However, bnd's model makes it quite easy to create a bundle out of multiple JARs. So lets add a new bnd file that merges javax activation and mail.

Actually, javax.activation and javax.mail are really bad citizens in an OSGi world. They use class loading hacks all over the place that make a mockery out of modularity. Part of the problem is that they really require certain private directories to be available from the client's class loader. In those cases it is sometimes necessary to make sure the layout of the bundles is exactly the same as the source bundles. 

The best way to achieve this is to _unroll_ the source bundles in the target bundle. You can unroll a JAR by prefixing it with a commercial at sign ('@') in an include resource operation. Lets get started on a javax.mail.all.bnd file:

	-includeresource:   \
		@jar/javax.activation.jar, \
		@jar/javax.mail.jar     

This instruction combines the two JARs into one.


	



## Alternative Wrap


## Errors & Warnings

## Failing

## Settings

## System Commands

## Upto








bnd is the Swiss army knife of OSGi, it is used for creating and working with OSGi bundles. Its primary goal is take the pain out of developing bundles. With OSGi you are forced to provide additional metadata in the JAR's manifest to verify the consistency of your "class path". This metadata must be closely aligned with the class files in the bundle and the policies that a company has about versioning. Maintaining this metdata is an error prone chore because many aspects are redundant.

bnd's raison d'etre is therefore to remove the chores and use the redundancy to create the manifest from the class files instead of maintaining it by hand. The core task is therefore to analyze the class files and find any dependencies. These dependencies are then merged with ''instructions'' supplied by the user. For example, adding a version to all imported packages from a specific library can be specified as:

    Import-Package: com.library.*; version = 1.21

The OSGi manifest must explicitly mention a package, bnd allows the use of wildcards. bnd contains many more such conveniences. bnd roots are about 10 years old and bnd has therefore a large number of functions that remove such chores. These range from simplifying the use of OSGi Declarative Services, working with Spring and Blueprint, WAR and WAB files, version analysis, project dependencies, and much more.

Over time bnd started to appear in many different incarnations. It is an an ant task, a command line utility, and a bundle for Eclipse. Other projects have used bndlib to create a maven plugin, bndtools and Sigil both Eclipse IDEs, and others. By keeping the core library small and uncoupled (bnd has no external connections except Java 5), it is easy to embed the functionality in other projects.

## Workflow
Traditionally, JAR files were made with the JDK jar tool, the jar ant task, or the Maven packager. All these tools share the same concept. The developer creates a directory image of the jar by copying files to a directory; this directory is then jarred. This model can be called the ''push'' model. Obviously this method works well.

bnd works differently, it uses the ''pull'' model. Instructions in the bnd file describe the contents of the desired JAR file without writing this structure to disk. The contents from the output can come from the class path or from anywhere in the file system. For example, the following instruction includes the designated packages in the JAR:

  Private-Package: com.example.*
 
bnd can create a JAR from packages the sources, directories or other JAR files. You never have to copy files around, the instructions that Bnd receives are sufficient to retrieve the files from their original location, preprocessing or filtering when required.

The Jar is constructed from 3 different arguments:

    Export-Package
    Private-Package
    Include-Resource

Private-Package and Export-Package contain ''instructions''. Instructions are patterns + attributes and directives, looking like normal OSGi attributes and directives. For example:

    Export-Package: com.acme.*;version=1.2

Each instruction is applied to each package on the classpath in the definition order. That is, if an earlier instruction matches, the later instruction never gets a chance to do its work. If an instruction matches its attributes and properties are applied to the packages. The difference between the Private-Package argument and the Export-Package arguments is that the export version selects the packages for export. If the packages overlap between the two, the export wins.

An instruction can also be negative when it starts with a '!'. In that case the package is excluded from the selection. For example:

    Export-Package: !com.acme.impl, com.acme.*;version=1.2

Note that the instructions are applied in order. If the ! instruction was at the end in the previous example, it would not have done its work because the com.acme.* would already have matched.

The Include-Resource argument can be used to copy resources from the file system in the JAR. This is useful for licenses, images, etc. The instructions in the argument can be a directory, a file, or an inline JAR. The default JAR path is the the root for a directory or the filename for a file. The path can be overridden. Instructions that are enclosed in curly braces, like {license.txt}, are pre-processed, expanding any macros in the file.

Once the JAR is created, the bnd program analyzes the classes and creates an import list with all the packages that are not contained in the jar but which are referred to. This import list is matched against the Import-Package instructions. Normally, the Import-Package argument is *; all referred packages will be imported. However, sometimes it is necessary to ignore an import or provide attributes on the import statement. For example, make the import optional or discard the import:

    Import-Package: !com.acme.*, *;resolution:=optional

The arguments to bnd are normal given as a set of properties. Properties that begin with an upper case are copied to the manifest (possibly after processing). Lower case properties are used for macro variables but are not set as headers in the manifest.

After the JAR is created, the bnd program will verify the result. This will check the resulting manifest in painstaking detail.

The bnd program works on a higher level than the traditional jarring; this might take some getting used to. However, it is much more elegant to think in packages than that it is to think in files. The fact that the bnd understands the semantics of a bundle, allows it to detect many errors and also allows bundles to be created with almost no special information. 

bnd will not create an output file if none of the resources is newer than an existing output file.

The program is available in several forms: command line, ant task, maven plugin, and an Eclipse plugin.

## Tips
There are some common pitfalls that can be prevented by following the tips:

* Keep it simple. bnd's defaults are pretty good and not specifying is usually the best solution. KISS!
* Think packages ... yes it feels redundant to specify the packages that are in your source directory but your artifact will get a life of its own over time. Many IDEs and build tools restricted us to one artifact per project but bnd allows many artifacts, allowing the choice of granularity to you. As OSGi's packages can be easily refactored you can design the contents of your artifacts depending on the deployment needs. Think packages!
* Private is always better than export, only use export when you absolute need it.
* Not versioning an exported package is at your own peril. Sorry, that is false, it is at the peril of your users.
* Do not use the Bundle-ClassPath, if you need to include whole JARs, see the @ option at Include-Resource
* If you do not understand a header, remove it
* If you have a problem, make an example that is as small as possible and send it to [me](mailto:Peter.Kriens@aQute.biz).


[1]: http://www.bndtools.org
[2]: https://repo.maven.apache.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar
[3]: http://ebr.springsource.com/repository/app/
[4]: http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#JARManifest
[5]: 170-versioning.html
[6]: 120-install.html
