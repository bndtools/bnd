package aQute.launchpad;

import static aQute.bnd.service.specifications.BuilderSpecification.PROJECT;
import static aQute.bnd.service.specifications.BuilderSpecification.WORKSPACE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Version;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.configurator.ConfiguratorConstants;

import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.regex.PatternConstants;
import aQute.lib.strings.Strings;

/**
 * Provides all builder functions. Specified as default methods so that we can
 * create sub builder interfaces that inherit all functions of the outer one.
 */
public interface BundleSpecBuilder {
	String	CONFIGURATION_JSON	= "OSGI-INF/configurator/configuration.json";
	Pattern	SYMBOLICNAME		= Pattern.compile(PatternConstants.SYMBOLICNAME);

	BundleBuilder x();

	/**
	 * Bundle-SymbolicName
	 */
	default BundleSpecBsn bundleSymbolicName(String name) {
		BundleBuilder x = x();
		if (name == null) {
			throw new IllegalArgumentException("bsn cannot be null");
		}
		if (!SYMBOLICNAME.matcher(name)
			.matches()) {
			throw new IllegalArgumentException("invalid bsn: " + name);
		}
		x.spec.bundleSymbolicName.clear();
		x.spec.bundleSymbolicName.put(name, new LinkedHashMap<>());

		return new BundleSpecBsn() {

			@Override
			public BundleBuilder x() {
				return x;
			}

			@Override
			public String bsn() {
				return name;
			}
		};
	}

	/**
	 * The values for a Fragment Attachment directive
	 */
	enum FragmentAttachment {
		always("always"),
		never("never"),
		resolve_time("resolve-time");

		String value;

		FragmentAttachment(String name) {
			this.value = name;
		}
	}

	/**
	 * Sub builder for Bundle Symbolic Name directives and attributes
	 */
	interface BundleSpecBsn extends BundleSpecBuilder {

		/**
		 * Make this bundle a singleton bundle
		 */
		default BundleSpecBsn singleton() {
			directive("singleton", "true");
			return this;
		}

		/**
		 * Set the bundle's fragment attachment policy
		 *
		 * @param attachment the attachment policy
		 */
		default BundleSpecBsn fragmentAttachment(FragmentAttachment attachment) {
			directive("fragment-attachment", attachment.value);
			return this;
		}

		/**
		 * Set the mandatory matching attributes
		 *
		 * @param attributes the matching attributes
		 */
		default BundleSpecBsn mandatory(String... attributes) {
			directive("mandatory", Stream.of(attributes)
				.collect(Strings.joining()));
			return this;
		}

		/**
		 * Bundle-SymbolicName attribute
		 *
		 * @param key the attribute name
		 * @param value the value
		 */
		default BundleSpecBsn attribute(String key, String value) {
			x().add("Bundle-SymbolicName", x().spec.bundleSymbolicName, bsn(), key, value);
			return this;
		}

		/**
		 * Bundle-SymbolicName directive
		 *
		 * @param key the directive name (no ':' at end)
		 * @param value the value
		 */
		default BundleSpecBsn directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}

