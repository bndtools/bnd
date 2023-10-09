package aQute.bnd.osgi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aQute.bnd.build.model.EE;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.exceptions.Exceptions;

/**
 * Multi Release jars are another error magnet extension to Java. Instead of
 * having the flat class space of Java and Javac, it allows developers to put
 * multiple versions of a class in versioned directory.
 * <p>
 *
 * @see "https://docs.oracle.com/en/java/javase/19/docs/specs/jar/jar.html#multi-release-jar-files"
 *      <p>
 *      Multi-release JAR files
 *      <p>
 *      A multi-release JAR file allows for a single JAR file to support
 *      multiple major versions of Java platform releases. For example, a
 *      multi-release JAR file can depend on both the Java 8 and Java 9 major
 *      platform releases, where some class files depend on APIs in Java 8 and
 *      other class files depend on APIs in Java 9. This enables library and
 *      framework developers to decouple the use of APIs in a specific major
 *      version of a Java platform release from the requirement that all their
 *      users migrate to that major version. Library and framework developers
 *      can gradually migrate to and support new Java features while still
 *      supporting the old features.
 *      <p>
 *      A multi-release JAR file is identified by the main attribute:
 *
 *      <pre>
 *			Multi-Release: true
 *      </pre>
 *
 *      declared in the main section of the JAR Manifest.
 *      <p>
 *      Classes and resource files dependent on a major version, 9 or greater,
 *      of a Java platform release may be located under a versioned directory
 *      instead of under the top-level (or root) directory. The versioned
 *      directory is located under the the META-INF directory and is of the
 *      form:
 *
 *      <pre>
 *      META - INF / versions / N
 *      </pre>
 *
 *      where N is the string representation of the major version number of a
 *      Java platform release. Specifically N must conform to the specification:
 *      N: {1-9} {0-9}*
 *      <p>
 *      Any versioned directory whose value of N is less than 9 is ignored as is
 *      a string representation of N that does not conform to the above
 *      specification.
 *      <p>
 *      A class file under a versioned directory, of version N say, in a
 *      multi-release JAR must have a class file version less than or equal to
 *      the class file version associated with Nth major version of a Java
 *      platform release. If the class of the class file is public or protected
 *      then that class must preside over a class of the same fully qualified
 *      name and access modifier whose class file is present under the top-level
 *      directory. By logical extension this applies to a class of a class file,
 *      if present, under a versioned directory whose version is less than N.
 *      <p>
 *      If a multi-release JAR file is deployed on the class path or module path
 *      (as an automatic module or an explicit multi-release module) of major
 *      version N of a Java platform release runtime, then a class loader
 *      loading classes from that JAR file will first search for class files
 *      under the Nth versioned directory, then prior versioned directories in
 *      descending order (if present), down to a lower major version bound of 9,
 *      and finally under the top-level directory.
 *      <p>
 *      The public API exported by the classes in a multi-release JAR file must
 *      be exactly the same across versions, hence at a minimum why versioned
 *      public or protected classes for class files under a versioned directory
 *      must preside over classes for class files under the top-level directory.
 *      It is difficult and costly to perform extensive API verification checks
 *      as such tooling, such as the jar tool, is not required to perform
 *      extensive verification and a Java runtime is not required to perform any
 *      verification. A future release of this specification may relax the exact
 *      same API constraint to support careful evolution.
 *      <p>
 *      Resources under the META-INF directory cannot be versioned (such as for
 *      service configuration).
 *      <p>
 *      A multi-release JAR file can be signed.
 *      <p>
 *      Multi-release JAR files are not supported by the boot class loader of a
 *      Java runtime. If a multi-release JAR file is appended to the boot class
 *      path (with the -Xbootclasspath/a option) then the JAR is treated as if
 *      it is an ordinary JAR file. Modular multi-release JAR files
 *      <p>
 *      A modular multi-release JAR file is a multi-release JAR file that has a
 *      module descriptor, module-info.class, in the top-level directory (as for
 *      a modular JAR file), or directly in a versioned directory.
 *      <p>
 *      A public or protected class in a non-exported package (that is not
 *      declared as exported in the module descriptor) need not preside over a
 *      class of the same fully qualified name and access modifier whose class
 *      file is present under the top-level directory.
 *      <p>
 *      A module descriptor is generally treated no differently to any other
 *      class or resource file. A module descriptor may be present under a
 *      versioned area but not present under the top-level directory. This
 *      ensures, for example, only Java 8 versioned classes can be present under
 *      the top-level directory while Java 9 versioned classes (including, or
 *      perhaps only, the module descriptor) can be present under the 9
 *      versioned directory.
 *      <p>
 *      Any versioned module descriptor that presides over a lesser versioned
 *      module descriptor or that at the top-level, M say, must be identical to
 *      M, with two exceptions:
 *      <ul>
 *      <li>the presiding versioned descriptor can have different non-transitive
 *      requires clauses of java.* and jdk.* modules; and
 *      <li>the presiding versioned descriptor can have different uses clauses,
 *      even of service types defined outside of java.* and jdk.* modules.
 *      </ul>
 *      Tooling, such as the jar tool, should perform such verification of
 *      versioned module descriptors but a Java runtime is not required to
 *      perform any verification.
 */

