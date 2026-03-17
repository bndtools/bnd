---
title: Headers
layout: bnd
nav_order: 1
has_children: true
permalink: /headers/
---
Learn the basics of bnd headers on the [Generating JARs](/chapters/160-jars.html) page. 

<div>
<table class="property-index">
    <thead>
        <th>page</th>
        <th>Description</th>
        <th>Class</th>
    </thead>
    <tbody>
        {% for page in site.heads %}
        <tr>
            <td><a href="{{ page.url | prepend: site.baseurl }}">{{page.title | escape}}</a></td>
            <td>{{page.summary | escape}}</td>
            <td>{{page.class}}</td>
        </tr>
        {% endfor %}
    </tbody>
</table>
</div>
