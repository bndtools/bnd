package aQute.bnd.osgi;

import static aQute.lib.exceptions.ConsumerWithException.asConsumer;
import static java.util.Collections.emptySet;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.headers.BundleCategory;
import aQute.bnd.annotation.headers.BundleContributors;
import aQute.bnd.annotation.headers.BundleCopyright;
import aQute.bnd.annotation.headers.BundleDevelopers;
import aQute.bnd.annotation.headers.BundleDocURL;
import aQute.bnd.annotation.headers.BundleLicense;
import aQute.bnd.annotation.headers.Category;
import aQute.bnd.bundle.annotations.Capabilities;
import aQute.bnd.bundle.annotations.Capability;
import aQute.bnd.bundle.annotations.Header;
import aQute.bnd.bundle.annotations.Headers;
import aQute.bnd.bundle.annotations.Requirement;
import aQute.bnd.bundle.annotations.Requirements;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.MultiMap;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
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

	private static final Logger		logger	= LoggerFactory.getLogger(AnnotationHeaders.class);

	private static final Converter	CONVERTER;

	static {
		CONVERTER = new Converter();
		CONVERTER.hook(null, (t, o) -> {
			if (o.getClass()
				.isArray() && String.class.equals(t)) {
				return Strings.join(",", (Object[]) o);
			}
			return null;
		});
	}

	private static final Instruction	ANNOTATION_INSTRUCTION	= new Instruction("java.lang.annotation.Annotation");

	static final Pattern				SIMPLE_PARAM_PATTERN	= Pattern
		.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");

	// Annotations to ignore scanning further because they are known to be
	// uninteresting
	// to this scanner. This speeds us up (a little) and also avoids printing
	// warnings
	// for annotations that aren't supposed to be needed on the classpath
	// (usually
	// OSGi versioning annotations).
	static final Set<String>			DO_NOT_SCAN;

	static {
		DO_NOT_SCAN = Stream
			.of("org.osgi.annotation.versioning.ProviderType", "org.osgi.annotation.versioning.ConsumerType",
				"org.osgi.annotation.versioning.Version")
			.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
	}

	final Analyzer					analyzer;
	final MultiMap<String, String>	headers						= new MultiMap<>();

	//
	// Constant Strings for a fast switch statement
	//

	static final String				CARDINALITY					= "aQute.bnd.annotation.Cardinality";
	static final String				RESOLUTION					= "aQute.bnd.annotation.Resolution";
	static final String				BUNDLE_LICENSE				= "aQute.bnd.annotation.headers.BundleLicense";
	static final String				REQUIRE_CAPABILITY			= "aQute.bnd.annotation.headers.RequireCapability";
	static final String				PROVIDE_CAPABILITY			= "aQute.bnd.annotation.headers.ProvideCapability";
	static final String				BUNDLE_CATEGORY				= "aQute.bnd.annotation.headers.BundleCategory";
	static final String				BUNDLE_DOC_URL				= "aQute.bnd.annotation.headers.BundleDocURL";
	static final String				BUNDLE_DEVELOPERS			= "aQute.bnd.annotation.headers.BundleDevelopers";
	static final String				BUNDLE_CONTRIBUTORS			= "aQute.bnd.annotation.headers.BundleContributors";
	static final String				BUNDLE_COPYRIGHT			= "aQute.bnd.annotation.headers.BundleCopyright";
	static final String				STD_REQUIREMENT				= "org.osgi.annotation.bundle.Requirement";
	static final String				STD_REQUIREMENT_CARDINALITY	= "org.osgi.annotation.bundle.Requirement$Cardinality";
	static final String				STD_REQUIREMENT_RESOLUTION	= "org.osgi.annotation.bundle.Requirement$Resolution";
	static final String				STD_REQUIREMENTS			= "org.osgi.annotation.bundle.Requirements";
	static final String				STD_CAPABILITY				= "org.osgi.annotation.bundle.Capability";
	static final String				STD_CAPABILITIES			= "org.osgi.annotation.bundle.Capabilities";
	static final String				STD_HEADER					= "org.osgi.annotation.bundle.Header";
	static final String				STD_HEADERS					= "org.osgi.annotation.bundle.Headers";

	// Used to detect attributes and directives on Require-Capability and
	// Provide-Capability
	static final String				STD_ATTRIBUTE				= "org.osgi.annotation.bundle.Attribute";
	static final String				STD_DIRECTIVE				= "org.osgi.annotation.bundle.Directive";

	// Class we're currently processing
	Clazz							current;

	// The annotations we could not load. used to avoid repeatedly logging the
	// same missing annotation for the same project. Note that this should not
	// be reset for each #classStart(Clazz).
	final Set<String>				loggedMissing				= new HashSet<>();
	final Instructions				instructions;

	// we parse the annotations separately at the end
	boolean							finalizing;

	static String convert(Object value) {
		try {
			return CONVERTER.convert(String.class, value);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/*
	 * Initialize
	 */
	AnnotationHeaders(Analyzer analyzer) {
		this(analyzer, new Instructions("*"));
	}

	AnnotationHeaders(Analyzer analyzer, Instructions instructions) {
		this.analyzer = analyzer;
		this.instructions = instructions;
	}

	@Override
	public boolean classStart(Clazz c) {
		//
		// Parse any annotated classes except annotations
		//
		if (!c.isAnnotation() && !c.annotations()
			.isEmpty()) {

			for (Instruction instruction : instructions.keySet()) {
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						current = null;
						return false;
					}

					current = c;
					return true;
				}
			}
		}
		current = null;
		return false;
	}

	/*
	 * Called when an annotation is found. Dispatch on the known types.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void annotation(Annotation annotation) throws Exception {
		TypeRef name = annotation.getName();
		String fqn = name.getFQN();

		if (name.isJava() || DO_NOT_SCAN.contains(fqn))
			return;

		switch (fqn) {
			case BUNDLE_CATEGORY :
				doBundleCategory(annotation, annotation.getAnnotation(BundleCategory.class));
				break;
			case BUNDLE_CONTRIBUTORS :
				doBundleContributors(annotation, annotation.getAnnotation(BundleContributors.class));
				break;
			case BUNDLE_COPYRIGHT :
				doBundleCopyright(annotation, annotation.getAnnotation(BundleCopyright.class));
				break;
			case BUNDLE_DEVELOPERS :
				doBundleDevelopers(annotation, annotation.getAnnotation(BundleDevelopers.class));
				break;
			case BUNDLE_DOC_URL :
				doBundleDocURL(annotation, annotation.getAnnotation(BundleDocURL.class));
				break;
			case BUNDLE_LICENSE :
				doLicense(annotation, annotation.getAnnotation(BundleLicense.class));
				break;
			case PROVIDE_CAPABILITY :
				doProvideCapability(annotation,
					annotation.getAnnotation(aQute.bnd.annotation.headers.ProvideCapability.class));
				break;
			case REQUIRE_CAPABILITY :
				doRequireCapability(annotation,
					annotation.getAnnotation(aQute.bnd.annotation.headers.RequireCapability.class));
				break;
			case STD_CAPABILITIES :
				Capability[] capabilities = annotation.getAnnotation(Capabilities.class)
					.value();
				Object[] capAnnotations = annotation.get("value");
				for (int i = 0; i < capabilities.length; i++) {
					doCapability((Annotation) capAnnotations[i], capabilities[i]);
				}
				break;
			case STD_CAPABILITY :
				doCapability(annotation, annotation.getAnnotation(Capability.class));
				break;
			case STD_HEADER :
				Header header = annotation.getAnnotation(Header.class);
				add(annotation, header.name(), header.value());
				break;
			case STD_HEADERS :
				Header[] headers = annotation.getAnnotation(Headers.class)
					.value();
				Object[] headerAnnotations = annotation.get("value");
				for (int i = 0; i < headers.length; i++) {
					add((Annotation) headerAnnotations[i], headers[i].name(), headers[i].value());
				}
				break;
			case STD_REQUIREMENT :
				doRequirement(annotation, annotation.getAnnotation(Requirement.class));
				break;
			case STD_REQUIREMENTS :
				Requirement[] requirements = annotation.getAnnotation(Requirements.class)
					.value();
				Object[] reqAnnotations = annotation.get("value");
				for (int i = 0; i < requirements.length; i++) {
					doRequirement((Annotation) reqAnnotations[i], requirements[i]);
				}
				break;
			default :
				// Handle annotations in a repeatable container annotation
				Object value = annotation.get("value");
				if (value instanceof Object[]) {
					Object[] container = (Object[]) value;
					if ((container.length > 0) && (container[0] instanceof Annotation)) {
						if (Optional.ofNullable(analyzer.findClass(((Annotation) container[0]).getName()))
							.flatMap(c -> c.annotations("java/lang/annotation/Repeatable")
								.findFirst())
							.filter(a -> name.equals(a.get("value")))
							.isPresent()) {
							for (Object a : container) {
								Annotation repeatable = (Annotation) a;
								doAnnotatedAnnotation(repeatable, repeatable.getName(), emptySet(), new Attrs());
							}
						}
					}
				}
				doAnnotatedAnnotation(annotation, name, emptySet(), new Attrs());
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
	void doAnnotatedAnnotation(final Annotation annotation, TypeRef name, Set<String> processed, Attrs baseAttrs)
		throws Exception {

		final String fqn = name.getFQN();
		if (processed.contains(fqn)) {
			return;
		}

		// No point scanning types that definitely aren't going anywhere
		if (name.isJava() || DO_NOT_SCAN.contains(fqn)) {
			return;
		}

		final Clazz c = analyzer.findClass(name);
		if (c == null) {
			// Don't repeatedly log for the same missing annotation
			if (loggedMissing.add(fqn)) {
				// Only issue a warning if pedantic
				if (analyzer.isPedantic()) {
					analyzer.warning(
						"Unable to determine whether the meta annotation %s applied to type %s provides bundle annotations as it is not on the project build path. If this annotation does provide bundle annotations then it must be present on the build path in order to be processed",
						fqn, current.getFQN());
				} else {
					logger.debug(
						"Unable to determine whether the meta annotation {} applied to type {} provides bundle annotations as it is not on the project build path. If this annotation does provide bundle annotations then it must be present on the build path in order to be processed",
						fqn, current.getFQN());
				}
			}
		} else {
			// If this annotation has meta-annotations then it may be relevant
			// to us
			if (!c.annotations()
				.isEmpty()) {
				c.parseClassFileWithCollector(new MetaAnnotationCollector(c, annotation, processed, baseAttrs));
			}
		}
	}

	private final class MetaAnnotationCollector extends ClassDataCollector {
		private final Clazz			c;
		private final Annotation	annotation;
		private MethodDef			lastMethodSeen;
		private Set<String>			processed;
		private Attrs				attributesAndDirectives	= new Attrs();

		private MetaAnnotationCollector(Clazz c, Annotation annotation, Set<String> processed, Attrs baseAttrs) {
			this.c = c;
			this.annotation = annotation;
			this.processed = processed;
			this.attributesAndDirectives = new Attrs(baseAttrs);
		}

		@Override
		public void annotation(Annotation a) throws Exception {
			String fqn = a.getName()
				.getFQN();
			switch (fqn) {
				case BUNDLE_CATEGORY :
				case BUNDLE_CONTRIBUTORS :
				case BUNDLE_COPYRIGHT :
				case BUNDLE_DEVELOPERS :
				case BUNDLE_DOC_URL :
				case BUNDLE_LICENSE :
				case PROVIDE_CAPABILITY :
				case REQUIRE_CAPABILITY :
					// Bnd annotations support merging of child
					// properties,
					// but this is not in the specification as far as I
					// can tell
					a.merge(annotation);
					a.addDefaults(c);
					AnnotationHeaders.this.annotation(a);
					break;
				case STD_CAPABILITIES :
				case STD_CAPABILITY :
				case STD_REQUIREMENT :
				case STD_REQUIREMENTS :
					mergeAttributesAndDirectives(a);
					AnnotationHeaders.this.annotation(a);
					break;
				case STD_HEADER :
				case STD_HEADERS :
					AnnotationHeaders.this.annotation(a);
					break;
				case STD_ATTRIBUTE :
				case STD_DIRECTIVE :
					handleAttributeOrDirective(a);
					break;
				default :
					Set<String> processed = new HashSet<>(this.processed);
					processed.add(c.getFQN());
					doAnnotatedAnnotation(a, a.getName(), processed, attributesAndDirectives);
					break;
			}
		}

		private void mergeAttributesAndDirectives(Annotation a) {
			String fqn = a.getName()
				.getFQN();
			switch (fqn) {
				case STD_CAPABILITIES :
				case STD_REQUIREMENTS :
					Object[] annotations = a.get("value");
					for (int i = 0; i < annotations.length; i++) {
						mergeAttributesAndDirectives((Annotation) annotations[i]);
					}
					break;
				default :
					if (c.isAnnotation()) {
						c.methods()
							.forEach(asConsumer(method -> {
								TypeRef returnType = method.getType();
								if (returnType.isArray()) {
									returnType = returnType.getComponentTypeRef();
								}
								Clazz c = analyzer.findClass(returnType);
								if ((c != null) && c.is(QUERY.IMPLEMENTS, ANNOTATION_INSTRUCTION, analyzer)) {
									return;
								}
								Object object = getOrDefault(method);
								if (object == null)
									return;
								a.put("#" + method.getName(), object);
							}));
					}
					if (!attributesAndDirectives.isEmpty()) {
						Object[] original = a.get("attribute");
						int length = (original != null) ? original.length : 0;
						Object[] updated = new Object[length + attributesAndDirectives.size()];
						if (length > 0) {
							System.arraycopy(original, 0, updated, 0, length);
						}
						for (String key : attributesAndDirectives.keySet()) {
							updated[length++] = attributesAndDirectives.toString(key);
						}
						a.put("attribute", updated);
					}
					break;
			}
		}

		private void handleAttributeOrDirective(Annotation a) {
			Object o = annotation.get(lastMethodSeen.getName());

			if (o != null) {
				String attributeName = a.get("value");
				if (attributeName == null) {
					attributeName = lastMethodSeen.getName();
				}
				if (STD_DIRECTIVE.equals(a.getName()
					.getFQN())) {
					attributeName += ":";
				}
				if (!attributesAndDirectives.containsKey(attributeName)) {
					if (lastMethodSeen.getType()
						.getFQN()
						.equals(STD_REQUIREMENT_CARDINALITY)
						|| lastMethodSeen.getType()
							.getFQN()
							.equals(STD_REQUIREMENT_RESOLUTION)) {

						o = String.valueOf(o)
							.toLowerCase();
					}

					attributesAndDirectives.putTyped(attributeName, o);
				}
			}
		}

		@Override
		public void method(MethodDef defined) {
			lastMethodSeen = defined;
		}

		private Object getOrDefault(MethodDef method) {
			Object object = annotation.get(method.getName());

			if (object == null) {
				try {
					object = c.getDefaults()
						.get(method.getName());
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

			if ((object instanceof Object[]) && (((Object[]) object).length > 0)
				&& ((Object[]) object)[0] instanceof TypeRef) {

				Object[] typeRefs = (Object[]) object;
				Object[] copy = new Object[typeRefs.length];
				for (int i = 0; i < typeRefs.length; i++) {
					// we need to replace the value with the
					// target type
					if (Target.class.getName()
						.equals(typeRefs[i].toString())) {
						copy[i] = current.getClassName();
					} else {
						copy[i] = typeRefs[i];
					}
				}

				object = copy;
			} else if ((object instanceof TypeRef) && Target.class.getName()
				.equals(object.toString())) {

				// we need to replace the value with the
				// target type
				object = current.getClassName();
			}

			String returnFQN = method.getType()
				.getFQN();

			if ((object != null) && (returnFQN.equals(CARDINALITY) || returnFQN.equals(RESOLUTION)
				|| returnFQN.equals(STD_REQUIREMENT_CARDINALITY) || returnFQN.equals(STD_REQUIREMENT_RESOLUTION))) {

				object = String.valueOf(object)
					.toLowerCase();
			}

			return object;
		}

	}

	/*
	 * Called after the class space has been parsed. We then continue to parse
	 * the used annotations.
	 */
	@Override
	public void close() throws IOException {}

	/*
	 * Bundle-Developers header
	 */
	private void doBundleDevelopers(Annotation a, BundleDevelopers annotation) throws IOException {
		StringBuilder sb = new StringBuilder(annotation.value());
		if (!"".equals(annotation.name())) {
			sb.append(";name='");
			escape(sb, annotation.name());
			sb.append("'");
		}
		if (annotation.roles().length > 0) {
			sb.append(";roles='");
			escape(sb, annotation.roles());
			sb.append("'");
		}
		if (!"".equals(annotation.organizationUrl())) {
			sb.append(";organizationUrl='");
			escape(sb, annotation.organizationUrl());
			sb.append("'");
		}
		if (!"".equals(annotation.organization())) {
			sb.append(";organization='");
			escape(sb, annotation.organization());
			sb.append("'");
		}
		if (annotation.timezone() != 0)
			sb.append(";timezone=")
				.append(annotation.timezone());

		add(a, Constants.BUNDLE_DEVELOPERS, sb.toString());
	}

	/*
	 * Bundle-Contributors header
	 */

	private void doBundleContributors(Annotation a, BundleContributors annotation) throws IOException {
		StringBuilder sb = new StringBuilder(annotation.value());
		if (!"".equals(annotation.name())) {
			sb.append(";name='");
			escape(sb, annotation.name());
			sb.append("'");
		}
		if (annotation.roles().length > 0) {
			sb.append(";roles='");
			escape(sb, annotation.roles());
			sb.append("'");
		}
		if (!"".equals(annotation.organizationUrl())) {
			sb.append(";organizationUrl='");
			escape(sb, annotation.organizationUrl());
			sb.append("'");
		}
		if (!"".equals(annotation.organization())) {
			sb.append(";organization='");
			escape(sb, annotation.organization());
			sb.append("'");
		}
		if (annotation.timezone() != 0)
			sb.append(";timezone=")
				.append(annotation.timezone());
		add(a, Constants.BUNDLE_CONTRIBUTORS, sb.toString());
	}

	/*
	 * Bundle-Copyright header
	 */
	private void doBundleCopyright(Annotation a, BundleCopyright annotation) throws IOException {
		add(a, Constants.BUNDLE_COPYRIGHT, annotation.value());
	}

	/*
	 * Bundle-DocURL header
	 */
	private void doBundleDocURL(Annotation a, BundleDocURL annotation) throws IOException {
		add(a, Constants.BUNDLE_DOCURL, annotation.value());
	}

	/*
	 * Bundle-Category header
	 */
	private void doBundleCategory(Annotation a, BundleCategory annotation) throws IOException {
		if (annotation.custom().length > 0)
			for (String s : annotation.custom()) {
				add(a, Constants.BUNDLE_CATEGORY, s);
			}

		if (annotation.value() != null)
			for (Category s : annotation.value()) {
				add(a, Constants.BUNDLE_CATEGORY, s.toString());
			}
	}

	/*
	 * Provide-Capability header
	 */
	@SuppressWarnings("deprecation")
	private void doProvideCapability(Annotation a, aQute.bnd.annotation.headers.ProvideCapability annotation)
		throws Exception {

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

		add(a, Constants.PROVIDE_CAPABILITY, s);
	}

	/*
	 * Require-Capability header
	 */
	@SuppressWarnings("deprecation")
	private void doRequireCapability(Annotation a, aQute.bnd.annotation.headers.RequireCapability annotation)
		throws Exception {
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

		add(a, Constants.REQUIRE_CAPABILITY, s);
	}

	private void replaceParameters(Attrs attrs) throws IllegalArgumentException {
		for (Entry<String, String> entry : attrs.entrySet()) {
			StringBuilder sb = new StringBuilder();
			String value = entry.getValue();
			Matcher matcher = SIMPLE_PARAM_PATTERN.matcher(value);
			int start = 0;
			for (; matcher.find(); start = matcher.end()) {
				String key = matcher.group(1);
				String replacement = attrs.get(key);
				if (replacement == null) {
					replacement = matcher.group(0);
				} else if (SIMPLE_PARAM_PATTERN.matcher(replacement)
					.find()) {
					throw new IllegalArgumentException("nested substitutions not permitted");
				}
				sb.append(value, start, matcher.start())
					.append(replacement);
			}

			if (start != 0) {
				entry.setValue(sb.append(value, start, value.length())
					.toString());
			}
		}
	}

	/*
	 * Bundle-License header
	 */
	private void doLicense(Annotation a, BundleLicense annotation) throws Exception {
		Parameters p = new Parameters();
		p.put(annotation.name(), getAttributes(a, "name"));
		add(a, Constants.BUNDLE_LICENSE, p.toString());
	}

	/*
	 * Require-Capability header
	 */
	private void doRequirement(Annotation a, Requirement annotation) throws Exception {

		StringBuilder req = new StringBuilder();

		req.append(annotation.namespace());

		String filter = getFilter(a, annotation);

		if (!filter.isEmpty()) {
			try {
				Verifier.verifyFilter(filter, 0);
			} catch (Exception e) {
				analyzer.exception(e,
					"The Requirement annotation with namespace %s applied to class %s has invalid filter information.",
					annotation.namespace(), current.getFQN());
			}
			req.append(";filter:='")
				.append(filter)
				.append('\'');
		}

		if (a.containsKey("resolution")) {
			req.append(";resolution:=")
				.append(annotation.resolution());
		}

		if (a.containsKey("cardinality")) {
			req.append(";cardinality:=")
				.append(annotation.cardinality());
		}

		if (a.containsKey("effective")) {
			req.append(";effective:=");
			escape(req, annotation.effective());
		}

		for (String attr : annotation.attribute()) {
			req.append(';')
				.append(attr);
		}

		add(a, Constants.REQUIRE_CAPABILITY, req.toString());
	}

	private String getFilter(Annotation a, Requirement annotation) {
		StringBuilder filter = new StringBuilder();

		boolean addAnd = false;
		if (a.containsKey("filter")) {
			filter.append(annotation.filter());
			addAnd = true;
		}

		boolean andAdded = false;
		if (a.containsKey("name")) {
			filter.append('(')
				.append(annotation.namespace())
				.append('=')
				.append(annotation.name())
				.append(')');
			if (addAnd) {
				filter.insert(0, "(&")
					.append(')');
				andAdded = true;
			}
			addAnd = true;
		}

		if (a.containsKey("version")) {
			if (annotation.version()
				.indexOf('$') == -1) {

				Version floor;
				try {
					floor = Version.parseVersion(annotation.version());
				} catch (Exception e) {
					floor = null;
					analyzer.exception(e,
						"The version declared by the Requirement annotation attached to type %s is invalid",
						current.getFQN());
				}

				if (floor != null) {
					int current = filter.lastIndexOf(")");

					VersionRange range = new VersionRange(floor, floor.bumpMajor());
					String rangeFilter = range.toFilter();
					filter.append(rangeFilter.substring(2, rangeFilter.length() - 1));

					if (andAdded) {
						filter.deleteCharAt(current)
							.append(')');
					} else if (addAnd) {
						filter.insert(0, "(&")
							.append(')');
					}
				}
			} else {
				String floor = annotation.version();

				int current = filter.lastIndexOf(")");

				filter.append("(version>=")
					.append(floor)
					.append(")(!(version>=${versionmask;+00;")
					.append(floor)
					.append("}))");

				if (andAdded) {
					filter.deleteCharAt(current)
						.append(')');
				} else if (addAnd) {
					filter.insert(0, "(&")
						.append(')');
				}
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

		if (a.containsKey("name")) {
			cap.append(';')
				.append(annotation.namespace())
				.append('=')
				.append(annotation.name());
		}

		if (a.containsKey("version")) {
			if (annotation.version()
				.indexOf('$') == -1) {
				try {
					Version.parseVersion(annotation.version());
				} catch (Exception e) {
					analyzer.exception(e,
						"The version declared by the Capability annotation attached to type %s is invalid",
						current.getFQN());
				}
			}
			cap.append(";version:Version=")
				.append(annotation.version());
		}

		for (String attr : annotation.attribute()) {
			cap.append(';')
				.append(attr);
		}

		if (a.containsKey("uses")) {
			cap.append(a.stream("uses", TypeRef.class) //
				.map(TypeRef::getPackageRef)
				.map(PackageRef::getFQN)
				.distinct()
				.collect(Strings.joining(",", ";uses:=\"", "\"", "")));
		}

		if (a.containsKey("effective")) {
			cap.append(";effective:=");
			escape(cap, annotation.effective());
		}

		add(a, Constants.PROVIDE_CAPABILITY, cap.toString());
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
		List<String> ignoresList = Arrays.asList(ignores);
		Attrs attrs = new Attrs();
		MapStream.of(a.entrySet())
			.filterKey(key -> !ignoresList.contains(key))
			.forEachOrdered(attrs::putTyped);
		return attrs;
	}

	/*
	 * Adds a header. Will preprocess the text.
	 */
	private void add(Annotation annotation, String name, String value) throws IOException {
		if (value == null)
			return;

		Processor next = new Processor();
		next.addProperties(MapStream.of(annotation.entrySet())
			.filterKey(k -> k.startsWith("#"))
			.map((k, v) -> {
				if (k.equals("#uses") && (v instanceof Object[])) {
					Object[] array = (Object[]) v;
					if ((array.length > 0) && (array[0] instanceof TypeRef)) {
						String converted = Arrays.stream(array)
							.map(TypeRef.class::cast)
							.map(TypeRef::getPackageRef)
							.map(PackageRef::getFQN)
							.distinct()
							.collect(Collectors.joining(","));
						return MapStream.entry(k, converted);
					}
				}
				return MapStream.entry(k, convert(v));
			})
			.collect(MapStream.toMap()));
		next.setProperty("@class", current.getFQN());
		next.setProperty("@class-short", current.getClassName()
			.getShortName());
		PackageRef pref = current.getClassName()
			.getPackageRef();
		next.setProperty("@package", pref.getFQN());
		Attrs info = analyzer.getClasspathExports()
			.get(pref);
		if (info == null)
			info = analyzer.getContained()
				.get(pref);

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
			// Re-parse to strip off any macro output that resolved to empty
			// strings
			value = OSGiHeader.parseHeader(macro.process(value))
				.toString();
			headers.add(name, value);
			if (!analyzer.keySet()
				.contains(name)) {
				// The header isn't in the bnd configuration, so we need to add
				// it lest bnd completely ignores the header added by the
				// annotation
				analyzer.setProperty(name, value);
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
			Set<String> set = new TreeSet<>(headers.get(name));
			List<String> result = new ArrayList<>(set.size() + 1);
			if (value != null && !set.contains(value))
				result.add(value); // analyzer value at start of list
			result.addAll(set);
			return Strings.join(result);
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
