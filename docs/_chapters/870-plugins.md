---
title: Plugins
layout: default
---

## Plugins
Plugins are objects that can extend the functionality of bnd. They are called from inside bnd when a certain action should take place. For example, bnd uses a repository and plugins provide the actual repository implementations. Or for example, the SpringComponent analyzes the Spring files and adds references found in that XML to the imports.

A plugin is defined as:

	PLUGIN ::= FQN ( ';' \<directive\|attribute\> )*

The following directive is defined for all plugin:

||`path:` ||A path to the jar file that contains the plugin. The directory/jar at that location is placed on your classpath for that plugin.||

## Index

<div>
<dl class="property-index">

{% for c in site.plugins %}<dt><a href="{{ c.url | prepend: site.baseurl }}">{{c.title}}</a></dt><dd>{{c.summary}}</dd>
{% endfor %}

</dl>
</div>

