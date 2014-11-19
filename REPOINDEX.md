Introduction
============

RepoIndex program is a small Java program that generates repository indexes compliant with the Repository Service Specification version 1.0, as defined in the OSGi Service Platform Service Compendium, Release 5. It can recurse over a directory structure generates a repository.xml file. The URLs can be rewritten using a template.

RepoIndex is a command line application that can easily be integrated in scripts. It is also an OSGi bundle that publishes a service under the `ResourceIndexer` interface, and a standalone library that can be used in conventional Java runtimes. While primarily intended for indexing OSGi bundles, it can generate metadata for any arbitrary file type by extending it with pluggable `ResourceAnalyzer` objects.

Files
=====

RepoIndex is shipped as two alternative forms:

* `org.osgi.impl.bundle.repoindex.lib` is a pure library and OSGi bundle. Use this if you want to embed index generation functionality into an existing application. See sections "Library Usage" and "OSGi Bundle Usage" below.

* `org.osgi.impl.bundle.repoindex.cli` is a standalone command-line application. See section "Command Line Usage" below.

* `org.osgi.impl.bundle.repoindex.ant` is a convenience library for ANT. See section "Ant Task Usage" below.

**N.B.:** The `cli` and `ant` libraries do not depend on `lib`.

Command Line Usage
==================

The basic command line usage is as follows:

	java -jar org.osgi.impl.bundle.repoindex.cli.jar bundles/*.jar

This generates an index file in the local directory named `index.xml.gz` with metadata for all JAR files found under the `bundles` directory. The content URLs for the resources will be relative to the current directory, i.e.:

	<attribute name='osgi.content' value='bundles/foo.jar'/>

The full set of command line options can be obtained by executing with the `-h` option.

If custom resource analyzers are required (see below), these can be simply placed on the Java runtime classpath. In this case the `java -jar` launch method cannot be used, so it is necessary to launch with the application class name as follows:

	java -cp org.osgi.impl.bundle.repoindex.cli.jar;MyAnalyzer.jar \
	     org.osgi.impl.bundle.bindex.cli.Index \
	     bundles/*.jar

Ant Task Usage
==================

RepoIndex can be used as an Ant Task by adding a taskdef using the `org.osgi.impl.bundle.repoindex.ant.jar` library:

	<taskdef name="repoindex" classname="org.osgi.impl.bundle.repoindex.ant.RepoIndexTask" >
		<classpath>
			<path location="../cnf/path/to/org.osgi.impl.bundle.repoindex.ant.jar" />
		</classpath>
	</taskdef>

If custom resource analyzers are required (see below), these can be simply added to the classpath.

The available options are configured through the tasks attributes and the resources are specified using one or more filesets.

	<target name="generate-index">
		<repoindex name="My Repository" verbose="false" pretty="true" 
						compressed="false" out="${repository.dir}/index.xml">
			<fileset dir="${repository.dir}" includes="**/*.jar" />
		</repoindex>
	</target>
	

Library Usage
=============

RepoIndex can be used as a JAR library in a conventional Java application or web/JavaEE container by adding `org.osgi.impl.bundle.repoindex.lib.jar` to your application classpath. The API is as follows:

	RepoIndex indexer = new RepoIndex();
	// optional: add one or more custom resource analyzers
	indexer.add(new MyExtenderResourceAnalyzer(), null);

	// optional: set config params
	Map<String, String> config = new HashMap<String, String>();
	config.put(ResourceIndexer.REPOSITORY_NAME, "My Repository");

	Set<File> inputs = findInputs();
	OutputStream output = new FileOutputStream("index.xml.gz");
	indexer.index(inputs, output, config);

Note that it is not generally encouraged for client code to directly instantiate `RepoIndex` as shown, since this creates an implementation dependency. It is better to use the `ResourceIndexer` API interface along with a Dependency Injection framework such as Guice or Spring to supply the instance. Even better, use `ResourceIndexer` as an OSGi Service as shown in the next section.

Resource analyzers are added with an optional Filter, which is matched against incoming resources as they are processed. If the filter is `null` then the analyzer is invoked for all incoming resources. Filters are generated using the `org.osgi.framework.FrameworkUtil.createFilter()` method, for example:
	
	import static org.osgi.framework.FrameworkUtil.createFilter;
	// ...
	Filter warFilter = createFilter("(name=*.war)");
	indexer.add(new WarAnalyzer(), warFilter);

For more information on the filter string syntax and the properties available to match, see "Resource Analyzers" below.

OSGi Bundle Usage
=================

`org.osgi.impl.bundle.repoindex.lib` is also an OSGi bundle that publishes a service under the interface `org.osgi.service.indexer.ResourceIndexer` when in ACTIVE state. For example, to use the `ResourceIndexer` service from a Declarative Services component:

	@Reference
	public void setIndexer(ResourceIndexer indexer) {
		this.indexer = indexer;
	}
	
	public void doSomething() {
		// ...
		indexer.index(input, output, config);
	}

When used as an OSGi bundle, RepoIndex uses the "Whiteboard Pattern" to find custom resource analyzers. The filter, if required, can be given as a property of the service. For example to register an analyzer for WAR files, again using Declarative Services annotations:

	@Component(property = "filter=(name=*.war)")
	public class WarAnalyzer implements ResourceAnalyzer {
		// ...
	}

