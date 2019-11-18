---
layout: default
class: Macro
title: basenameext ';' PATH ( ';' EXTENSION )
summary: The basename of the given path optionally minus a specified extension
---

Returns the basename of the specified path optionally minus a specified extension. This is the last segment of the path. If an extension is specified, the basename is examined for a `.` separating the extension from the rest of the file name. If the extension of the basename matches the specified extension, this extension is removed from the basename before it is returned. The extension, if specified, may optionally start with `.`.

## Examples

    # returns 'abcdef.def'
    ${basenameext;abcdef.def}
    ${basenameext;/foo.bar/abcdef.def}
    ${basenameext;abcdef.def;bar}
    ${basenameext;/foo.bar/abcdef.def;bar}
    
    # returns 'abcdef'
    ${basenameext;abcdef.def;def}
    ${basenameext;/foo.bar/abcdef.def;def}
    ${basenameext;abcdef.def;.def}
    ${basenameext;/foo.bar/abcdef.def;.def}