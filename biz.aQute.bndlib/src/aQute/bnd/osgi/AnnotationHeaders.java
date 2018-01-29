package aQute.bnd.osgi;

import static java.util.stream.Collectors.toSet;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.annotation.bundle.Capabilities;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Headers;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.headers.BundleCategory;
import aQute.bnd.annotation.headers.BundleContributors;
import aQute.bnd.annotation.headers.BundleCopyright;
import aQute.bnd.annotation.headers.BundleDevelopers;
import aQute.bnd.annotation.headers.BundleDocURL;
import aQute.bnd.annotation.headers.BundleLicense;
import aQute.bnd.annotation.headers.Category;
import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.lib.strings.Strings;

/**
 * This class parses the 'header annotations'. Header annotations are
 * annotations that cause headers in the manifest. There are a number of those
 * headers annotations defined in the aQute.bnd.annotation.headers package, e.g.
 * {@link BundleCopyright}. This module applies the semantics of the defined
 * fields in those annotations. It is called at the post parse phase in
 * Analyzer. This {@link ClassDataCollector} is called for all classes in our
 * scope. We first look if any header annotations are applied. We also keep
 * track of what other annotations are applied to these classes. After all the
 * classes have been parsed, we look at any of the annotations that was applied
 * to one of the contained classes. These annotations are also parsed then to
 * check if they have header annotations applied to them.
 * <p>
 * This may sound a bit bizarre, so let me explain. The idea is that you can
 * create a custom annotation for a specific resource.
 * 
 * <pre>
 * &#064;RequireCapability(&quot;osgi.webresource;filter:='(&amp;(osgi.
 * webresource=/google/angular)(version&gt;=${&#064;version}))&quot;) &#064;interface
 * Angular {}
 * </pre>
 * 
 * Now all a user has to do is apply the @Angular annotation. It will then
 * automatically create a Require-Capability, with the version of the package.
 * 
 * <pre>
 *  &#64;Angular public class MySpace {...}
 * </pre>
 * 
 * {@link About} provides some more information.
 */
class AnnotationHeaders extends ClassDataCollector implements Closeable {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(AnnotationHeaders.class);

	static final Pattern			SIMPLE_PARAM_PATTERN	= Pattern
			.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");

	// Annotations to ignore scanning further because they are known to be
	// uninteresting
	// to this scanner. This speeds us up (a little) and also avoids printing
	// warnings
	// for annotations that aren't supposed to be needed on the classpath
	// (usually
	// OSGi versioning annotations).
	static final Set<String>	DO_NOT_SCAN;

	static {
		DO_NOT_SCAN = Stream.of("org.osgi.annotation.versioning.ProviderType",
				"org.osgi.annotation.versioning.ConsumerType", "org.osgi.annotation.versioning.Version")
				.collect(toSet());
	}

	final Analyzer					analyzer;
	final Set<String>				interesting				= new HashSet<>();
	final MultiMap<String,String>	headers					= new MultiMap<>();

	//
	// Constant Strings for a fast switch statement
	//

	static final String				BUNDLE_LICENSE			= "aQute.bnd.annotation.headers.BundleLicense";
	static final String				REQUIRE_CAPABILITY		= "aQute.bnd.annotation.headers.RequireCapability";
	static final String				PROVIDE_CAPABILITY		= "aQute.bnd.annotation.headers.ProvideCapability";
	static final String				BUNDLE_CATEGORY			= "aQute.bnd.annotation.headers.BundleCategory";
	static final String				BUNDLE_DOC_URL			= "aQute.bnd.annotation.headers.BundleDocURL";
	static final String				BUNDLE_DEVELOPERS		= "aQute.bnd.annotation.headers.BundleDevelopers";
	static final String				BUNDLE_CONTRIBUTORS		= "aQute.bnd.annotation.headers.BundleContributors";
	static final String				BUNDLE_COPYRIGHT		= "aQute.bnd.annotation.headers.BundleCopyright";
	static final String				STD_REQUIREMENT			= "org.osgi.annotation.bundle.Requirement";
	static final String				STD_REQUIREMENTS		= "org.osgi.annotation.bundle.Requirements";
	static final String				STD_CAPABILITY			= "org.osgi.annotation.bundle.Capability";
	static final String				STD_CAPABILITIES		= "org.osgi.annotation.bundle.Capabilities";
	static final String				STD_HEADER				= "org.osgi.annotation.bundle.Header";
	static final String				STD_HEADERS				= "org.osgi.annotation.bundle.Headers";

