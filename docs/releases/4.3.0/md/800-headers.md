___
layout: default
___
# Headers

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


