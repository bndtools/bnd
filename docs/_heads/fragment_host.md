---
layout: default
class: Header
title: Fragment-Host       ::= bundle-description 
summary: The Fragment-Host header defines the host bundles for this fragment.
---

The Fragment-Host manifest header links the fragment to its potential _hosts_. A fragment bundle is loaded in the 
same class loader as the _host_ that it will be attached to in runtime. When a fragment is attached to its host,
then some headers are _merged_. One of those headers is the Import Package header.

bnd will calculate the references without taking the host into account. If the fragment uses packages from the host, 
quite likely, then these would result in imports. For this reason, bnd will subtract any package that can be found
in the host from the import.