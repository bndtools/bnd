package aQute.bnd.build.model;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.osgi.resource.Requirement;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceLayout;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.ImportPattern;
import aQute.bnd.build.model.clauses.ServiceComponent;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.DefaultBooleanFormatter;
import aQute.bnd.build.model.conversions.DefaultFormatter;
import aQute.bnd.build.model.conversions.EEConverter;
import aQute.bnd.build.model.conversions.EEFormatter;
import aQute.bnd.build.model.conversions.HeaderClauseFormatter;
import aQute.bnd.build.model.conversions.HeaderClauseListConverter;
import aQute.bnd.build.model.conversions.MapFormatter;
import aQute.bnd.build.model.conversions.NewlineEscapedStringFormatter;
import aQute.bnd.build.model.conversions.NoopConverter;
import aQute.bnd.build.model.conversions.PropertiesConverter;
import aQute.bnd.build.model.conversions.PropertiesEntryFormatter;
import aQute.bnd.build.model.conversions.RequirementFormatter;
import aQute.bnd.build.model.conversions.RequirementListConverter;
import aQute.bnd.build.model.conversions.SimpleListConverter;
import aQute.bnd.build.model.conversions.VersionedClauseConverter;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.help.instructions.ResolutionInstructions;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.properties.Document;
import aQute.bnd.properties.IDocument;
import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;
import aQute.bnd.version.Version;
import aQute.lib.collections.Iterables;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;

/**
 * A model for a Bnd file. In the first iteration, use a simple Properties
 * object; this will need to be enhanced to additionally record formatting, e.g.
 * line breaks and empty lines, and comments.
 *
 * @author Neil Bartlett
 */
@SuppressWarnings("deprecation")
public class BndEditModel {

	private static final String													BUILDFILE							= Workspace.CNFDIR
		+ "/" + Workspace.BUILDFILE;
	public static final String													NEWLINE_LINE_SEPARATOR				= "\\n\\\n\t";
	public static final String													LIST_SEPARATOR						= ",\\\n\t";

	private static String[]														KNOWN_PROPERTIES					= new String[] {
		Constants.BUNDLE_LICENSE, Constants.BUNDLE_CATEGORY, Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION,
		Constants.BUNDLE_COPYRIGHT, Constants.BUNDLE_UPDATELOCATION, Constants.BUNDLE_VENDOR,
		Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL, Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_VERSION, Constants.BUNDLE_ACTIVATOR, Constants.EXPORT_PACKAGE, Constants.IMPORT_PACKAGE,
		Constants.PRIVATE_PACKAGE, Constants.PRIVATEPACKAGE, Constants.SOURCES, Constants.SERVICE_COMPONENT,
		Constants.CLASSPATH, Constants.BUILDPATH, Constants.RUNBUNDLES, Constants.RUNPROPERTIES, Constants.SUB,
		Constants.RUNFRAMEWORK, Constants.RUNFW, Constants.RUNVM, Constants.RUNPROGRAMARGS, Constants.DISTRO,
		// BndConstants.RUNVMARGS,
		// BndConstants.TESTSUITES,
		Constants.TESTCASES, Constants.PLUGIN, Constants.PLUGINPATH, Constants.RUNREPOS, Constants.RUNREQUIRES,
		Constants.RUNEE, Constants.RUNBLACKLIST, Constants.BUNDLE_BLUEPRINT, Constants.INCLUDE_RESOURCE,
		Constants.STANDALONE
	};

	public static final String													PROP_WORKSPACE						= "_workspace";

	public static final String													BUNDLE_VERSION_MACRO				= "${"
		+ Constants.BUNDLE_VERSION + "}";

	private static final Map<String, Converter<? extends Object, String>>		converters							= new HashMap<>();
	private static final Map<String, Converter<String, ? extends Object>>		formatters							= new HashMap<>();
	// private final DataModelHelper obrModelHelper = new DataModelHelperImpl();

	private File																bndResource;
	private String																bndResourceName;

	private final PropertyChangeSupport											propChangeSupport					= new PropertyChangeSupport(
		this);
	private Properties															properties							= new UTF8Properties();
	private final Map<String, Object>											objectProperties					= new HashMap<>();
	private final Map<String, String>											changesToSave						= new TreeMap<>();
	private Project																project;

	private volatile boolean													dirty;

	// CONVERTERS
	private final static Converter<List<VersionedClause>, String>				buildPathConverter					= new HeaderClauseListConverter<>(
		new Converter<VersionedClause, HeaderClause>() {
																															@Override
																															public VersionedClause convert(
																																HeaderClause input)
																																throws IllegalArgumentException {
																																if (input == null)
																																	return null;
																																return new VersionedClause(
																																	input
																																		.getName(),
																																	input
																																		.getAttribs());
																															}

																															@Override
																															public VersionedClause error(
																																String msg) {
																																return null;
																															}
																														});

	private final static Converter<List<VersionedClause>, String>				clauseListConverter					= new HeaderClauseListConverter<>(
		new VersionedClauseConverter());
	private final static Converter<String, String>								stringConverter						= new NoopConverter<>();
	private final static Converter<Boolean, String>								includedSourcesConverter			= new Converter<Boolean, String>() {
																														@Override
																														public Boolean convert(
																															String string)
																															throws IllegalArgumentException {
																															return Boolean
																																.valueOf(
																																	string);
																														}

																														@Override
																														public Boolean error(
																															String msg) {
																															return Boolean.FALSE;
																														}
																													};
	private final static Converter<List<String>, String>						listConverter						= SimpleListConverter
		.create();

