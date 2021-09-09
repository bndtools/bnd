Bndtools Development: Tips and Tricks
=====================================

Adding Error/Warning Markers
----------------------------

Bndtools aims to be a thin wrapper over bnd. This means that it is comparatively rare that bndtools 
should be creating errors or warnings. In most cases bnd is responsible for generating errors at
build time, it's then bndtools job to display them nicely to the user.

### Step 1 - Customise the bnd error


Make sure that bnd is generating an appropriate error or warning using bnd's reporting API. By default
all build errors are displayed as markers on the bnd.bnd file. This isn't usually the right file, and
even when it is there isn't enough information to pick the correct line. To fix this you need to add
information to the Location object associated with the error/warning. Importantly you must add a
custom details object to the location. This should be your own object type, and not reused by any
other error reporting.

e.g.

    SetLocation location = analyzer.error("A clever error message");
    location.line(1234)
        .method("doStuff")
        .details(new org.bndtools.example.MyCustomizedLocationObject());


### Step 2 - Create a BuildErrorHandler


Once bnd is generating extra error information then bndtools can use it to generate appropriate 
markers. This is achieved through the use of the org.bndtools.build.api.BuildErrorDetailsHandler
(it's normally best to extend org.bndtools.build.api.AbstractBuildErrorDetailsHandler). The method
responsible for adding markers is generateMarkerData(), which returns a list of MarkerData objects.
Each MarkerData will be used to create a Marker in a file. Normally an error should map to a single
marker, although sometimes more than one is appropriate (e.g. if the error can be fixed by changing
one of several files).

AbstractBuildErrorDetailsHandler provides convenience methods for adding markers to Java source files.
Markers must be created with a message (which is typically the message written by bnd). Markers should
also have a line number, or a start/end location. The convenience methods set the start/end location
for you, but you have to set the message yourself.

e.g.


    MyCustomizedLocationObject errorInfo = (MyCustomizedLocationObject) location.details;

    IJavaProject javaProject = JavaCore.create(project);

    Map<String,Object> attribs = new HashMap<String,Object>();
    attribs.put(IMarker.MESSAGE, location.message.trim());

    MarkerData md = null;
    if (errorInfo.isTypeLevel) {
      md = createTypeMarkerData(javaProject, errorInfo.className, attribs, false);
    } else if (errorInfo.isMethodLevel) {
      md = createMethodMarkerData(javaProject, errorInfo.className, errorInfo.methodName, 
              errorInfo.methodSignature, attribs, false);
    }

    if (md == null) {
      // No other marker could be created, so add a marker to the bnd file
      result.add(new MarkerData(getDefaultResource(project), attribs, false));
    }

    result.add(md);

    return result;
    
    
### Step 3 - Hook in to the Eclipse plugin registry

Bndtools uses the Eclipse plugin registry to discover BuildErrorDetailsHandler instances. To hook
in to this you need to add the following to your plugin.xml


    <extension point="bndtools.core.buildErrorDetailsHandlers">
      <handler typeMatch="org.bndtools.example.MyCustomizedLocationObject" 
          class="org.bndtools.example.handler.MyCustomBuildErrorDetailsHandler" />
    </extension>
    
    
### Step 4 - You're done!

Error markers will now appear in the right places. More work can be done to add quick fixes, but
it's much harder than adding the markers.


# Using the `ExtensionFacade`
-------------------------------

## What is the `ExtensionFacade`?

`org.bndtools.facade.ExtensionFacade` was developed by the core Bndtools
team as part of the `bndtools.core` project. It acts as a bridge between the
Eclipse extension registry and OSGi Declarative Services components. Thus it
allows you to deploy Eclipse extensions in your plugins that are implemented as
OSGi Declarative Services components.

## Why use the `ExtensionFacade`?

Eclipse extensions tend to be quite static. Although in theory the ExtensionRegistry
was meant to allow dynamic extensions, in practice the assumption that the extension
lifecycle is equal to the workbench lifecycle has been accidentally baked in to
many Eclipse plugins (including core Eclipse plugins). This means that in
practice you can't restart a plugin in a running Eclipse instance if it
registers extensions.

The main purpose of `ExtensionFacade` is to allow extension implementations
to be decoupled a bit from the registry, allowing the ExtensionRegistry to be
shielded from the dynamic nature of the underlying implementation. This allows
you to develop Eclipse plugins with extension implementations that can be
dynamically restarted without requiring a restart of the entire Workbench.

This feature will most of the time not be noticed by the average Eclipse user.
However, it is extremely useful to the Eclipse plugin developer who uses Bndtools
as their plugin development platform, as it enables you to make use of Bndtools'
live coding/testing feature. This significantly improves the rate at which you can
develop/deploy/test changes to your plugin.

## How to use the `ExtensionFacade`

### Modes of operation
There are two ways to use the `ExtensionFacade`:

1. Factory mode - in this case, the `ExtensionFacade` simply acts as a
factory object, directly instantiating and returning the extension implementation
objects. If the backing component service is not available, it will throw a
runtime exception at instantiation time.
2. Full proxy mode - in this case, the `ExtensionFacade` returns a
dynamic proxy implementation of the extension interface. If the backing
component is not available, then the proxy implementation will throw a runtime
exception any time one of its methods is invoked.

A comparison of the advantages of each mode follows.

#### Full proxy mode

The full proxy mode presents the same extension object to the rest of the system
for the life of the Workbench. This makes it more suitable when you have
components in the system that are hanging on to references to the extension
objects for extended periods, as it will allow the backing component object 
instances to be cleaned up. For example, if the extension client caches the
reference to the extension object in a static class variable, there is no easy
way to clear that reference when your service restarts. Using the proxy mode,
the extension client's cache keeps the reference to the proxy, while the backing
service is allowed to come-and-go as its bundle is stopped/started.

However, there are times when the full proxy mode won't work:

* When the clients of the extension make assumptions about the concrete type
of the extension object. An example is in the source lookup code, which at one
point specifically looks for a subclass of `AbstractSourceLookupDirector` - 
even if your backing service extends `AbstractSourceLookupDirector`, the
returned proxy object does not.
* The proxy mode makes use of Java's dynamic proxies, and these can only be used
when the base extension type is an interface. Some of the extension objects in
Eclipse (eg, builders) have a class or an abstract class as their base class.

The full proxy mode also involves a theoretical performance overhead for
requests (though this is not likely to be of practical significance).

#### Factory mode

The factory mode directly returns a reference to the backing component service
every time `create()` is called. This means that the returned object it will
be the exact same type as the actual extension object. This means it doesn't
have the limitations that the proxy mode has - it can be used with extensions
that have a class as their base type, and it will not cause code to break if it
is expecting a particular concrete subtype.

The biggest problem with the factory mode is that it loses control over the
lifecycle of the extension object. It is mostly useful where the lifecycle can be
controlled in some other manner, otherwise you may not be able to release
references to stale components when their implementation bundle is stopped or be
able to instantiate the new implementation after the freshly-installed bundle is
started.

### When to use each mode

Generally speaking, you will most likely get smoother results using the proxy
mode when you can, and use the factory mode as a fallback if you run into one of
the proxy mode's limitations.

There also exists the possibility of extensions where neither approach will work
properly - eg, a client of an extension that has a class as its base type, but
the client also hangs on to references in a static variable. Unfortunately in 
this case you'll have to abandon the use of the `ExtensionFacade` altogether.

### How to use

1. Register your extension in the `plugin.xml` of a bundle that won't be
restarted. For Bndtools, the best choice is probably `bndtools.core`.
   1. The `id` attribute of the extension point element must match the
   `component.name` property of the backing component.
   2. In the attribute where you normally put the fqn of your implementation class,
   instead put the fqn of the facade, ie `org.bndtools.facade.ExtensionFacade`.
   This name of this attribute depends on the extension point, that you're
   implementing, but it's usually `class` or `delegate`.
   3. If you are using the proxy mode, add a ':' and then the fqn of the
   extension base type to the `ExtensionFacade` attribute.
```
   		<launchConfigurationType
			id="bndtools.launch.OSGiRunLaunchDelegate"
			delegate="org.bndtools.facade.ExtensionFacade:org.eclipse.debug.core.model.ILaunchConfigurationDelegate2"
			...
```
2. Implement your extension in your implementation bundle. The implementation
bundle should not register any extensions in its `plugin.xml` otherwise you
will not be able to restart it.
   1. Create a Declarative Services component that implements/extends the base
   type of the extension that you are implementing.
   2. Ensure that the `component.name` property is the same as the `id`
   field in the `plugin.xml` of the plugin that registers the extension (set
   in step 1.i). By default, this is the fqn of the implementation class.
   3. Set the correct component scope. Usually the component will need to be
   prototype scope, otherwise the implementation instances will interfere with
   each other - however, if your component implementation is stateless, then you
   can use default scope and all of the extension objects will share the same
   backend implementation.

That's all that is needed. With this setup, you can restart your implementation
bundle dynamically without breaking your running Eclipse workbench.

#### Programmatic instantiation of extensions

Sometimes in your Eclipse plugin code you will want to directly instantiate a
component implementation programmatically. However, this creates a hard dependency
from your plugin on implementation bundle, so that you can't restart the implementation bundle
unless your plugin also restarts. If your plugin is designed to be able to
restart dynamically then this will not be a problem, but if it registers its
own Eclipse plugins then it will prevent you from restarting your implementation
bundle.

A way around this is to use the `ExtensionFacade` programmatically to
reference the extension object. An example is in
`bndtools.editor.pages.RunAction`. As a nice additional feature, you
can then programmatically register callbacks on the facade to be notified when
a new backing component arrives or is closed. `RunAction` uses this feature
to enable/disable itself depending on whether or not the backing component is
present.

## Examples of the `ExtensionFacade` in action

The prototype example of how to use the `ExtensionFacade` is in the 
`org.bndtools.launch` bundle, which contains the launch-related code
for Bndtools. This bundle was used as a proving ground for the
`ExtensionFacade`'s initial development. The extensions are registered in
`bndtools.core`'s `plugin.xml`, using the `ExtensionFacade`
configured as described above.
