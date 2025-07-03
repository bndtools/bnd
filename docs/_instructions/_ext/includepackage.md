---
layout: default
class: Builder
title: -includepackage PACKAGE-SPEC, ( ',' PACKAGE-SPEC )*
summary:  Include a number of packages from the class path
---

The `-includepackage` instruction falls in the family of instructions to pull in packages from the current class path. The
instructions lists one of more package specifications that can contain wildcards. Any attributes or directives are 
ignored. This instruction only operates during the _expand_ phase of the JAR. It has not further semantics.

The primary motivation for this instruction is the use of the `@Export` annotation. Using that annoation puts
bnd in a bind. A Private-Package should be private and an Export-Package should be exported. Since a bnd file
should always have the final say, the `@Export` instruction needed an instruction that took in the packages but
that did not put export semantics on it.

This instruction is also taken into account when taken the decision to automatically fill up the JAR from the
current project. If this instruction is set (or any other construction instruction) it is assumed that the user
wants to be in control and the instructions are followed. If none of these instructions is set then the JAR is 
constructed from the project's output folder.

When the packages selected by the `-includepackage` instructions overlap with either Private-Package or Export-Package
then the Export-Package has the highest priority and then Private-Package. That is, any `@Export` in a package that is selected 
by Export-Package or Private-Package is ignored silently. 

Notice that an annotation like `@Version` in a a package like selected can still provide the version if the Export-Package 
header for that package provides no version.
 
## Example

The following example includes all packages from `com.example.*` except `com.example.bar`.

    -includepackage !com.example.bar, com.example.*