	private final static Converter<List<ExportedPackage>, String>				exportPackageConverter				= new HeaderClauseListConverter<>(
		new Converter<ExportedPackage, HeaderClause>() {
																															@Override
																															public ExportedPackage convert(
																																HeaderClause input) {
																																if (input == null)
																																	return null;
																																return new ExportedPackage(
																																	input
																																		.getName(),
																																	input
																																		.getAttribs());
																															}

																															@Override
																															public ExportedPackage error(
																																String msg) {
																																return ExportedPackage
																																	.error(
																																		msg);
																															}
																														});

	private final static Converter<List<ServiceComponent>, String>				serviceComponentConverter			= new HeaderClauseListConverter<>(
		new Converter<ServiceComponent, HeaderClause>() {
																															@Override
																															public ServiceComponent convert(
																																HeaderClause input)
																																throws IllegalArgumentException {
																																if (input == null)
																																	return null;
																																return new ServiceComponent(
																																	input
																																		.getName(),
																																	input
																																		.getAttribs());
																															}

																															@Override
																															public ServiceComponent error(
																																String msg) {
																																return ServiceComponent
																																	.error(
																																		msg);
																															}
																														});
	private final static Converter<List<ImportPattern>, String>					importPatternConverter				= new HeaderClauseListConverter<>(
		new Converter<ImportPattern, HeaderClause>() {
																															@Override
																															public ImportPattern convert(
																																HeaderClause input)
																																throws IllegalArgumentException {
																																if (input == null)
																																	return null;
																																return new ImportPattern(
																																	input
																																		.getName(),
																																	input
																																		.getAttribs());
																															}

																															@Override
																															public ImportPattern error(
																																String msg) {
																																return ImportPattern
																																	.error(
																																		msg);
																															}
																														});

	private final static Converter<Map<String, String>, String>					propertiesConverter					= new PropertiesConverter();

	private final static Converter<List<Requirement>, String>					requirementListConverter			= new RequirementListConverter();
	private final static Converter<EE, String>									eeConverter							= new EEConverter();

	// Converter<ResolveMode, String> resolveModeConverter =
	// EnumConverter.create(ResolveMode.class, ResolveMode.manual);

	// FORMATTERS
	private final static Converter<String, String>								newlineEscapeFormatter				= new NewlineEscapedStringFormatter();
	private final static Converter<String, Boolean>								defaultFalseBoolFormatter			= new DefaultBooleanFormatter(
		false);
	private final static Converter<String, Collection<?>>						stringListFormatter					= new CollectionFormatter<>(
		LIST_SEPARATOR, (String) null);

	private final static Converter<List<HeaderClause>, String>					headerClauseListConverter			= new HeaderClauseListConverter<>(
		new NoopConverter<>());
	private final static Converter<String, Collection<? extends HeaderClause>>	headerClauseListFormatter			= new CollectionFormatter<>(
		LIST_SEPARATOR, new HeaderClauseFormatter(), null);

	private final static Converter<String, Collection<? extends HeaderClause>>	complexHeaderClauseListFormatter	= new CollectionFormatter<>(
		LIST_SEPARATOR, new HeaderClauseFormatter(true), null);

	private final static Converter<String, Map<String, String>>					propertiesFormatter					= new MapFormatter(
		LIST_SEPARATOR, new PropertiesEntryFormatter(), null);

	private final static Converter<String, Collection<? extends Requirement>>	requirementListFormatter			= new CollectionFormatter<>(
		LIST_SEPARATOR, new RequirementFormatter(), null);

	private final static Converter<String, Collection<? extends HeaderClause>>	standaloneLinkListFormatter			= new CollectionFormatter<>(
		LIST_SEPARATOR, new HeaderClauseFormatter(), "");

	private final static Converter<String, EE>									eeFormatter							= new EEFormatter();
	private final static Converter<String, Collection<? extends String>>		runReposFormatter					= new CollectionFormatter<>(
		LIST_SEPARATOR, Constants.EMPTY_HEADER);
	private Workspace															workspace;
	private IDocument															document;

	// for change detection when multiple wizards look at the same model
	private long																lastChangedAt;
	/**
	 * Is either the workspace (when cnf/build.bnd) or a project (when its
	 * bnd.bnd) or a random bndrun linked to workspace (event if it isn't a
	 * bndrun). Primary purpose is to walk the inheritance chain implied in the
	 * Workspace/Project/sub bnd files/bndrun files
	 */
	Processor																	owner;

	// Converter<String, ResolveMode> resolveModeFormatter =
	// EnumFormatter.create(ResolveMode.class, ResolveMode.manual);

