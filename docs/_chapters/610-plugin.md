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

bnd current supports the following plugin types:

<table>
	<tr>
		<th>Plugin Name</th>
		<th>Summary</th>
	</tr>
{% for plugin in site.plugins %}<tr><td><a href="{{ plugin.url | prepend: site.baseurl }}">{{plugin.title}}</a></td><td>{{plugin.summary}}</td></tr>
{% endfor %}
</table>

