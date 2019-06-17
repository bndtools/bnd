---
order: 305
title: Startlevels
layout: default
---

One of the primary authors of bnd has always objected to startlevels. His motivation was twofold:

* A dynamic OSGi system should be ablet to startup in any order. Startlevels are often abused to hide bundles
  that do not handle their dynamic dependencies correctly. Hiding these bugs by controlling the start ordering
  is dangerous since it removes the symptom but the cause can still bite at a later inconvenient time.
* Start levels are global data about a set of bundles. OSGi is quite elegant that it stores virtually all
  meta data inside the bundle, not requiring global information. However, for start levels you need 
  data outside the bundles. This often becomes wrong over time.

With this disclaimer out of the way, there are actually cases where startlevels are quite important to 
improve the non-functional aspects.

* Jojo – Declarative services provides an elegant model to build components. However, they usually depend on
  configuration. If the component is started before configuration admin is started then it will start only to
  be immediately brought down when it receives its configuration. Using the [OSGi Configurator][1], the component
  can be brought down a second time. This _jojo_ effect is highly undesirable at startup. (Although the good part is
  that it exposes a lot of bugs.)
* Logging – A number of bundles, where the most prevalent is the logger, are more useful when they are running
  right from the beginning.

## Startlevel Support in bnd

Run configurations in bnd are described in _bndrun_ files. The list of bundles to run are listed in the  [-runbundles]
instruction. The `runbundles` instruction has a `startlevel` attribute that specifies the startlevel of each bundle.

Since the `-runbundles` instruction is frequently calculated by the resolver support in bnd it is not possible to manually
assign the `startlevel` attributes to specific bundles. For this reason there is a _decorator_ support in bnd for some selected 
instructions. A decorator is a header with the same name but ending with a plus sign (`+`). This instruction acts like a selector
and can be used to assign `startlevel` attributes on the `-runbundles`.

### Launching

The [launcher] is responsible for installing the bundles after starting or communicating with a framework. The
default bnd launcher will assign each bundle a startlevel, where bundles that do not have a specified `startlevel` are
assigned one level higher than the maximum specified levels.

The default launcher will then move the framework startlevel to 2 higher than the highest specified start level.

### Resolving

The resolve support in bnd can automatically assign `startlevel` attributes to the `-runbundles` based on different ordering
strategies. There are two strategies that use the dependency graph. This graph can be sorted in _topological_ order.
This means that a resource is always listed ahead of any of its dependencies when there are no cycles.

The [-runstartlevel] instruction controls the ordering and assigned start levels. 

## Strategies

At the time of this writing it is not yet clear what the best strategy is, and it may depend.

Ordering by the topological sort (a resource is followed by its dependencies) will start something like the log service
last and any applications bundles first. Although at first sight this feels wrong (the log service should be started first
to capture the events at startup) it does solve the jojo problem because one of the last bundles started will be the
Service Component Runtime that will activate all components. At that time all initialization should have taken place.

Reversing the topplogical sort will start the something like SCR and log first because they have few or no dependencies.
Application bundles are then started latest. 

## Numbering

Traditionally start levels are managed by an application bundle. Shell commands can increase and decrease start levels.
For this reason, start levels often use _nice_ numbers.

Using the resolver the need for _nice_ numbers is diminished since bnd is now taking care. The basic default model
is to assign bundles sequentially in selected order from an initial level stepping with 10. The [-runstartlevel] instruction
can provide the initial level and the step if desired.

The launcher will by default move the framework then to a start level that includes any used start levels. This
default behavior can be blocked by specifyig the `org.osgi.framework.startlevel.beginning` property. If this
property is set bnd will assume that there is an agent that will handle the runtime start levels.



[1]: https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html 
[-runbundles]: /instructions/runbundles.html
[-runstartlevel]: /instructions/runstartlevel.html
[launcher]: /chapters/launching.html
