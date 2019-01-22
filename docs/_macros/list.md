---
layout: default
class: Macro
title: list (';' KEY)*
summary: Returns a list of the values of the named properties with escaped semicolons.
---

The specified keys is looked up in properties and their values returned in a list. Any unescaped semicolons are escaped with a backslash. It is useful when the arguments to a macro are a list whose elements may contain semicolons and you need to manipulate the list.

    deps: com.foo;version=latest, com.bar;version=latest
    -buildpath: ${replace;${list;deps};$;\\;strategy=highest}

Results in `-buildpath` having the value

    com.foo;version=latest;strategy=highest, com.bar;version=latest;strategy=highest
