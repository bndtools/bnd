package aQute.bnd.osgi;

import java.io.*;
import java.util.*;

import aQute.bnd.annotation.headers.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.collections.*;
import aQute.lib.strings.*;

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
 * @RequireCapability("osgi.webresource;filter:='(&(osgi.webresource=/google/angular)(version>=${@version}))")
 * @interface Angular {
 * }
 */
class AnnotationHeaders extends ClassDataCollector implements Closeable {

	final Analyzer					analyzer;
	final Set<TypeRef>				interesting	= new HashSet<TypeRef>();
	final Set<TypeRef>				used		= new HashSet<TypeRef>();
	final MultiMap<String,String>	headers		= new MultiMap<String,String>();
	final TypeRef					bundleLicenseRef;
	final TypeRef					requireCapabilityRef;
	final TypeRef					provideCapabilityRef;
	final TypeRef					bundleCategoryRef;
	final TypeRef					bundleDocURLRef;
	final TypeRef					bundleDeveloperRef;
	final TypeRef					bundleContributorRef;
	final TypeRef					bundleCopyrightRef;

	Clazz							current;
	boolean							finalizing;

	AnnotationHeaders(Analyzer analyzer) {
		this.analyzer = analyzer;

		//
		// The analyser has its own domain of type refs, so we need to get our
		// standard set
		//

		interesting.add(bundleLicenseRef = analyzer.getTypeRefFromFQN(BundleLicense.class.getName()));
		interesting.add(requireCapabilityRef = analyzer.getTypeRefFromFQN(RequireCapability.class.getName()));
		interesting.add(provideCapabilityRef = analyzer.getTypeRefFromFQN(ProvideCapability.class.getName()));
		interesting.add(bundleCategoryRef = analyzer.getTypeRefFromFQN(BundleCategory.class.getName()));
		interesting.add(bundleDocURLRef = analyzer.getTypeRefFromFQN(BundleDocURL.class.getName()));
		interesting.add(bundleDeveloperRef = analyzer.getTypeRefFromFQN(BundleDevelopers.class.getName()));
		interesting.add(bundleContributorRef = analyzer.getTypeRefFromFQN(BundleContributors.class.getName()));
		interesting.add(bundleCopyrightRef = analyzer.getTypeRefFromFQN(BundleCopyright.class.getName()));
	}

	@Override
	public boolean classStart(Clazz c) {
		if (finalizing) {
			current = c;
			return true;
		}

		current = null;

		if (!c.isAnnotation()) {
			if (c.annotations != null)
				used.addAll(c.annotations);
			if (c.annotations != null && containsAny(interesting, c.annotations)) {
				current = c;
				return true;
			}
		}
		return false;
	}

	public void annotation(Annotation annotation) throws Exception {
		TypeRef name = annotation.getName();
		if (interesting.contains(name)) {
			if (name == bundleLicenseRef)
				doLicense(annotation.getAnnotation(BundleLicense.class));
			else if (name == requireCapabilityRef)
				doRequireCapability(annotation.getAnnotation(RequireCapability.class));
			else if (name == provideCapabilityRef)
				doProvideCapability(annotation.getAnnotation(ProvideCapability.class));
			else if (name == bundleCategoryRef)
				doBundleCategory(annotation.getAnnotation(BundleCategory.class));
			else if (name == bundleDocURLRef)
				doBundleDocURL(annotation.getAnnotation(BundleDocURL.class));
			else if (name == bundleDeveloperRef)
				doBundleDeveloper(annotation.getAnnotation(BundleDevelopers.class));
			else if (name == bundleContributorRef)
				doBundleContributor(annotation.getAnnotation(BundleContributors.class));
			else if (name == bundleCopyrightRef)
				doBundeCopyright(annotation.getAnnotation(BundleCopyright.class));
			else
				analyzer.error("Unknon annotation %s", name);
		}
	}

