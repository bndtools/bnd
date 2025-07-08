---
layout: default
class: Project
title: -consumer-policy VERSION-MASK
summary: Specify the default version bump policy for a consumer when a binary incompatible change is detected. 
---

The `-consumer-policy` instruction defines the semantic versioning policy to be used when a type is a _consumer_. A consumer is in general a type that is implemented by classes that are just users of the package. In contrast, a _provider_ is the party that is basically responsible for the contract defined in the package. For example, when you implement Event Admin, the org.osgi.service.event package is your responsibility so the types you need to implement like `EventAdmin` are _provider types_. (These types are annotated with a `@ProviderType` annotation.) A casual user of the Event Admin service will be a consumer, the `EventHandler` type is therefore annotated with a`@ConsumerType`.

The purpose of this distinction is [semantic versioning][1]. It turns out that the relation between a consumer and a provider is not symmetric. A provider is tightly bound to a contract while a consumer is expected to have backward compatibility. Virtually any change to the contract requires the provider to adapt while a consumer is in almost all cases protect against changes.

This asymmetry has a consequence for the semantic versioning. In the OSGi, the semantics are defined that a micro change does not affect the provider nor the consumer. A minor change affects the provider, and a major change affects both. Therefore, a bundle that implements a provider type must import a range from `major.minor.micro` ... `major.minor+1.0`. A bundle that implements a consumer type must import    `major.minor.micro` ... `major+1.0.0`. 

In theory, bnd could have hard coded these policies but there are always cases where the policy is just not right. The `-consumer-policy` specifies the macro to use for calculating the version range. The default definition is:

	-consumer-policy ${range;[==,+)} 
	-provider-policy ${range;[==,=+)}
	
The [range][3] macro works very much like the [version][4] macro. It uses a template to define a change the range/version.

The provider and consumer policy are global and this is not very convenient if you want to make an exception just for a specific bundle. For example, a bundle coming from [Gavin King's Ceylon][2]. For this reason, you can also specify a policy on an import:

	Import-Package com.gavinking.*;version="${range;[--,++)}", * 	


The counterpart of the `-consumer-policy` is of course the [-provider-policy][5].

[1]: /chapters/170-versioning.html
[2]: https://twitter.com/1ovthafew/status/705011392861114368
[3]: /macros/range.html
[4]: /macros/version.html
[5]: /instructions/provider_policy.html
