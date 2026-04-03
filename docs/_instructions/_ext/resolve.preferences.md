---
layout: default
class: Workspace
title: -resolve.preferences qname ( ',' qname )
summary: Override the default order and selection of repositories
---

The resolver normally finds a lost of capabilities that match a given requirement. This list has an order defined by the context. However, in certain occasions this order is not the desired order. The `-resolve.preferences` allows you to override this context order. It is an ordered list of Bundle Symbolic Names. The list of capabilities will always be adjusted to have the bundles in the `-resolver.preferences` always first when they are present.

For example:

	`-resolve.preferences` : \
		com.example.bundle.most.priority, \
		com.example.bundle.less.priority, \
		com.example.whatever

Given that for a requirement the capabilties come from:

	com.example.some.bundle, 
	om.example.bundle.less.priority, 
	com.example.another.bundle, 
	com.example.most.priority

Then the resulting order will be:

	com.example.most.priority
	om.example.bundle.less.priority, 
	com.example.some.bundle, 
	com.example.another.bundle, 

Preferences should only be used when blacklisting is not a better solution.