	// Used to detect attributes and directives on Require-Capability and
	// Provide-Capability
	static final String				STD_ATTRIBUTE		= "org.osgi.annotation.bundle.Attribute";
	static final String				STD_DIRECTIVE		= "org.osgi.annotation.bundle.Directive";

	// Class we're currently processing
	Clazz							current;

	// The meta annotations we have processed, used to avoid infinite loops
	// This Set must be cleared for each #classStart(Clazz)
	final Set<String>				processed				= new HashSet<>();

	// The annotations we could not load. used to avoid repeatedly logging the
	// same missing annotation for the same project. Note that this should not
	// be reset for each #classStart(Clazz).
	final Set<String>				loggedMissing		= new HashSet<>();

	// we parse the annotations separately at the end
	boolean							finalizing;

	/*
	 * Initialize
	 */
	AnnotationHeaders(Analyzer analyzer) {
		this.analyzer = analyzer;

		//
		// The analyser has its own domain of type refs, so we need to get our
		// standard set to do fast comparisons
		//

		interesting.add(BUNDLE_LICENSE);
		interesting.add(REQUIRE_CAPABILITY);
		interesting.add(PROVIDE_CAPABILITY);
		interesting.add(BUNDLE_CATEGORY);
		interesting.add(BUNDLE_DOC_URL);
		interesting.add(BUNDLE_DEVELOPERS);
		interesting.add(BUNDLE_CONTRIBUTORS);
		interesting.add(BUNDLE_COPYRIGHT);
		interesting.add(STD_REQUIREMENT);
		interesting.add(STD_REQUIREMENTS);
		interesting.add(STD_CAPABILITY);
		interesting.add(STD_CAPABILITIES);
		interesting.add(STD_HEADER);
		interesting.add(STD_HEADERS);
	}

	@Override
	public boolean classStart(Clazz c) {
		processed.clear();
		//
		// Parse any classes except annotations
		//
		if (!c.isAnnotation() && c.annotations != null) {

			current = c;
			return true;
		}
		current = null;
		return false;
	}

	/*
	 * Called when an annotation is found. Dispatch on the known types.
	 */
	public void annotation(Annotation annotation) throws Exception {
		TypeRef name = annotation.getName();
		String fqn = name.getFQN();

		if (name.isJava() || DO_NOT_SCAN.contains(fqn))
			return;

		switch (fqn) {
			case BUNDLE_LICENSE :
				doLicense(annotation);
				break;
			case REQUIRE_CAPABILITY :
				doRequireCapability(annotation);
				break;
			case PROVIDE_CAPABILITY :
				doProvideCapability(annotation);
				break;
			case BUNDLE_CATEGORY :
				doBundleCategory(annotation.getAnnotation(BundleCategory.class));
				break;
			case BUNDLE_DOC_URL :
				doBundleDocURL(annotation.getAnnotation(BundleDocURL.class));
				break;
			case BUNDLE_DEVELOPERS :
				doBundleDevelopers(annotation.getAnnotation(BundleDevelopers.class));
				break;
			case BUNDLE_CONTRIBUTORS :
				doBundleContributors(annotation.getAnnotation(BundleContributors.class));
				break;
			case BUNDLE_COPYRIGHT :
				doBundeCopyright(annotation.getAnnotation(BundleCopyright.class));
				break;
			case STD_REQUIREMENT :
				doRequirement(annotation, annotation.getAnnotation(Requirement.class));
				break;
			case STD_REQUIREMENTS :
				Requirement[] requirements = annotation.getAnnotation(Requirements.class).value();
				Object[] reqAnnotations = annotation.get("value");
				for (int i = 0; i < requirements.length; i++) {
					doRequirement((Annotation) reqAnnotations[i], requirements[i]);
				}
				break;
			case STD_CAPABILITY :
				doCapability(annotation,
						annotation.getAnnotation(Capability.class));
				break;
			case STD_CAPABILITIES :
				Capability[] capabilities = annotation.getAnnotation(Capabilities.class).value();
				Object[] capAnnotations = annotation.get("value");
				for (int i = 0; i < capabilities.length; i++) {
					doCapability((Annotation) capAnnotations[i], capabilities[i]);
				}
				break;
			case STD_HEADER :
				Header header = annotation.getAnnotation(Header.class);
				add(header.name(), header.value());
				break;
			case STD_HEADERS :
				for (Header h : annotation.getAnnotation(Headers.class).value()) {
					add(h.name(), h.value());
				}
				break;
			default :
				doAnnotatedAnnotation(annotation, name);
				break;
		}
	}

