package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;

import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class HeaderExtractors {

	final static private List<HeaderExtractor> EXTRACTORS;

	static {

		EXTRACTORS = new LinkedList<>();

		EXTRACTORS.add(new ActivatorExtractor());
		EXTRACTORS.add(new CategoryExtractor());
		EXTRACTORS.add(new ClassPathExtractor());
		EXTRACTORS.add(new ContactAddressExtractor());
		EXTRACTORS.add(new CopyrightExtractor());
		EXTRACTORS.add(new DescriptionExtractor());
		EXTRACTORS.add(new DeveloperExtractor());
		EXTRACTORS.add(new SCMExtractor());
		EXTRACTORS.add(new DocUrlExtractor());
		EXTRACTORS.add(new DynamicImportExtractor());
		EXTRACTORS.add(new ExportExtractor());
		EXTRACTORS.add(new FragmentHostExtractor());
		EXTRACTORS.add(new IconExtractor());
		EXTRACTORS.add(new ImportExtractor());
		EXTRACTORS.add(new LazyActivationExtractor());
		EXTRACTORS.add(new LicenseExtractor());
		EXTRACTORS.add(new ManifestVersionExtractor());
		EXTRACTORS.add(new NameExtractor());
		EXTRACTORS.add(new NativeCodeExtractor());
		EXTRACTORS.add(new ProvideCapabilityExtractor());
		EXTRACTORS.add(new RequireBundleExtractor());
		EXTRACTORS.add(new RequiredCapabilityExtractor());
		EXTRACTORS.add(new RequiredExecutionEnvironmentExtractor());
		EXTRACTORS.add(new SymbolicNameExtractor());
		EXTRACTORS.add(new VendorExtractor());
		EXTRACTORS.add(new VersionExtractor());
	}

	static public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();

		for (final HeaderExtractor h : EXTRACTORS) {
			result.addAll(h.extract(manifest, jar, reporter));
		}
		return result;
	}
}