	public void close() throws IOException {
		finalizing = true;
		try {
			for (TypeRef typeRef : used) {
				Clazz c = analyzer.findClass(typeRef);
				c.parseClassFileWithCollector(this);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void doBundleDeveloper(BundleDevelopers annotation) {
		StringBuilder sb = new StringBuilder(annotation.value());
		if (annotation.name() != null)
			sb.append(";name='").append(escape(annotation.name())).append("'");
		if (annotation.roles() != null)
			sb.append(";roles='").append(escape(Strings.join(annotation.roles()))).append("'");
		if (annotation.organizationUrl() != null)
			sb.append(";organizationUrl='").append(escape(annotation.organizationUrl())).append("'");
		if (annotation.organization() != null)
			sb.append(";organization='").append(escape(annotation.organization())).append("'");
		if (annotation.timezone() != 0)
			sb.append(";timezone=").append(annotation.timezone());

		add(Constants.BUNDLE_DEVELOPERS, sb.toString());
	}

	private void doBundleContributor(BundleContributors annotation) {
		StringBuilder sb = new StringBuilder(annotation.value());
		if (annotation.name() != null)
			sb.append(";name='").append(escape(annotation.name())).append("'");
		if (annotation.roles() != null)
			sb.append(";roles='").append(escape(Strings.join(annotation.roles()))).append("'");
		if (annotation.organizationUrl() != null)
			sb.append(";organizationUrl='").append(escape(annotation.organizationUrl())).append("'");
		if (annotation.organization() != null)
			sb.append(";organization='").append(escape(annotation.organization())).append("'");
		if (annotation.timezone() != 0)
			sb.append(";timezone=").append(annotation.timezone());

		add(Constants.BUNDLE_CONTRIBUTORS, sb.toString());
	}

	private void doBundeCopyright(BundleCopyright annotation) {
		add(Constants.BUNDLE_COPYRIGHT, annotation.value());
	}

	private void doBundleDocURL(BundleDocURL annotation) {
		add(Constants.BUNDLE_DOCURL, annotation.value());
	}

	private void doBundleCategory(BundleCategory annotation) {
		if (annotation.custom() != null)
			for (String s : annotation.custom()) {
				add(Constants.BUNDLE_CATEGORY, s);
			}

		if (annotation.value() != null)
			for (Category s : annotation.value()) {
				add(Constants.BUNDLE_CATEGORY, s.toString());
			}
	}

	private void doProvideCapability(ProvideCapability annotation) {
		StringBuilder sb = new StringBuilder(annotation.ns());
		if (annotation.name() != null)
			sb.append(";").append(annotation.ns()).append("='").append(annotation.name()).append("'");
		if (annotation.uses() != null)
			sb.append(";").append("uses:='").append(Strings.join(",", annotation.uses())).append("'");
		if (annotation.mandatory() != null)
			sb.append(";").append("mandatory:='").append(Strings.join(",", annotation.mandatory())).append("'");
		if (annotation.version() != null)
			sb.append(";").append("version:Version='").append(annotation.version()).append("'");
		if (annotation.value() != null)
			sb.append(";").append(annotation.value());
		if (annotation.effective() != null)
			sb.append(";effective:='").append(annotation.effective()).append("'");

		add(Constants.PROVIDE_CAPABILITY, sb.toString());
	}

	private void doRequireCapability(RequireCapability annotation) {
		StringBuilder sb = new StringBuilder(annotation.ns());
		if (annotation.filter() != null)
			sb.append(";filter:='").append(annotation.filter()).append("'");
		if (annotation.effective() != null)
			sb.append(";effective:='").append(annotation.effective()).append("'");
		if (annotation.resolution() != null)
			sb.append(";resolution:='").append(annotation.resolution()).append("'");

		if (annotation.value() != null)
			sb.append(";").append(annotation.value());

		add(Constants.REQUIRE_CAPABILITY, sb.toString());
	}

	private void doLicense(BundleLicense annotation) {
		StringBuilder sb = new StringBuilder(annotation.name());
		if (!annotation.description().equals(""))
			sb.append(";description='").append(annotation.description().replaceAll("'", "\\'")).append("'");
		if (!annotation.link().equals(""))
			sb.append(";link='").append(annotation.link().replaceAll("'", "\\'")).append("'");
		add(Constants.BUNDLE_LICENSE, sb.toString());
	}

	private void add(String name, String value) {
		if (value == null)
			return;

		@SuppressWarnings("unused")
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
		 * These strings come from code, which might also be included
		 * from external parties. So we just do not want to call any
		 * system commands from these sources
		 */
		boolean prev = macro.setNosystem(true);
		try {
			value = macro.process(value);
			headers.add(name, value);
			next.close();
		}
		finally {
			macro.setNosystem(prev);
		}
	}

	public String getHeader(String name) {
		String value = analyzer.getProperty(name);
		if (headers.containsKey(name)) {
			//
			// Remove duplicates
			//
			Set<String> set = new TreeSet<String>(headers.get(name));
			String header = Strings.join(set);
			if (value == null)
				return header;
			else
				return value + "," + header;
		}
		return value;
	}

	private <T> boolean containsAny(Set<T> a, Set<T> b) {
		for (T aa : a)
			if (b.contains(aa))
				return true;

		return false;
	}

	private CharSequence escape(String s) {
		return s.replaceAll("'", "\\'");
	}

}
