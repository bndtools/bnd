package aQute.bnd.build.model;

import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.resource.*;

import aQute.bnd.build.model.clauses.*;
import aQute.bnd.build.model.conversions.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.bnd.version.*;
import aQute.libg.tuple.*;

/**
 * A model for a Bnd file. In the first iteration, use a simple Properties
 * object; this will need to be enhanced to additionally record formatting, e.g.
 * line breaks and empty lines, and comments.
 * 
 * @author Neil Bartlett
 */
public class BndEditModel {

	public static final String										NEWLINE_LINE_SEPARATOR		= "\\n\\\n\t";
	public static final String										LIST_SEPARATOR				= ",\\\n\t";

	private static final String									ISO_8859_1					= "ISO-8859-1";												//$NON-NLS-1$

	private static String[]										KNOWN_PROPERTIES			= new String[] {
			Constants.BUNDLE_LICENSE, Constants.BUNDLE_CATEGORY,
			Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_COPYRIGHT, Constants.BUNDLE_UPDATELOCATION,
			Constants.BUNDLE_VENDOR, Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL,
			Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_VERSION, Constants.BUNDLE_ACTIVATOR,
			Constants.EXPORT_PACKAGE, Constants.IMPORT_PACKAGE, aQute.bnd.osgi.Constants.PRIVATE_PACKAGE,
			aQute.bnd.osgi.Constants.SOURCES,
			aQute.bnd.osgi.Constants.SERVICE_COMPONENT, aQute.bnd.osgi.Constants.CLASSPATH,
			aQute.bnd.osgi.Constants.BUILDPATH, aQute.bnd.osgi.Constants.BUILDPACKAGES,
			aQute.bnd.osgi.Constants.RUNBUNDLES, aQute.bnd.osgi.Constants.RUNPROPERTIES, aQute.bnd.osgi.Constants.SUB,
			aQute.bnd.osgi.Constants.RUNFRAMEWORK, aQute.bnd.osgi.Constants.RUNFW,
			aQute.bnd.osgi.Constants.RUNVM,
			// BndConstants.RUNVMARGS,
			// BndConstants.TESTSUITES,
			aQute.bnd.osgi.Constants.TESTCASES, aQute.bnd.osgi.Constants.PLUGIN, aQute.bnd.osgi.Constants.PLUGINPATH,
			aQute.bnd.osgi.Constants.RUNREPOS, aQute.bnd.osgi.Constants.RUNREQUIRES, aQute.bnd.osgi.Constants.RUNEE};

	public static final String										BUNDLE_VERSION_MACRO		= "${"
																										+ Constants.BUNDLE_VERSION
																										+ "}";

	private final Map<String,Converter< ? extends Object,String>>	converters					= new HashMap<String,Converter< ? extends Object,String>>();
	private final Map<String,Converter<String, ? extends Object>>	formatters					= new HashMap<String,Converter<String, ? extends Object>>();
	// private final DataModelHelper obrModelHelper = new DataModelHelperImpl();

	private final PropertyChangeSupport								propChangeSupport			= new PropertyChangeSupport(
																										this);
	private final Properties										properties					= new Properties();

	private File													bndResource;
	private boolean													projectFile;
	private final Map<String,Object>								objectProperties			= new HashMap<String,Object>();
	private final Map<String,String>								changesToSave				= new HashMap<String,String>();

