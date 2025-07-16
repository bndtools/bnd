---
order: 400
title: bnd CLI Commands
layout: default
---

The command line interface (bnd CLI) provides various tools to invoke bnd functions and features. It is using bndlib under the hood just like other tooling like bndtools, maven or gradle plugins do too.

Checkout the [tutorial](/chapters/123-tour-workspace.html) which makes use of the CLI.

## Installation

See here to [install the CLI](/chapters/120-install.html#command-line).

## Use

The bnd CLI can be invoked in several different ways:

* bnd ''general-options'' ''cmd'' ''cmd-options''
* bnd ''general-options'' ''<file>.jar''
* bnd ''general-options'' ''<file>.bnd''

In this text `bnd` is used as if it is a command line program. This should be set up as: 

  java -jar <path to bnd>.jar ...

### General Options

||General Option ||Description ||
||--debug ||Show log debug output||
||--failok ||Turns errors into warnings so command always succeeds ||
||--exceptions ||Will print the exception when the software has ran into a bad exception and bails out. Normally only a message is printed. For debugging or diagnostic reasons, the exception stack trace can be very helpful. ||

Show [list of all options](/commands/bnd.html) for the bnd CLI.


### Reference


<div>

<div>
<table class="property-index">
    <thead>
        <th>page</th>
        <th>Description</th>
        <th>Class</th>
    </thead>
    <tbody>
        {% for page in site.commands %}
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
