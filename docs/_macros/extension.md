---
layout: default
class: Macro
title: extension ';' PATH
summary: The file extension of the given path or empty string if no extension
---

Returns the file extension of the specified path. The last segment of the path is examined for a `.` separating the extension from the rest of the file name. The extension is returned or, if the file name has no extension, an empty string is returned.

## Examples

    # returns 'def'
    ${extension;abcdef.def}
    ${extension;/foo.bar/abcdefxyz.def}
    
    # Returns empty string
    ${extension;abcdefxyz}
    ${extension;/foo.bar/abcdefxyz}
