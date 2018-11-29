package aQute.bnd.remote.junit;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Version;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

/**
 * Provides all builder functions. Specified as default methods so that we can
 * create sub builder interfaces that inherit all functions of the outer one.
 * 
 */
public interface BundleSpecBuilder {
	static final String CONFIGURATION_JSON = "configuration/configuration.json";

	BundleBuilder x();

	/**
	 * Bundle-SymbolicName
	 * 
	 * @param name bundle symbolic name
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecBsn bundleSymbolicName(String name) {
		requireNonNull(name, "Bundle Symbolic Name cannot be null");
		BundleBuilder x = x();
		x.spec.bundleSymbolicName.put(name, new LinkedHashMap<>());
		if (x.spec.bundleSymbolicName.size() > 1) {
			throw new IllegalArgumentException("You can only have one Bundle-SymbolicName");
		}
		return new BundleSpecBsn() {

			@Override
			public BundleBuilder x() {
				return x;
			}

			public String bsn() {
				return name;
			}
		};
	}

	/**
	 * The values for a Fragment Attachment directive
	 */
	enum FragmentAttachment {
		ALWAYS("always"),
		NEVER("never"),
		RESOLVE_TIME("resolve-time");

		private final String value;

		FragmentAttachment(String name) {
			this.value = name;
		}

		@Override
		public String toString() {
			return value;
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
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecBsn fragmentAttachment(FragmentAttachment attachment) {
			requireNonNull(attachment, "Fragment attachment cannot be null");
			directive("fragment-attachment", attachment.toString());
			return this;
		}

		/**
		 * Set the mandatory matching attributes
		 * 
		 * @param attributes the matching attributes
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecBsn mandatory(String... attributes) {
			requireNonNull(attributes, "'attributes' cannot be null");
			directive("mandatory", Stream.of(attributes)
					.collect(Collectors.joining(",")));
			return this;
		}

		/**
		 * Bundle-SymbolicName attribute
		 * 
		 * @param key the attribute name
		 * @param value the value
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecBsn attribute(String key, String value) {
			requireNonNull(key, "Attribute key cannot be null");
			requireNonNull(value, "Attribute value cannot be null");

			x().add("Bundle-SymbolicName", x().spec.bundleSymbolicName, bsn(), key, value);
			return this;
		}

		/**
		 * Bundle-SymbolicName directive
		 * 
		 * @param key the directive name (no ':' at end)
		 * @param value the value
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecBsn directive(String key, String value) {
			requireNonNull(key, "Directive key cannot be null");
			requireNonNull(value, "Directive value cannot be null");

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
	 * 
	 * @param version the bundle version
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecBuilder bundleVersion(String version) {
		requireNonNull(version, "Version cannot be null");
		if (x().spec.bundleVersion != null) {
			throw new IllegalArgumentException("Bundle-Version was already set to: " + x().spec.bundleVersion);
		}
		x().spec.bundleVersion = version;
		new Version(version);
		return this;
	}

	/**
	 * Bundle-Activator
	 * 
	 * @param bundleActivator bundle activator
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecBuilder bundleActivator(String bundleActivator) {
		requireNonNull(bundleActivator, "Bundle activator cannot be null");
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
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecBuilder bundleActivator(Class<? extends BundleActivator> bundleActivator) {
		requireNonNull(bundleActivator, "Bundle activator cannot be null");
		this.bundleActivator(bundleActivator.getName());
		this.addResource(bundleActivator);
		return this;
	}

	/**
	 * Fragment-Host
	 * 
	 * @param name fragment host name
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecFragmentHost fragmentHost(String name) {
		requireNonNull(name, "Fragment host name cannot be null");
		BundleBuilder x = x();
		x.spec.fragmentHost.put(name, new LinkedHashMap<>());
		if (x.spec.fragmentHost.size() > 1) {
			throw new IllegalArgumentException(
				"Only one Fragment-Host can be specified. The previous one was " + x.spec.fragmentHost);
		}
		return new BundleSpecFragmentHost() {

			/*
			 * access to the underlying builder
			 */
			@Override
			public BundleBuilder x() {
				return x;
			}

			/*
			 * access to the current fragment host
			 */
			public String fragmenthost() {
				return name;
			}

		};
	}

	/**
	 * Extension options for the Fragment-Host
	 */
	enum Extension {
		FRAMEWORK("framework"),
		BOOTCLASSPATH("bootclasspath");

		private final String value;