public class JPMSModule {
	final static Pattern		VERSIONED_P						= Pattern
		.compile("META-INF/versions/(?<release>\\d+)/(?<path>.*)");

	public static final String	MULTI_RELEASE_HEADER			= "Multi-Release";
	public static final String	VERSIONS_PATH					= "META-INF/versions/";
	public static final String	MODULE_INFO_CLASS				= "module-info.class";
	public static final String	OSGI_VERSIONED_MANIFEST_PATH	= "OSGI-INF/MANIFEST.MF";

	final Jar					jar;
	Optional<ModuleAttribute>	moduleAttribute;

	/**
	 * Cosntructor
	 *
	 * @param jar the base for this module
	 */

	public JPMSModule(Jar jar) {
		this.jar = jar;
	}

	/**
	 * Get the underlying JAR
	 */
	public Jar getJar() {
		return jar;
	}

	/**
	 * Get the path to a resource in a version directory
	 *
	 * @param release the release version of the VM
	 * @param path the path in the version directory
	 * @return a path
	 */
	public static String getVersionedPath(int release, String path) {

		assert release > 8 && release < 100;

		StringBuilder sb = new StringBuilder();
		sb.append("META-INF/versions/")
			.append(release);
		if (path != null)
			sb.append('/')
				.append(path);
		return sb.toString();
	}

	/**
	 * Return the available releases in this modules. This does not include the
	 * base, we do not know what version that is
	 * <p>
	 *
	 * @return a map with the release number -> a JAR.
	 */

	public SortedSet<Integer> getVersions() {

		SortedSet<Integer> result = new TreeSet<>();

		for (int i = 9; i <= EE.MAX_SUPPORTED_RELEASE; i++) {
			if (jar.getDirectories()
				.containsKey(getVersionedPath(i, null)))
				result.add(i);
		}
		return result;
	}

	/**
	 * Return a Jar that contains only the resources for a specific release
	 * version.
	 *
	 * @param release the release number
	 * @return the Jar will only the release classes & resources
	 */
	public Jar getReleaseOnly(int release) {
		Jar versioned = new Jar(this.jar.getName() + "-" + release) {
			@Override
			public void close() {}
		};
		versioned.setSource(this.jar.getSource());

		for (Map.Entry<String, Resource> e : this.jar.getResources()
			.entrySet()) {
			String path = e.getKey();
			Matcher m = VERSIONED_P.matcher(path);
			if (m.matches()) {
				int r = Integer.parseInt(m.group("release"));
				if (r == release) {
					String relative = m.group("path");
					versioned.putResource(relative, e.getValue());
				}
			}
		}
		return versioned;
	}

