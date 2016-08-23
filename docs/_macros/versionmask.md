---
layout: default
class: Macro
title: versionmask MASK VERSION?
summary: Modify a version using a template
---

The `versionmask` macro takes a template (MASK) and a version. It then uses the template
to modify the version. This is useful to get rid of parts of the version. For example,
to get rid of the qualifier the following macro is useful:

	${versionmask;===;1.2.3.awfulqualifier} -> 1.2.3

It is also possible to modify the template. For example, if the next minor version is
sought, then the following macro is useful:

	${versionmask;=+;1.2.3.awfulqualifier} -> 1.3

The syntax for the `versionmask` macro is:

	versionmask ::= mask
	mask  		::= m ( m ( m ( q )? )? )?
	m     ::= [0-9=+-~]
	q     ::= [0-9=~Ss]

The MASK can consists of 1 to 4 template characters. Each character maps to a _version
part_.The first character maps to the major, the second to the minor, the third to the
micro, and the last to the qualifier. These characters can be:

* `=` – Keep the version part
* `-` – Decrement the version part
* `+` – Increment the version part
* `[0-9]` – Replace the version part
* `~` – Ignore the version part
* `S` – If the qualifier equals `SNAPSHOT` or ends with `-SNAPSHOT`, then the qualifier
will be ignored and the resulting version will be suffixed with `-SNAPSHOT`. This
results in a maven version which is not a valid OSGi version. Otherwise, the existing
qualifier, if any, will be kept. For example, `${versionmask;===S;1.2.3.SNAPSHOT}` will
return `1.2.3-SNAPSHOT` and `${versionmask;===S;1.2.3.QUAL}` will return `1.2.3.QUAL`. 
* `s` – If the qualifier equals `SNAPSHOT` or ends with `-SNAPSHOT`, then the qualifier
will be ignored and the resulting version will be suffixed with `-SNAPSHOT`. This
results in a maven version which is not a valid OSGi version. Otherwise, the existing
qualifier, if any, will be ignored. For example, `${versionmask;===s;1.2.3.SNAPSHOT}`
will return `1.2.3-SNAPSHOT` and `${versionmask;===s;1.2.3.QUAL}` will return `1.2.3`.  

In many places in bnd, the `${@}` macro is set as an implicit variable. For example, if
the bnd analyzer calculates the import range it sets this to the version of the package
that is imported. Therefore, if the version is not specified in this macro as the second
argument then the `${@}` macro is expanded and used as the value for the version. A use
of this is in the Import Package statement. There you can use the `${versionmask}` macro
to clean up the imported version. (Though the [range][1] macro is better suited for
this.)

	Import-Package com.exmaple.foo;version=${versionmask;==}

This macro is also available under the name `version`. The name was changed because this
often collided with macros that were defined by the user.

[1]: /macros/range.html
