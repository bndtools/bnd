package aQute.bnd.build.model;

import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.resource.Requirement;

import aQute.bnd.build.model.clauses.*;
import aQute.bnd.build.model.conversions.*;
import aQute.lib.osgi.Constants;
import aQute.lib.properties.*;
import aQute.libg.header.*;
import aQute.libg.tuple.*;
import aQute.libg.version.Version;

/**
 * A model for a Bnd file. In the first iteration, use a simple Properties
 * object; this will need to be enhanced to additionally record formatting, e.g.
 * line breaks and empty lines, and comments.
 * 
 * @author Neil Bartlett
 */
public class BndEditModel {

	public static final String										LINE_SEPARATOR				= " \\\n\t";
	public static final String										LIST_SEPARATOR				= ",\\\n\t";

	protected static final String									ISO_8859_1					= "ISO-8859-1";												//$NON-NLS-1$

	protected static String[]										KNOWN_PROPERTIES			= new String[] {
			Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_VERSION, Constants.BUNDLE_ACTIVATOR,
			Constants.EXPORT_PACKAGE, Constants.IMPORT_PACKAGE, aQute.lib.osgi.Constants.PRIVATE_PACKAGE,
			aQute.lib.osgi.Constants.SOURCES,
			aQute.lib.osgi.Constants.SERVICE_COMPONENT, aQute.lib.osgi.Constants.CLASSPATH,
			aQute.lib.osgi.Constants.BUILDPATH, aQute.lib.osgi.Constants.BUILDPACKAGES,
			aQute.lib.osgi.Constants.RUNBUNDLES, aQute.lib.osgi.Constants.RUNPROPERTIES, aQute.lib.osgi.Constants.SUB,
			aQute.lib.osgi.Constants.RUNFRAMEWORK,
			aQute.lib.osgi.Constants.RUNVM,
			// BndConstants.RUNVMARGS,
			// BndConstants.TESTSUITES,
			aQute.lib.osgi.Constants.TESTCASES, aQute.lib.osgi.Constants.PLUGIN, aQute.lib.osgi.Constants.PLUGINPATH,
			aQute.lib.osgi.Constants.RUNREPOS, aQute.lib.osgi.Constants.RUNREQUIRES, aQute.lib.osgi.Constants.RUNEE};

	public static final String										BUNDLE_VERSION_MACRO		= "${"
																										+ Constants.BUNDLE_VERSION
																										+ "}";

	protected final Map<String,Converter< ? extends Object,String>>	converters					= new HashMap<String,Converter< ? extends Object,String>>();
	protected final Map<String,Converter<String, ? extends Object>>	formatters					= new HashMap<String,Converter<String, ? extends Object>>();
	// private final DataModelHelper obrModelHelper = new DataModelHelperImpl();

	private final PropertyChangeSupport								propChangeSupport			= new PropertyChangeSupport(
																										this);
	private final Properties										properties					= new Properties();

	private File													bndResource;
	private boolean													projectFile;
	private final Map<String,Object>								objectProperties			= new HashMap<String,Object>();
	private final Map<String,String>								changesToSave				= new HashMap<String,String>();