	/**
	 * Put a resource in a Jar in the META-INF/versions/ subtree for Multi
	 * Release Jars.
	 *
	 * @param release the release number (> 8)
	 * @param path the relative path of the resource
	 * @param resource the resource
	 * @return true if this modified the set of resources
	 */
	public boolean putResource(int release, String path, Resource resource) {
		if (release < 9)
			return getJar().putResource(path, resource);
		String versionedPath = getVersionedPath(release, path);
		return getJar().putResource(versionedPath, resource);
	}

	/**
	 * Get a resource from a Jar that is in the versioned area.
	 *
	 * @param release the release number we're looking for
	 * @param path the path
	 * @return the resource or null if not existent
	 */
	public Resource getResource(int release, String path) {
		if (release < 9)
			return getJar().getResource(path);

		String versionedPath = getVersionedPath(release, path);
		return getJar().getResource(versionedPath);
	}

	/**
	 * Find a resource from a Jar. Start in the normal space than look in the
	 * increasing versioned areas.
	 *
	 * @param path the path
	 * @param release the release to start. If < 0, start from default and go up
	 *            in releases. If <9, use default. Otherwise start there and go
	 *            down, ending in default
	 * @return the resource or null if not existent
	 */
	public Optional<Resource> findResource(String path, int release) {
		List<Integer> order = new ArrayList<>(getVersions());
		order.add(0, 0);

		if (release <= 0) {
			// order is ok
		} else {
			for (Iterator<Integer> it = order.iterator(); it.hasNext();) {
				int n = it.next();
				if (n > release) {
					it.remove();
				}
			}
			Collections.reverse(order);
		}

		for (int n : order) {
			Resource resource = getResource(n, path);
			if (resource != null)
				return Optional.ofNullable(resource);
		}
		return Optional.empty();
	}

	/**
	 * Checks if this JAR has version directories. This does not look at the
	 * manifest header.
	 *
	 * @return if this is a multi release JAR (has versions)
	 */
	public boolean isMultiRelease() {
		return !getVersions().isEmpty();
	}

	/**
	 * Returns the name of the module, either from the module descriptor or
	 * automatically generated from the JAR file manifest.
	 *
	 * @return An optional string containing the module name, or an empty
	 *         optional if the module name cannot be determined.
	 * @throws Exception If an error occurs while parsing the module descriptor
	 *             or manifest.
	 */
	public Optional<String> getModuleName() throws Exception {
		return moduleAttribute().map(a -> a.module_name)
			.or(this::automaticModuleName);
	}

	/**
	 * Returns the name of the module, automatically generated from the JAR file
	 * manifest.
	 *
	 * @return An optional string containing the automatic module name, or an
	 *         empty optional if the automatic module name cannot be determined.
	 */
	Optional<String> automaticModuleName() {
		return manifest().map(m -> m.getMainAttributes()
			.getValue(Constants.AUTOMATIC_MODULE_NAME));
	}