	/**
	 * Handle the case where an annotation is annotated by one of our header
	 * annotations.
	 * 
	 * @param annotation
	 * @param name
	 * @throws Exception
	 */
	void doAnnotatedAnnotation(final Annotation annotation, TypeRef name) throws Exception {
		final String fqn = name.getFQN();
		if (processed.contains(fqn)) {
			analyzer.getLogger().debug("Detected an annotation cycle when processing %s. The cycled annotation was %s",
					current.getFQN(), fqn);
			return;
		}
		final Clazz c = analyzer.findClass(name);
		if (c != null && c.annotations != null) {
			boolean scanThisType = false;
			for (TypeRef tr : c.annotations) {
				String trName = tr.getFQN();
				// No point in scanning core Java annotations
				if (tr.isJava() || DO_NOT_SCAN.contains(trName)) {
					continue;
				}

				if (interesting.contains(trName)) {
					scanThisType = true;
				} else {
					processed.add(fqn);
					doAnnotatedAnnotation(annotation, tr);
				}
			}
			if (scanThisType) {
				c.parseClassFileWithCollector(new ClassDataCollector() {
					private MethodDef			lastMethodSeen;

					private Attrs	attributesAndDirectives	= new Attrs();
					
					@Override
					public void annotation(Annotation a) throws Exception {
						if (STD_ATTRIBUTE.equals(a.getName()
							.getFQN()) || STD_DIRECTIVE.equals(
								a.getName()
									.getFQN())) {
							handleAttributeOrDirective(a);
						}

						if (interesting.contains(a.getName().getFQN())) {
							// Bnd annotations support merging of child properties,
							// but this is not in the specification as far as I can tell
							if(isBndAnnotation(a)) {
								a.merge(annotation);
								a.addDefaults(c);
							} else if (isRequirementOrCapability(a)) {
								mergeAttributesAndDirectives(a);
							}
							AnnotationHeaders.this.annotation(a);
						}
					}

					private void mergeAttributesAndDirectives(Annotation a) {
						if (STD_CAPABILITIES.equals(a.getName()
							.getFQN()) || STD_REQUIREMENTS.equals(
								a.getName()
									.getFQN())) {
							Object[] annotations = a.get("value");
							for (int i = 0; i < annotations.length; i++) {
								mergeAttributesAndDirectives((Annotation) annotations[i]);
							}
						} else {
							Stream<String> toAdd = attributesAndDirectives.entrySet()
								.stream()
								.map(e -> e.getKey() + "=" + e.getValue());
							
							String[] original = a.get("attribute");
							original = original == null ? new String[0] : original;
							
							String[] updated = Stream.concat(Arrays.stream(original), toAdd)
								.collect(Collectors.toList())
								.toArray(original);

							a.put("attribute", updated);
						}
					}

					private void handleAttributeOrDirective(Annotation a) {
						Object o = annotation.get(lastMethodSeen.getName());

						if (o != null) {
							String attributeName = a.get("value");
							if (attributeName == null) {
								attributeName = lastMethodSeen.getName();
							}
							if (STD_ATTRIBUTE.equals(a.getName()
								.getFQN())) {
								attributesAndDirectives.putTyped(attributeName, o);
							} else {
								attributesAndDirectives.putTyped(attributeName + ":", o);
							}
						}
					}

					@Override
					public void method(MethodDef defined) {
						lastMethodSeen = defined;
					}
				});
			}
		} else if (c == null) {
			// Don't repeatedly log for the same missing annotation
			if (loggedMissing.add(fqn)) {
				// Only issue a warning if pedantic
				if (analyzer.isPedantic()) {
					analyzer.warning(
							"Unable to determine whether the meta annotation %s applied to type %s provides bundle annotations as it is not on the project build path. If this annotation does provide bundle annotations then it must be present on the build path in order to be processed",
							fqn, current.getFQN());
				} else {
					LOGGER.info(
							"Unable to determine whether the meta annotation {} applied to type {} provides bundle annotations as it is not on the project build path. If this annotation does provide bundle annotations then it must be present on the build path in order to be processed",
							fqn, current.getFQN());
				}
			}
		}
	}

