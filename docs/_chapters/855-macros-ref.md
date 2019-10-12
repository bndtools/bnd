---
title: Macro Index
layout: default
---

<div>
<dl class="property-index">

{% for macro in site.macros %}<dt><a href="{{ macro.url | prepend: site.baseurl }}">{{macro.title}}</a></dt><dd>{{macro.summary}}</dd>
{% endfor %}

</dl>
</div>