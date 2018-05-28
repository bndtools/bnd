package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HeaderExtractors {
	
	final static private List<HeaderExtractor> EXTRACTORS;
	
	static {
		
		EXTRACTORS = new LinkedList<>();
		
		EXTRACTORS.add(new ActivatorExtractor());
		EXTRACTORS.add(new CategoryExtractor());
		EXTRACTORS.add(new ClasspathExtractor());
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
	
	static public Map<String, Object> extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		final Map<String, Object> headers = new LinkedHashMap<>();
		
		for (final HeaderExtractor h : EXTRACTORS) {
			final Object dto = h.extract(manifest, jar, reporter);
			if (dto != null) {
				headers.put(h.getEntryName(), dto);
			}
		}
		return headers;
	}
}