	private boolean isBndAnnotation(Annotation a) {
		return a.getName().getFQN().startsWith("aQute.bnd.annotation.headers");
	}

	private boolean isRequirementOrCapability(Annotation a) {
		String name = a.getName()
			.getFQN();
		return STD_CAPABILITIES.equals(name) || STD_CAPABILITY.equals(name) || STD_REQUIREMENTS.equals(name)
			|| STD_REQUIREMENT.equals(name);
	}

	/*
	 * Called after the class space has been parsed. We then continue to parse
	 * the used annotations.
	 */
	public void close() throws IOException {}

	/*
	 * Bundle-Developers header
	 */
	private void doBundleDevelopers(BundleDevelopers annotation) throws IOException {
		StringBuilder sb = new StringBuilder(annotation.value());
		if (annotation.name() != null) {
			sb.append(";name='");
			escape(sb, annotation.name());
			sb.append("'");
		}
		if (annotation.roles() != null) {
			sb.append(";roles='");
			escape(sb, annotation.roles());
			sb.append("'");
		}
		if (annotation.organizationUrl() != null) {
			sb.append(";organizationUrl='");
			escape(sb, annotation.organizationUrl());
			sb.append("'");
		}
		if (annotation.organization() != null) {
			sb.append(";organization='");
			escape(sb, annotation.organization());
			sb.append("'");
		}
		if (annotation.timezone() != 0)
			sb.append(";timezone=").append(annotation.timezone());

		add(Constants.BUNDLE_DEVELOPERS, sb.toString());
	}

	/*
	 * Bundle-Contributors header
	 */

	private void doBundleContributors(BundleContributors annotation) throws IOException {
		StringBuilder sb = new StringBuilder(annotation.value());
		if (annotation.name() != null) {
			sb.append(";name='");
			escape(sb, annotation.name());
			sb.append("'");
		}
		if (annotation.roles() != null) {
			sb.append(";roles='");
			escape(sb, annotation.roles());
			sb.append("'");
		}
		if (annotation.organizationUrl() != null) {
			sb.append(";organizationUrl='");
			escape(sb, annotation.organizationUrl());
			sb.append("'");
		}
		if (annotation.organization() != null) {
			sb.append(";organization='");
			escape(sb, annotation.organization());
			sb.append("'");
		}
		if (annotation.timezone() != 0)
			sb.append(";timezone=").append(annotation.timezone());
		add(Constants.BUNDLE_CONTRIBUTORS, sb.toString());
	}

	/*
	 * Bundle-Copyright header
	 */
	private void doBundeCopyright(BundleCopyright annotation) throws IOException {
		add(Constants.BUNDLE_COPYRIGHT, annotation.value());
	}

	/*
	 * Bundle-DocURL header
	 */
	private void doBundleDocURL(BundleDocURL annotation) throws IOException {
		add(Constants.BUNDLE_DOCURL, annotation.value());
	}

	/*
	 * Bundle-Category header
	 */
	private void doBundleCategory(BundleCategory annotation) throws IOException {
		if (annotation.custom() != null)
			for (String s : annotation.custom()) {
				add(Constants.BUNDLE_CATEGORY, s);
			}

		if (annotation.value() != null)
			for (Category s : annotation.value()) {
				add(Constants.BUNDLE_CATEGORY, s.toString());
			}
	}

