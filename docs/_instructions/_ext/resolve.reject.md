---
layout: default
class: Workspace
title: -resolve.reject ( '@'? namespace ( ';filter:=' FILTER )? ), 
summary: Controls rejection of capabilities during resolving.
---

The resolver will request capabilities from the resolve context. In bnd, the resolve hook can be
used to reject some of these capabilities. The `-resolve.reject` instruction can control the default
hook. Without this instruction, nothing is rejected and the resolver sees all capabilities. With this
hook it is possible to reject

* A complete namespace – Just use the namespace, this will reject all capabilities in that namespace
* Capabilities in a given namespace where a filter expression matches – Use the standard requirement syntax.
* On the resource – Prefix the requirement with an @ and it will be applied on the capabilities of the parent resource. If at least one is found, the capability is rejected.
* A combination of the above.

To directly reject a namespace `foo`, the instruction is simply:

    -resolve.reject foo

This will reject any capability in the namespace `foo`. To reject a namespace `foo` where a filter matches, the instruction is:

    -resolve.reject foo;filter:='(foo=3)`

This will reject any capability in the namespace `foo` that has an attribute `foo` that has the value 3.

Sometimes it is necessary to reject a capability depending on its resource. For example, you want to reject
capabilities that come from fragments. You can achieve this by prefixing the requirement with
a `@` (commercial at sign). For example, if you want to reject any capabilities that come from a resource that has identity type `osgi.fragment`.

    -resolve.reject @osgi.identity;filter:='(type=osgi.fragment)'

 You can add multiple requirements in the specification. These requirements will be or'ed together. The following
 specification:
 
    -resolve.reject @foo, baz
 
 Will reject any capabiliy from a resource that has a `foo` capability and it will remove all `baz` capabilities.
 

## Example

Before bnd release 7, the resolver always rejected resources that were neither a bundle nor a fragment. This was
changed to allow more scenarios. The filtering was moved to the list of bundles where it is necessary that the resources
are all bundles.

This should normally not make a difference during resolving. However, in some cases you want to have the old 
situation. The following example restores the pre release 7 situation. 

    -resolve.reject     @osgi.identity;filter:='(!(|(type=osgi.bundle)(type=osgi.fragment)))'



