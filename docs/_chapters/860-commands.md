---
title: Command Reference
layout: default
---

## Main options

    [ -f, --full ]             - Do full
    [ -p, --project <string> ] - Identify another project
    [ -t, --test ]             - Build for test


## Reference

<div>
<dl class="property-index">

{% for c in site.commands %}<dt><a href="{{ c.url | prepend: site.github.url }}">{{c.title}}</a></dt><dd>{{c.summary}}</dd>
{% endfor %}

</dl>
</div>

