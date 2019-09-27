package bndtools.tasks;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.bndtools.utils.osgi.BundleUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.version.VersionRange;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.NotFilter;
import aQute.libg.filters.Operator;
import aQute.libg.filters.SimpleFilter;
import bndtools.model.resolution.RequirementWrapper;

public abstract class BndBuilderCapReqLoader implements CapReqLoader {

	protected final File file;

	public BndBuilderCapReqLoader(File file) {
		this.file = file;
	}

	@Override
	public String getShortLabel() {
		return file.getName();
	}

	@Override
	public String getLongLabel() {
		return file.getName() + " - " + file.getParentFile()
			.getAbsolutePath();
	}

	protected abstract Builder getBuilder() throws Exception;

	@Override
	public Map<String, List<Capability>> loadCapabilities() throws Exception {
		Builder builder = getBuilder();
		if (builder == null)
			return Collections.emptyMap();

		Jar jar = builder.getJar();
		if (jar == null)
			return Collections.emptyMap();

		Manifest manifest = jar.getManifest();
		if (manifest == null)
			return Collections.emptyMap();

		Attributes attribs = manifest.getMainAttributes();
		Map<String, List<Capability>> capMap = new HashMap<>();

		// Load export packages
		String exportsPkgStr = attribs.getValue(Constants.EXPORT_PACKAGE);
		Parameters exportsMap = new Parameters(exportsPkgStr);
		for (Entry<String, Attrs> entry : exportsMap.entrySet()) {
			String pkg = Processor.removeDuplicateMarker(entry.getKey());
			org.osgi.framework.Version version = org.osgi.framework.Version.parseVersion(entry.getValue()
				.getVersion());
			CapReqBuilder cb = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE)
				.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, pkg)
				.addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
			// TODO attributes and directives
			addCapability(capMap, cb.buildSyntheticCapability());
		}

		// Load identity/bundle/host
		String bsn = BundleUtils.getBundleSymbolicName(attribs);
		if (bsn != null) { // Ignore if not a bundle
			org.osgi.framework.Version version = org.osgi.framework.Version
				.parseVersion(attribs.getValue(Constants.BUNDLE_VERSION));
			// TODO attributes and directives
			addCapability(capMap,
				new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
					.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, bsn)
					.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version)
					.buildSyntheticCapability());
			addCapability(capMap,
				new CapReqBuilder(BundleNamespace.BUNDLE_NAMESPACE).addAttribute(BundleNamespace.BUNDLE_NAMESPACE, bsn)
					.addAttribute(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version)
					.buildSyntheticCapability());
			addCapability(capMap,
				new CapReqBuilder(HostNamespace.HOST_NAMESPACE).addAttribute(HostNamespace.HOST_NAMESPACE, bsn)
					.addAttribute(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version)
					.buildSyntheticCapability());
		}

		// Generic capabilities
		String providesStr = attribs.getValue(Constants.PROVIDE_CAPABILITY);
		Parameters provides = new Parameters(providesStr);
		for (Entry<String, Attrs> entry : provides.entrySet()) {
			String ns = Processor.removeDuplicateMarker(entry.getKey());
			Attrs attrs = entry.getValue();

			CapReqBuilder cb = new CapReqBuilder(ns);
			for (String key : attrs.keySet()) {
				if (key.endsWith(":"))
					cb.addDirective(key.substring(0, key.length() - 1), attrs.get(key));
				else
					cb.addAttribute(key, attrs.getTyped(key));
			}
			addCapability(capMap, cb.buildSyntheticCapability());
		}

		return capMap;
	}

	@Override
	public Map<String, List<RequirementWrapper>> loadRequirements() throws Exception {
		Builder builder = getBuilder();
		if (builder == null)
			return Collections.emptyMap();

		Jar jar = builder.getJar();
		if (jar == null)
			return Collections.emptyMap();
		Manifest manifest = jar.getManifest();
		if (manifest == null)
			return Collections.emptyMap();

		Attributes attribs = manifest.getMainAttributes();
		Map<String, List<RequirementWrapper>> requirements = new HashMap<>();

		// Process imports
		String importPkgStr = attribs.getValue(Constants.IMPORT_PACKAGE);
		Parameters importsMap = new Parameters(importPkgStr);
		for (Entry<String, Attrs> entry : importsMap.entrySet()) {
			String pkgName = Processor.removeDuplicateMarker(entry.getKey());
			Attrs attrs = entry.getValue();

			CapReqBuilder rb = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
			String filter = createVersionFilter(PackageNamespace.PACKAGE_NAMESPACE, pkgName,
				attrs.get(Constants.VERSION_ATTRIBUTE), PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			rb.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
			if (Constants.RESOLUTION_OPTIONAL.equals(attrs.get(Constants.RESOLUTION_DIRECTIVE + ":")))
				rb.addDirective(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);

			Collection<Clazz> importers = findImportingClasses(pkgName, builder);

			RequirementWrapper rw = new RequirementWrapper();
			rw.requirement = rb.buildSyntheticRequirement();
			rw.requirers = importers;

			addRequirement(requirements, rw);
		}

		// Process require-bundle
		String requireBundleStr = attribs.getValue(Constants.REQUIRE_BUNDLE);
		Parameters requireBundles = new Parameters(requireBundleStr);
		for (Entry<String, Attrs> entry : requireBundles.entrySet()) {
			String bsn = Processor.removeDuplicateMarker(entry.getKey());
			Attrs attrs = entry.getValue();

			CapReqBuilder rb = new CapReqBuilder(BundleNamespace.BUNDLE_NAMESPACE);
			String filter = createVersionFilter(BundleNamespace.BUNDLE_NAMESPACE, bsn,
				attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE),
				AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
			rb.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
			if (Constants.RESOLUTION_OPTIONAL.equals(attrs.get(Constants.RESOLUTION_DIRECTIVE + ":")))
				rb.addDirective(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);

			RequirementWrapper rw = new RequirementWrapper();
			rw.requirement = rb.buildSyntheticRequirement();
			addRequirement(requirements, rw);
		}

		// Process generic requires
		String requiresStr = attribs.getValue(Constants.REQUIRE_CAPABILITY);
		Parameters requires = new Parameters(requiresStr);
		for (Entry<String, Attrs> entry : requires.entrySet()) {
			String ns = Processor.removeDuplicateMarker(entry.getKey());
			Attrs attrs = entry.getValue();

			CapReqBuilder rb = new CapReqBuilder(ns);
			for (String key : attrs.keySet()) {
				if (key.endsWith(":"))
					rb.addDirective(key.substring(0, key.length() - 1), attrs.get(key));
				else
					rb.addAttribute(key, attrs.getTyped(key));
			}

			RequirementWrapper rw = new RequirementWrapper();
			rw.requirement = rb.buildSyntheticRequirement();
			addRequirement(requirements, rw);
		}

		return requirements;
	}

	private static void addCapability(Map<String, List<Capability>> capMap, Capability cap) {
		List<Capability> capsForNs = capMap.get(cap.getNamespace());
		if (capsForNs == null) {
			capsForNs = new LinkedList<>();
			capMap.put(cap.getNamespace(), capsForNs);
		}
		capsForNs.add(cap);
	}

	private static void addRequirement(Map<String, List<RequirementWrapper>> requirements, RequirementWrapper req) {
		List<RequirementWrapper> listForNs = requirements.get(req.requirement.getNamespace());
		if (listForNs == null) {
			listForNs = new LinkedList<>();
			requirements.put(req.requirement.getNamespace(), listForNs);
		}
		listForNs.add(req);
	}

	private static final String createVersionFilter(String ns, String value, String rangeStr, String versionAttr) {
		SimpleFilter pkgNameFilter = new SimpleFilter(ns, value);

		Filter filter = pkgNameFilter;
		if (rangeStr != null) {
			VersionRange range = new VersionRange(rangeStr);

			Filter left;
			if (range.includeLow())
				left = new SimpleFilter(versionAttr, Operator.GreaterThanOrEqual, range.getLow()
					.toString());
			else
				left = new NotFilter(new SimpleFilter(versionAttr, Operator.LessThanOrEqual, range.getLow()
					.toString()));

			Filter right;
			if (!range.isRange())
				right = null;
			else if (range.includeHigh())
				right = new SimpleFilter(versionAttr, Operator.LessThanOrEqual, range.getHigh()
					.toString());
			else
				right = new NotFilter(new SimpleFilter(versionAttr, Operator.GreaterThanOrEqual, range.getHigh()
					.toString()));

			AndFilter combined = new AndFilter().addChild(pkgNameFilter)
				.addChild(left);
			if (right != null)
				combined.addChild(right);
			filter = combined;
		}
		return filter.toString();
	}

	static List<Clazz> findImportingClasses(String pkgName, Builder builder) throws Exception {
		List<Clazz> classes = new LinkedList<>();
		Collection<Clazz> importers = builder.getClasses("", "IMPORTING", pkgName);

		// Remove *this* package
		for (Clazz clazz : importers) {
			String fqn = clazz.getFQN();
			int dot = fqn.lastIndexOf('.');
			if (dot >= 0) {
				String pkg = fqn.substring(0, dot);
				if (!pkgName.equals(pkg))
					classes.add(clazz);
			}
		}
		return classes;
	}

	public File getFile() {
		return file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BndBuilderCapReqLoader other = (BndBuilderCapReqLoader) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}

}