	static {
		// register converters
		converters.put(Constants.BUNDLE_LICENSE, stringConverter);
		converters.put(Constants.BUNDLE_CATEGORY, stringConverter);
		converters.put(Constants.BUNDLE_NAME, stringConverter);
		converters.put(Constants.BUNDLE_DESCRIPTION, stringConverter);
		converters.put(Constants.BUNDLE_COPYRIGHT, stringConverter);
		converters.put(Constants.BUNDLE_UPDATELOCATION, stringConverter);
		converters.put(Constants.BUNDLE_VENDOR, stringConverter);
		converters.put(Constants.BUNDLE_CONTACTADDRESS, stringConverter);
		converters.put(Constants.BUNDLE_DOCURL, stringConverter);
		converters.put(Constants.BUILDPATH, buildPathConverter);
		converters.put(Constants.RUNBUNDLES, clauseListConverter);
		converters.put(Constants.BUNDLE_SYMBOLICNAME, stringConverter);
		converters.put(Constants.BUNDLE_VERSION, stringConverter);
		converters.put(Constants.BUNDLE_ACTIVATOR, stringConverter);
		converters.put(Constants.OUTPUT, stringConverter);
		converters.put(Constants.SOURCES, includedSourcesConverter);
		converters.put(Constants.PRIVATE_PACKAGE, listConverter);
		converters.put(Constants.PRIVATEPACKAGE, listConverter);
		converters.put(Constants.CLASSPATH, listConverter);
		converters.put(Constants.EXPORT_PACKAGE, exportPackageConverter);
		converters.put(Constants.SERVICE_COMPONENT, serviceComponentConverter);
		converters.put(Constants.IMPORT_PACKAGE, importPatternConverter);
		converters.put(Constants.RUNFRAMEWORK, stringConverter);
		converters.put(Constants.RUNFW, stringConverter);
		converters.put(Constants.SUB, listConverter);
		converters.put(Constants.RUNPROPERTIES, propertiesConverter);
		converters.put(Constants.RUNVM, stringConverter);
		converters.put(Constants.RUNPROGRAMARGS, stringConverter);
		// converters.put(BndConstants.RUNVMARGS, stringConverter);
		converters.put(Constants.TESTCASES, listConverter);
		converters.put(Constants.RUNREQUIRES, requirementListConverter);
		converters.put(Constants.RUNEE, eeConverter);
		converters.put(Constants.RUNREPOS, listConverter);
		// converters.put(BndConstants.RESOLVE_MODE, resolveModeConverter);
		converters.put(Constants.BUNDLE_BLUEPRINT, headerClauseListConverter);
		converters.put(Constants.INCLUDE_RESOURCE, listConverter);
		converters.put(Constants.INCLUDERESOURCE, listConverter);
		converters.put(Constants.STANDALONE, headerClauseListConverter);

		formatters.put(Constants.BUNDLE_LICENSE, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_CATEGORY, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_NAME, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_DESCRIPTION, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_COPYRIGHT, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_UPDATELOCATION, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_VENDOR, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_CONTACTADDRESS, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_DOCURL, newlineEscapeFormatter);

		formatters.put(Constants.BUILDPATH, headerClauseListFormatter);
		formatters.put(Constants.RUNBUNDLES, headerClauseListFormatter);
		formatters.put(Constants.BUNDLE_SYMBOLICNAME, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_VERSION, newlineEscapeFormatter);
		formatters.put(Constants.BUNDLE_ACTIVATOR, newlineEscapeFormatter);
		formatters.put(Constants.OUTPUT, newlineEscapeFormatter);
		formatters.put(Constants.SOURCES, defaultFalseBoolFormatter);
		formatters.put(Constants.PRIVATE_PACKAGE, stringListFormatter);
		formatters.put(Constants.PRIVATEPACKAGE, stringListFormatter);
		formatters.put(Constants.CLASSPATH, stringListFormatter);
		formatters.put(Constants.EXPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(Constants.SERVICE_COMPONENT, headerClauseListFormatter);
		formatters.put(Constants.IMPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(Constants.RUNFRAMEWORK, newlineEscapeFormatter);
		formatters.put(Constants.RUNFW, newlineEscapeFormatter);
		formatters.put(Constants.SUB, stringListFormatter);
		formatters.put(Constants.RUNPROPERTIES, propertiesFormatter);
		formatters.put(Constants.RUNVM, newlineEscapeFormatter);
		formatters.put(Constants.RUNPROGRAMARGS, newlineEscapeFormatter);
		// formatters.put(BndConstants.RUNVMARGS, newlineEscapeFormatter);
		// formatters.put(BndConstants.TESTSUITES, stringListFormatter);
		formatters.put(Constants.TESTCASES, stringListFormatter);
		formatters.put(Constants.RUNREQUIRES, requirementListFormatter);
		formatters.put(Constants.RUNEE, eeFormatter);
		formatters.put(Constants.RUNREPOS, runReposFormatter);
		// formatters.put(BndConstants.RESOLVE_MODE, resolveModeFormatter);
		formatters.put(Constants.BUNDLE_BLUEPRINT, headerClauseListFormatter);
		formatters.put(Constants.INCLUDE_RESOURCE, stringListFormatter);
		formatters.put(Constants.INCLUDERESOURCE, stringListFormatter);
		formatters.put(Constants.STANDALONE, standaloneLinkListFormatter);

		converters.put(Constants.PLUGIN, headerClauseListConverter);
		formatters.put(Constants.PLUGIN, complexHeaderClauseListFormatter);

	}

	public BndEditModel() {}

	public BndEditModel(BndEditModel model) {
		this();
		this.bndResource = model.bndResource;
		this.workspace = model.workspace;
		this.properties.putAll(model.properties);
		this.changesToSave.putAll(model.changesToSave);
	}

	public BndEditModel(Workspace workspace) {
		this();
		this.workspace = workspace;
	}

	public BndEditModel(IDocument document) throws IOException {
		this();
		loadFrom(document);
	}

	public BndEditModel(Project bndrun) throws IOException {
		this(bndrun.getWorkspace());
		this.project = bndrun;
		File propertiesFile = bndrun.getPropertiesFile();
		this.owner = workspace.findProcessor(propertiesFile)
			.orElse(bndrun);

		if (propertiesFile.isFile())
			this.document = new Document(IO.collect(propertiesFile));
		else
			this.document = new Document("");
		loadFrom(this.document);
	}

	public void loadFrom(IDocument document) throws IOException {
		try (InputStream in = toEscaped(document.get())) {
			loadFrom(in);
		}
	}

	public InputStream toEscaped(String text) throws IOException {
		StringReader unicode = new StringReader(text);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		while (true) {
			int c = unicode.read();
			if (c < 0)
				break;
			if (c >= 0x7F)
				bout.write(String.format("\\u%04X", c)
					.getBytes());
			else
				bout.write((char) c);
		}

		return new ByteArrayInputStream(bout.toByteArray());
	}

	public InputStream toAsciiStream(IDocument doc) throws IOException {
		saveChangesTo(doc);
		return toEscaped(doc.get());
	}

	public void loadFrom(File file) throws IOException {
		loadFrom(IO.stream(file));
	}

	public void loadFrom(InputStream inputStream) throws IOException {
		try {
			// Clear and load
			// The reason we skip standalone workspace properties
			// as parent is that they are copy of the Run file.
			// and confuse this edit model when you remove a
			// property.

			if (this.workspace != null && this.workspace.getLayout() != WorkspaceLayout.STANDALONE) {
				properties = (Properties) this.workspace.getProperties()
					.clone();
			} else {
				properties.clear();
			}
			properties.load(inputStream);
			objectProperties.clear();
			changesToSave.clear();

			// Fire property changes on all known property names
			for (String prop : KNOWN_PROPERTIES) {
				// null values for old and new forced the change to be fired
				propChangeSupport.firePropertyChange(prop, null, null);
			}
		} finally {
			inputStream.close();
		}

	}

	public void saveChangesTo(IDocument document) {
		this.lastChangedAt = System.currentTimeMillis();
		for (Iterator<Entry<String, String>> iter = changesToSave.entrySet()
			.iterator(); iter.hasNext();) {
			Entry<String, String> entry = iter.next();

			String propertyName = entry.getKey();
			String stringValue = entry.getValue();

			updateDocument(document, propertyName, stringValue);

			//
			// Ensure that properties keeps reflecting the current document
			// value
			//
			String value = cleanup(stringValue);
			if (value == null)
				value = "";

			if (propertyName != null)
				properties.setProperty(propertyName, value);

			iter.remove();
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
			buffer.append(name)
				.append(": ")
				.append(value);
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<String> getAllPropertyNames() {
		return StreamSupport.stream(Iterables.iterable(properties.propertyNames(), String.class::cast)
			.spliterator(), false)
			.collect(toList());
	}

	public Converter<Object, String> lookupConverter(String propertyName) {
		@SuppressWarnings("unchecked")
		Converter<Object, String> converter = (Converter<Object, String>) converters.get(propertyName);
		return converter;
	}

	public Converter<String, Object> lookupFormatter(String propertyName) {
		@SuppressWarnings("unchecked")
		Converter<String, Object> formatter = (Converter<String, Object>) formatters.get(propertyName);
		return formatter;
	}

	public Object genericGet(String propertyName) {
		Converter<? extends Object, String> converter = converters.get(propertyName);
		if (converter == null)
			converter = new NoopConverter<>();
		return doGetObject(propertyName, converter);
	}

	public void genericSet(String propertyName, Object value) {
		Object oldValue = genericGet(propertyName);
		@SuppressWarnings("unchecked")
		Converter<String, Object> formatter = (Converter<String, Object>) formatters.get(propertyName);
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
		doSetObject(Constants.BUNDLE_UPDATELOCATION, getBundleUpdateLocation(), bundleUpdateLocation,
			newlineEscapeFormatter);
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
		doSetObject(Constants.BUNDLE_CONTACTADDRESS, getBundleContactAddress(), bundleContactAddress,
			newlineEscapeFormatter);
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
		return doGetObject(Constants.OUTPUT, stringConverter);
	}

	public void setOutputFile(String name) {
		doSetObject(Constants.OUTPUT, getOutputFile(), name, newlineEscapeFormatter);
	}

	public boolean isIncludeSources() {
		return doGetObject(Constants.SOURCES, includedSourcesConverter);
	}

	public void setIncludeSources(boolean includeSources) {
		boolean oldValue = isIncludeSources();
		doSetObject(Constants.SOURCES, oldValue, includeSources, defaultFalseBoolFormatter);
	}

	public List<String> getPrivatePackages() {
		List<String> privatePackagesEntries1 = getEntries(Constants.PRIVATEPACKAGE, listConverter);
		List<String> privatePackagesEntries2 = getEntries(Constants.PRIVATE_PACKAGE, listConverter);

		return Stream.concat(privatePackagesEntries1.stream(), privatePackagesEntries2.stream())
			.distinct()
			.collect(toList());
	}

	public void setPrivatePackages(List<String> newPackages) {
		List<String> privatePackagesEntries1 = getEntries(Constants.PRIVATEPACKAGE, listConverter);
		List<String> privatePackagesEntries2 = getEntries(Constants.PRIVATE_PACKAGE, listConverter);

		Set<String> privatePackages = Stream.concat(privatePackagesEntries1.stream(), privatePackagesEntries2.stream())
			.collect(toSet());

		List<String> addedEntries = disjunction(newPackages, privatePackages);
		List<String> removedEntries = disjunction(privatePackages, newPackages);

		privatePackagesEntries1.removeAll(removedEntries);
		if (privatePackagesEntries1.isEmpty()) {
			removeEntries(Constants.PRIVATEPACKAGE);
		} else {
			setEntries(privatePackagesEntries1, Constants.PRIVATEPACKAGE);
		}

		privatePackagesEntries2.removeAll(removedEntries);
		if (privatePackagesEntries2.isEmpty()) {
			removeEntries(Constants.PRIVATE_PACKAGE);
		} else {
			setEntries(privatePackagesEntries2, Constants.PRIVATE_PACKAGE);
		}

		if (hasPrivatePackageInstruction()) {
			privatePackagesEntries1.addAll(addedEntries);
			setEntries(privatePackagesEntries1, Constants.PRIVATEPACKAGE);
		} else {
			privatePackagesEntries2.addAll(addedEntries);
			setEntries(privatePackagesEntries2, Constants.PRIVATE_PACKAGE);
		}
	}

	private void setEntries(List<? extends String> packages, String key) {
		List<String> oldPackages = getEntries(key, listConverter);
		doSetObject(key, oldPackages, packages, stringListFormatter);
	}

	private void removeEntries(String key) {
		List<String> oldPackages = getEntries(key, listConverter);
		doRemoveObject(key, oldPackages, null, stringListFormatter);
	}

	public void addPrivatePackage(String packageName) {
		String key = hasPrivatePackageInstruction() ? Constants.PRIVATEPACKAGE : Constants.PRIVATE_PACKAGE;
		List<String> packages = getEntries(key, listConverter);
		packages.add(packageName);
		setEntries(packages, key);
	}

	private boolean hasPrivatePackageInstruction() {
		return properties.containsKey(Constants.PRIVATEPACKAGE);
	}

	@SuppressWarnings("unchecked")
	private <E> List<String> getEntries(String instruction, Converter<? extends E, ? super String> converter) {
		List<String> entries = (List<String>) doGetObject(instruction, converter);
		return entries == null ? new ArrayList<>() : entries;
	}

	public List<ExportedPackage> getSystemPackages() {
		return doGetObject(Constants.RUNSYSTEMPACKAGES, exportPackageConverter);
	}

	public void setSystemPackages(List<? extends ExportedPackage> packages) {
		List<ExportedPackage> oldPackages = getSystemPackages();
		doSetObject(Constants.RUNSYSTEMPACKAGES, oldPackages, packages, headerClauseListFormatter);
	}

	public List<String> getClassPath() {
		return doGetObject(Constants.CLASSPATH, listConverter);
	}

	public void setClassPath(List<? extends String> classPath) {
		List<String> oldClassPath = getClassPath();
		doSetObject(Constants.CLASSPATH, oldClassPath, classPath, stringListFormatter);
	}

	public List<ExportedPackage> getExportedPackages() {
		return doGetObject(Constants.EXPORT_PACKAGE, exportPackageConverter);
	}

	public void setExportedPackages(List<? extends ExportedPackage> exports) {
		boolean referencesBundleVersion = false;

		if (exports != null) {
			for (ExportedPackage pkg : exports) {
				String versionString = pkg.getVersionString();
				if (versionString != null && versionString.contains(BUNDLE_VERSION_MACRO)) {
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
		exports = (exports == null) ? new ArrayList<>() : new ArrayList<>(exports);
		exports.add(export);
		setExportedPackages(exports);
	}

	public List<String> getDSAnnotationPatterns() {
		return doGetObject(Constants.DSANNOTATIONS, listConverter);
	}

	public void setDSAnnotationPatterns(List<? extends String> patterns) {
		List<String> oldValue = getDSAnnotationPatterns();
		doSetObject(Constants.DSANNOTATIONS, oldValue, patterns, stringListFormatter);
	}

	public List<ServiceComponent> getServiceComponents() {
		return doGetObject(Constants.SERVICE_COMPONENT, serviceComponentConverter);
	}

	public void setServiceComponents(List<? extends ServiceComponent> components) {
		List<ServiceComponent> oldValue = getServiceComponents();
		doSetObject(Constants.SERVICE_COMPONENT, oldValue, components, headerClauseListFormatter);
	}

	public List<ImportPattern> getImportPatterns() {
		return doGetObject(Constants.IMPORT_PACKAGE, importPatternConverter);
	}

	public void setImportPatterns(List<? extends ImportPattern> patterns) {
		List<ImportPattern> oldValue = getImportPatterns();
		doSetObject(Constants.IMPORT_PACKAGE, oldValue, patterns, headerClauseListFormatter);
	}

	public List<VersionedClause> getBuildPath() {
		return doGetObject(Constants.BUILDPATH, buildPathConverter);
	}

	public List<VersionedClause> getTestPath() {
		return doGetObject(Constants.TESTPATH, buildPathConverter);
	}

	public void setBuildPath(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		if (oldValue == null)
			oldValue = Collections.emptyList();

		doSetObject(Constants.BUILDPATH, oldValue, paths, headerClauseListFormatter);
	}

	public void addPath(VersionedClause versionedClause, String header) {
		List<VersionedClause> oldValue = doGetObject(header, buildPathConverter);
		List<VersionedClause> newValue = oldValue == null ? new ArrayList<>() : new ArrayList<>(oldValue);
		newValue.add(versionedClause);
		doSetObject(header, oldValue, newValue, headerClauseListFormatter);
	}

	public void addPath(BundleId bundleId, String header) {
		addPath(new VersionedClause(bundleId), header);
	}

	public void setTestPath(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getTestPath();
		doSetObject(Constants.TESTPATH, oldValue, paths, headerClauseListFormatter);
	}

	public List<VersionedClause> getRunBundles() {
		return doGetObject(Constants.RUNBUNDLES, clauseListConverter);
	}

	public void setRunBundles(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getRunBundles();
		doSetObject(Constants.RUNBUNDLES, oldValue, paths, headerClauseListFormatter);
	}

	public List<VersionedClause> getRunBundlesDecorator() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNBUNDLES_DECORATOR, clauseListConverter);
	}

	public void setRunBundlesDecorator(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getRunBundlesDecorator();
		doSetObject(aQute.bnd.osgi.Constants.RUNBUNDLES_DECORATOR, oldValue, paths, headerClauseListFormatter);
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
		return doGetObject(Constants.SUB, listConverter);
	}

	public void setSubBndFiles(List<String> subBndFiles) {
		List<String> oldValue = getSubBndFiles();
		doSetObject(Constants.SUB, oldValue, subBndFiles, stringListFormatter);
	}

	public Map<String, String> getRunProperties() {
		return doGetObject(Constants.RUNPROPERTIES, propertiesConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunProperties(java.util.Map)
	 */
	public void setRunProperties(Map<String, String> props) {
		Map<String, String> old = getRunProperties();
		doSetObject(Constants.RUNPROPERTIES, old, props, propertiesFormatter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#getRunVMArgs()
	 */
	public String getRunVMArgs() {
		return doGetObject(Constants.RUNVM, stringConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunVMArgs(java.lang.String)
	 */
	public void setRunVMArgs(String args) {
		String old = getRunVMArgs();
		doSetObject(Constants.RUNVM, old, args, newlineEscapeFormatter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#getRunProgramArgs()
	 */
	public String getRunProgramArgs() {
		return doGetObject(Constants.RUNPROGRAMARGS, stringConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunProgramArgs(java.lang.String)
	 */
	public void setRunProgramArgs(String args) {
		String old = getRunProgramArgs();
		doSetObject(Constants.RUNPROGRAMARGS, old, args, newlineEscapeFormatter);
	}

	public List<String> getTestSuites() {
		List<String> testCases = doGetObject(Constants.TESTCASES, listConverter);
		testCases = testCases != null ? testCases : Collections.emptyList();

		List<String> result = new ArrayList<>(testCases.size());
		result.addAll(testCases);
		return result;
	}

	public void setTestSuites(List<String> suites) {
		List<String> old = getTestSuites();
		doSetObject(Constants.TESTCASES, old, suites, stringListFormatter);
	}

	public List<HeaderClause> getPlugins() {
		return doGetObject(Constants.PLUGIN, headerClauseListConverter);
	}

	public void setPlugins(List<HeaderClause> plugins) {
		List<HeaderClause> old = getPlugins();
		doSetObject(Constants.PLUGIN, old, plugins, complexHeaderClauseListFormatter);
	}

	public List<String> getPluginPath() {
		return doGetObject(Constants.PLUGINPATH, listConverter);
	}

	public void setPluginPath(List<String> pluginPath) {
		List<String> old = getPluginPath();
		doSetObject(Constants.PLUGINPATH, old, pluginPath, stringListFormatter);
	}

	public List<String> getDistro() {
		return doGetObject(Constants.DISTRO, listConverter);
	}

	public void setDistro(List<String> distros) {
		List<String> old = getPluginPath();
		doSetObject(Constants.DISTRO, old, distros, stringListFormatter);
	}

	public List<String> getRunRepos() {
		return doGetObject(Constants.RUNREPOS, listConverter);
	}

	public void setRunRepos(List<String> repos) {
		List<String> old = getRunRepos();
		doSetObject(Constants.RUNREPOS, old, repos, runReposFormatter);
	}

	public String getRunFramework() {
		return doGetObject(Constants.RUNFRAMEWORK, stringConverter);
	}

	public String getRunFw() {
		return doGetObject(Constants.RUNFW, stringConverter);
	}

	public EE getEE() {
		return doGetObject(Constants.RUNEE, eeConverter);
	}

	public void setEE(EE ee) {
		EE old = getEE();
		doSetObject(Constants.RUNEE, old, ee, eeFormatter);
	}

	public void setRunFramework(String clause) {
		assert (Constants.RUNFRAMEWORK_SERVICES.equalsIgnoreCase(clause.trim())
			|| Constants.RUNFRAMEWORK_NONE.equalsIgnoreCase(clause.trim()));
		String oldValue = getRunFramework();
		doSetObject(Constants.RUNFRAMEWORK, oldValue, clause, newlineEscapeFormatter);
	}

	public void setRunFw(String clause) {
		String oldValue = getRunFw();
		doSetObject(Constants.RUNFW, oldValue, clause, newlineEscapeFormatter);
	}

	public List<Requirement> getRunRequires() {
		return doGetObject(Constants.RUNREQUIRES, requirementListConverter);
	}

	public void setRunRequires(List<Requirement> requires) {
		List<Requirement> oldValue = getRunRequires();
		doSetObject(Constants.RUNREQUIRES, oldValue, requires, requirementListFormatter);
	}

	public List<Requirement> getRunBlacklist() {
		return doGetObject(Constants.RUNBLACKLIST, requirementListConverter);
	}

	public void setRunBlacklist(List<Requirement> requires) {
		List<Requirement> oldValue = getRunBlacklist();
		doSetObject(Constants.RUNBLACKLIST, oldValue, requires, requirementListFormatter);
	}

	public List<HeaderClause> getStandaloneLinks() {
		return doGetObject(Constants.STANDALONE, headerClauseListConverter);
	}

	public void setStandaloneLinks(List<HeaderClause> headers) {
		List<HeaderClause> old = getStandaloneLinks();
		doSetObject(Constants.STANDALONE, old, headers, standaloneLinkListFormatter);
	}

	public List<HeaderClause> getIgnoreStandalone() {
		List<HeaderClause> v = doGetObject(Constants.IGNORE_STANDALONE, headerClauseListConverter);
		if (v != null)
			return v;

		//
		// compatibility fixup
		v = doGetObject("x-ignore-standalone", headerClauseListConverter);
		if (v == null)
			return null;

		setIgnoreStandalone(v);
		doSetObject("x-ignore-standalone", v, null, standaloneLinkListFormatter);

		return doGetObject(Constants.IGNORE_STANDALONE, headerClauseListConverter);
	}

	public void setIgnoreStandalone(List<HeaderClause> headers) {
		List<HeaderClause> old = getIgnoreStandalone();
		doSetObject(Constants.IGNORE_STANDALONE, old, headers, standaloneLinkListFormatter);
	}

	private <R> R doGetObject(String name, Converter<? extends R, ? super String> converter) {
		try {
			R result;
			if (objectProperties.containsKey(name)) {
				@SuppressWarnings("unchecked")
				R temp = (R) objectProperties.get(name);
				result = temp;
			} else if (changesToSave.containsKey(name)) {
				result = converter.convert(changesToSave.get(name));
				objectProperties.put(name, result);
			} else if (properties.containsKey(name)) {
				result = converter.convert(properties.getProperty(name));
				objectProperties.put(name, result);
			} else {
				result = converter.convert(null);
			}

			return result;
		} catch (Exception e) {
			return converter.error(e.getMessage());
		}
	}

	private <T> void doRemoveObject(String name, T oldValue, T newValue, Converter<String, ? super T> formatter) {
		objectProperties.remove(name);
		properties.remove(name);
		String v = formatter.convert(newValue);
		changesToSave.put(name, v);
		dirty = true;
		propChangeSupport.firePropertyChange(name, oldValue, newValue);
	}

	private <T> void doSetObject(String name, T oldValue, T newValue, Converter<String, ? super T> formatter) {
		objectProperties.put(name, newValue);
		String v = formatter.convert(newValue);
		changesToSave.put(name, v);
		dirty = true;
		propChangeSupport.firePropertyChange(name, oldValue, newValue);
	}

	public boolean isProjectFile() {
		return Project.BNDFILE.equals(getBndResourceName());
	}

	public boolean isBndrun() {
		return getBndResourceName().endsWith(Constants.DEFAULT_BNDRUN_EXTENSION);
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

	public String getBndResourceName() {
		if (bndResourceName == null)
			return "";
		return bndResourceName;
	}

	public void setBndResourceName(String bndResourceName) {
		this.bndResourceName = bndResourceName;
	}

	public List<HeaderClause> getBundleBlueprint() {
		return doGetObject(Constants.BUNDLE_BLUEPRINT, headerClauseListConverter);
	}

	public void setBundleBlueprint(List<HeaderClause> bundleBlueprint) {
		List<HeaderClause> old = getPlugins();
		doSetObject(Constants.BUNDLE_BLUEPRINT, old, bundleBlueprint, headerClauseListFormatter);
	}

	public void addBundleBlueprint(String location) {
		List<HeaderClause> bpLocations = getBundleBlueprint();
		if (bpLocations == null)
			bpLocations = new ArrayList<>();
		else
			bpLocations = new ArrayList<>(bpLocations);
		bpLocations.add(new HeaderClause(location, null));
		setBundleBlueprint(bpLocations);
	}

	public List<String> getIncludeResource() {
		List<String> includeResourceEntries1 = getEntries(Constants.INCLUDERESOURCE, listConverter);
		List<String> includeResourceEntries2 = getEntries(Constants.INCLUDE_RESOURCE, listConverter);

		return Stream.concat(includeResourceEntries1.stream(), includeResourceEntries2.stream())
			.distinct()
			.collect(toList());
	}

	@Deprecated
	public void setIncludeResource(List<String> newEntries) {
		List<String> resourceEntries1 = getEntries(Constants.INCLUDERESOURCE, listConverter).stream()
			.toList();
		List<String> resourceEntries2 = getEntries(Constants.INCLUDE_RESOURCE, listConverter);

		Set<String> resourceEntries = Stream.concat(resourceEntries1.stream(), resourceEntries2.stream())
			.collect(toSet());

		List<String> addedEntries = disjunction(newEntries, resourceEntries);
		List<String> removedEntries = disjunction(resourceEntries, newEntries);

		resourceEntries1.removeAll(removedEntries);
		if (resourceEntries1.isEmpty()) {
			removeEntries(Constants.INCLUDERESOURCE);
		} else {
			setEntries(resourceEntries1, Constants.INCLUDERESOURCE);
		}

		resourceEntries2.removeAll(removedEntries);
		if (resourceEntries2.isEmpty()) {
			removeEntries(Constants.INCLUDE_RESOURCE);
		} else {
			setEntries(resourceEntries2, Constants.INCLUDE_RESOURCE);
		}

		if (hasIncludeResourceHeaderLikeInstruction()) {
			resourceEntries2.addAll(addedEntries);
			setEntries(resourceEntries2, Constants.INCLUDE_RESOURCE);
		} else {
			resourceEntries1.addAll(addedEntries);
			setEntries(resourceEntries1, Constants.INCLUDERESOURCE);
		}
	}

	public void addIncludeResource(String resource) {
		String key = hasIncludeResourceHeaderLikeInstruction() ? Constants.INCLUDE_RESOURCE : Constants.INCLUDERESOURCE;
		List<String> entries = getEntries(key, listConverter);
		entries.add(resource);
		setEntries(entries, key);
	}

	private boolean hasIncludeResourceHeaderLikeInstruction() {
		return properties.containsKey(Constants.INCLUDE_RESOURCE);
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public Project getProject() {
		return project;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void setWorkspace(Workspace workspace) {
		Workspace old = this.workspace;
		this.workspace = workspace;
		propChangeSupport.firePropertyChange(PROP_WORKSPACE, old, workspace);
	}

	public String getGenericString(String name) {
		return doGetObject(name, stringConverter);
	}

	public void setGenericString(String name, String value) {
		doSetObject(name, getGenericString(name), value, stringConverter);
	}

	/**
	 * Return a processor for this model. This processor is based on the parent
	 * project or the bndrun file. It will contain the properties of the project
	 * file and the changes from the model.
	 *
	 * @return a processor that reflects the actual project or bndrun file setup
	 * @throws Exception
	 */
	public Processor getProperties() throws Exception {
		File source = getBndResource();
		Processor parent;

		if (project != null) {
			parent = project;
			if (source == null) {
				source = project.getPropertiesFile();
			}
		} else if (workspace != null && isCnf()) {
			parent = workspace;
			if (source == null) {
				source = workspace.getPropertiesFile();
			}
		} else if (source != null) {
			parent = Workspace.getRun(source);
			if (parent == null) {
				parent = new Processor();
				parent.setProperties(source);
			}
		} else {
			parent = new Processor(properties, false);
		}

		UTF8Properties p = new UTF8Properties(parent.getProperties());

		if (!changesToSave.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			changesToSave.forEach((key, value) -> sb.append(key)
				.append(':')
				.append(' ')
				.append(value)
				.append('\n')
				.append('\n'));
			p.load(sb.toString(), null, null);
		}

		Processor result;
		p = p.replaceHere(parent.getBase());
		result = new Processor(parent, p, false);
		result.setBase(parent.getBase());
		if (source != null)
			result.setPropertiesFile(source);
		return result;
	}

	private String cleanup(String value) {
		if (value == null)
			return null;

		return value.replaceAll("\\\\\n", "");
	}

	private static <E> List<E> disjunction(final Collection<E> collection, final Collection<?> remove) {
		final List<E> list = new ArrayList<>();
		for (final E obj : collection) {
			if (!remove.contains(obj)) {
				list.add(obj);
			}
		}
		return list;
	}

	/**
	 * Return the saved changes in document format.
	 */

	public Map<String, String> getDocumentChanges() {
		return changesToSave;
	}

	/**
	 * If this BndEditModel was created with a project then this method will
	 * save the changes in the document and will store them in the associated
	 * file.
	 *
	 * @throws IOException
	 */
	public void saveChanges() throws IOException {
		assert document != null
			&& project != null : "you can only call saveChanges when you created this edit model with a project";

		saveChangesTo(document);
		store(document, getProject().getPropertiesFile());
		dirty = false;
	}

	public static void store(IDocument document, File file) throws IOException {
		IO.store(document.get(), file);
	}

	public ResolutionInstructions.ResolveMode getResolveMode() {
		String resolve = getGenericString(Constants.RESOLVE);
		if (resolve != null) {
			try {
				return aQute.lib.converter.Converter.cnv(ResolutionInstructions.ResolveMode.class, resolve);
			} catch (Exception e) {
				project.error("Invalid value for %s: %s. Allowed values are %s", Constants.RESOLVE, resolve,
					ResolutionInstructions.ResolveMode.class.getEnumConstants());
			}
		}
		return ResolutionInstructions.ResolveMode.manual;
	}

	public void setResolveMode(ResolutionInstructions.ResolveMode resolveMode) {
		setGenericString(Constants.RESOLVE, resolveMode.name());
	}

	/**
	 * @return true if there is a discrepancy between the project's file and the
	 *         document
	 */
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean isDirty) {
		this.dirty = isDirty;
	}

	public void load() throws IOException {
		loadFrom(project.getPropertiesFile());
	}

	/**
	 * If this is on the cnf project
	 *
	 * @return true if it is the cnf project
	 */
	public boolean isCnf() {
		return bndResource != null && Workspace.CNFDIR.equals(bndResource.getParentFile()
			.getName()) && Workspace.BUILDFILE.equals(bndResource.getName());
	}

	/**
	 * Use the built in formatters to take an unformatted header and turn it
	 * into a formatted header useful in the editor, for example escaped
	 * newlines.
	 *
	 * @param <T> the intermediate type, doesn't matter
	 * @param header the name of the instruction
	 * @param input the source string
	 * @return the input or a formatted input if there is converter
	 */

	@SuppressWarnings("unchecked")
	public static <T> String format(String header, String input) {
		Converter<T, String> converter = (Converter<T, String>) converters.get(header);
		if (converter == null)
			return input;

		T converted = converter.convert(input);

		Converter<String, T> formatter = (Converter<String, T>) formatters.get(header);
		return formatter.convert(converted);
	}

	@SuppressWarnings("unchecked")
	public <T extends Collection<Object>> String add(String header, String toAdd) {
		try {
			Converter<T, String> converter = (Converter<T, String>) converters.get(header);
			T last = converter.convert(toAdd);
			T oldValue = doGetObject(header, converter);

			@SuppressWarnings("rawtypes")
			T newValue = (T) last.getClass()
				.newInstance();
			if (oldValue != null)
				newValue.addAll(oldValue);
			else
				oldValue = (T) last.getClass()
					.newInstance();

			newValue.addAll(last);

			Converter<String, T> formatter = (Converter<String, T>) formatters.get(header);
			doSetObject(header, oldValue, newValue, formatter);
			return header + ": " + formatter.convert(newValue);
		} catch (IllegalArgumentException | InstantiationException | IllegalAccessException e) {
			throw Exceptions.duck(e);
		}
	}

	public long getLastChangedAt() {
		return lastChangedAt;
	}
}
