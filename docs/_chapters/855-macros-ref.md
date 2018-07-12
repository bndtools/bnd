---
title: Macro Index
layout: default
---

<div>
<dl class="property-index">

{% for macro in site.macros %}<dt><a href="{{ macro.url | prepend: site.baseurl }}">{{macro.title | escape}}</a></dt><dd>{{macro.summary | escape}}</dd>
{% endfor %}

</dl>
</div>
