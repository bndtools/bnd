---
layout: default
class: Builder
title: -wab FILE ( ',' FILE )*
summary: Create a Web Archive Bundle (WAB) or a WAR
---
In OSGi Enterprise 4.2 the concept of Web Archive Bundles were introduced. Web Archive Bundles are 100% normal bundles following all the rules of OSGi. Their speciality is that they can be mapped to a web server following several of the rules of Java Enterprise Edition's Servlet model. The big difference is that the WARs of the servlet model have a rather strict layout of their archive because the servlet container also handles class loading. In OSGi, the class loading is very well specified and it would therefore be wrong to create special rules.

However, the OSGi supports the Bundle-Classpath header. This header allows the organization of the internal layout. It turns out that it is possible to create a valid Web Application Bundle (WAB) that is also a valid Web ARchive (WAR). Being deploy an archive both to OSGi and an application server obviously has advantages. bnd therefore supports a number of instructions that make it easy to create these dual mode archives.

The `-wab` instruction instructs bnd to move the root of the created archive to WEB-INF/classes. That is, you build your bundle in the normal way,not using the `Bundle-ClassPath`. The `-wab` command then moves the root of the archive so the complete class path for the bundle is no inside the WEB-INF/classes directory. It then adjusts the `Bundle-ClassPath` header to reflect this new location of the classes and resources.

The new root now only contains WEB-INF. In the Servlet specification, the root of the archive is mapped to the server's context URL. It is therefore often necessary to place static files in the root. For this reason, the `-wab` instruction has the same form as [Include-Resource](includeresource.html) header, and performs the same function. However, it performs this function of copying resources from the file system after the classes and resources of the original bundle have been moved to WEB-INF.

For example, the following code creates a simple WAB/WAR:

```
  Private-Package:   com.example.impl.*
  Export-Package:    com.example.service.myapi
  Include-Resource:  resources/
  -wab:              static-pages/
```

The layout of the resulting archive is:

```
  WEB-INF/classes/com/example/impl/
  WEB-INF/classes/com/example/service/myapi/
  WEB-INF/classes/resources/
  index.html // from static-pages
```

The `Bundle-ClassPath` is `WEB-INF/classes`.

WARs can carry a `WEB-INF/lib` directory. Any archive in this directory is mapped to the class path of the WAR. The OSGi specifications do not recognize directories with archives it is therefore necessary to list these archives also on the `Bundle-ClassPath` header. This is cumbersome to do by hand so the `-wablib` command will take a list of paths. 

```
  Private-Package:   com.example.impl.*
  Export-Package:    com.example.service.myapi
  Include-Resource:  resources/
  -wab:              static-pages/
  -wablib:			 lib/a.jar, lib/b.jar
```

This results in a layout of:

```
  WEB-INF/classes/com/example/impl/
  WEB-INF/classes/com/example/service/myapi/
  WEB-INF/classes/resources/
  WEB-INF/lib/
    a.jar
    b.jar
  index.html ( from static-pages)
```

The `Bundle-ClassPath` is now set to `WEB-INF/classes,WEB-INF/lib/a.jar,WEB-INF/lib/a.jar`

```java
	/**
	 * Turn this normal bundle in a web and add any resources.
	 *
	 * @throws Exception
	 */
	private Jar doWab(Jar dot) throws Exception {
		String wab = getProperty(WAB);
		String wablib = getProperty(WABLIB);
		if (wab == null && wablib == null)
			return dot;

		trace("wab %s %s", wab, wablib);
		setBundleClasspath(append("WEB-INF/classes", getProperty(BUNDLE_CLASSPATH)));

		Set<String> paths = new HashSet<String>(dot.getResources().keySet());

		for (String path : paths) {
			if (path.indexOf('/') > 0 && !Character.isUpperCase(path.charAt(0))) {
				trace("wab: moving: %s", path);
				dot.rename(path, "WEB-INF/classes/" + path);
			}
		}

		Parameters clauses = parseHeader(getProperty(WABLIB));
		for (String key : clauses.keySet()) {
			File f = getFile(key);
			addWabLib(dot, f);
		}
		doIncludeResource(dot, wab);
		return dot;
	}
```
