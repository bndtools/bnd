---
layout: default
class: Workspace
title: -workingset PARAMETER ( ',' PARAMETER ) *
summary: Group the workspace into different working sets
---

A _workingset_ is a name for a group of projects that have something in common. You can _tag_ a project for one or more workingsets using the `-workingset` instruction in the `bnd.bnd` file. For example, to make a project member of the `Implementations` and `Drivers` working sets you can enter the following in a bnd file:

    -workingset Implementations, Drivers

An IDE can use this information to provide a grouping to projects. However, this information can also be used to tag bundles in the Manifest, like:

    Bundle-Category ${-workingset.*}

The syntax for the instruction is:

    workingset := '-workingset' tag ( ',' tag )*
    tag        := NAME ( ';member=' TRUTHY )?
    NAME       := <JavaIdentifierPart+>

The instruction is a _merged_ instruction so you can also set:

    -workingset.all All

Note that, the name of the working set must use the pattern of Java identifier.

## More Advanced Usage

As you could see in the syntax, you can also specify a `member` attribute on the tag. This member attribute evaluates to a _truthy_. A truthy is `true` if it is not empty, 0, nor `false`. The truthy is well supported by macros so you can now use the bnd macro language to decide if a project should be a member of a working set or not. This can be used in many ways. For example, you could use it to do name matching. By placing the following in the `cnf/build.bnd` file you do not have to place the `-workingset` instruction in each bnd file.

    -workingset =  \
      impl;member=${filter;${p};.*\.provider}, \
      api;member=${filter;${p};.*\.api}, \
      test;member=${filter;${p};.*\.test}

The feature will create working sets as demanded but will reuse existing working set with the matching name. If no `-workingset` instruction is given, the working sets are not touched in any way for that project. That is, they are then not removed from existing sets.

## Manual Workingsets

In some cases it is necessary to maintain a working set manual. Such a workingset is then stored in Eclipse and not shared with the team. To
create a manual working set, use a name that is outside the specified NAME pattern. For example, use a name that starts with a 
dot (`.`) like `.Private`. Since you cannot use these names in the `-workingset` instruction (they generate an error)
bnd will never look at workingsets with such a name.

