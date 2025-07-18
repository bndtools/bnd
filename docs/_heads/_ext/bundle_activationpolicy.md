---
layout: default
class: Header
title: Bundle-ActivationPolicy ::= policy ( ';' directive )*
summary: The Bundle-ActivationPolicy specifies how the framework should activate the bundle once started.
---

# Bundle-ActivationPolicy

The `Bundle-ActivationPolicy` header specifies how the OSGi framework should activate the bundle once it has been started. The most common value for this header is `lazy`, which indicates that the bundle should be started in lazy activation mode. In this mode, the bundle's activator is not called until the first class from the bundle is loaded.

This header can also include additional directives, such as `include` and `exclude`, to further control activation behavior. The header is typically used as follows:

```
Bundle-ActivationPolicy: lazy
```

For more details, see the [OSGi Specification](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.lifecycle.html#i3270439).

If the header is set incorrectly (for example, with no argument or with too many arguments), bnd will issue a warning. The value should be set to `lazy` for standard lazy activation.


---
TODO Needs review - AI Generated content