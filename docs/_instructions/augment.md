---
layout: default
class: Workspace
title: -augment PARAMETER ( ',' PARAMETER ) *
summary: Add requirements and capabilities to the resources during resolving.
---

The `-augment` instruction can be used to _augment_ resources in the repositories. Augmenting is adding additional capabilities and requirements. When bnd resolves a project or bndrun file then, it will read this variable (it is a merge property so you can also use additional keys like `-augment.xyz`) and use it to _decorate_ the repository entries.

The key of the `PARAMETER` is for the bundle symbolic name. It can contain the * wildcard character to match multiple bundles. The bundle symbolic name must be allowed as a value in a filter it is therefore **not** a globbing expression. 

The following directives and attribute are architected:

* `version` – A version range. If a single version is given it will be used as `[<version>,∞)`. The version range can be prefixed with an '@' for a consumer range (to the next major) or a provider range (to the next minor) when the '@' is a suffix of the version. The range can restrict the augmentation to a limited set of bundles. 
* `capability:` – The `capability:` directive specifies a `Provide-Capability` instruction, this will therefore likely have to be quoted. Any number of clauses can be specified.
* `requirement:` – The `requirement:` directive specifies a `Require-Capability` instruction.
  
To augment the repositories during a resolve, bnd will find all bundles that match the bundle symbolic name and fall within the defined range. If no range is given, only the bundle symbolic name will constrain the search. Each found bundle will then be decorated with the capabilities and requirements defined in the `capability:` and `requirement:` directive.

For example, we need to provide an extender capability to a bundle with the bundle symbolic name `com.example.prime`.

	-augment.prime = \
		com.example.prime; \
			capability:='osgi.extender; \
				osgi.extender=osgi.component; \
				version:Version=1.2'

The `capability:` and `requirement:` directives follow all the rules of the Provide-Capability and Require-Capability headers respectively. For the resolver, it is as if these headers were specified in their manifests. There is one exception, the `cap:` and `req:` directives also support capabilities from the `osgi.wiring.*` name spaces.

## Caveats

It should be realized that the augmentation is _only_ used during resolving. These requirements & capabilities are **not** added to the actual bundles during runtime. The primary purpose is to ensure that the resolve can properly assemble a system; great care should be taken to not create assemblies that cannot be resolved by the framework.