	// CONVERTERS
	private Converter<List<VersionedClause>,String>				buildPathConverter			= new ClauseListConverter<VersionedClause>(
																										new Converter<VersionedClause,Pair<String,Attrs>>() {
																											public VersionedClause convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new VersionedClause(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	private Converter<List<VersionedClause>,String>				buildPackagesConverter		= new ClauseListConverter<VersionedClause>(
																										new Converter<VersionedClause,Pair<String,Attrs>>() {
																											public VersionedClause convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new VersionedClause(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	private Converter<List<VersionedClause>,String>				clauseListConverter			= new ClauseListConverter<VersionedClause>(
																										new VersionedClauseConverter());
	private Converter<String,String>								stringConverter				= new NoopConverter<String>();
	private Converter<Boolean,String>								includedSourcesConverter	= new Converter<Boolean,String>() {
																									public Boolean convert(
																											String string)
																											throws IllegalArgumentException {
																										return Boolean
																												.valueOf(string);
																									}
																								};
	private Converter<List<String>,String>						listConverter				= SimpleListConverter
																										.create();
	private Converter<List<HeaderClause>,String>					headerClauseListConverter	= new HeaderClauseListConverter();
	private ClauseListConverter<ExportedPackage>					exportPackageConverter		= new ClauseListConverter<ExportedPackage>(
																										new Converter<ExportedPackage,Pair<String,Attrs>>() {
																											public ExportedPackage convert(
																													Pair<String,Attrs> input) {
																												return new ExportedPackage(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	private Converter<List<ServiceComponent>,String>				serviceComponentConverter	= new ClauseListConverter<ServiceComponent>(
																										new Converter<ServiceComponent,Pair<String,Attrs>>() {
																											public ServiceComponent convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new ServiceComponent(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	private Converter<List<ImportPattern>,String>					importPatternConverter		= new ClauseListConverter<ImportPattern>(
																										new Converter<ImportPattern,Pair<String,Attrs>>() {
																											public ImportPattern convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new ImportPattern(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});

	private Converter<Map<String,String>,String>					propertiesConverter			= new PropertiesConverter();
	
	private Converter<List<Requirement>,String>					requirementListConverter	= new RequirementListConverter();
	private Converter<EE,String>									eeConverter					= new EEConverter();

	// Converter<ResolveMode, String> resolveModeConverter =
	// EnumConverter.create(ResolveMode.class, ResolveMode.manual);

	// FORMATTERS
	private Converter<String,String>								newlineEscapeFormatter		= new NewlineEscapedStringFormatter();
	private Converter<String,Boolean>								defaultFalseBoolFormatter	= new DefaultBooleanFormatter(
																										false);
	private Converter<String,Collection< ? >>						stringListFormatter			= new CollectionFormatter<Object>(
																										LIST_SEPARATOR,
																										(String) null);
	private Converter<String,Collection< ? extends HeaderClause>>	headerClauseListFormatter	= new CollectionFormatter<HeaderClause>(
																										LIST_SEPARATOR,
																										new HeaderClauseFormatter(),
																										null);
	private Converter<String,Map<String,String>>					propertiesFormatter			= new MapFormatter(
																										LIST_SEPARATOR,
																										new PropertiesEntryFormatter(),
																										null);
	
	private Converter<String,Collection< ? extends Requirement>>	requirementListFormatter	= new CollectionFormatter<Requirement>(
																										LIST_SEPARATOR,
																										new RequirementFormatter(),
																										null);

	private Converter<String,EE>									eeFormatter					= new EEFormatter();
	private Converter<String,Collection< ? extends String>>			runReposFormatter			= new CollectionFormatter<String>(
																										LIST_SEPARATOR,
																										aQute.bnd.osgi.Constants.EMPTY_HEADER);

	// Converter<String, ResolveMode> resolveModeFormatter =
	// EnumFormatter.create(ResolveMode.class, ResolveMode.manual);

	@SuppressWarnings("deprecation")
	public BndEditModel() {
		// register converters
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_LICENSE, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_CATEGORY, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_NAME, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_DESCRIPTION, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_COPYRIGHT, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_UPDATELOCATION, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_VENDOR, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_CONTACTADDRESS, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUNDLE_DOCURL, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.BUILDPATH, buildPathConverter);
		converters.put(aQute.bnd.osgi.Constants.BUILDPACKAGES, buildPackagesConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNBUNDLES, clauseListConverter);
		converters.put(Constants.BUNDLE_SYMBOLICNAME, stringConverter);
		converters.put(Constants.BUNDLE_VERSION, stringConverter);
		converters.put(Constants.BUNDLE_ACTIVATOR, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.OUTPUT, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.SOURCES, includedSourcesConverter);
		converters.put(aQute.bnd.osgi.Constants.PRIVATE_PACKAGE, listConverter);
		converters.put(aQute.bnd.osgi.Constants.CLASSPATH, listConverter);
		converters.put(Constants.EXPORT_PACKAGE, exportPackageConverter);
		converters.put(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
		converters.put(Constants.IMPORT_PACKAGE, importPatternConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNFRAMEWORK, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNFW, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.SUB, listConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNPROPERTIES, propertiesConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNVM, stringConverter);
		// converters.put(BndConstants.RUNVMARGS, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.TESTSUITES, listConverter);
		converters.put(aQute.bnd.osgi.Constants.TESTCASES, listConverter);
		converters.put(aQute.bnd.osgi.Constants.PLUGIN, headerClauseListConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNREQUIRES, requirementListConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNEE, eeConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNREPOS, listConverter);
		// converters.put(BndConstants.RESOLVE_MODE, resolveModeConverter);

		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_LICENSE, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_CATEGORY, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_NAME, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_DESCRIPTION, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_COPYRIGHT, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_UPDATELOCATION, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_VENDOR, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_CONTACTADDRESS, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUNDLE_DOCURL, newlineEscapeFormatter);

		formatters.put(aQute.bnd.osgi.Constants.BUILDPATH, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.BUILDPACKAGES, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNBUNDLES, headerClauseListFormatter);
		formatters.put(Constants.BUNDLE_SYMBOLICNAME, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_VERSION, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_ACTIVATOR, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.OUTPUT, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.SOURCES, defaultFalseBoolFormatter);
		formatters.put(aQute.bnd.osgi.Constants.PRIVATE_PACKAGE, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.CLASSPATH, stringListFormatter);
		formatters.put(Constants.EXPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, headerClauseListFormatter);
		formatters.put(Constants.IMPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNFRAMEWORK, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNFW, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.SUB, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNPROPERTIES, propertiesFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNVM, newlineEscapeFormatter);
		// formatters.put(BndConstants.RUNVMARGS, newlineEscapeFormatter);
		// formatters.put(BndConstants.TESTSUITES, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.TESTCASES, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.PLUGIN, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNREQUIRES, requirementListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNEE, eeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNREPOS, runReposFormatter);
		// formatters.put(BndConstants.RESOLVE_MODE, resolveModeFormatter);
	}

	public void loadFrom(IDocument document) throws IOException {
		InputStream inputStream = new ByteArrayInputStream(document.get().getBytes(ISO_8859_1));
		loadFrom(inputStream);
	}

	public void loadFrom(File file) throws IOException {
		loadFrom(new BufferedInputStream(new FileInputStream(file)));
	}

	public void loadFrom(InputStream inputStream) throws IOException {
		try {
			// Clear and load
			properties.clear();
			properties.load(inputStream);
			objectProperties.clear();
			changesToSave.clear();

			// Fire property changes on all known property names
			for (String prop : KNOWN_PROPERTIES) {
				// null values for old and new forced the change to be fired
				propChangeSupport.firePropertyChange(prop, null, null);
			}
		}
		finally {
			inputStream.close();
		}

	}

	public void saveChangesTo(IDocument document) {
		for (Iterator<Entry<String,String>> iter = changesToSave.entrySet().iterator(); iter.hasNext();) {
			Entry<String,String> entry = iter.next();
			iter.remove();

			String propertyName = entry.getKey();
			String stringValue = entry.getValue();

			updateDocument(document, propertyName, stringValue);
		}
	}

	private static IRegion findEntry(IDocument document, String name) throws Exception {
		PropertiesLineReader reader = new PropertiesLineReader(document);
		LineType type = reader.next();
		while (type != LineType.eof) {
			if (type == LineType.entry) {
				String key = reader.key();
				if (name.equals(key))
					return reader.region();
			}
			type = reader.next();
		}
		return null;
	}

	private static void updateDocument(IDocument document, String name, String value) {
		String newEntry;
		if (value != null) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(name).append(": ").append(value);
			newEntry = buffer.toString();
		} else {
			newEntry = "";
		}

		try {
			IRegion region = findEntry(document, name);
			if (region != null) {
				// Replace an existing entry
				int offset = region.getOffset();
				int length = region.getLength();

				// If the replacement is empty, remove one extra character to
				// the right, i.e. the following newline,
				// unless this would take us past the end of the document
				if (newEntry.length() == 0 && offset + length + 1 < document.getLength()) {
					length++;
				}
				document.replace(offset, length, newEntry);
			} else if (newEntry.length() > 0) {
				// This is a new entry, put it at the end of the file

				// Does the last line of the document have a newline? If not,
				// we need to add one.
				if (document.getLength() > 0 && document.getChar(document.getLength() - 1) != '\n')
					newEntry = "\n" + newEntry;
				document.replace(document.getLength(), 0, newEntry);
			}
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<String> getAllPropertyNames() {
		List<String> result = new ArrayList<String>(properties.size());

		Enumeration<String> names = (Enumeration<String>) properties.propertyNames();

		while (names.hasMoreElements()) {
			result.add(names.nextElement());
		}
		return result;
	}

	public Object genericGet(String propertyName) {
		Converter< ? extends Object,String> converter = converters.get(propertyName);
		if (converter == null)
			converter = new NoopConverter<String>();
		return doGetObject(propertyName, converter);
	}

	public void genericSet(String propertyName, Object value) {
		Object oldValue = genericGet(propertyName);
		Converter<String,Object> formatter = (Converter<String,Object>) formatters.get(propertyName);
		if (formatter == null)
			formatter = new DefaultFormatter();
		doSetObject(propertyName, oldValue, value, formatter);
	}

	public String getBundleLicense() {
		return doGetObject(Constants.BUNDLE_LICENSE, stringConverter);
	}

	public void setBundleLicense(String bundleLicense) {
		doSetObject(Constants.BUNDLE_LICENSE, getBundleLicense(), bundleLicense, newlineEscapeFormatter);
	}

	public String getBundleCategory() {
		return doGetObject(Constants.BUNDLE_CATEGORY, stringConverter);
	}

	public void setBundleCategory(String bundleCategory) {
		doSetObject(Constants.BUNDLE_CATEGORY, getBundleCategory(), bundleCategory, newlineEscapeFormatter);
	}

	public String getBundleName() {
		return doGetObject(Constants.BUNDLE_NAME, stringConverter);
	}

	public void setBundleName(String bundleName) {
		doSetObject(Constants.BUNDLE_NAME, getBundleName(), bundleName, newlineEscapeFormatter);
	}

	public String getBundleDescription() {
		return doGetObject(Constants.BUNDLE_DESCRIPTION, stringConverter);
	}

	public void setBundleDescription(String bundleDescription) {
		doSetObject(Constants.BUNDLE_DESCRIPTION, getBundleDescription(), bundleDescription, newlineEscapeFormatter);
	}

	public String getBundleCopyright() {
		return doGetObject(Constants.BUNDLE_COPYRIGHT, stringConverter);
	}

	public void setBundleCopyright(String bundleCopyright) {
		doSetObject(Constants.BUNDLE_COPYRIGHT, getBundleCopyright(), bundleCopyright, newlineEscapeFormatter);
	}

	public String getBundleUpdateLocation() {
		return doGetObject(Constants.BUNDLE_UPDATELOCATION, stringConverter);
	}

	public void setBundleUpdateLocation(String bundleUpdateLocation) {
		doSetObject(Constants.BUNDLE_UPDATELOCATION, getBundleUpdateLocation(), bundleUpdateLocation, newlineEscapeFormatter);
	}

	public String getBundleVendor() {
		return doGetObject(Constants.BUNDLE_VENDOR, stringConverter);
	}

	public void setBundleVendor(String bundleVendor) {
		doSetObject(Constants.BUNDLE_VENDOR, getBundleVendor(), bundleVendor, newlineEscapeFormatter);
	}

	public String getBundleContactAddress() {
		return doGetObject(Constants.BUNDLE_CONTACTADDRESS, stringConverter);
	}

	public void setBundleContactAddress(String bundleContactAddress) {
		doSetObject(Constants.BUNDLE_CONTACTADDRESS, getBundleContactAddress(), bundleContactAddress, newlineEscapeFormatter);
	}

	public String getBundleDocUrl() {
		return doGetObject(Constants.BUNDLE_DOCURL, stringConverter);
	}

	public void setBundleDocUrl(String bundleDocUrl) {
		doSetObject(Constants.BUNDLE_DOCURL, getBundleDocUrl(), bundleDocUrl, newlineEscapeFormatter);
	}

	public String getBundleSymbolicName() {
		return doGetObject(Constants.BUNDLE_SYMBOLICNAME, stringConverter);
	}

	public void setBundleSymbolicName(String bundleSymbolicName) {
		doSetObject(Constants.BUNDLE_SYMBOLICNAME, getBundleSymbolicName(), bundleSymbolicName, newlineEscapeFormatter);
	}

	public String getBundleVersionString() {
		return doGetObject(Constants.BUNDLE_VERSION, stringConverter);
	}

	public void setBundleVersion(String bundleVersion) {
		doSetObject(Constants.BUNDLE_VERSION, getBundleVersionString(), bundleVersion, newlineEscapeFormatter);
	}

	public String getBundleActivator() {
		return doGetObject(Constants.BUNDLE_ACTIVATOR, stringConverter);
	}

	public void setBundleActivator(String bundleActivator) {
		doSetObject(Constants.BUNDLE_ACTIVATOR, getBundleActivator(), bundleActivator, newlineEscapeFormatter);
	}

	public String getOutputFile() {
		return doGetObject(aQute.bnd.osgi.Constants.OUTPUT, stringConverter);
	}

	public void setOutputFile(String name) {
		doSetObject(aQute.bnd.osgi.Constants.OUTPUT, getOutputFile(), name, newlineEscapeFormatter);
	}

	public boolean isIncludeSources() {
		return doGetObject(aQute.bnd.osgi.Constants.SOURCES, includedSourcesConverter);
	}

	public void setIncludeSources(boolean includeSources) {
		boolean oldValue = isIncludeSources();
		doSetObject(aQute.bnd.osgi.Constants.SOURCES, oldValue, includeSources, defaultFalseBoolFormatter);
	}

	public List<String> getPrivatePackages() {
		return doGetObject(aQute.bnd.osgi.Constants.PRIVATE_PACKAGE, listConverter);
	}

	public void setPrivatePackages(List< ? extends String> packages) {
		List<String> oldPackages = getPrivatePackages();
		doSetObject(aQute.bnd.osgi.Constants.PRIVATE_PACKAGE, oldPackages, packages, stringListFormatter);
	}

	public List<ExportedPackage> getSystemPackages() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNSYSTEMPACKAGES, exportPackageConverter);
	}

	public void setSystemPackages(List< ? extends ExportedPackage> packages) {
		List<ExportedPackage> oldPackages = getSystemPackages();
		doSetObject(aQute.bnd.osgi.Constants.RUNSYSTEMPACKAGES, oldPackages, packages, headerClauseListFormatter);
	}

	public List<String> getClassPath() {
		return doGetObject(aQute.bnd.osgi.Constants.CLASSPATH, listConverter);
	}

	public void addPrivatePackage(String packageName) {
		List<String> packages = getPrivatePackages();
		if (packages == null)
			packages = new ArrayList<String>();
		else
			packages = new ArrayList<String>(packages);
		packages.add(packageName);
		setPrivatePackages(packages);
	}

	public void setClassPath(List< ? extends String> classPath) {
		List<String> oldClassPath = getClassPath();
		doSetObject(aQute.bnd.osgi.Constants.CLASSPATH, oldClassPath, classPath, stringListFormatter);
	}

	public List<ExportedPackage> getExportedPackages() {
		return doGetObject(Constants.EXPORT_PACKAGE, exportPackageConverter);
	}

	public void setExportedPackages(List< ? extends ExportedPackage> exports) {
		boolean referencesBundleVersion = false;

		if (exports != null) {
			for (ExportedPackage pkg : exports) {
				String versionString = pkg.getVersionString();
				if (versionString != null && versionString.indexOf(BUNDLE_VERSION_MACRO) > -1) {
					referencesBundleVersion = true;
				}
			}
		}
		List<ExportedPackage> oldValue = getExportedPackages();
		doSetObject(Constants.EXPORT_PACKAGE, oldValue, exports, headerClauseListFormatter);

		if (referencesBundleVersion && getBundleVersionString() == null) {
			setBundleVersion(Version.emptyVersion.toString());
		}
	}

	public void addExportedPackage(ExportedPackage export) {
		List<ExportedPackage> exports = getExportedPackages();
		exports = (exports == null) ? new ArrayList<ExportedPackage>() : new ArrayList<ExportedPackage>(exports);
		exports.add(export);
		setExportedPackages(exports);
	}

	public List<String> getDSAnnotationPatterns() {
		return doGetObject(aQute.bnd.osgi.Constants.DSANNOTATIONS, listConverter);
	}

	public void setDSAnnotationPatterns(List< ? extends String> patterns) {
		List<String> oldValue = getDSAnnotationPatterns();
		doSetObject(aQute.bnd.osgi.Constants.DSANNOTATIONS, oldValue, patterns, stringListFormatter);
	}

	public List<ServiceComponent> getServiceComponents() {
		return doGetObject(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
	}

	public void setServiceComponents(List< ? extends ServiceComponent> components) {
		List<ServiceComponent> oldValue = getServiceComponents();
		doSetObject(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, oldValue, components, headerClauseListFormatter);
	}

	public List<ImportPattern> getImportPatterns() {
		return doGetObject(Constants.IMPORT_PACKAGE, importPatternConverter);
	}

	public void setImportPatterns(List< ? extends ImportPattern> patterns) {
		List<ImportPattern> oldValue = getImportPatterns();
		doSetObject(Constants.IMPORT_PACKAGE, oldValue, patterns, headerClauseListFormatter);
	}

	public List<VersionedClause> getBuildPath() {
		return doGetObject(aQute.bnd.osgi.Constants.BUILDPATH, buildPathConverter);
	}

	public void setBuildPath(List< ? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		doSetObject(aQute.bnd.osgi.Constants.BUILDPATH, oldValue, paths, headerClauseListFormatter);
	}

	public List<VersionedClause> getBuildPackages() {
		return doGetObject(aQute.bnd.osgi.Constants.BUILDPACKAGES, buildPackagesConverter);
	}

	public void setBuildPackages(List< ? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPackages();
		doSetObject(aQute.bnd.osgi.Constants.BUILDPACKAGES, oldValue, paths, headerClauseListFormatter);
	}

	public List<VersionedClause> getRunBundles() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNBUNDLES, clauseListConverter);
	}

	public void setRunBundles(List< ? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		doSetObject(aQute.bnd.osgi.Constants.RUNBUNDLES, oldValue, paths, headerClauseListFormatter);
	}

	public boolean isIncludedPackage(String packageName) {
		final Collection<String> privatePackages = getPrivatePackages();
		if (privatePackages != null) {
			if (privatePackages.contains(packageName))
				return true;
		}
		final Collection<ExportedPackage> exportedPackages = getExportedPackages();
		if (exportedPackages != null) {
			for (ExportedPackage pkg : exportedPackages) {
				if (packageName.equals(pkg.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public List<String> getSubBndFiles() {
		return doGetObject(aQute.bnd.osgi.Constants.SUB, listConverter);
	}

	public void setSubBndFiles(List<String> subBndFiles) {
		List<String> oldValue = getSubBndFiles();
		doSetObject(aQute.bnd.osgi.Constants.SUB, oldValue, subBndFiles, stringListFormatter);
	}

	public Map<String,String> getRunProperties() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNPROPERTIES, propertiesConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunProperties(java.util.Map)
	 */
	public void setRunProperties(Map<String,String> props) {
		Map<String,String> old = getRunProperties();
		doSetObject(aQute.bnd.osgi.Constants.RUNPROPERTIES, old, props, propertiesFormatter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#getRunVMArgs()
	 */
	public String getRunVMArgs() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNVM, stringConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunVMArgs(java.lang.String)
	 */
	public void setRunVMArgs(String args) {
		String old = getRunVMArgs();
		doSetObject(aQute.bnd.osgi.Constants.RUNVM, old, args, newlineEscapeFormatter);
	}

	@SuppressWarnings("deprecation")
	public List<String> getTestSuites() {
		List<String> testCases = doGetObject(aQute.bnd.osgi.Constants.TESTCASES, listConverter);
		testCases = testCases != null ? testCases : Collections.<String> emptyList();

		List<String> testSuites = doGetObject(aQute.bnd.osgi.Constants.TESTSUITES, listConverter);
		testSuites = testSuites != null ? testSuites : Collections.<String> emptyList();

		List<String> result = new ArrayList<String>(testCases.size() + testSuites.size());
		result.addAll(testCases);
		result.addAll(testSuites);
		return result;
	}

	@SuppressWarnings("deprecation")
	public void setTestSuites(List<String> suites) {
		List<String> old = getTestSuites();
		doSetObject(aQute.bnd.osgi.Constants.TESTCASES, old, suites, stringListFormatter);
		doSetObject(aQute.bnd.osgi.Constants.TESTSUITES, null, null, stringListFormatter);
	}

	public List<HeaderClause> getPlugins() {
		return doGetObject(aQute.bnd.osgi.Constants.PLUGIN, headerClauseListConverter);
	}

	public void setPlugins(List<HeaderClause> plugins) {
		List<HeaderClause> old = getPlugins();
		doSetObject(aQute.bnd.osgi.Constants.PLUGIN, old, plugins, headerClauseListFormatter);
	}

	public List<String> getPluginPath() {
		return doGetObject(aQute.bnd.osgi.Constants.PLUGINPATH, listConverter);
	}

	public void setPluginPath(List<String> pluginPath) {
		List<String> old = getPluginPath();
		doSetObject(aQute.bnd.osgi.Constants.PLUGINPATH, old, pluginPath, stringListFormatter);
	}
	
    public List<String> getRunRepos() {
        return doGetObject(aQute.bnd.osgi.Constants.RUNREPOS, listConverter);
    }

    public void setRunRepos(List<String> repos) {
        List<String> old = getRunRepos();
        doSetObject(aQute.bnd.osgi.Constants.RUNREPOS, old, repos, runReposFormatter);
    }
    
    public String getRunFramework() {
        return doGetObject(aQute.bnd.osgi.Constants.RUNFRAMEWORK, stringConverter);
    }

    public String getRunFw() {
        return doGetObject(aQute.bnd.osgi.Constants.RUNFW, stringConverter);
    }

    public EE getEE() {
        return doGetObject(aQute.bnd.osgi.Constants.RUNEE, eeConverter);
    }

    public void setEE(EE ee) {
        EE old = getEE();
        doSetObject(aQute.bnd.osgi.Constants.RUNEE, old, ee, eeFormatter);
    }

    
    public void setRunFramework(String clause) {
        assert (Constants.RUNFRAMEWORK_SERVICES.equals(clause.toLowerCase().trim()) ||
                Constants.RUNFRAMEWORK_NONE.equals(clause.toLowerCase().trim()));
        String oldValue = getRunFramework();
        doSetObject(aQute.bnd.osgi.Constants.RUNFRAMEWORK, oldValue, clause, newlineEscapeFormatter);
    }
    
    public void setRunFw(String clause) {
        String oldValue = getRunFw();
        doSetObject(aQute.bnd.osgi.Constants.RUNFW, oldValue, clause, newlineEscapeFormatter);
    }

    public List<Requirement> getRunRequires() {
    	return doGetObject(aQute.bnd.osgi.Constants.RUNREQUIRES, requirementListConverter);
    }
    
    public void setRunRequires(List<Requirement> requires) {
    	List<Requirement> oldValue = getRunRequires();
    	doSetObject(aQute.bnd.osgi.Constants.RUNREQUIRES, oldValue, requires, requirementListFormatter);
    }


	private <R> R doGetObject(String name, Converter< ? extends R, ? super String> converter) {
		R result;
		if (objectProperties.containsKey(name)) {
			R temp = (R) objectProperties.get(name);
			result = temp;
		} else if (changesToSave.containsKey(name)) {
			result = converter.convert(changesToSave.get(name));
			objectProperties.put(name, result);
		} else if (properties.containsKey(name)) {
			result = converter.convert(properties.getProperty(name));
			objectProperties.put(name, result);
		} else {
			result = null;
		}
		return result;
	}

	private <T> void doSetObject(String name, T oldValue, T newValue, Converter<String, ? super T> formatter) {
		objectProperties.put(name, newValue);
		changesToSave.put(name, formatter.convert(newValue));
		propChangeSupport.firePropertyChange(name, oldValue, newValue);
	}

	public void setProjectFile(boolean projectFile) {
		this.projectFile = projectFile;
	}

	public boolean isProjectFile() {
		return this.projectFile;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propChangeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

	public void setBndResource(File bndResource) {
		this.bndResource = bndResource;
	}

	public File getBndResource() {
		return bndResource;
	}
}
