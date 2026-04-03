---
layout: default
class: Builder
title: -noproxyinterfaces BOOLEAN
since: 7.2.0
---

Normally, Bnd examines the method bodies of classes looking for the instruction sequence:

```
Proxy.newProxyInstance(ClassLoader, Class[], InvocationHandler)
```

When Bnd detects a call to `Proxy.newProxyInstance`, it inspects the `Class[]` argument — for example, `new Class[] { SomeInterface.class, ... }` — and treats all listed interfaces as if they were implemented by the class. This means that any types referenced in the parameters or return types of those interfaces must also be imported.

## Example

Consider the following code:

```java
return (ServletContext) Proxy.newProxyInstance(
    curClassLoader, 
    new Class[] { ServletContext.class }, 
    new AdaptorInvocationHandler()
);
```

Bnd will automatically add the package `javax.servlet.descriptor` to the `Import-Package` header, because the detected `ServletContext` interface declares the method `JspConfigDescriptor getJspConfigDescriptor()`.

## Limitations

Interface detection only occurs when the `Class[]` array is created inline (that is, using the `anewarray` bytecode pattern shown above).
If the array is obtained from a field, local variable, or method parameter, Bnd cannot reliably determine its contents from the bytecode, and therefore no interfaces will be detected in those cases.


## Disabling Proxy Interface Detection

The `-noproxyinterfaces` instruction can be used to tell Bnd **not** to search method bodies for calls to `Proxy.newProxyInstance`.

For example:

```
-noproxyinterfaces: true
```