	// CONVERTERS
	protected Converter<List<VersionedClause>,String>				buildPathConverter			= new ClauseListConverter<VersionedClause>(
																										new Converter<VersionedClause,Pair<String,Attrs>>() {
																											public VersionedClause convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new VersionedClause(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	protected Converter<List<VersionedClause>,String>				buildPackagesConverter		= new ClauseListConverter<VersionedClause>(
																										new Converter<VersionedClause,Pair<String,Attrs>>() {
																											public VersionedClause convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new VersionedClause(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	protected Converter<List<VersionedClause>,String>				clauseListConverter			= new ClauseListConverter<VersionedClause>(
																										new VersionedClauseConverter());
	protected Converter<String,String>								stringConverter				= new NoopConverter<String>();
	protected Converter<Boolean,String>								includedSourcesConverter	= new Converter<Boolean,String>() {
																									public Boolean convert(
																											String string)
																											throws IllegalArgumentException {
																										return Boolean
																												.valueOf(string);
																									}
																								};
	protected Converter<VersionPolicy,String>						versionPolicyConverter		= new Converter<VersionPolicy,String>() {
																									public VersionPolicy convert(
																											String string)
																											throws IllegalArgumentException {
																										return VersionPolicy
																												.parse(string);
																									}
																								};
	protected Converter<List<String>,String>						listConverter				= SimpleListConverter
																										.create();
	protected Converter<List<HeaderClause>,String>					headerClauseListConverter	= new HeaderClauseListConverter();
	protected ClauseListConverter<ExportedPackage>					exportPackageConverter		= new ClauseListConverter<ExportedPackage>(
																										new Converter<ExportedPackage,Pair<String,Attrs>>() {
																											public ExportedPackage convert(
																													Pair<String,Attrs> input) {
																												return new ExportedPackage(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	protected Converter<List<ServiceComponent>,String>				serviceComponentConverter	= new ClauseListConverter<ServiceComponent>(
																										new Converter<ServiceComponent,Pair<String,Attrs>>() {
																											public ServiceComponent convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new ServiceComponent(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});
	protected Converter<List<ImportPattern>,String>					importPatternConverter		= new ClauseListConverter<ImportPattern>(
																										new Converter<ImportPattern,Pair<String,Attrs>>() {
																											public ImportPattern convert(
																													Pair<String,Attrs> input)
																													throws IllegalArgumentException {
																												return new ImportPattern(
																														input.getFirst(),
																														input.getSecond());
																											}
																										});

	protected Converter<Map<String,String>,String>					propertiesConverter			= new PropertiesConverter();
	
	protected Converter<List<Requirement>,String>					requirementListConverter	= new RequirementListConverter();
	protected Converter<EE,String>									eeConverter					= new EEConverter();

	// Converter<ResolveMode, String> resolveModeConverter =
	// EnumConverter.create(ResolveMode.class, ResolveMode.manual);

	// FORMATTERS
	protected Converter<String,Object>								defaultFormatter			= new DefaultFormatter();
	protected Converter<String,String>								newlineEscapeFormatter		= new NewlineEscapedStringFormatter();
	protected Converter<String,Boolean>								defaultFalseBoolFormatter	= new DefaultBooleanFormatter(
																										false);
	protected Converter<String,Collection< ? >>						stringListFormatter			= new CollectionFormatter<Object>(
																										LIST_SEPARATOR,
																										(String) null);
	protected Converter<String,Collection< ? extends HeaderClause>>	headerClauseListFormatter	= new CollectionFormatter<HeaderClause>(
																										LIST_SEPARATOR,
																										new HeaderClauseFormatter(),
																										null);
	protected Converter<String,Map<String,String>>					propertiesFormatter			= new MapFormatter(
																										LIST_SEPARATOR,
																										new PropertiesEntryFormatter(),
																										null);
	
	protected Converter<String,Collection< ? extends Requirement>>	requirementListFormatter	= new CollectionFormatter<Requirement>(
																										LIST_SEPARATOR,
																										new RequirementFormatter(),
																										null);

	protected Converter<String,EE>									eeFormatter					= new EEFormatter();
	Converter<String,Collection< ? extends String>>					runReposFormatter			= new CollectionFormatter<String>(
																										LIST_SEPARATOR,
																										aQute.lib.osgi.Constants.EMPTY_HEADER);

	// Converter<String, ResolveMode> resolveModeFormatter =
	// EnumFormatter.create(ResolveMode.class, ResolveMode.manual);

	@SuppressWarnings("deprecation")
	public BndEditModel() {
		// register converters
		converters.put(aQute.lib.osgi.Constants.BUILDPATH, buildPathConverter);
		converters.put(aQute.lib.osgi.Constants.BUILDPACKAGES, buildPackagesConverter);
		converters.put(aQute.lib.osgi.Constants.RUNBUNDLES, clauseListConverter);
		converters.put(Constants.BUNDLE_SYMBOLICNAME, stringConverter);
		converters.put(Constants.BUNDLE_VERSION, stringConverter);
		converters.put(Constants.BUNDLE_ACTIVATOR, stringConverter);
		converters.put(aQute.lib.osgi.Constants.OUTPUT, stringConverter);
		converters.put(aQute.lib.osgi.Constants.SOURCES, includedSourcesConverter);
		converters.put(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, listConverter);
		converters.put(aQute.lib.osgi.Constants.CLASSPATH, listConverter);
		converters.put(Constants.EXPORT_PACKAGE, exportPackageConverter);
		converters.put(aQute.lib.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
		converters.put(Constants.IMPORT_PACKAGE, importPatternConverter);
		converters.put(aQute.lib.osgi.Constants.RUNFRAMEWORK, stringConverter);
		converters.put(aQute.lib.osgi.Constants.SUB, listConverter);
		converters.put(aQute.lib.osgi.Constants.RUNPROPERTIES, propertiesConverter);
		converters.put(aQute.lib.osgi.Constants.RUNVM, stringConverter);
		// converters.put(BndConstants.RUNVMARGS, stringConverter);
		converters.put(aQute.lib.osgi.Constants.TESTSUITES, listConverter);
		converters.put(aQute.lib.osgi.Constants.TESTCASES, listConverter);
		converters.put(aQute.lib.osgi.Constants.PLUGIN, headerClauseListConverter);
		converters.put(aQute.lib.osgi.Constants.RUNREQUIRES, requirementListConverter);
		converters.put(aQute.lib.osgi.Constants.RUNEE, eeConverter);
		converters.put(aQute.lib.osgi.Constants.RUNREPOS, listConverter);
		// converters.put(BndConstants.RESOLVE_MODE, resolveModeConverter);

		formatters.put(aQute.lib.osgi.Constants.BUILDPATH, headerClauseListFormatter);
		formatters.put(aQute.lib.osgi.Constants.BUILDPACKAGES, headerClauseListFormatter);
		formatters.put(aQute.lib.osgi.Constants.RUNBUNDLES, headerClauseListFormatter);
		formatters.put(Constants.BUNDLE_SYMBOLICNAME, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_VERSION, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_ACTIVATOR, newlineEscapeFormatter);
		formatters.put(aQute.lib.osgi.Constants.OUTPUT, newlineEscapeFormatter);
		formatters.put(aQute.lib.osgi.Constants.SOURCES, defaultFalseBoolFormatter);
		formatters.put(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, stringListFormatter);
		formatters.put(aQute.lib.osgi.Constants.CLASSPATH, stringListFormatter);
		formatters.put(Constants.EXPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(aQute.lib.osgi.Constants.SERVICE_COMPONENT, headerClauseListFormatter);
		formatters.put(Constants.IMPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(aQute.lib.osgi.Constants.RUNFRAMEWORK, newlineEscapeFormatter);
		formatters.put(aQute.lib.osgi.Constants.SUB, stringListFormatter);
		formatters.put(aQute.lib.osgi.Constants.RUNPROPERTIES, propertiesFormatter);
		formatters.put(aQute.lib.osgi.Constants.RUNVM, newlineEscapeFormatter);
		// formatters.put(BndConstants.RUNVMARGS, newlineEscapeFormatter);
		// formatters.put(BndConstants.TESTSUITES, stringListFormatter);
		formatters.put(aQute.lib.osgi.Constants.TESTCASES, stringListFormatter);
		formatters.put(aQute.lib.osgi.Constants.PLUGIN, headerClauseListFormatter);
		formatters.put(aQute.lib.osgi.Constants.RUNREQUIRES, requirementListFormatter);
		formatters.put(aQute.lib.osgi.Constants.RUNEE, eeFormatter);
		formatters.put(aQute.lib.osgi.Constants.RUNREPOS, runReposFormatter);
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

	protected static IRegion findEntry(IDocument document, String name) throws Exception {
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

	protected static void updateDocument(IDocument document, String name, String value) {
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
		return doGetObject(aQute.lib.osgi.Constants.OUTPUT, stringConverter);
	}

	public void setOutputFile(String name) {
		doSetObject(aQute.lib.osgi.Constants.OUTPUT, getOutputFile(), name, newlineEscapeFormatter);
	}

	public boolean isIncludeSources() {
		return doGetObject(aQute.lib.osgi.Constants.SOURCES, includedSourcesConverter);
	}

	public void setIncludeSources(boolean includeSources) {
		boolean oldValue = isIncludeSources();
		doSetObject(aQute.lib.osgi.Constants.SOURCES, oldValue, includeSources, defaultFalseBoolFormatter);
	}

	public List<String> getPrivatePackages() {
		return doGetObject(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, listConverter);
	}

	public void setPrivatePackages(List< ? extends String> packages) {
		List<String> oldPackages = getPrivatePackages();
		doSetObject(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, oldPackages, packages, stringListFormatter);
	}

	public List<ExportedPackage> getSystemPackages() {
		return doGetObject(aQute.lib.osgi.Constants.RUNSYSTEMPACKAGES, exportPackageConverter);
	}

	public void setSystemPackages(List< ? extends ExportedPackage> packages) {
		List<ExportedPackage> oldPackages = getSystemPackages();
		doSetObject(aQute.lib.osgi.Constants.RUNSYSTEMPACKAGES, oldPackages, packages, headerClauseListFormatter);
	}

	public List<String> getClassPath() {
		return doGetObject(aQute.lib.osgi.Constants.CLASSPATH, listConverter);
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
		doSetObject(aQute.lib.osgi.Constants.CLASSPATH, oldClassPath, classPath, stringListFormatter);
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
		return doGetObject(aQute.lib.osgi.Constants.DSANNOTATIONS, listConverter);
	}

	public void setDSAnnotationPatterns(List< ? extends String> patterns) {
		List<String> oldValue = getDSAnnotationPatterns();
		doSetObject(aQute.lib.osgi.Constants.DSANNOTATIONS, oldValue, patterns, stringListFormatter);
	}

	public List<ServiceComponent> getServiceComponents() {
		return doGetObject(aQute.lib.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
	}

	public void setServiceComponents(List< ? extends ServiceComponent> components) {
		List<ServiceComponent> oldValue = getServiceComponents();
		doSetObject(aQute.lib.osgi.Constants.SERVICE_COMPONENT, oldValue, components, headerClauseListFormatter);
	}

	public List<ImportPattern> getImportPatterns() {
		return doGetObject(Constants.IMPORT_PACKAGE, importPatternConverter);
	}

	public void setImportPatterns(List< ? extends ImportPattern> patterns) {
		List<ImportPattern> oldValue = getImportPatterns();
		doSetObject(Constants.IMPORT_PACKAGE, oldValue, patterns, headerClauseListFormatter);
	}

	public List<VersionedClause> getBuildPath() {
		return doGetObject(aQute.lib.osgi.Constants.BUILDPATH, buildPathConverter);
	}

	public void setBuildPath(List< ? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		doSetObject(aQute.lib.osgi.Constants.BUILDPATH, oldValue, paths, headerClauseListFormatter);
	}

	public List<VersionedClause> getBuildPackages() {
		return doGetObject(aQute.lib.osgi.Constants.BUILDPACKAGES, buildPackagesConverter);
	}

	public void setBuildPackages(List< ? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPackages();
		doSetObject(aQute.lib.osgi.Constants.BUILDPACKAGES, oldValue, paths, headerClauseListFormatter);
	}

	public List<VersionedClause> getRunBundles() {
		return doGetObject(aQute.lib.osgi.Constants.RUNBUNDLES, clauseListConverter);
	}

	public void setRunBundles(List< ? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		doSetObject(aQute.lib.osgi.Constants.RUNBUNDLES, oldValue, paths, headerClauseListFormatter);
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
		return doGetObject(aQute.lib.osgi.Constants.SUB, listConverter);
	}

	public void setSubBndFiles(List<String> subBndFiles) {
		List<String> oldValue = getSubBndFiles();
		doSetObject(aQute.lib.osgi.Constants.SUB, oldValue, subBndFiles, stringListFormatter);
	}

	public Map<String,String> getRunProperties() {
		return doGetObject(aQute.lib.osgi.Constants.RUNPROPERTIES, propertiesConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunProperties(java.util.Map)
	 */
	public void setRunProperties(Map<String,String> props) {
		Map<String,String> old = getRunProperties();
		doSetObject(aQute.lib.osgi.Constants.RUNPROPERTIES, old, props, propertiesFormatter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#getRunVMArgs()
	 */
	public String getRunVMArgs() {
		return doGetObject(aQute.lib.osgi.Constants.RUNVM, stringConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunVMArgs(java.lang.String)
	 */
	public void setRunVMArgs(String args) {
		String old = getRunVMArgs();
		doSetObject(aQute.lib.osgi.Constants.RUNVM, old, args, newlineEscapeFormatter);
	}

	@SuppressWarnings("deprecation")
	public List<String> getTestSuites() {
		List<String> testCases = doGetObject(aQute.lib.osgi.Constants.TESTCASES, listConverter);
		testCases = testCases != null ? testCases : Collections.<String> emptyList();

		List<String> testSuites = doGetObject(aQute.lib.osgi.Constants.TESTSUITES, listConverter);
		testSuites = testSuites != null ? testSuites : Collections.<String> emptyList();

		List<String> result = new ArrayList<String>(testCases.size() + testSuites.size());
		result.addAll(testCases);
		result.addAll(testSuites);
		return result;
	}

	@SuppressWarnings("deprecation")
	public void setTestSuites(List<String> suites) {
		List<String> old = getTestSuites();
		doSetObject(aQute.lib.osgi.Constants.TESTCASES, old, suites, stringListFormatter);
		doSetObject(aQute.lib.osgi.Constants.TESTSUITES, null, null, stringListFormatter);
	}

	public List<HeaderClause> getPlugins() {
		return doGetObject(aQute.lib.osgi.Constants.PLUGIN, headerClauseListConverter);
	}

	public void setPlugins(List<HeaderClause> plugins) {
		List<HeaderClause> old = getPlugins();
		doSetObject(aQute.lib.osgi.Constants.PLUGIN, old, plugins, headerClauseListFormatter);
	}

	public List<String> getPluginPath() {
		return doGetObject(aQute.lib.osgi.Constants.PLUGINPATH, listConverter);
	}

	public void setPluginPath(List<String> pluginPath) {
		List<String> old = getPluginPath();
		doSetObject(aQute.lib.osgi.Constants.PLUGINPATH, old, pluginPath, stringListFormatter);
	}
	
    public List<String> getRunRepos() {
        return doGetObject(aQute.lib.osgi.Constants.RUNREPOS, listConverter);
    }

    public void setRunRepos(List<String> repos) {
        List<String> old = getRunRepos();
        doSetObject(aQute.lib.osgi.Constants.RUNREPOS, old, repos, runReposFormatter);
    }
    
    public String getRunFramework() {
        return doGetObject(aQute.lib.osgi.Constants.RUNFRAMEWORK, stringConverter);
    }

    public EE getEE() {
        return doGetObject(aQute.lib.osgi.Constants.RUNEE, eeConverter);
    }

    public void setEE(EE ee) {
        EE old = getEE();
        doSetObject(aQute.lib.osgi.Constants.RUNEE, old, ee, eeFormatter);
    }

    
    public void setRunFramework(String clause) {
        String oldValue = getRunFramework();
        doSetObject(aQute.lib.osgi.Constants.RUNFRAMEWORK, oldValue, clause, newlineEscapeFormatter);
    }
    
    public List<Requirement> getRunRequires() {
    	return doGetObject(aQute.lib.osgi.Constants.RUNREQUIRES, requirementListConverter);
    }
    
    public void setRunRequires(List<Requirement> requires) {
    	List<Requirement> oldValue = getRunRequires();
    	doSetObject(aQute.lib.osgi.Constants.RUNREQUIRES, oldValue, requires, requirementListFormatter);
    }


	protected <R> R doGetObject(String name, Converter< ? extends R, ? super String> converter) {
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

	protected <T> void doSetObject(String name, T oldValue, T newValue, Converter<String, ? super T> formatter) {
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