Resource Analyzers
==================

RepoIndex is expected to be used primarily for analyzing and indexing OSGi bundles. However it is designed to be extensible to analyze any other kind of resource, since the OSGi Repository specification supports arbitrary resource types. It can also be extended to extract additional metadata from existing known types such as OSGi bundles.

For example, we may wish to extend RepoIndex to understand configuration files, script files, or native libraries. Alternatively we may wish to process custom extender headers from the MANIFEST.MF of OSGi bundles.

The `ResourceAnalyzer` interfaces defines a single method `analyzeResource`:

	public interface ResourceAnalyzer {
		static final String FILTER = "filter";
		void analyzeResource(Resource resource,
				List<Capability> capabilities,
				List<Requirement> requirements) throws Exception;
	}

The `analyzeResource` method takes a `Resource` object and the lists of already discovered Requirements and Capabilities. An analyzer is permitted to add zero to many of each but it must not remove or alter any existing entries. A `Builder` class is provided as a convenience for constructing instances of Capability and Requirement.

The `Resource` interface is an abstraction over the types of resource that may be supplied to the analyzer. Analyzer implementations are not expected to implement `Resource`. The abridged interface definition is as follows:

	public interface Resource {
		String getLocation();
		Dictionary<String, Object> getProperties();
		long getSize();
		InputStream getStream() throws IOException;
		Manifest getManifest() throws IOException;
		List<String> listChildren(String prefix) throws IOException;
		Resource getChild(String path) throws IOException;
		void close();
	}

We expect that in the majority of cases the resource will be an OSGi bundle, and therefore a JAR file. Therefore we provide the `getManifest`, `listChildren` and `getChild` methods as optimisations, so that every analyzer does not need to re-parse the JAR contents. If the resource does not contain a `META-INF/MANIFEST.MF` then `getManifest` will return `null`. If the resource is not a "compound" resource such as a JAR or ZIP then `listChildren` and `getChild` will return `null` for all input values.

Access to the underlying content of the file is always available through the `getStream` method but this should be treated as an expensive operation that should only be called when the data required is not otherwise available from the properties, manifest or children.

Example Analyzer
----------------

The following example shows an analyzer for a custom extender header, `Help-Docs`. If the analyzer finds this header in the bundle manifest, it generates both a capability and a requirement:

	public class HelpExtenderAnalyzer {
		public void analyzeResource(Resource resource,
									List<Capability> caps,
									List<Requirement> reqs) {

			// Ignore this resource if no META-INF/MANIFEST.MF
			// or Help-Docs header
			Manifest manifest = resource.getManifest();
			if (manifest == null)
				return;
			String help = manifest.getMainAttributes().getValue("Help-Docs");
			if (help == null)
				return;
			
			// Generate a new requirement
			Requirement req = new Builder()
				.setNamespace("help.system")
				.addDirective("filter", "(&(version>=1.0)(!(version>=2.0)))")
				.buildRequirement();
			reqs.add(req);
			
			// Generate a new capability
			String bsn = manifest.getMainAttributes()
								 .getValue(Constants.BUNDLE_SYMBOLICNAME);
			Capability cap = new Builder()
				.setNamespace("help.doc")
				.addAttribute("version", new Version("1.0"))
				.addAttribute("bsn", bsn);
			caps.add(cap);
		}
	}

Resource Properties and Filters
-------------------------------

The `getProperties` method on `Resource` returns a dictionary of properties for the resource. These properties can be used to define filters so that an analyzer can be invoked only for a subset of interesting resources. The property names are defined as static constants on the `Resource` interface:

* `name`: the simple name of the resource, i.e. usually the filename excluding its location path;
* `location`: the full location of the resource (also available from the `getLocation` method);
* `size`: the size (as a `Long` value) of the resource in bytes (also available from the `getSize` method);
* `lastmodified`: the last-modified timestamp of the resource, as a `Long` value representing milliseconds since the epoch (00:00:00 GMT, January 1, 1970).

The filter expression syntax is described in section 3.2.7 of the OSGi Core Specification.

For example, to select files named either `foo.jar` or ending with the `.ear` extension, created on or after midnight on 1 January 2010:

	(&(|(name=foo.jar)(name=*.ear))(lastmodified>=1262217600753))

Packaging Resource Analyzers
----------------------------

Resource analyzers should be packaged for delivery as OSGi bundles that register services under the `ResourceAnalyzer` interface. The RepoIndex command-line application uses a lightweight OSGi-like runtime called [PojoSR](https://code.google.com/p/pojosr/) to configure services, and looks for resource analyzers using the whiteboard pattern.

Note that if an analyzer implementation relies on Declarative Service to manage and register it as a service, then it will be necessary to place a Declarative Service implementation (also known as a Service Component Runtime or SCR) on the RepoIndex classpath. For example the Apache Felix SCR can be used as follows:

	java -cp org.osgi.impl.bundle.repoindex.cli.jar;org.apache.felix.scr-1.6.0.jar;MyAnalyzer.jar \
			 org.osgi.impl.bundle.bindex.cli.Index \
			 bundles/*.jar
