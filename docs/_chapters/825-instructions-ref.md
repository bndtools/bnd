---
title: Instruction Index
layout: default
---

<div>
<table class="property-index">
    <thead>
        <th>Instruction</th>
        <th>Description</th>
        <th>Class</th>
    </thead>
    <tbody>
        {% for instruction in site.instructions %}
        <tr>
            <td><a href="{{ instruction.url | prepend: site.baseurl }}">{{instruction.title | escape}}</a></td>
            <td>{{instruction.summary | escape}}</td>
            <td>{{instruction.class}}</td>
        </tr>
        {% endfor %}
    </tbody>
</table>
</div>