		/*
		 * the actual bsn
		 */
		String bsn();

	}

	/**
	 * Bundle-Version
	 */
	default BundleSpecBuilder bundleVersion(String version) {
		if (x().spec.bundleVersion != null) {
			throw new IllegalArgumentException("Bundle-Version was already set to: " + x().spec.bundleVersion);
		}
		x().spec.bundleVersion = version;
		new Version(version);
		return this;
	}

	/**
	 * Bundle-Activator
	 */
	default BundleSpecBuilder bundleActivator(String bundleActivator) {
		if (x().spec.bundleActivator != null) {
			throw new IllegalArgumentException("Bundle-Activator was already set to: " + x().spec.bundleActivator);
		}
		x().spec.bundleActivator = bundleActivator;
		return this;
	}

	/**
	 * Bundle-Activator - Set the Bundle-Activator via a class. This will set
	 * the Bundle-Activator header and ensures the package of the given class is
	 * imported from the class path. This allows the caller to use inner classes
	 * as an activator. The class is also added as a resource so that it could
	 * potentially also be a DS component. However, this resource is only used
	 * by bnd since in runtime we will load the class from the classpath.
	 *
	 * @param bundleActivator the class that is intended to be the Bundle
	 *            Activator
	 */
	default BundleSpecBuilder bundleActivator(Class<? extends BundleActivator> bundleActivator) {
		this.bundleActivator(bundleActivator.getName());
		this.addResource(bundleActivator);
		return this;
	}

	/**
	 * Fragment-Host
	 */

	default BundleSpecFragmentHost fragmentHost(String name) {
		BundleBuilder x = x();
		x.spec.fragmentHost.put(name, new LinkedHashMap<>());

		if (x.spec.fragmentHost.size() > 1)
			throw new IllegalArgumentException(
				"Only one Fragment-Host can be specified. The previous one was " + x.spec.fragmentHost);

		return new BundleSpecFragmentHost() {

			/*
			 * access to the underlying builder
			 */
			@Override
			public BundleBuilder x() {
				return x;
			}

			/*
			 * access to the current fragmenthost
			 */
			@Override
			public String fragmenthost() {
				return name;
			}

		};
	}

	/**
	 * Extension options for the Fragment-Host
	 */
	enum Extension {
		framework,
		bootclasspath
	}

	interface BundleSpecFragmentHost extends BundleSpecBuilder {

		/**
		 * extension
		 *
		 * @param extension the actual extension type
		 */
		default BundleSpecBuilder extension(Extension extension) {
			directive("extension", extension.toString());
			return this;
		}

		/**
		 * The Fragment-Host bundle-version attribute
		 *
		 * @param versionRange the range of version for the fragment host
		 */
		@Override
		default BundleSpecFragmentHost bundleVersion(String versionRange) {
			attribute("bundle-version", versionRange);
			return this;
		}

		/**
		 * Fragment-Host attribute
		 *
		 * @param key the attribute name
		 * @param value the attribute value
		 */
		default BundleSpecFragmentHost attribute(String key, String value) {
			x().add("Fragment-Host", x().spec.fragmentHost, fragmenthost(), key, value);
			return this;
		}

		/**
		 * Fragment-Host directive
		 *
		 * @param key the directive name without the ':'
		 * @param value the directive value
		 */
		default BundleSpecFragmentHost directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}

		/*
		 * the currently set fragment host
		 */
		String fragmenthost();
	}

	/**
	 * Require-Bundle
	 */

	default BundleSpecBuilderRequireBundle requireBundle(String name) {
		BundleBuilder x = x();
		String actualName = x().prepare(name, x.spec.requireBundle);

		return new BundleSpecBuilderRequireBundle() {

			/*
			 * Access to the underlying builder
			 */
			@Override
			public BundleBuilder x() {
				return x;
			}

			/*
			 * Access to the current requirebundle statement
			 */
			@Override
			public String requirebundle() {
				return actualName;
			}
		};
	}

	/**
	 * Require-Bundle sub editor
	 */
	interface BundleSpecBuilderRequireBundle extends BundleSpecBuilder {

		/**
		 * Require-Bundle reexport directive
		 */
		default BundleSpecBuilderRequireBundle reepexort() {
			directive("visibility", "reexport");
			return this;
		}

		/**
		 * Require-Bundle optional directive
		 */
		default BundleSpecBuilderRequireBundle optional() {
			directive("resolution", "optional");
			return this;
		}

		/**
		 * Require-Bundle bundleVersion attribute
		 *
		 * @param versionRange the version range of the required bundles
		 */
		@Override
		default BundleSpecBuilderRequireBundle bundleVersion(String versionRange) {
			attribute("bundle-version", versionRange);
			return this;
		}

		/**
		 * Require-Bundle attribute
		 *
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 */
		default BundleSpecBuilderRequireBundle attribute(String key, String value) {
			x().add("Require-Bundle ", x().spec.requireBundle, requirebundle(), key, value);
			return this;
		}

		/**
		 * Require-Bundle directive
		 *
		 * @param key the name of the directive (without the ':')
		 * @param value the value of the directive
		 */
		default BundleSpecBuilderRequireBundle directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}

		/*
		 * The current require bundle bsn
		 */
		String requirebundle();
	}

	/**
	 * Import-Package
	 *
	 * @param name name of the package to be imported, may contain wildcards
	 */

	default BundleSpecImportPackage importPackage(String packageName) {
		BundleBuilder x = x();
		x.spec.importPackage.computeIfAbsent(packageName, k -> new LinkedHashMap<>());

		return new BundleSpecImportPackage() {

			/*
			 * Access tot the underlying builder
			 */
			@Override
			public BundleBuilder x() {
				return x;
			}

			/*
			 * Currently edited package name
			 */
			@Override
			public String packageName() {
				return packageName;
			}
		};
	}

	/**
	 * Sub interface for editing an Import-Package addition
	 */
	interface BundleSpecImportPackage extends BundleSpecBuilder {

		/**
		 * Set the resolution to optional
		 */
		default BundleSpecImportPackage optional() {
			directive("resolution", "optional");
			return this;
		}

		/**
		 * Set the package version attribute
		 *
		 * @param versionRange the version range of the required package
		 */
		default BundleSpecImportPackage version(String versionRange) {
			attribute("version", versionRange);
			return this;
		}

		/**
		 * Set the import from specific bundle attribute
		 *
		 * @param bundleSymbolicName the bundle to import the package from
		 */
		default BundleSpecImportPackage bundle_symbolic_name(String bundleSymbolicName) {
			attribute("bundle-symbolic-name", bundleSymbolicName);
			return this;
		}

		/**
		 * Set the import from specific bundle version attribute
		 *
		 * @param versionRange the version range of the required package
		 */
		default BundleSpecImportPackage bundle_version(String versionRange) {
			attribute("bundle-version", versionRange);
			return this;
		}

		/**
		 * The Import-Package attribute
		 *
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 */
		default BundleSpecImportPackage attribute(String key, String value) {
			x().add("Import-Package", x().spec.importPackage, packageName(), key, value);
			return this;
		}

		/**
		 * The Import-Package directive
		 *
		 * @param key the name of the directive (without the ':')
		 * @param value the value of the directive
		 */
		default BundleSpecImportPackage directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}

		/*
		 * Currently edited package name
		 */
		String packageName();

	}

	/**
	 * Export-Package
	 */

	default BundleSpecExportPackage exportPackage(String name) {
		BundleBuilder x = x();
		String packageName = x().prepare(name, x.spec.exportPackage);

		return new BundleSpecExportPackage() {

			/*
			 * (non-Javadoc)
			 * @see aQute.launchpad.BundleSpecBuilder#x()
			 */
			@Override
			public BundleBuilder x() {
				return x;
			}

			/*
			 * the currently exported package name
			 */
			@Override
			public String packageName() {
				return packageName;
			}
		};
	}

	/**
	 * The sub interface for editing an exported package
	 */
	interface BundleSpecExportPackage extends BundleSpecBuilder {

		/**
		 * Set the uses directive
		 *
		 * @param packageNames packages used by the current export package
		 */
		default BundleSpecExportPackage uses(String... packageNames) {
			directive("uses", x().join(packageNames));
			return this;
		}

		/**
		 * Set the mandatory attributes
		 *
		 * @param attributeNames the mandatory attribute names
		 */
		default BundleSpecExportPackage mandatory(String... attributeNames) {
			directive("mandatory", x().join(attributeNames));
			return this;
		}

		/**
		 * Set included classes
		 *
		 * @param classNames the included class names
		 */
		default BundleSpecExportPackage include(String... classNames) {
			directive("include", x().join(classNames));
			return this;
		}

		/**
		 * Set included classes
		 *
		 * @param classNames the included class names
		 */
		default BundleSpecExportPackage include(@SuppressWarnings("rawtypes") Class... classes) {
			return this.include(Stream.of(classes)
				.map(Class::getName)
				.toArray(String[]::new));
		}

		default BundleSpecExportPackage exclude(String... classNames) {
			directive("exclude", x().join(classNames));
			return this;
		}

		default BundleSpecExportPackage version(String version) {
			attribute("version", version);
			return this;
		}

		default BundleSpecExportPackage attribute(String key, String value) {
			x().add("Export-Package", x().spec.exportPackage, packageName(), key, value);
			return this;
		}

		default BundleSpecExportPackage directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}

		String packageName();

	}

	/**
	 * -exportcontents
	 */

	default BundleSpecExportContents exportContent(String name) {
		BundleBuilder x = x();
		String actualName = x().prepare(name, x.spec.exportContents);

		return new BundleSpecExportContents() {

			@Override
			public BundleBuilder x() {
				return x;
			}

			@Override
			public String packageName() {
				return actualName;
			}
		};
	}

	interface BundleSpecExportContents extends BundleSpecBuilder {
		String packageName();

		default BundleSpecExportContents uses(String... packageNames) {
			directive("uses", x().join(packageNames));
			return this;
		}

		default BundleSpecExportContents mandatory(String... attributeNames) {
			directive("mandatory", x().join(attributeNames));
			return this;
		}

		default BundleSpecExportContents include(String... classNames) {
			directive("include", x().join(classNames));
			return this;
		}

		default BundleSpecExportContents exclude(String... classNames) {
			directive("exclude", x().join(classNames));
			return this;
		}

		default BundleSpecExportContents version(String version) {
			attribute("version", version);
			return this;
		}

		default BundleSpecExportContents attribute(String key, String value) {
			x().add("-exportcontents", x().spec.exportPackage, packageName(), key, value);
			return this;
		}

		default BundleSpecExportContents directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}
	}

	/**
	 * -privatepackage
	 */

	default BundleSpecBuilder privatePackage(String name) {
		BundleBuilder x = x();
		x().prepare(name, x.spec.privatePackage);
		return this;
	}

	/**
	 * Provide-Capability
	 */

	default BundleSpecProvideCapability provideCapability(String namespace) {
		BundleBuilder x = x();
		String actualNamespace = x().prepare(namespace, x.spec.exportPackage);

		return new BundleSpecProvideCapability() {

			@Override
			public BundleBuilder x() {
				return x;
			}

			@Override
			public String namespace() {
				return actualNamespace;
			}
		};
	}

	interface BundleSpecProvideCapability extends BundleSpecBuilder {
		String namespace();

		default BundleSpecProvideCapability uses(String... packageNames) {
			directive("uses", x().join(packageNames));
			return this;
		}

		default BundleSpecProvideCapability effective(String effective) {
			directive("effective", effective);
			return this;
		}

		default BundleSpecProvideCapability version(String version) {
			attribute("version:Version", version);
			return this;
		}

		default BundleSpecProvideCapability primary(String name) {
			directive(namespace(), name);
			return this;
		}

		default BundleSpecProvideCapability attribute(String key, String value) {
			x().add("Provide-Capability", x().spec.provideCapability, namespace(), key, value);
			return this;
		}

		default BundleSpecProvideCapability directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}
	}

	/**
	 * Require-Capability
	 */

	default BundleSpecRequireCapability requireCapability(String namespace) {
		BundleBuilder x = x();
		String actualNamespace = x().prepare(namespace, x.spec.exportPackage);

		return new BundleSpecRequireCapability() {

			@Override
			public BundleBuilder x() {
				return x;
			}

			@Override
			public String namespace() {
				return actualNamespace;
			}
		};
	}

	interface BundleSpecRequireCapability extends BundleSpecBuilder {
		String namespace();

		default BundleSpecRequireCapability effective(String effective) {
			directive("effective", effective);
			return this;
		}

		default BundleSpecRequireCapability optional() {
			directive("resolution", "optional");
			return this;
		}

		default BundleSpecRequireCapability multiple() {
			directive("cardinality", "multiple");
			return this;
		}

		default BundleSpecRequireCapability filter(String filter) {
			directive("filter", filter);
			return this;
		}

		default BundleSpecRequireCapability attribute(String key, String value) {
			x().add("Require-Capability", x().spec.requireCapability, namespace(), key, value);
			return this;
		}

		default BundleSpecRequireCapability directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}
	}

	/**
	 * Include-Resource
	 */

	default BundleSpecIncludeResource includeResource(String target, String source, boolean preprocess,
		boolean absentOk) {
		String key = target == null ? source : target + "=" + source;
		if (absentOk)
			key = "-" + key;

		if (preprocess)
			key = "{" + key + "}";
		return includeResource(key);
	}

	default BundleSpecIncludeResource includeResource(String spec) {
		BundleBuilder x = x();
		String actualSpec = x().prepare(spec, x().spec.includeresource);

		return new BundleSpecIncludeResource() {

			@Override
			public BundleBuilder x() {
				return x;
			}

			@Override
			public String spec() {
				return actualSpec;
			}
		};
	}

	default BundleSpecIncludeResource include(String source) {
		return includeResource(null, source, false, false);
	}

	default BundleSpecIncludeResource includeIfPresent(String source) {
		return includeResource(null, source, false, true);
	}

	default BundleSpecIncludeResource preprocessIfPresent(String source) {
		return includeResource(null, source, true, true);
	}

	default BundleSpecIncludeResource preprocess(String source) {
		return includeResource(null, source, true, false);
	}

	default BundleSpecIncludeResource include(String target, String source) {
		return includeResource(null, source, false, false);
	}

	default BundleSpecIncludeResource includeIfPresent(String target, String source) {
		return includeResource(null, source, false, true);
	}

	default BundleSpecIncludeResource preprocessIfPresent(String target, String source) {
		return includeResource(null, source, true, true);
	}

	default BundleSpecIncludeResource preprocess(String target, String source) {
		return includeResource(null, source, true, false);
	}

	interface BundleSpecIncludeResource extends BundleSpecBuilder {
		String spec();

		default BundleSpecIncludeResource literal(String literal) {
			attribute("literal", literal);
			return this;
		}

		default BundleSpecIncludeResource attribute(String key, String value) {
			x().add("-includeresource", x().spec.includeresource, spec(), key, value);
			return this;
		}

		default BundleSpecIncludeResource directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}
	}

	default BundleSpecBuilder failOk() {
		x().spec.failOk = true;
		return this;
	}

	default BundleSpecBuilder sources() {
		x().spec.sources = true;
		return this;
	}

	default BundleSpecBuilder instruction(String instruction, String value) {
		x().spec.other.put(instruction, value);
		return this;
	}

	default BundleSpecBuilder header(String header, String value) {
		x().spec.other.put(header, value);
		return this;
	}

	default BundleSpecBuilder classpath(File... files) {
		for (File f : files) {
			classpath(f.getAbsolutePath());
		}
		return this;
	}

	default BundleSpecBuilder classpath(String path) {
		x().spec.classpath.add(path);
		return this;
	}

	/**
	 * A class add will add the resource of the class to the bundle but it will
	 * import the class' package from the framework (-classpath). This way DS
	 * can do its magic since the component class will come from the classpath
	 * although bnd calculates the XML.
	 *
	 * @param class1 the class to add as a resource
	 */
	default BundleSpecBuilder addResource(Class<?> class1) {
		includeResource(class1.getName()
			.replace('.', '/') + ".class").attribute("class", class1.getName());
		importPackage(class1.getPackage()
			.getName()
			.toString());
		return this;
	}

	/**
	 * Actually add the specified class into the bundle. This contrasts with
	 * {@link #addResource(Class)}, which imports the specified class via OSGi
	 * (typically from the system classloader). Classes added using this method
	 * will be loaded from within the framework, rather than by the system
	 * framework.
	 *
	 * @param class1 the class to add as a resource
	 */
	default BundleSpecBuilder addResourceWithCopy(Class<?> class1) {
		String className = class1.getName();
		String path = className.replace('.', '/') + ".class";
		URL url = class1.getClassLoader()
			.getResource(path);
		if (url == null) {
			throw new IllegalArgumentException(
				"Couldn't find class file for " + className + ", possibly a synthetic class");
		}
		addResource(path, url);
		return this;
	}

	default BundleSpecBuilder addResource(String path, URL url) {
		try {
			File f;
			if ("file".equals(url.getProtocol())) {
				f = new File(url.toURI());
			} else {
				f = File.createTempFile("xx", "conf");
				IO.store(url.openStream(), f);
			}
			x().addClose(() -> IO.delete(f));
			includeResource(path, IO.absolutePath(f), false, false);
			return this;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Adds a configuration compatible as described by the Configurator spec. It
	 * will automatically add the necessary Requirement Header.
	 *
	 * @param config the {@link File} containing the configuration
	 * @return the current version of the {@link BundleSpecBuilder}
	 */
	default BundleSpecBuilder addConfiguration(File config) {
		try {
			return addConfiguration(config.toURI()
				.toURL());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Adds a configuration compatible as described by the Configurator spec. It
	 * will automatically add the necessary Requirement Header.
	 *
	 * @param config the content for the configuration.
	 * @return the current version of the {@link BundleSpecBuilder}
	 */
	default BundleSpecBuilder addConfiguration(String config) {
		try {
			File f = Files.createTempFile("x", "config")
				.toFile();
			f.deleteOnExit();
			IO.store(config, f);
			addConfiguration(f);
			x().addClose(() -> IO.delete(f));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
		return this;
	}

	/**
	 * Adds a configuration compatible as described by the Configurator spec. It
	 * will automatically add the necessary Requirement Header.
	 *
	 * @param url the URL, where the Configuration can be found
	 * @return the current version of the {@link BundleSpecBuilder}
	 */
	default BundleSpecBuilder addConfiguration(URL url) {
		return requireCapability(ExtenderNamespace.EXTENDER_NAMESPACE)
			.filter(String.format("(%s=%s)", ExtenderNamespace.EXTENDER_NAMESPACE,
				ConfiguratorConstants.CONFIGURATOR_EXTENDER_NAME))
			.addResource(CONFIGURATION_JSON, url);
	}

	default Bundle install() throws Exception {
		BuilderSpecification spec = x().spec;
		byte[] build = LaunchpadBuilder.workspace.build(LaunchpadBuilder.projectDir.getAbsolutePath(), spec);
		String location;
		if (spec.location == null) {
			String name = spec.bundleSymbolicName.toString();
			String version = spec.bundleVersion;
			version = (version == null) ? "0" : version;
			location = name + '-' + version;
		} else {
			location = spec.location;
		}

		ByteArrayInputStream bin = new ByteArrayInputStream(build);
		return x().ws.getBundleContext()
			.installBundle(location, bin);
	}

	default Bundle start() {
		try {
			Bundle b = install();
			x().ws.start(b);
			return b;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	default BundleSpecBuilder resourceOnly() {
		x().spec.resourceOnly = true;
		return this;
	}

	/**
	 * Inherit the build instructions from the project
	 */
	@Deprecated
	default BundleSpecBuilder inherit() {
		x().spec.parent.add(BuilderSpecification.PROJECT);
		return this;
	}

	/**
	 * Inherit the build instructions from the project
	 */
	default BundleSpecBuilder project() {
		return parent(BuilderSpecification.PROJECT);
	}

	/**
	 * Inherit the build instructions from the workspace
	 */
	default BundleSpecBuilder workspace() {
		return parent(BuilderSpecification.WORKSPACE);
	}

	/**
	 * Inherit the build instructions from a file. This file must exist. It is
	 * used as the properties file for the used builder. The further
	 * specifications from this builder fully overwrite these properties.
	 *
	 * @param path
	 */
	default BundleSpecBuilder parent(String path) {

		List<String> paths = x().spec.parent;
		String last = paths.isEmpty() ? null : paths.get(paths.size() - 1);

		boolean isTerminatedWithProjectOrWorkspace = PROJECT.equals(last) || WORKSPACE.equals(last);

		if (isTerminatedWithProjectOrWorkspace)
			throw new IllegalArgumentException(
				"no entries can be added after either PROJECT or WORKSPACE is set as parent");

		boolean isFileParent = !WORKSPACE.equals(path) && !PROJECT.equals(path);

		if (isFileParent) {
			File f = IO.getFile(x().ws.projectDir, path);
			if (!f.isFile())
				throw new IllegalArgumentException("No such parent file " + f.getAbsolutePath());

			path = f.getAbsolutePath();
		}
		x().spec.parent.add(path);
		return this;
	}

	/**
	 * Specify the install location of a bundle. This is the value that is
	 * passed to {@Link BundleContext#installBundle(String,
	 * java.io.InputStream)} as the location parameter.
	 * <p>
	 * If not specified, the location will default to the string
	 * <tt>bsn-bundleVersion</tt>.
	 *
	 * @param location the location string to use for this bundle.
	 * @return The builder object for chained invocations.
	 */
	default BundleSpecBuilder location(String location) {
		x().spec.location = location;
		return this;
	}
}
