# Bndtools Development: Tips and Tricks

Here are some tips for developing in bnd / bndtools.

## Unit Tests


### Soft Assertions

We often use `org.assertj.core.api.SoftAssertions` (in combination with the JUnit `SoftAssertionsExtension`) in contrast to "hard" assertions (as in org.`assertj.core.api.Assertions.assertThat()`). 

> With soft assertions AssertJ collects all assertion errors instead of stopping at the first one.
> 	This is especially useful for long tests like end to end tests as we can fix all reported errors at once and avoid multiple failing runs.

See https://assertj.github.io/doc/#assertj-core-soft-assertions for more details.

You can use them on class level or method level.

#### Class level

```java
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
public class MyTest {

	@InjectSoftAssertions
	SoftAssertions softly;

	@Test
	public void test1() {
		softly.assertThat("foo bar")
			.contains("bar")
			.doesNotContain("baz");
	}
}
```

#### Method level

```java
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
public class MyTest {

	@Test
	public void test1(SoftAssertions softly) {
		softly.assertThat("foo bar")
			.contains("bar")
			.doesNotContain("baz");
	}
}
```

Note that the method parameter does not need the annotation.
Use that, if you just have very few test methods need SoftAssertions.

#### Assertions on collections

You get getter diagnostics from AssertJ if you assert directly on the collection, rather than retrieving the property (e.g. `size()`) or element and asserting on the property/element. This is because when it is operating directly on the collection AssertJ has more information about what you're expecting and can provide more debugging information about what was actually there.

**Example:**

For example, suppose that you want to check a list of warnings containing three warnings: `"blah", "foo", "snee"`.
And let's say, you are expecting `bar`.

```java
List<String> warnings = List.of("blah", "foo", "snee");
List<String> errors = List.of();

// ...some setup of the processor, which produces some warnings, but no errors
softly.assertThat(warnings).as("i expect warnings").containsExactly("bar"); 
softly.assertThat(errors).as("errors").isEmpty();
```

You would get an error like this if `bar` is not contained:

```
Multiple Failures (1 failure)
-- failure 1 --
[i expect warnings] 
Expecting actual:
  ["blah", "foo", "snee"]
to contain exactly (and in same order):
  ["bar"]
but some elements were not found:
  ["bar"]
and others were not expected:
  ["blah", "foo", "snee"]
```

Also, when using soft assertions, it's good to use `as()` (gives the assertion a descriptive name), because this will be included in the diagnostic and when there are multiple failures it's easier to see at a glace which failures apply.

Note: Similar methods also exist for `Iterable`, `Map` and `Properties`.

#### Limits of soft assertions

There are limits on the applicability of soft assertions. They work well when each assertion does not depend on prior assertions to be successful. Such as making a variety of checks on the contents of a collection. But when later assertions will not work when a previous assertion fails, then the previous assertion should not be soft. So sometimes a test method will need to use a mixture of "hard" assertions and then soft assertions.

But sometimes also that mix of "hard" and "soft" assertions needs care:
The problem with mixing hard assertions is that, if it fails, none of the soft assertion results are reported.

Another way around this is to use `softly.wasSuccess()`, which returns `false` if the previous soft assertion failed. You can use this to conditionally execute the dependent assertions.

## Temporary folders

In tests you often need a temporary folder to put files for your testcases. To make working with temp folders easier (so you do not forget to delete the folder after your test), we have a `@InjectTemporaryDirectory` annotation which helps with that.

You can put it on class level or in a parameter in a method. Each test method automatically has a distinct folder given by this annotation.

### Class level

```java
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import org.junit.jupiter.api.Test;

public class MyTest {

	@InjectTemporaryDirectory
	File tmp;

	@Test
	public void test1() {}

}
```


### Method level:

```java
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import org.junit.jupiter.api.Test;

public class MyTest {

	@Test
	public void test1(@InjectTemporaryDirectory
	File tmp) {}
}

```

Use that, if you just have very few test methods needing temp folders.



## Adding Error/Warning Markers

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


## Using the `ExtensionFacade`

### What is the `ExtensionFacade`?

`org.bndtools.facade.ExtensionFacade` was developed by the core Bndtools
team as part of the `bndtools.core` project. It acts as a bridge between the
Eclipse extension registry and OSGi Declarative Services components. Thus it
allows you to deploy Eclipse extensions in your plugins that are implemented as
OSGi Declarative Services components.

### Why use the `ExtensionFacade`?

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

### How to use the `ExtensionFacade`

#### Modes of operation
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

##### Full proxy mode

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

##### Factory mode

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

#### When to use each mode

Generally speaking, you will most likely get smoother results using the proxy
mode when you can, and use the factory mode as a fallback if you run into one of
the proxy mode's limitations.

There also exists the possibility of extensions where neither approach will work
properly - eg, a client of an extension that has a class as its base type, but
the client also hangs on to references in a static variable. Unfortunately in 
this case you'll have to abandon the use of the `ExtensionFacade` altogether.

