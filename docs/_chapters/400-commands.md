---
order: 400
title: Commands
layout: default
---

## Use
The command line tool can be invoked in several different ways:

* bnd ''general-options'' ''cmd'' ''cmd-options''
* bnd ''general-options'' ''<file>.jar''
* bnd ''general-options'' ''<file>.bnd''

In this text `bnd` is used as if it is a command line program. This should be set up as: 

  java -jar <path to bnd>.jar ...

### General Options

||!General Option ||!Description ||
||-failok ||Same as the property -failok. The current run will create a JAR file even if there were errors. ||
||-exceptions ||Will print the exception when the software has ran into a bad exception and bails out. Normally only a message is printed. For debugging or diagnostic reasons, the exception stack trace can be very helpful. ||

### Main options

    [ -f, --full ]             - Do full
    [ -p, --project <string> ] - Identify another project
    [ -t, --test ]             - Build for test
    [ -o, --output <string> ]  - Specify the output file path. The default is
                                output.jar in the current directory


### Reference


<div>
<dl class="property-index">

{% for c in site.commands %}<dt><a href="{{ c.url | prepend: site.baseurl }}">{{c.title | escape}}</a></dt><dd>{{c.summary | escape}}</dd>
{% endfor %}

</dl>
</div>