	/*
	 * Provide-Capability header
	 */
	private void doProvideCapability(Annotation a) throws Exception {
		ProvideCapability annotation = a.getAnnotation(ProvideCapability.class);

		Parameters p = new Parameters();
		Attrs attrs = getAttributes(a, "ns");
		directivesAndVersion(attrs, "uses", "mandatory", "effective");
		p.put(annotation.ns(), attrs);

		String value = attrs.remove("name");
		if (value != null)
			attrs.put(annotation.ns(), value);

		value = attrs.remove("value");

		String s = p.toString();
		if (value != null)
			s += ";" + annotation.value();

		add(Constants.PROVIDE_CAPABILITY, s);
	}

	/*
	 * Require-Capability header
	 */
	private void doRequireCapability(Annotation a) throws Exception {
		RequireCapability annotation = a.getAnnotation(RequireCapability.class);
		Parameters p = new Parameters();
		Attrs attrs = getAttributes(a, "ns");
		directivesAndVersion(attrs, "filter", "effective", "resolution");
		replaceParameters(attrs);

		if ("".equals(attrs.get(Constants.FILTER_DIRECTIVE)))
			attrs.remove(Constants.FILTER_DIRECTIVE);

		p.put(annotation.ns(), attrs);

		String s = p.toString();

		String extra = annotation.extra();
		if (extra != null) {
			extra = extra.trim();
			if (extra.length() > 0)
				s += ";" + extra;
		}

		add(Constants.REQUIRE_CAPABILITY, s);
	}

	private void replaceParameters(Attrs attrs) throws IllegalArgumentException {
		for (Entry<String,String> entry : attrs.entrySet()) {
			boolean modified = false;
			StringBuffer sb = new StringBuffer();

			Matcher matcher = SIMPLE_PARAM_PATTERN.matcher(entry.getValue());
			while (matcher.find()) {
				modified = true;
				String key = matcher.group(1);
				String substitution = attrs.get(key);
				if (substitution == null) {
					matcher.appendReplacement(sb, "");
					sb.append(matcher.group(0));
				} else if (SIMPLE_PARAM_PATTERN.matcher(substitution).find())
					throw new IllegalArgumentException("nested substitutions not permitted");
				else
					matcher.appendReplacement(sb, substitution);
			}

			if (modified) {
				matcher.appendTail(sb);
				entry.setValue(sb.toString());
			}
		}
	}

	/*
	 * Bundle-License header
	 */
	private void doLicense(Annotation a) throws Exception {
		BundleLicense annotation = a.getAnnotation(BundleLicense.class);
		Parameters p = new Parameters();
		p.put(annotation.name(), getAttributes(a, "name"));
		add(Constants.BUNDLE_LICENSE, p.toString());
	}

	/*
	 * Require-Capability header
	 */
	private void doRequirement(Annotation a, Requirement annotation) throws Exception {

		StringBuilder req = new StringBuilder();

		req.append(annotation.namespace());

		String filter = getFilter(a, annotation);

		if (filter.isEmpty()) {
			analyzer.error(
					"The Requirement annotation with namespace %s applied to class %s did not define any filter information.",
					annotation.namespace(), current.getFQN());
			return;
		} else {
			req.append(";filter:='").append(filter).append('\'');
		}

		if (a.keySet().contains("resolution")) {
			req.append(";resolution:=")
				.append(annotation.resolution());
		}

		if (a.keySet().contains("cardinality")) {
			req.append(";cardinality:=")
				.append(annotation.cardinality());
		}

		if (a.keySet().contains("effective")) {
			req.append(";effective:=");
			escape(req, annotation.effective());
		}

		for (String attr : annotation.attribute()) {
			req.append(';').append(attr);
		}

		add(Constants.REQUIRE_CAPABILITY, req.toString());
	}

	private String getFilter(Annotation a, Requirement annotation) {
		StringBuilder filter = new StringBuilder();

		boolean addAnd = false;
		if (a.keySet().contains("filter")) {
			filter.append(annotation.filter());
			addAnd = true;
		}

		boolean andAdded = false;
		if (a.keySet().contains("name")) {
			filter.append('(').append(annotation.namespace()).append('=').append(annotation.name()).append(')');
			if (addAnd) {
				filter.insert(0, "(&").append(')');
				andAdded = true;
			}
			addAnd = true;
		}

		if (a.keySet().contains("version")) {
			Version floor = Version.parseVersion(annotation.version());
			Version max = new Version(floor.getMajor() + 1);

			int current = filter.lastIndexOf(")");

			filter.append("(&(version>=").append(floor).append(")(!(version>=").append(max).append(")))");

			if (andAdded) {
				filter.deleteCharAt(current).append(')');
			} else if (addAnd) {
				filter.insert(0, "(&").append(')');
			}
		}
		return filter.toString();
	}

