---
layout: bnd
title: Plugins
nav_order: 3
has_children: true
permalink: /plugins/
---

Repository plugins are usually referenced in cnf/build.bnd and implement the Tagged interface.

The tags property of repositories’ configuration allows to add a comma separated list of tags to a repository. These tags will be used for filtering a list of repositories. For example the -runrepos instruction in .bndrun considers only those repositories for resolution which have either the resolve tag or no tags property defined. This allows including and excluding repositories based on their tags.
