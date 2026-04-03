package aQute.bnd.osgi.metainf;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * Holds our knowledge about the META-INF/services/ files.
 * <p>
 * These are files that are used by Java in the Service Loader. Normally they
 * have the name of a service type and are filled with their implementations,
 * one per line.
 * <p>
 * We allow these lines to be annotated like Java types. Annotations are in the
 * comments to not disturb other users of these files. They look like:
 *
 * <pre>
 * #import aQute.bnd.annotation.spi.ServiceProvider
 * </pre>
 */
public class MetaInfService {

	static final String		META_INF_SERVICES_STEM		= "META-INF/services";
	static final String		META_INF_SERVICES_PREFIX	= META_INF_SERVICES_STEM + "/";
	static final String		FQN_S						= "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
	static final Pattern	IMPORT_P					= Pattern
		.compile("#import\\s+(?<fqn>" + FQN_S + ")\\s*;?\\s*$");
	static final Pattern	ANNOTATION_P				= Pattern
		.compile("#@(?<fqn>" + FQN_S + ")\\s*(\\((?<attrs>.*)" + "\\)\\s*)?;?\\s*$");

	/**
	 * get the META-INF service files from a JAR
	 *
	 * @param jar the jar
	 */

	public static Map<String, MetaInfService> getServiceFiles(Jar jar) throws Exception {
		Map<String, MetaInfService> result = new TreeMap<>();

		Map<String, Resource> map = jar.getDirectories()
			.getOrDefault(META_INF_SERVICES_STEM, Collections.emptyMap());

		if (map == null) {
			// can happen when META-INF/services is empty, but has subfolders
			return result;
		}

		for (Map.Entry<String, Resource> e : map.entrySet()) {
			String path = e.getKey();
			Resource resource = e.getValue();
			if (path.startsWith(META_INF_SERVICES_PREFIX)) {
				String serviceName = Strings.stripPrefix(path, META_INF_SERVICES_PREFIX);
				result.put(serviceName, new MetaInfService(serviceName, resource));
			}
		}
		return result;
	}

	/**
	 * A MetaInfService file consists of implementations.
	 */
	public class Implementation {

		final Attrs			serviceProvider	= new Attrs();
		final String		implementationName;
		final List<String>	comments;
		final Parameters	annotations;

		Implementation(String name, Parameters annotations, List<String> comments) {
			this.annotations = new Parameters(annotations);
			this.comments = new ArrayList<>(comments);
			this.implementationName = name;
		}

		/**
		 * Get the fully qualified class name of the implementation
		 */
		public String getImplementationName() {
			return implementationName;
		}

		/**
		 * Get the fully qualified class name of the service type
		 */
		public String getServiceName() {
			return serviceName;
		}

		/**
		 * Get the comments that preceded this implementation definition
		 */
		public Optional<String> getComments() {
			if (comments == null || comments.isEmpty())
				return Optional.empty();

			return Optional.of(comments.stream()
				.collect(Collectors.joining("\n")));
		}

		/**
		 * Get the annotations. The key is the fqn of the annotation class, the
		 * attrs the values
		 */
		public Parameters getAnnotations() {
			return annotations;
		}

		/**
		 * Map this implementation to its original source code, this includes
		 * any comments ahead of it.
		 */

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			append(sb);
			return sb.toString();
		}

		void append(StringBuilder sb) {
			comments.forEach(s -> line(sb, s));
			line(sb, implementationName);
		}

	}

	final String						serviceName;
	final Map<String, Implementation>	implementations	= new LinkedHashMap<>();
	final List<String>					post;

	boolean								dirty;

	/**
	 * Create a MetaInfService from a resource
	 *
	 * @param serviceName the fqn for the service interface
	 * @param resource the resource
	 */
	public MetaInfService(String serviceName, Resource resource) throws Exception {
		this(serviceName, resource.openInputStream());

	}

	/**
	 * Create a MetaInfService from an input stream
	 *
	 * @param serviceName the fqn for the service interface
	 * @param in the input stream
	 */
	public MetaInfService(String serviceName, InputStream in) throws IOException {
		this(serviceName, IO.collect(in));
	}

	/**
	 * Create a MetaInfService from a reader
	 *
	 * @param serviceName the fqn for the service interface
	 * @param in the reader
	 */
	public MetaInfService(String serviceName, Reader in) throws IOException {
		this(serviceName, IO.collect(in));
	}

	/**
	 * Create an empty
	 *
	 * @param serviceName the fqn for the service interface
	 */
	public MetaInfService(String serviceName) {
		this.serviceName = serviceName;
		this.post = Collections.emptyList();
	}

	/**
	 * Create from a source. The source should be the content as defined for
	 * files in the META-INF/services directory. This may include imports and
	 * annotations in the comments.
	 *
	 * @param serviceName the fqn for the service interface
	 * @param source the source code
	 */
	public MetaInfService(String serviceName, String source) {
		this.serviceName = serviceName;

		Map<String, String> imports = new HashMap<>();
		Parameters annotations = new Parameters();
		List<String> comments = new ArrayList<>();

		for (String line : Strings.splitLines(source)) {
			line = Strings.trim(line);
			if (line.isEmpty()) {
				comments.add(line);
				continue;
			}

			if (line.startsWith("#")) {
				comments.add(line);
				Matcher m = IMPORT_P.matcher(line);
				if (m.matches()) {
					String fqn = m.group("fqn");
					imports.put(shortName(fqn), fqn);
					continue;
				}

				m = ANNOTATION_P.matcher(line);
				if (m.matches()) {
					String fqn = imports.computeIfAbsent(m.group("fqn"), k -> k);
					Attrs attrs = OSGiHeader.parseProperties(m.group("attrs"));
					attrs.addDirectiveAliases();
					annotations.add(fqn, attrs);
					continue;
				}
				// just comment
				continue;
			}
			implementations.put(line, new Implementation(line, annotations, comments));
			annotations.clear();
			comments.clear();
		}
		this.post = comments;
	}

	/**
	 * Add an extra implementation to the service files.
	 *
	 * @param serviceImpl the implementation fqn
	 * @param annotations the annotations for this implementation
	 * @param comments the comments that come before the definition
	 */
	public boolean add(String serviceImpl, Parameters annotations, String... comments) {
		if (annotations == null) {
			annotations = new Parameters();
		}
		Implementation old = implementations.put(serviceImpl,
			new Implementation(serviceImpl, annotations, Arrays.asList(comments)));
		this.dirty = true;
		return old == null;
	}

	/**
	 * Get the current list of implementations
	 */

	public Map<String, Implementation> getImplementations() {
		return new LinkedHashMap<>(implementations);
	}

	/**
	 * Return if changes were made after the constructor parsing the source.
	 */

	public boolean isDirty() {
		return dirty;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		implementations.values()
			.forEach(impl -> impl.append(sb));
		post.forEach(l -> line(sb, l));
	}

	private void line(StringBuilder sb, String s) {
		sb.append(s)
			.append('\n');
	}

	private String shortName(String fqn) {
		int n = fqn.lastIndexOf('$');
		if (n < 0) {
			n = fqn.lastIndexOf('.');
		}
		return fqn.substring(n + 1);
	}

}
