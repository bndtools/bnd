Bndtools Development: Tips and Tricks
=====================================

Adding Error/Warning Markers
----------------------------

Bndtools aims to be a thin wrapper over bnd. This means that it is comparatively rare that bndtools 
should be creating errors or warnings. In most cases bnd is responsible for generating errors at
build time, it's then bndtools job to display them nicely to the user.

Step 1 - Customise the bnd error
--------------------------------

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


Step 2 - Create a BuildErrorHandler
-----------------------------------

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
    
    
Step 3 - Hook in to the Eclipse plugin registry
-----------------------------------------------

Bndtools uses the Eclipse plugin registry to discover BuildErrorDetailsHandler instances. To hook
in to this you need to add the following to your plugin.xml


    <extension point="bndtools.core.buildErrorDetailsHandlers">
      <handler typeMatch="org.bndtools.example.MyCustomizedLocationObject" 
          class="org.bndtools.example.handler.MyCustomBuildErrorDetailsHandler" />
    </extension>
    
    
Step 4 - You're done!
---------------------

Error markers will now appear in the right places. More work can be done to add quick fixes, but
it's much harder than adding the markers.
