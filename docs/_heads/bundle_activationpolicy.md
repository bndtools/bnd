---
layout: default
class: Header
title: Bundle-ActivationPolicy ::= policy ( ';' directive )*
summary: The Bundle-ActivationPolicy specifies how the framework should activate the bundle once started.
---

Activation Policies
The activation of a bundle can also be deferred to a later time from its start using an activation policy. This policy is specified in the Bundle-ActivationPolicy header with the following syntax:
Bundle-ActivationPolicy ::= policy ( ';' directive )*
policy ::= 'lazy'
The only policy defined is the lazy activation policy. If no Bundle-ActivationPolicy header is speci- fied, the bundle will use eager activation.
Lazy Activation Policy
A lazy activation policy indicates that the bundle, once started, must not be activated until it re- ceives the first request to load a class. This request can originate either during normal class load- ing or via the Bundle loadClass method. Resource loading and a request for a class that is re-direct- ed to another bundle must not trigger the activation. The first request is relative to the bundle class loader, a bundle will not be lazily started if it is stopped and then started again without being re- freshed in the mean time.
This change from the default eager activation policy is reflected in the state of the bundle and its events. When a bundle is started using a lazy activation policy, the following steps must be taken:
• A Bundle Context is created for the bundle.
• The bundle state is moved to the STARTING state.
• The LAZY_ACTIVATION event is fired.
• The system waits for a class load from the bundle to occur.
• The normal STARTING event is fired.
• The bundle is activated.
• The bundle state is moved to ACTIVE.
• The STARTED event is fired.
If the activation fails because the Bundle Activator start method has thrown an exception, the bun- dle must be stopped without calling the Bundle Activator stop method. These steps are pictured in a flow chart in Figure 4.5. This flow chart also shows the difference in activation policy of the normal eager activation and the lazy activation.
•
￼￼Page 110
OSGi Core Release 6
Life Cycle Layer Version 1.8
The Bundle Object
￼￼Figure 4.5
Starting with eager activation versus lazy activation
￼￼￼￼￼￼started? no
yes
￼￼￼￼￼￼￼state=STARTING
￼￼￼￼￼￼￼￼lazyactivation? yes no
exception?
Waitforclass load trigger
event LAZY_ACTIVATION
￼￼￼￼￼￼￼￼￼￼event STARTING
￼￼activate the bundle
￼￼￼￼￼￼￼yes
no
￼￼￼￼￼￼￼￼￼￼￼state=STOPPING event STOPPING state=RESOLVED event STOPPED
state=ACTIVE event STARTED
￼￼￼￼￼￼￼￼￼￼The lazy activation policy allows a Framework implementation to defer the creation of the bundle class loader and activation of the bundle until the bundle is first used; potentially saving resources and initialization time during startup.
By default, any class loaded from the bundle can trigger the lazy activation, however, resource loads must not trigger the activation. The lazy activation policy can define which classes cause the activa- tion with the following directives:
• include - A list of package names that must trigger the activation when a class is loaded from any of these packages. The default is all package names present in the bundle.
• exclude - A list of package names that must not trigger the activation of the bundle when a class is loaded from any of these packages. The default is no package names.
For example:
Bundle-ActivationPolicy: lazy; «
    include:="com.acme.service.base,com.acme.service.help"
When a class load triggers the lazy activation, the Framework must first define the triggering class. This definition can trigger additional lazy activations. These activations must be deferred until all transitive class loads and defines have finished. Thereafter, the activations must be executed in the reverse order of detection. That is, the last detected activation must be executed first. Only after
￼￼OSGi Core Release 6
Page 111
The Bundle Object Life Cycle Layer Version 1.8
￼￼4.4.6.3
all deferred activations are finished must the class load that triggered the activation return with the loaded class. If an error occurs during this process, it should be reported as a Framework ERROR event. However, the class load must succeed normally. A bundle that fails its lazy activation should not be activated again until the framework is restarted or the bundle is explicitly started by calling the Bundle start method.


	public boolean verifyActivationPolicy(String policy) {
		Parameters map = parseHeader(policy);
		if (map.size() == 0)
			warning(Constants.BUNDLE_ACTIVATIONPOLICY + " is set but has no argument %s", policy);
		else if (map.size() > 1)
			warning(Constants.BUNDLE_ACTIVATIONPOLICY + " has too many arguments %s", policy);
		else {
			Map<String,String> s = map.get("lazy");
			if (s == null)
				warning(Constants.BUNDLE_ACTIVATIONPOLICY + " set but is not set to lazy: %s", policy);
			else
				return true;
		}

		return false;
	}