		Extension(String name) {
			this.value = name;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	interface BundleSpecFragmentHost extends BundleSpecBuilder {

		/**
		 * extension
		 * 
		 * @param extension the actual extension type
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecBuilder extension(Extension extension) {
			requireNonNull(extension, "Extension cannot be null");
			directive("extension", extension.toString());
			return this;
		}

		/**
		 * The Fragment-Host bundle-version attribute
		 * 
		 * @param versionRange the range of version for the fragment host
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		@Override
		default BundleSpecFragmentHost bundleVersion(String versionRange) {
			requireNonNull(versionRange, "Version range cannot be null");
			attribute("bundle-version", versionRange);
			return this;
		}

		/**
		 * Fragment-Host attribute
		 * 
		 * @param key the attribute name
		 * @param value the attribute value
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecFragmentHost attribute(String key, String value) {
			requireNonNull(key, "Attribute key cannot be null");
			requireNonNull(value, "Attribute value cannot be null");

			x().add("Fragment-Host", x().spec.fragmentHost, fragmenthost(), key, value);
			return this;
		}

		/**
		 * Fragment-Host directive
		 * 
		 * @param key the directive name without the ':'
		 * @param value the directive value
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecFragmentHost directive(String key, String value) {
			requireNonNull(key, "Directive key cannot be null");
			requireNonNull(value, "Directive value cannot be null");

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
	 * 
	 * @param name bundle symbolic name
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecBuilderRequireBundle requireBundle(String name) {
		requireNonNull(name, "Bundle Symbolic Name cannot be null");
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
		 * @param versionRange
		 *            the version range of the required bundles
		 */
		default BundleSpecBuilderRequireBundle bundleVersion(String versionRange) {
			attribute("bundle-version", versionRange);
			return this;
		}

		/**
		 * Require-Bundle attribute
		 * 
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecBuilderRequireBundle attribute(String key, String value) {
			requireNonNull(key, "Key cannot be null");
			requireNonNull(value, "Value cannot be null");

			x().add("Require-Bundle ", x().spec.requireBundle, requirebundle(), key, value);
			return this;
		}

		/**
		 * Require-Bundle directive
		 * 
		 * @param key the name of the directive (without the ':')
		 * @param value the value of the directive
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
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
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecImportPackage importPackage(String name) {
		requireNonNull(name, "Package name cannot be null");
		BundleBuilder x = x();
		String packageName = x().prepare(name, x.spec.importPackage);

		return new BundleSpecImportPackage() {

			/*
			 * Access the underlying builder
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
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecImportPackage version(String versionRange) {
			requireNonNull(versionRange, "Version range cannot be null");
			attribute("version", versionRange);
			return this;
		}

		/**
		 * Set the import from specific bundle attribute
		 * 
		 * @param bundleSymbolicName the bundle to import the package from
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecImportPackage bundle_symbolic_name(String bundleSymbolicName) {
			requireNonNull(bundleSymbolicName, "Bundle symbolic name cannot be null");
			attribute("bundle-symbolic-name", bundleSymbolicName);
			return this;
		}

		/**
		 * Set the import from specific bundle version attribute
		 * 
		 * @param versionRange the version range of the required package
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecImportPackage bundle_version(String versionRange) {
			requireNonNull(versionRange, "Version range cannot be null");
			attribute("bundle-version", versionRange);
			return this;
		}

		/**
		 * The Import-Package attribute
		 * 
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecImportPackage attribute(String key, String value) {
			requireNonNull(key, "Key cannot be null");
			requireNonNull(value, "Value cannot be null");

			x().add("Import-Package", x().spec.importPackage, packageName(), key, value);
			return this;
		}

		/**
		 * The Import-Package directive
		 * 
		 * @param key the name of the directive (without the ':')
		 * @param value the value of the directive
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
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
			 * 
			 * @see aQute.bnd.remote.junit.BundleSpecBuilder#x()
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
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecExportPackage uses(String... packageNames) {
			requireNonNull(packageNames, "'packageNames' cannot be null");
			directive("uses", x().join(packageNames));
			return this;
		}

		/**
		 * Set the mandatory attributes
		 * 
		 * @param attributeNames the mandatory attribute names
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecExportPackage mandatory(String... attributeNames) {
			requireNonNull(attributeNames, "'attributeNames' cannot be null");
			directive("mandatory", x().join(attributeNames));
			return this;
		}

		/**
		 * Set included classes
		 * 
		 * @param classNames the included class names
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecExportPackage include(String... classNames) {
			requireNonNull(classNames, "'classNames' cannot be null");
			directive("include", x().join(classNames));
			return this;
		}

		/**
		 * Set included classes
		 * 
		 * @param classNames the included class names
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecExportPackage include(@SuppressWarnings("rawtypes") Class... classes) {
			requireNonNull(classes, "'classes' cannot be null");
			return this.include(Stream.of(classes).map(Class::getName).toArray(String[]::new));
		}

		/**
		 * exclude directive
		 * 
		 * @param classNames the class names to exclude
		 * @throws IllegalArgumentException if the specified argument is
		 *             {@code null}
		 */
		default BundleSpecExportPackage exclude(String... classNames) {
			requireNonNull(classNames, "'classNames' cannot be null");
			directive("exclude", x().join(classNames));
			return this;
		}

		default BundleSpecExportPackage version(String version) {
			attribute("version", version);
			return this;
		}

		/**
		 * Export-Package attribute
		 * 
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecExportPackage attribute(String key, String value) {
			requireNonNull(key, "Key cannot be null");
			requireNonNull(value, "Value cannot be null");

			x().add("Export-Package", x().spec.exportPackage, packageName(), key, value);
			return this;
		}

		/**
		 * Export-Package directive
		 * 
		 * @param key the name of the directive
		 * @param value the value of the directive
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecExportPackage directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}

		String packageName();

	}

	/**
	 * -exportcontents
	 * 
	 * @param name the pacakge name
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecExportContents exportContent(String name) {
		requireNonNull(name, "Package name cannot be null");
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
			requireNonNull(packageNames, "'packageNames' cannot be null");
			directive("uses", x().join(packageNames));
			return this;
		}

		default BundleSpecExportContents mandatory(String... attributeNames) {
			requireNonNull(attributeNames, "'attributeNames' cannot be null");
			directive("mandatory", x().join(attributeNames));
			return this;
		}

		default BundleSpecExportContents include(String... classNames) {
			requireNonNull(classNames, "'classNames' cannot be null");
			directive("include", x().join(classNames));
			return this;
		}

		default BundleSpecExportContents exclude(String... classNames) {
			requireNonNull(classNames, "'classNames' cannot be null");
			directive("exclude", x().join(classNames));
			return this;
		}

		default BundleSpecExportContents version(String version) {
			attribute("version", version);
			return this;
		}

		/**
		 * Export contents attribute
		 * 
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecExportContents attribute(String key, String value) {
			requireNonNull(key, "Key cannot be null");
			requireNonNull(value, "Value cannot be null");

			x().add("-exportcontents", x().spec.exportPackage, packageName(), key, value);
			return this;
		}

		/**
		 * Export contents directive
		 * 
		 * @param key the name of the directive
		 * @param value the value of the directive
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecExportContents directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}
	}

	/**
	 * -privatepackage
	 * 
	 * @param name the pacakge name
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecBuilder privatePackage(String name) {
		requireNonNull(name, "Package name cannot be null");
		BundleBuilder x = x();
		x().prepare(name, x.spec.privatePackage);
		return this;
	}

	/**
	 * Provide-Capability
	 * 
	 * @param namespace capability namespace
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecProvideCapability provideCapability(String namespace) {
		requireNonNull(namespace, "Namespace name cannot be null");
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
			requireNonNull(packageNames, "'packageNames' cannot be null");
			directive("uses", x().join(packageNames));
			return this;
		}

		default BundleSpecProvideCapability effective(String effective) {
			directive("effective", effective);
			return this;
		}

		default BundleSpecProvideCapability version(String version) {
			attribute("version", version);
			return this;
		}

		default BundleSpecProvideCapability primary(String name) {
			directive(namespace(), name);
			return this;
		}

		/**
		 * Provide-Capability attribute
		 * 
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecProvideCapability attribute(String key, String value) {
			requireNonNull(key, "Key cannot be null");
			requireNonNull(value, "Value cannot be null");

			x().add("Provide-Capability", x().spec.provideCapability, namespace(), key, value);
			return this;
		}

		/**
		 * Provide-Capability directive
		 * 
		 * @param key the name of the directive
		 * @param value the value of the directive
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecProvideCapability directive(String key, String value) {
			attribute(key + ":", value);
			return this;
		}
	}

	/**
	 * Require-Capability
	 * 
	 * @param namespace capability namespace
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecRequireCapability requireCapability(String namespace) {
		requireNonNull(namespace, "Namespace name cannot be null");
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

		/**
		 * Require-Capability attribute
		 * 
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecRequireCapability attribute(String key, String value) {
			requireNonNull(key, "Key cannot be null");
			requireNonNull(value, "Value cannot be null");

			x().add("Require-Capability", x().spec.requireCapability, namespace(), key, value);
			return this;
		}

		/**
		 * Require-Capability directive
		 * 
		 * @param key the name of the directive
		 * @param value the value of the directive
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
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
		requireNonNull(source, "'source' cannot be null");
		String key = target == null ? source : target + "=" + source;
		if (absentOk)
			key = "-" + key;

		if (preprocess)
			key = "{" + key + "}";
		return includeResource(key);
	}

	/**
	 * -includeresource
	 * 
	 * @param spec the specification
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecIncludeResource includeResource(String spec) {
		requireNonNull(spec, "Specification cannot be null");
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

		/**
		 * -includeresource attribute
		 * 
		 * @param key the name of the attribute
		 * @param value the value of the attribute
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
		default BundleSpecIncludeResource attribute(String key, String value) {
			requireNonNull(key, "Key cannot be null");
			requireNonNull(value, "Value cannot be null");

			x().add("-includeresource", x().spec.includeresource, spec(), key, value);
			return this;
		}

		/**
		 * -includeresource directive
		 * 
		 * @param key the name of the directive
		 * @param value the value of the directive
		 * @throws IllegalArgumentException if any of the specified arguments is
		 *             {@code null}
		 */
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
		requireNonNull(instruction, "Instruction cannot be null");
		requireNonNull(value, "Value cannot be null");

		x().spec.other.put(instruction, value);
		return this;
	}

	default BundleSpecBuilder header(String header, String value) {
		requireNonNull(header, "Header cannot be null");
		requireNonNull(value, "Value cannot be null");

		x().spec.other.put(header, value);
		return this;
	}

	default BundleSpecBuilder classpath(File... files) {
		requireNonNull(files, "'files' cannot be null");
		for (File f : files) {
			classpath(f.getAbsolutePath());
		}
		return this;
	}

	default BundleSpecBuilder classpath(String path) {
		requireNonNull(path, "Classpath entry cannot be null");
		x().spec.classpath.add(path);
		return this;
	}

	/**
	 * A class add will add the resource of the class to the bundle but it will
	 * import the class' package from the framework (-classpath). This way DS
	 * can do its magic since the component class will come from the classpath
	 * although bnd calculates the XML.
	 * 
	 * @param clazz the class to add as a resource
	 * @throws IllegalArgumentException if the specified argument is
	 *             {@code null}
	 */
	default BundleSpecBuilder addResource(Class<?> clazz) {
		requireNonNull(clazz, "Resource type cannot be null");
		includeResource(clazz.getName().replace('.', '/') + ".class").attribute("class", clazz.getName());
		importPackage(clazz.getPackage().getName());
		return this;
	}

	default BundleSpecBuilder addResource(String path, URL url) {
		requireNonNull(path, "Resource path cannot be null");
		requireNonNull(url, "URL cannot be null");

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

	default BundleSpecBuilder addConfiguration(File config) {
		try {
			addResource(CONFIGURATION_JSON, config.toURI().toURL());
			return this;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	default BundleSpecBuilder addConfiguration(String config) {
		try {
			File f = Files.createTempFile("x", "config").toFile();
			f.deleteOnExit();
			IO.store(config, f);
			addConfiguration(f);
			x().addClose(() -> IO.delete(f));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
		return this;
	}

	default BundleSpecBuilder addConfiguration(URL url) {
		return addResource(CONFIGURATION_JSON, url);
	}

	default Bundle install() throws Exception {
		byte[] build = JUnitFrameworkBuilder.workspace.build(JUnitFrameworkBuilder.projectDir.getAbsolutePath(),
				x().spec);
		String name = x().spec.bundleSymbolicName.toString();
		ByteArrayInputStream bin = new ByteArrayInputStream(build);
		return x().ws.getBundleContext().installBundle(name, bin);
	}

	default Bundle start() {
		try {
			Bundle b = install();
			b.start();
			return b;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	default BundleSpecBuilder resourceOnly() {
		x().spec.resourceOnly = true;
		return this;
	}

	default BundleSpecBuilder inherit() {
		x().spec.inherit = true;
		return this;
	}
}