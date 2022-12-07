This is an example bundle that provides a Declarative Services component, using
the standard DS annotations from OSGi Release 5 (DS 1.2).

Points of interest:

* The component class is annotated with @Component from the standard DS
annotations. The -dsannotations instructions in bnd.bnd is necessary to tell
bnd to scan for annotated classes.

* Because the component implements an interface that is annotated
@ProviderType, bnd will generate a version range of [1.0,1.1) on the
Import-Package. This is the correct import range for the provider role.
