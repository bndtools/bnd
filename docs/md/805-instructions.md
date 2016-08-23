___
layout: default
___
# Instructions

The bnd format is very similar to the manifest. Though it is read with the Properties class, you can actually use the ':' as separator to make it look more like a manifest file. The only thing you should be aware of is that the line continuation method of the Manifest (a space as the first character on the line) is not supported. Line continuations are indicated with the backslash ('\' \u005C) as the last character of the line. Lines may have any length. 

The most common mistake is missing the escape. The following does not what people expect it to do:

    Header: abc=def,
      gih=jkl

This is actually defining 2 headers. You can fold lines by escaping the newline:

    Header: abc=def, \\
      gih=jkl

You can add comments with a # on the first character of the line:

    # This is a comment

White spaces around the key and value are trimmed.

See [  Properties][http://java.sun.com/j2se/1.4.2/docs/api/java/util/Properties.html ] for more information about the format.


## Types of Instructions
There are different instructions in the properties file:

||
||!Type ||!Example ||!Description ||
||Manifest headers ||Bundle-Description: ... ||When the first character is a upper case character. These headers are copied to the manifest or augmented by bnd. ||
||Variables ||version=3.0 ||Variables are lower case headers. Headers can contain references to other headers using macro expansion. Variables are not copied to the manifest. See [Macros][#macros] ||
||Directives ||-include: deflts.bnd ||Directives start with a '-' sign. A directive is an instruction to bnd to do something special. See [Directives][#directives] ||

[[#directives]]
##  Bnd Directives

||
||!Directive ||!Format ||!Description ||
||`-classpath`||[ LIST][#list ] ||Add the listed files to the current class path. The files must be addressed relative to the properties file. The files must be either a JAR file or a directory. For example:\\
`-classpath= acme.jar, junit.jar, bin` ||
||`-debug`||`true|false`||Generate debugging information. This will save embedded jar files in the target directory when they are generated on the fly.||
||`-donotcopy`||[ REGEX][#regex ] ||During copying of files from the classpath, file system, or other places, this filter is used to prevent copies. For example, normally `CVS` and `.svn` directories should not be copied. The default is therefore `(CVS|.svn)`. Example:  \\
`-donotcopy= (CVS|.svn|.+.bak|~.+)` ||
||`-exportcontents`||[ PATTERN][#LIST|LIST]] of [[#PATTERN ]||The content of this header augments the Export-Package header, but only for the manifest calculation. That is, Export-Package is used to calculate the contents of the JAR, but then this instruction can be used change or add instructions for the manifest generation.||
||-failok ||true | false ||In certain cases, errors should not abort the creation of the bundle. For example test cases often require the creation of an invalid JAR. If this flag is set to true, errors will create a target bundle (when possible) and errors are only listed. When failok is false, the default, any error will not create a target bundle and will delete the bundle file. Example:  \\
`-failok= true` ||
||-include ||[ LIST][#list ] ||This property will include the list of files in the given order. The files are relative from the bnd file itself. If this directive is used inside an included properties file, then the including file is the base. Includes are very useful to keep headers like Bundle-Vendor, Bundle-Copyright central. If the extension of the file is .mf, then the file is parsed as a manifest file. By default, a property defined in an include file override earlier definitions, this implies that any property in the bnd file is overridden if defined in an include file. The include files are read in the order they are listed where later files override earlier files. If there are multiple definitions for the same property, then the last definitions wins. If the path of an included file starts with a ~, then it will '''not''' override earlier set properties.\\
You can use properties like ${user.home} in file names. If the file does not exist, an error is generated. If the filename is prefixed with a '-' sign then no error is generated when the file is absent. For example:  \\
\\
`-include= ~${user.home}/deflts.bnd, META-INF/MANIFEST.MF`\\
`-include= a.props, ~META-INF/MANIFEST.MF` ||
||`-manifest`||FILE||Overrides the generation of a manifest and uses the given file instead.||
||`-metatype`||[Analyzes the classes in the JAR for metatype interfaces (Metadata.OCD annotation). See [[MetaType][#LIST | LIST]] of [[#PATTERN | PATTERN]]|].||
||`-nomanifest`||false|true||Generate a JAR without a manifest||
||`-nodefaultversion`||false|true||If true, do not make exported packages without a version inherit the bundle version. See [[Versioning]]||
||`-nope`||true|false||Do not build a bundle, deprecated use `-nobundles=true`||
||`-nouses`||`true|false`||Do not calculate the `uses:` directive.||
||`-output`||PATH||Store the file (if applicable) under the path||
||`-plugin`||[plugins][#LIST|LIST]] of [[#PLUGIN|PLUGIN]]||Define the plugins that bnd should use. A plugin is a class that is used at certain phases in the bundle generation. That place is defined by the interfaces it implements. Plugins are defined in [[#plugins].||
||`-removeheaders`||[LIST][#LIST] of string||Removes the given headers from the output manifest. This feature can be useful if you are wrapping a bundle and it contains for example a Require-Bundle header.||
||`-snapshot`||`repl`||Replacement for the .SNAPSHOT qualifier used in maven. If a Bundle version uses SNAPSHOT as the qualifier then bnd will replace this with whatever `-snapshot` is set to. For example:\\
\\
  `-snapshot: ${tstamp}`\\
\\
This replaces the .SNAPSHOT qualifier with a timestamp. If -snapshot is not set the SNAPSHOT qualifier will not be replaced.||
||`-sources`||`true|false`||Include sources||
||`-wab`||[-wablib][#LIST|LIST]] of iclause||Turn a bundle into a Web Archive Bundle (WAB) that can also be used as a WAR, See [[#wab|-wab]] and [[#wablib]||
||`-wablib`||[-wablib][#LIST|LIST]] of [[#PATTERN|PATTERN]]||Turn a bundle into a Web Archive Bundle (WAB) that can also be used as a WAR, See [[#wab|-wab]] and [[#wablib]||

## Headers
||!Headers ||!Format ||!Description ||
||`Bundle-ClassPath`|| ||Defines the internal bundle class path, is taken into accont by bnd. That is, classes will be analyzed according to this path. The files/directories on the Bundle-ClassPath must be present in the bundle. Use Include-Resource to include these jars/directories in your bundle. In general you should not use Bundle-ClassPath since it makes things more complicated than necessary. Use the @ option in the Include-Resource to unroll the jars into the JAR.||
||`Bundle-\\
ManifestVersion`||2 ||The Bundle-ManifestVersion is always set to 2, there is no way to override this. ||
||`Bundle-Name`|| ||If the Bundle-Name is not set, it will default to the Bundle-SymbolicName. ||
||`Bundle-\\
SymbolicName`|| ||The Bundle-SymbolicName header can be set by the user. The default is the name of the main bnd file, or if the main bnd file is called bnd.bnd, it will be the name of the directory of the bnd file. An interesting variable is ${project} that will be set to this default name. ||
||`Bundle-Version`||VERSION ||The version of the bundle. If no such header is provided, a version of 0 will be set. ||
||`Conditional-Package`||[PATTERN][#LIST|LIST]] of [[#PATTERN] || '''experimental''' Works as private package but will only include the packages when they are imported. When this header is used, bnd will recursively add packages that match the patterns until there are no more additions.||
||`Export-Package`||[See ExportPackage][#LIST | LIST]] of [[#PATTERN | PATTERN]] ||The Export-Package header lists the packages that the bundle should export, and thus contain. [[#export-package].||
||`Fragment-Host`|| ||Ignored by bnd||
||`Import-Package`||[See Import Package][#LIST | LIST]] of [[#PATTERN | PATTERN]] ||The Import-Package header lists the packages that are required by the contained packages. [[#import-package].||
||`Include-Resource`||[See Include Resource][#LIST|LIST]] of iclause||The Include-Resource instruction makes it possible to include arbitrary resources; it contains a list of resource paths. [[#include-resource].||
||`Private-Package`||[See Private Package][#LIST | LIST]] of [[#PATTERN | PATTERN]] ||The Private-Package header lists the packages that the bundle should contain but not export. [[#private-package].||
||`Require-Bundle`|| ||Ignored by bnd||
||`Service-Component`||[Service Component Header][#LIST | LIST]] of `component` || See [[#component].||


## Basic Types
||[[#list]][[#LIST]]LIST||A comma separated list. Items should be quoted with '"' if the contain commas. In general, a list item can also define attributes and directives on an item.||
||[[#pattern]][[#PATTERN]]PATTERN||A pattern matches some entity: a package, a directory, etc. Patterns are based on Java regular expressions but are preprocessed before compiled. Any dots ('.') are replaced with \. to make them match the input and not act as the 'any character' operator. Any '?' or '*' is prefixed with a dot to make it match any character. As an extra convenience, if the string ends with \..*, an additional pattern is added to match the complete string with out the \..*. The effect is that something like com.acme.* matches com.acme and all its sub packages. It is also to negate a pattern by prefixing it with an exclamation mark ('!'). For example:\\
   `Import-Package: !com.sun.*, *` \\
indicates that any imports to com.sun should not be imported.||
||[[#regex]]REGEX||A regular expressions||


[[#export-package]]
## Export-Package
The bnd definition allows the specification to be done using ''patterns'', a modified regular expression. All patterns in the definition are matched against every package on the [ class path][#CLASSPATH ]. If the pattern is a negating pattern (starts with !) and it is matched, then the package is completely excluded. Normal patterns cause the package to be included in the resulting bundle. Patterns can include both directives and attributes, these items will be copied to the output. The list is ordered, earlier patterns take effect before later patterns. The following examples copies everything on the class path except for packages starting with `com`. The default for Export-Package is "*", which can result in quite large bundles. If the source packages have an associated version (from their manifest of packageinfo file), then this version is automatically added to the clauses.

  Export-Package= !com.*, *

Exports are automatically imported. This features can be disabled with a special directive on the export instruction: `-noimport:=true`. For example:
  
  Export-Package= com.acme.impl.*;-noimport:=true, *

Bnd will automatically calculate the `uses:` directive. This directive is used by the OSGi framework to create a consistent class space for a bundle. The Export-Package statement allows this directive to be overridden on a package basis by specifying the directive in an Export-Package instruction. 

  Export-package = com.acme.impl.*;uses="my.special.import"

However, in certain cases it is necessary to augment the uses clause. It is therefore possible to use the special name `<<USES>>` in the clause. Bnd will replace this special name with the calculated uses set. Bnd will remove any extraneous commas when the `<<USES>>` is empty.

  Export-package = com.acme.impl.*;uses:="my.special.import,<<USES>>"

Directives that are not part of the OSGi specification will give a warning unless they are prefixed with a 'x-'.

###Split packages
Bnd traverse the packages on the classpath and copies them to the output based on the instructions given by the Export-Package and Private-Package headers. This opens up for the possibility that there are multiple packages with the same name on the class path. It is better to avoid this situation because it means there is no cohesive definition of the package and it is just, eh, messy. However, there are valid cases that packages should be merged from different sources. For example, when a standard package needs to be merged with implementation code like the osgi packages sometimes (unfortunately) do. Without any extra instructions, bnd will merge multiple packages where the last one wins if the packages contain duplicate resources, but it will give a warning to notify the unwanted case of split packages.

The `-split-package:` directive on the Export-Package/Private-Package clause allows fine grained control over what should be done with split packages. The following values are architected:

||`merge-first`||Merge split packages but do not add resources that come later in the classpath. That is, the first resource wins. This is the default, although the default will generate a warning||
||`merge-last`||Merge split packages but overwrite resources that come earlier in the classpath. That is, the last resource wins.||
||`first`||Do not merge, only use the first package found||
||`error`||Generate an error when a split package is detected||

For example:

  Private-Package: test.pack;-split-package:=merge-first




[[#private-package]]
## Private Package
The method of inclusion is identical to the Export-Package header, the only difference is, is that these packages are not exported. This header will be copied to the manifest. If a package is selected by noth the export and private package headers, then the export takes precedence.

  Private-Package= com.*

[[#import-package]]
## Import Package
The Import-Package header lists the packages that are required by the contained packages. The default for this header is "*", resulting in importing all referred packages. This header therefore rarely has to be specified. However, in certain cases there is an unwanted import. The import is caused by code that the author knows can never be reached. This import can be removed by using a negating pattern. A pattern is inserted in the import as an extra import when it contains no wildcards and there is no referral to that package. This can be used to add an import statement for a package that is not referred to by your code but is still needed, for example, because the class is loaded by name.

For example:
  Import-Package: !org.apache.commons.log4j, com.acme.*,
     com.foo.extra

During processing, bnd will attempt to find the exported version of imported packages. If no version or version range is specified on the import instruction, the exported version will then be used though the micro part and the qualifier are dropped. That is, when the exporter is `1.2.3.build123`, then the import version will be 1.2. If a specific version (range) is specified, this will override any found version. This default an be overridden with the [-versionpolicy][#versionpolicy] command.

If an explicit version is given, then ${@} can be used to substitute the found version in a range. In those cases, the version macro can be very useful to calculate ranges or drop specific parts of the version. For example:

  Import-Package: org.osgi.framework;version="[1.3,2.0)"
  Import-Package: org.osgi.framework;version=${@}
  Import-Package: org.osgi.framework;version="[${version;==;${@}},${version;=+;${@}})"

If an imported package uses mandatory attributes, then bnd will attempt to add those attributes to the import statement. However, in certain (bizarre!) cases this is not wanted. It is therefore possible to remove an attribute from the import clause. This is done with the `-remove-attribute:` directive or by setting the value of an attribute to !. The parameter of the `-remove-attribute` directive is an instruction and can use the standard options with !, *, ?, etc.

  Import-Package: org.eclipse.core.runtime;-remove-attribute:common,*

Or

  Import-Package: org.eclipse.core.runtime;common=!,*

Directives that are not part of the OSGi specification will give a warning unless they are prefixed with a 'x-'.

[[#include-resource]]
## Include Resource
The resources will be copied into the target jar file. The iclause can have the following forms:

  iclause    ::= inline | copy
  copy       ::= '{' process '}' | process
  process    ::= assignment | simple
  assignment ::= PATH '=' simple
  simple     ::= PATH parameter*
  inline     ::= '@' PATH ( '!/' PATH? ('/**' | '/*')? )?
  parameters ::= 'flatten' | 'recursive' | 'filter'

In the case of `assignment` or `simple`, the PATH parameter can point to a file or directory. It is also possible to use the name.ext path of a JAR file on the classpath, that is, ignoring the directory. The `simple` form will place the resource in the target JAR with only the file name, therefore without any path components. That is, including src/a/b.c will result in a resource b.c in the root of the target JAR. 

If the PATH points to a directory, the directory name itself is not used in the target JAR path. If the resource must be placed in a subdirectory of the target jar, use the `assignment` form. If the file is not found, bnd will traverse the classpath to see of any entry on the classpath matches the given file name (without the directory) and use that when it matches. The `inline` requires a ZIP or JAR file, which will be completely expanded in the target JAR (except the manifest), unless followed with a file specification. The file specification can be a specific file in the jar or a directory followed by ** or *. The ** indicates recursively and the * indicates one level. If just a directory name is given, it will mean **.

The `simple` and `assigment` forms can be encoded with curly braces, like `{foo.txt}`. This indicates that the file should be preprocessed (or filtered as it is sometimes called). Preprocessed files can use the same variables and macros as defined in the [macro section][#macros].

The `recursive:` directive indicates that directories must be recursively included.

The `flatten:` directive indicates that if the directories are recursively searched, the output must not create any directories. That is all resources are flattened in the output directory.

The `filter:` directive is an optional filter on the resources. This uses the same format as the instructions. Only the file name is verified against this instruction.

 Include-Resource: @osgi.jar,[=\ =]
    {LICENSE.txt},[=\ =]
    acme/Merge.class=src/acme/Merge.class


[[#wab]]
## Web Archive Bundles
In OSGi Enterprise 4.2 the concept of Web Archive Bundles were introduced. Web Archive Bundles are 100% normal bundles following all the rules of OSGi. Their speciality is that they can be mapped to a web server following several of the rules of Java Enterprise Edition's Servlet model. The big difference is that the WARs of the servlet model have a rather strict layout of their archive because the servlet container also handles class loading. In OSGi, the class loading is very well specified and it would therefore be wrong to create special rules.

However, the OSGi supports the Bundle-Classpath header. This header allows the organization of the internal layout. It turns out that it is possible to create a valid Web Application Bundle (WAB) that is also a valid Web ARchive (WAR). Being deploy an archive both to OSGi and an application server obviously has advantages. bnd therefore supports a number of instructions that make it easy to create these dual mode archives.

The `-wab` instruction instructs bnd to move the root of the created archive to WEB-INF/classes. That is, you build your bundle in the normal way,not using the `Bundle-ClassPath`. The `-wab` command then moves the root of the archive so the complete class path for the bundle is no inside the WEB-INF/classes directory. It then adjusts the `Bundle-ClassPath` header to reflect this new location of the classes and resources.

The new root now only contains WEB-INF. In the Servlet specification, the root of the archive is mapped to the server's context URL. It is therefore often necessary to place static files in the root. For this reason, the `-wab` instruction has the same form as [Include-Resource][#include-resource] header, and performs the same function. However, it performs this function of copying resources from the file system after the classes and resources of the original bundle have been moved to WEB-INF.

For example, the following code creates a simple WAB/WAR:

  Private-Package:   com.example.impl.*
  Export-Package:    com.example.service.myapi
  Include-Resource:  resources/
  -wab:              static-pages/
  
The layout of the resulting archive is:

  WEB-INF/classes/com/example/impl/
  WEB-INF/classes/com/example/service/myapi/
  WEB-INF/classes/resources/
  index.html // from static-pages

The `Bundle-ClassPath` is `WEB-INF/classes`.

WARs can carry a WEB-INF/lib directory. Any archive in this directory is mapped to the class path of the WAR. The OSGi specifications do not recognize directories with archives it is therefore necessary to list these archives also on the `Bundle-ClassPath` header. This is cumbersome to do by hand so the `-wablib` command will take a list of paths. 

  Private-Package:   com.example.impl.*
  Export-Package:    com.example.service.myapi
  Include-Resource:  resources/
  -wab:              static-pages/
  -wablib:			 lib/a.jar, lib/b.jar
  
This results in a layout of:

  WEB-INF/classes/com/example/impl/
  WEB-INF/classes/com/example/service/myapi/
  WEB-INF/classes/resources/
  WEB-INF/lib/
    a.jar
    b.jar
  index.html ( from static-pages)
  
The `Bundle-ClassPath` is now set to `WEB-INF/classes,WEB-INF/lib/a.jar,WEB-INF/lib/a.jar`
