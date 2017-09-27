package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.List;

class Extractors {

	final static public List<HeaderExtractor>	HEADERS_EXTRACTORS;
	final static public List<MetadataExtractor>	METADATA_EXTRACTORS;

	static {
		METADATA_EXTRACTORS = new LinkedList<>();

		METADATA_EXTRACTORS.add(new ManifestExtractor());
		METADATA_EXTRACTORS.add(new ComponentExtractor());
		METADATA_EXTRACTORS.add(new MetatypeExtractor());

		HEADERS_EXTRACTORS = new LinkedList<>();

		HEADERS_EXTRACTORS.add(new ActivatorExtractor());
		HEADERS_EXTRACTORS.add(new CategoryExtractor());
		HEADERS_EXTRACTORS.add(new ClassPathExtractor());
		HEADERS_EXTRACTORS.add(new ContactAddressExtractor());
		HEADERS_EXTRACTORS.add(new CopyrightExtractor());
		HEADERS_EXTRACTORS.add(new DescriptionExtractor());
		HEADERS_EXTRACTORS.add(new DeveloperExtractor());
		HEADERS_EXTRACTORS.add(new DocUrlExtractor());
		HEADERS_EXTRACTORS.add(new DynamicImportExtractor());
		HEADERS_EXTRACTORS.add(new ExportExtractor());
		HEADERS_EXTRACTORS.add(new FragmentHostExtractor());
		HEADERS_EXTRACTORS.add(new IconExtractor());
		HEADERS_EXTRACTORS.add(new ImportExtractor());
		HEADERS_EXTRACTORS.add(new LazyActivationExtractor());
		HEADERS_EXTRACTORS.add(new LicenseExtractor());
		HEADERS_EXTRACTORS.add(new ManifestVersionExtractor());
		HEADERS_EXTRACTORS.add(new NameExtractor());
		HEADERS_EXTRACTORS.add(new NativeCodeExtractor());
		HEADERS_EXTRACTORS.add(new ProvideCapabilityExtractor());
		HEADERS_EXTRACTORS.add(new RequireBundleExtractor());
		HEADERS_EXTRACTORS.add(new RequiredCapabilityExtractor());
		HEADERS_EXTRACTORS.add(new RequiredExecutionEnvironmentExtractor());
		HEADERS_EXTRACTORS.add(new SymbolicNameExtractor());
		HEADERS_EXTRACTORS.add(new VendorExtractor());
		HEADERS_EXTRACTORS.add(new VersionExtractor());
	}
}
