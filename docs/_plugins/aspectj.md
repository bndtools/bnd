---
title: AspectJ Plugin
layout: default
summary: Weave an executable JAR during compile time (experimental)
---

Aspects make it possible to centralize _cross cutting concerns_. 

The biz.aQute.aspectj.plugin.AspectJ plugin allows you to weave an executable JAR during export. It
provides a new export type:

    bnd.executablejar.aspectj
    
This export will use the bnd default export type to create an executable JAR. It will then weave
all the bundles inside this executable jar according to some _aspect bundles_. An aspect bundle
is created with either the [ajc compiler][1] or uses the annotations based definition of AspectJ code.

Any aspect bundles are compiled to convert the annotations to actual code and added to the
dependencies. 

The export function has the following arguments:

* `match` – A globbing expression on the bundle names to weave. The expression matches if the glob can be 
  _found_. I.e. it is not necessary to full match since it is applied to the path inside the bundle. The default
  is all bundles (*).
* `aspectpath` – List of bundle symbolic names of aspect bundles that need to be applied
* `ajc` – A comma separated list of ajc options

The ajc is invoked with the same `javac` source and target as defined in the workspace.

## Example

To enable the AspectJ plugin, add a plugin in the worksapce `build.bnd` file. (There is a context
menu entry for this.)

    -plugins \
        ...., \
        aQute.bnd.aspectj.plugin.AspectJ

The following code defines an aspect with the annotation model in a bundle called 'aspect':
```

    @Aspect
    public class AspectHandler {
        @Before("execution(void *.start(org.osgi.framework.BundleContext))")
        public void myadvice(JoinPoint jp) {
            BundleContext c = (BundleContext) jp.getArgs()[0];
            System.out.println("Starting bundle : " + c.getBundle());
        }
    }
```
The cutpoint will print out a message on each invocation of a Bundle Activator start method.

To test this create a `test.bndrun` in an export project:

```
-runfw: org.apache.felix.framework;version='[6.0.2,6.0.2]'
-runee: JavaSE-1.8
-runrequires: \
    bnd.identity;id='exporter',\
    bnd.identity;id='org.apache.felix.gogo.command',\
    bnd.identity;id='org.apache.felix.gogo.runtime',\
    bnd.identity;id='org.apache.felix.gogo.shell'
```
You can then resolve it and run it to see if the shell works.

Next is to export the `test.bndrun` in the `bnd.bnd` file of a project:

```
-export \
    test.bndrun; \
        type            = bnd.executablejar.aspectj; \
        match           = *; \
        aspectpath      = aspect
```
This will export the the executable JAR while it applies the bundle `aspect` to all bundles.     


## Limitations

The Aspectj compiler is trying hard to crush the fences of modularity. Currently the bundles are woven one by one, which means 
some dependencies are not visible to them. The plugin needs some experience and feedback to grow. However, it already seems
quite usable in its current incarnation.

One issue is the 'compiling' of the annotations that happens on the `-aspectpath`. This means that the 'compiled' (not
woven) aspects must be in the output. The rules are not always completely clear and we're working on making this
work in all OSGi scenarios. Please provide feedback.


[1]: https://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html