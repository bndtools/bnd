---
layout: default
---

# Background
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

The bnd program works on a higher level then traditional jarring; this might take some getting used to. However, it is much more elegant to think in packages than that it is to think in files. The fact that bnd understand the semantics of a bundle allows it to detect many errors and allows bundles to be created with almost no special information. 

bnd will not create an output file if none of the resources is newer than an existing output file.

The program is available in several forms: command line, ant task, maven plugin, and an Eclipse plugin.

##Tips
There are some common pitfalls that can be prevented by following the tips:

* Keep it simple. bnd's defaults are pretty good and not specifying is usually the best solution. KISS!
* Think packages ... yes it feels redundant to specify the packages that are in your source directory but your artifact will get a life of its own over time. Many IDEs and build tools restricted us to one artifact per project but bnd allows many artifacts, allowing the choice of granularity to you. As OSGi's packages can be easily refactored you can design the contents of your artifacts depending on the deployment needs. Think packages!
* Private is always better than export, only use export when you absolute need it.
* Not versioning an exported package is at your own peril. Sorry, that is false, it is at the peril of your users.
* Do not use the Bundle-ClassPath, if you need to include whole JARs, see the @ option at Include-Resource
* If you do not understand a header, remove it
* If you have a problem, make an example that is as small as possible and send it to [me][mailto:Peter.Kriens@aQute.biz|me].