	/**
	 * Returns the manifest for the JAR file, or an empty optional if the
	 * manifest cannot be read.
	 *
	 * @return An optional manifest for the JAR file.
	 */
	Optional<Manifest> manifest() {
		try {
			return Optional.ofNullable(jar.getManifest());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Returns the version of the module, if present in the module descriptor.
	 *
	 * @return An optional string containing the module version, or an empty
	 *         optional if the module version cannot be determined.
	 * @throws Exception If an error occurs while parsing the module descriptor.
	 */
	public Optional<String> getModuleVersion() throws Exception {
		return moduleAttribute().map(a -> a.module_version);
	}

	/*
	 * Returns the module attribute for the module descriptor, if present.
	 * @return An optional module attribute containing the name and version of
	 * the module, or an empty optional if the module descriptor cannot be found
	 * or parsed.
	 * @throws Exception If an error occurs while parsing the module descriptor.
	 */
	Optional<ModuleAttribute> moduleAttribute() throws Exception {
		if (moduleAttribute != null) {
			return moduleAttribute;
		}

		return findResource(Constants.MODULE_INFO_CLASS, -1).map(Clazz::parse)
			.flatMap(ci -> ci.getAttribute(ModuleAttribute.class));
	}

	/**
	 * Return a manifest for OSGi.
	 * <p>
	 * The Framework must first look in the versioned directory for the major
	 * version of the current Java platform and then prior versioned directories
	 * in descending order. The first supplemental manifest file found must be
	 * used and the Framework must replace the values of the following manifest
	 * headers in the manifest with the values of these headers, if present, in
	 * the supplemental manifest file.
	 *
	 * <pre>
	    Import-Package
	    Require-Capability
	 * </pre>
	 *
	 * Any other headers in the supplemental manifest file must be ignored.
	 *
	 * @param release the version of the VM
	 * @return a Manifest
	 */

	public Manifest getManifest(int release) {
		try {
			Manifest defaultManifest = getJar().getManifest();
			if (defaultManifest == null)
				return new Manifest();

			if (release < 9)
				return defaultManifest;

			Manifest r = findResource(OSGI_VERSIONED_MANIFEST_PATH, release).map(rr -> {
				try {
					return new Manifest(rr.openInputStream());
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			})
				.orElseGet(Manifest::new);

			if (r.equals(defaultManifest))
				return defaultManifest;

			Manifest result = new Manifest(defaultManifest);
			copy(result, r, Constants.IMPORT_PACKAGE, Constants.REQUIRE_CAPABILITY);

			return result;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Copies specified headers from one manifest to another.
	 *
	 * @param result The manifest to copy the headers to. If null, a new
	 *            manifest will be created.
	 * @param r The manifest to copy the headers from. If null, no headers will
	 *            be copied.
	 * @param headers The headers to copy.
	 * @return The manifest with the copied headers.
	 */
	public static Manifest copy(Manifest result, Manifest r, String... headers) {
		if (result == null)
			result = new Manifest();
		if (r == null)
			return result;

		for (String header : headers) {
			String value = r.getMainAttributes()
				.getValue(header);
			if (value != null)
				result.getMainAttributes()
					.putValue(header, value);
			else
				result.getMainAttributes()
					.putValue(header, "");
		}
		return result;
	}

	/**
	 * Gets a new JAR file that includes all the files from this JAR file and
	 * all previous versions up to and including the specified release.
	 *
	 * @param release The release number to include in the new JAR file.
	 * @return A new JAR file containing all the files from this JAR file and
	 *         all previous versions up to and including the specified release.
	 */
	public Jar getRelease(int release) {
		Jar target = new Jar(jar.getName());
		target.addAll(jar, new Instruction("!META-INF/versions/*"));

		for (int r : getVersions()) {
			if (release < r)
				break;

			Jar delta = getReleaseOnly(r);
			target.addAll(delta);
		}
		return target;
	}

	/**
	 * Gets the release number of the next version after the specified release.
	 *
	 * @param release The release number.
	 * @return The release number of the next version after the specified
	 *         release, or Integer.MAX_VALUE if there are no more versions.
	 */

	public int getNextRelease(int release) {
		SortedSet<Integer> versions = getVersions();
		SortedSet<Integer> tailSet = versions.tailSet(release + 1);
		if (tailSet.isEmpty())
			return Integer.MAX_VALUE;

		return tailSet.first();
	}

	/**
	 * Cleanup a bsn so that it matches a JPMS module name
	 *
	 * @param bsn a symbolic name or null
	 * @return a name that matches the JPMS specification for a module name or
	 *         null if the input was null
	 */
	public static String cleanupName(String bsn) {
		if (bsn == null)
			return null;

		String[] split = bsn.split("[^A-Za-z0-9]+");
		return Stream.of(split)
			.filter(str -> !str.isEmpty())
			.collect(Collectors.joining("."));
	}

}