	/*
	 * Provide-Capability header
	 */
	private void doCapability(Annotation a, Capability annotation) throws Exception {

		StringBuilder cap = new StringBuilder();

		cap.append(annotation.namespace());

		if (a.keySet().contains("name")) {
			cap.append(';').append(annotation.namespace()).append('=').append(annotation.name());
		}

		if (a.keySet().contains("version")) {
			try {
				Version.parseVersion(annotation.version());
				cap.append(";version:Version=").append(annotation.version());
			} catch (Exception e) {
				analyzer.error("The version declared by the Capability annotation attached to type %s is invalid",
						current.getFQN());
			}
		}

		for (String attr : annotation.attribute()) {
			cap.append(';').append(attr);
		}

		Arrays.stream(annotation.uses())
				.map(Class::getPackage)
				.map(Package::getName)
				.distinct()
				.reduce((x, y) -> x + "," + y)
				.ifPresent(s -> cap.append(";uses:=").append(s));

		if (a.keySet().contains("effective")) {
			cap.append(";effective:=");
			escape(cap, annotation.effective());
		}

		add(Constants.PROVIDE_CAPABILITY, cap.toString());
	}

	private void directivesAndVersion(Attrs attrs, String... directives) {
		for (String directive : directives) {
			String s = attrs.remove(directive);
			if (s != null) {
				attrs.put(directive + ":", s);
			}
		}

		String remove = attrs.remove(Constants.VERSION_ATTRIBUTE);
		if (remove != null) {
			attrs.putTyped("version", Version.parseVersion(remove));
		}
	}

	private Attrs getAttributes(Annotation a, String... ignores) {
		Attrs attrs = new Attrs();
		outer: for (String key : a.keySet()) {

			for (String ignore : ignores) {
				if (key.equals(ignore))
					continue outer;
			}

			attrs.putTyped(key, a.get(key));
		}
		return attrs;
	}

	/*
	 * Adds a header. Will preprocess the text.
	 */
	private void add(String name, String value) throws IOException {
		if (value == null)
			return;

		Processor next = new Processor(analyzer);
		next.setProperty("@class", current.getFQN());
		next.setProperty("@class-short", current.getClassName().getShortName());
		PackageRef pref = current.getClassName().getPackageRef();
		next.setProperty("@package", pref.getFQN());
		Attrs info = analyzer.getClasspathExports().get(pref);
		if (info == null)
			info = analyzer.getContained().get(pref);

		if (info != null && info.containsKey("version")) {
			next.setProperty("@version", info.get("version"));
		}
		Macro macro = next.getReplacer();

		/*
		 * These strings come from code, which might also be included from
		 * external parties. So we just do not want to call any system commands
		 * from these sources
		 */
		boolean prev = macro.setNosystem(true);
		try {
			value = macro.process(value);
			headers.add(name, value);
			if (!analyzer.keySet()
				.contains(name)) {
				// The header isn't in the bnd configuration, so we need to add
				// it lest bnd completely ignores the header added by the
				// annotation
				analyzer.set(name, value);
			}
			next.close();
		} finally {
			macro.setNosystem(prev);
		}
	}

	/*
	 * This method is a pass thru for the properties of the analyzer. If we have
	 * such a header, we get the analyzer header and concatenate our values
	 * after removing dups.
	 */

	public String getHeader(String name) {
		String value = analyzer.getProperty(name);
		if (headers.containsKey(name)) {
			//
			// Remove duplicates and sort
			//
			Set<String> set = new TreeSet<String>(headers.get(name));
			if (value != null)
				set.add(value);
			return Strings.join(set);
		}
		return value;
	}

	private void escape(StringBuilder app, String s[]) throws IOException {
		String joined = Strings.join(s);
		escape(app, joined);
	}

	private void escape(StringBuilder app, String s) throws IOException {
		Processor.quote(app, s);
	}

}