#### How to use

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
   in step 1.i). By default, this is the fqn of the implementation class. (Note:
   there are some cases where you might need the `id` to be different from
   the `component.name` - in such cases, see the section "Overriding the
   `component.name`" below.)
   3. Set the correct component scope. Usually the component will need to be
   prototype scope, otherwise the implementation instances will interfere with
   each other - however, if your component implementation is stateless, then you
   can use default scope and all of the extension objects will share the same
   backend implementation.

That's all that is needed. With this setup, you can restart your implementation
bundle dynamically without breaking your running Eclipse workbench.

##### Programmatic instantiation of extensions

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

### Overriding the `component.name`

By default, the `ExtensionFacade` will use the extension element's `id` attribute to
look for a DS component with a `component.name` that matches. However, sometimes you
want two extensions that point to the same DS component. Because having extensions with
duplicate ids is not a good idea, the `ExtensionFacade` provides an alternative method
to specify the `component.name` to match: by adding `:<component.name>` to the end
of the class.

If you need to combine the factory mode with this feature, leave the name of the class
blank (eg, `class="org.bndtools.facade.ExtensionFacade::my.component.name"`).

### Examples of the `ExtensionFacade` in action

The prototype example of how to use the `ExtensionFacade` is in the 
`org.bndtools.launch` bundle, which contains the launch-related code
for Bndtools. This bundle was used as a proving ground for the
`ExtensionFacade`'s initial development. The extensions are registered in
`bndtools.core` and `bndtools.m2e`'s `plugin.xml` files, using the
`ExtensionFacade` configured as described above.


## Generating JDK properties files

The files like `JavaSE_17.properties` (read by `EE.init()` (see [EE.java](EE.java))) 
contain all java packages provided by a given JDK and are generated using a CLI tool https://github.com/bjhargrave/java-platform-packages

To make it a bit easier to run the tool to generate the files for new JDKs the following section will give some options.

### Helper Scripts

The scripts below are also available under https://github.com/chrisrueger/jdk-packages

Let's say there is a new JDK 21 and you want to create a `JavaSE_21.properties` for it.
Checkout this repo and run the following commands inside the repo:

```
./download_jdk.sh 21 mac aarch64

./list_jdk_packages_for_bnd.sh 21 mac aarch64 > "JavaSE_21.properties"
```

This will create a `JavaSE_21.properties`, which you can then copy to this folder here ([biz.aQute.bndlib/src/aQute/bnd/build/model/](.)).

In case this repository above is not available anymore you can also create the following scripts yourself.


#### Script for generating the properties

This script generates the .properties file for a specific JDK.
It requires that the other script for downloading the JDK has run before (see below).

- *filename:* `list_jdk_packages_for_bnd.sh`
- *Example:* `./list_jdk_packages_for_bnd.sh 21 mac aarch64 > "JavaSE_21.properties"`

```shell

#!/bin/bash

# This script is specific for [bnd / bndtools](https://github.com/bndtools/bnd). 
# It is basically a CLI wrapper around https://github.com/bjhargrave/java-platform-packages

# Check if all required parameters are provided
if [ $# -ne 3 ]; then
    echo "Usage: $0 <JDK_VERSION> <OS> <ARCH>"
    echo "Example: $0 17 linux x64"
    echo "         $0 17 mac aarch64"
    echo "         $0 17 windows x64"
    exit 1
fi

# Mandatory parameters
version="$1"   # JDK Version (e.g., 17 for JDK 17)
OS="$2"        # OS Type (e.g., "linux", "mac", "windows")
ARCH="$3"      # Architecture (e.g., "x64", "aarch64" for Mac M1)

# Directory where JDKs are stored
JDK_DIR="./jdks-$OS-$ARCH/jdk-$version"

JAVA_SRC_DIR="javasrc_temp/io/hargrave/java/packages"
mkdir -p "$JAVA_SRC_DIR"
curl -L "https://raw.githubusercontent.com/bjhargrave/java-platform-packages/refs/heads/master/src/main/java/io/hargrave/java/packages/CalculateJavaPlatformPackages.java" -o "$JAVA_SRC_DIR/CalculateJavaPlatformPackages.java"

if [ -d "$JDK_DIR" ]; then
    if [ -x "$JDK_DIR/bin/java" ]; then
        JAVA_CMD="$JDK_DIR/bin/java"  # Linux JDK path
        JAVAC_CMD="$JDK_DIR/bin/javac"  
    elif [ -x "$JDK_DIR/Contents/Home/bin/java" ]; then
        JAVA_CMD="$JDK_DIR/Contents/Home/bin/java"  # macOS JDK path
        JAVAC_CMD="$JDK_DIR/Contents/Home/bin/javac"  
    else
        echo "Error: No Java executable found in $JDK_DIR"
        exit 1
    fi

    # Ensure the Java executable exists
    if [ -x "$JAVA_CMD" ]; then
        # Run the module listing command directly with this JDK
        $JAVAC_CMD $JAVA_SRC_DIR/CalculateJavaPlatformPackages.java
        $JAVA_CMD -cp javasrc_temp io.hargrave.java.packages.CalculateJavaPlatformPackages
    else
        echo "Error: No Java executable found in $JDK_DIR"
    fi

    echo
fi
```

#### Script for downloading the JDK

- *filename:* `download_jdk.sh`
- *Example:* `./download_jdk.sh 21 mac aarch64`

```shell

#!/bin/bash

# Check if all required parameters are provided
if [ $# -ne 3 ]; then
    echo "Usage: $0 <JDK_VERSION> <OS> <ARCH>"
    echo "Example: $0 17 linux x64"
    echo "         $0 17 mac aarch64"
    echo "         $0 17 windows x64"
    exit 1
fi

# Mandatory parameters
version="$1"   # JDK Version (e.g., 17 for JDK 17)
OS="$2"        # OS Type (e.g., "linux", "mac", "windows")
ARCH="$3"      # Architecture (e.g., "x64", "aarch64" for Mac M1)

# Directory to store JDKs
JDK_DIR="./jdks-$OS-$ARCH"
mkdir -p "$JDK_DIR"

# Adoptium base URL (update this if needed)
ADOPTIUM_URL="https://api.adoptium.net/v3/binary/latest"


# Download and extract each JDK
echo "Downloading JDK $version..."

# Construct the download URL
JDK_FILE="$JDK_DIR/jdk-$version.tar.gz"
JDK_EXTRACT_DIR="$JDK_DIR/jdk-$version"

curl -L "$ADOPTIUM_URL/$version/ga/$OS/$ARCH/jdk/hotspot/normal/eclipse?project=jdk" -o "$JDK_FILE"

echo "Extracting JDK $version..."
mkdir -p "$JDK_EXTRACT_DIR"
tar -xzf "$JDK_FILE" --strip-components=1 -C "$JDK_EXTRACT_DIR"

rm "$JDK_FILE"  # Clean up downloaded archive

echo "All JDKs downloaded and extracted!"
```


## Macro Documentation Testing

### Overview

The test `/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java` is a comprehensive test suite for validating macro documentation examples in `docs/_macros/*.md` files.

### Purpose

The goal of test suite is that:
1. All macro examples in documentation actually work
2. Expected outputs match actual macro behavior
3. Syntax errors in documentation are caught
4. Users can rely on documentation examples


### Test Structure

Each test follows this pattern:

```java
// ===== filename.md =====
@Test
public void testMacroName() throws IOException {
    try (Processor p = new Processor()) {
        String result = p.getReplacer().process("${macro;args}");
        assertThat(result).isEqualTo("expected");
    }
}
```

The comment indicates which documentation file the test validates.

### Running Tests

```bash
# Run all macro documentation tests
./gradlew :biz.aQute.bndlib.tests:test --tests "test.MacroTestsForDocsExamples"

# Run specific test
./gradlew :biz.aQute.bndlib.tests:test --tests "test.MacroTestsForDocsExamples.testAverage_SimpleList"

# Run with more detail
./gradlew :biz.aQute.bndlib.tests:test --tests "test.MacroTestsForDocsExamples" --info
```

### Macro Behavior Reference

#### Common Patterns:

**Boolean-like Returns**:
- `startswith`, `endswith`: Return original string (truthy) or empty (falsy), not "true"/"false"
- `def`: Returns property value, not boolean
- `is`: Returns "true" or "false"

**Numeric Returns**:
- `average`, `sum`: Strip `.0` from whole numbers (3 not 3.0)
- `rand`: Returns integer
- `random`: Returns string identifier (different macro!)

**List Operations**:
- Most combine multiple list arguments first
- Zero-based indexing
- Negative indices count from end

**File Operations**:
- Often require absolute paths
- Platform-specific behavior (Windows vs Unix)
- Path separator differences

### Contributing Macro Documentation and Examples

If you create a new macro then add a new file in `docs/_macros/*.md` with the filename being the name of the macro.

When adding examples to documentation (`docs/_macros/*.md` files):
1. Add corresponding test to this file
2. Run test to verify example works
3. Include comment indicating which .md file
4. Test both success and failure cases where applicable

## See Also

- `MacroTest.java` - Original comprehensive macro tests
- `docs/_macros/` - Macro documentation files
- `biz.aQute.bndlib/src/aQute/bnd/osgi/Macro.java` - Macro implementations

Macros are in the following classes, recognized by methods starting with an underscore:

- `biz.aQute.bndlib/src/aQute/bnd/build/Project.java`
- `biz.aQute.bndlib/src/aQute/bnd/osgi/Analyzer.java`
- `biz.aQute.bndlib/src/aQute/bnd/osgi/Builder.java`
- `biz.aQute.bndlib/src/aQute/bnd/build/Workspace.java`
