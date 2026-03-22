---
layout: bnd
title: Plugins
nav_order: 5
has_children: true
permalink: /plugins/
has_toc: false
---

## Plugins
Plugins are objects that can extend the functionality of bnd. They are called from inside bnd when a certain action should take place. For example, bnd uses a repository and plugins provide the actual repository implementations. Or for example, the SpringComponent analyzes the Spring files and adds references found in that XML to the imports.

A plugin is defined as:

    PLUGIN ::= FQN ( ';' \<directive\|attribute\> )*

The following directive is defined for all plugin:

| Directive | Description |
| --- | --- |
| `path:` | A path to the jar file that contains the plugin. The directory/jar at that location is placed on your classpath for that plugin. |

## Tagging of repository plugins

Repository plugins are usually referenced in `cnf/build.bnd` and implement the [Tagged](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/service/tags/Tagged.java) interface. 

The `tags` property of repositories' configuration allows to add a comma separated list of tags to a repository. These tags will be used for filtering a list of repositories. 
For example the [-runrepos](/instructions/runrepos.html) instruction in `.bndrun` considers only those repositories for resolution which have either the `resolve` tag or no `tags` property defined. This allows including and excluding repositories based on their tags.

## Index

<div>

<div>
<table class="property-index">
    <thead>
        <th>page</th>
        <th>Description</th>
        <th>Class</th>
    </thead>
    <tbody>
        {% for page in site.plugins %}
        <tr>
            <td><a href="{{ page.url | prepend: site.baseurl }}">{{page.title | escape}}</a></td>
            <td>{{page.summary | escape}}</td>
            <td>{{page.class}}</td>
        </tr>
        {% endfor %}
    </tbody>
</table>
</div>


</div>
