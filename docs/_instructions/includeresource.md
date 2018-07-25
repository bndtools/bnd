---
layout: default
class: Builder
title: -includeresource iclause   
summary:  Include resources from the file system
---

The resources will be copied into the target jar file. The `iclause` can have the following forms:

```
  iclause    ::= inline | copy
  copy       ::= '{' process '}' | process
  process    ::= assignment | simple
  assignment ::= PATH '=' simple
  simple     ::= PATH parameter*
  inline     ::= '@' PATH ( '!/' PATH? ('/**' | '/*')? )?
  parameters ::= 'flatten' | 'recursive' | 'filter'
```

In the case of `assignment` or `simple`, the PATH parameter can point to a file or directory. It is also possible to use the name.ext path of a JAR file on the classpath, that is, ignoring the directory. The `simple` form will place the resource in the target JAR with only the file name, therefore without any path components. That is, including src/a/b.c will result in a resource b.c in the root of the target JAR. 

If the PATH points to a directory, the directory name itself is not used in the target JAR path. If the resource must be placed in a subdirectory of the target jar, use the `assignment` form. If the file is not found, bnd will traverse the classpath to see of any entry on the classpath matches the given file name (without the directory) and use that when it matches. The `inline` requires a ZIP or JAR file, which will be completely expanded in the target JAR (except the manifest), unless followed with a file specification. The file specification can be a specific file in the jar or a directory followed by ** or *. The ** indicates recursively and the * indicates one level. If just a directory name is given, it will mean **.

The `simple` and `assigment` forms can be encoded with curly braces, like `{foo.txt}`. This indicates that the file should be preprocessed (or filtered as it is sometimes called). Preprocessed files can use the same variables and macros as defined in the macro section.

The `recursive:` directive indicates that directories must be recursively included.

The `flatten:` directive indicates that if the directories are recursively searched, the output must not create any directories. That is all resources are flattened in the output directory.

The `filter:` directive is an optional filter on the resources. This uses the same format as the instructions. Only the file name is verified against this instruction.

 Include-Resource: @osgi.jar,[=\ =]
    {LICENSE.txt},[=\ =]
    acme/Merge.class=src/acme/Merge.class

