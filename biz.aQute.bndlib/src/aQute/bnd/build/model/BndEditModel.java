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
import aQute.bnd.build.Run;
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
import aQute.bnd.help.instructions.ResolutionInstructions;
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
public class BndEditModel {

	public static final String										NEWLINE_LINE_SEPARATOR		= "\\n\\\n\t";
	public static final String										LIST_SEPARATOR				= ",\\\n\t";

	private static String[]											KNOWN_PROPERTIES			= new String[] {
		Constants.BUNDLE_LICENSE, Constants.BUNDLE_CATEGORY, Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION,
		Constants.BUNDLE_COPYRIGHT, Constants.BUNDLE_UPDATELOCATION, Constants.BUNDLE_VENDOR,
		Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL, Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_VERSION, Constants.BUNDLE_ACTIVATOR, Constants.EXPORT_PACKAGE, Constants.IMPORT_PACKAGE,
		Constants.PRIVATE_PACKAGE, Constants.PRIVATEPACKAGE, aQute.bnd.osgi.Constants.SOURCES,
		aQute.bnd.osgi.Constants.SERVICE_COMPONENT, aQute.bnd.osgi.Constants.CLASSPATH,
		aQute.bnd.osgi.Constants.BUILDPATH, aQute.bnd.osgi.Constants.RUNBUNDLES, aQute.bnd.osgi.Constants.RUNPROPERTIES,
		aQute.bnd.osgi.Constants.SUB, aQute.bnd.osgi.Constants.RUNFRAMEWORK, aQute.bnd.osgi.Constants.RUNFW,
		aQute.bnd.osgi.Constants.RUNVM, aQute.bnd.osgi.Constants.RUNPROGRAMARGS, aQute.bnd.osgi.Constants.DISTRO,
		// BndConstants.RUNVMARGS,
		// BndConstants.TESTSUITES,
		aQute.bnd.osgi.Constants.TESTCASES, aQute.bnd.osgi.Constants.PLUGIN, aQute.bnd.osgi.Constants.PLUGINPATH,
		aQute.bnd.osgi.Constants.RUNREPOS, aQute.bnd.osgi.Constants.RUNREQUIRES, aQute.bnd.osgi.Constants.RUNEE,
		aQute.bnd.osgi.Constants.RUNBLACKLIST, Constants.BUNDLE_BLUEPRINT, Constants.INCLUDE_RESOURCE,
		Constants.STANDALONE
	};

	public static final String										PROP_WORKSPACE				= "_workspace";

	public static final String										BUNDLE_VERSION_MACRO		= "${"
		+ Constants.BUNDLE_VERSION + "}";

	private final Map<String, Converter<? extends Object, String>>	converters					= new HashMap<>();
	private final Map<String, Converter<String, ? extends Object>>	formatters					= new HashMap<>();
	// private final DataModelHelper obrModelHelper = new DataModelHelperImpl();

	private File													bndResource;
	private String													bndResourceName;

	private final PropertyChangeSupport								propChangeSupport			= new PropertyChangeSupport(
		this);
	private Properties												properties					= new UTF8Properties();
	private final Map<String, Object>								objectProperties			= new HashMap<>();
	private final Map<String, String>								changesToSave				= new TreeMap<>();
	private Project													project;

	private volatile boolean										dirty;

	// CONVERTERS
	private Converter<List<VersionedClause>, String>				buildPathConverter			= new HeaderClauseListConverter<>(
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
	private Converter<List<VersionedClause>, String>				buildPackagesConverter		= new HeaderClauseListConverter<>(
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
																											return VersionedClause
																												.error(
																													msg);
																										}
																									});
	private Converter<List<VersionedClause>, String>				clauseListConverter			= new HeaderClauseListConverter<>(
		new VersionedClauseConverter());
	private Converter<String, String>								stringConverter				= new NoopConverter<>();
	private Converter<Boolean, String>								includedSourcesConverter	= new Converter<Boolean, String>() {
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
	private Converter<List<String>, String>							listConverter				= SimpleListConverter
		.create();

	private Converter<List<HeaderClause>, String>					headerClauseListConverter	= new HeaderClauseListConverter<>(
		new NoopConverter<>());

	private Converter<List<ExportedPackage>, String>				exportPackageConverter		= new HeaderClauseListConverter<>(
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

	private Converter<List<ServiceComponent>, String>				serviceComponentConverter	= new HeaderClauseListConverter<>(
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
	private Converter<List<ImportPattern>, String>					importPatternConverter		= new HeaderClauseListConverter<>(
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

	private Converter<Map<String, String>, String>					propertiesConverter			= new PropertiesConverter();

	private Converter<List<Requirement>, String>					requirementListConverter	= new RequirementListConverter();
	private Converter<EE, String>									eeConverter					= new EEConverter();

	// Converter<ResolveMode, String> resolveModeConverter =
	// EnumConverter.create(ResolveMode.class, ResolveMode.manual);

	// FORMATTERS
	private Converter<String, String>								newlineEscapeFormatter		= new NewlineEscapedStringFormatter();
	private Converter<String, Boolean>								defaultFalseBoolFormatter	= new DefaultBooleanFormatter(
		false);
	private Converter<String, Collection<?>>						stringListFormatter			= new CollectionFormatter<>(
		LIST_SEPARATOR, (String) null);
	private Converter<String, Collection<? extends HeaderClause>>	headerClauseListFormatter	= new CollectionFormatter<>(
		LIST_SEPARATOR, new HeaderClauseFormatter(), null);
	private Converter<String, Map<String, String>>					propertiesFormatter			= new MapFormatter(
		LIST_SEPARATOR, new PropertiesEntryFormatter(), null);

	private Converter<String, Collection<? extends Requirement>>	requirementListFormatter	= new CollectionFormatter<>(
		LIST_SEPARATOR, new RequirementFormatter(), null);

	private Converter<String, Collection<? extends HeaderClause>>	standaloneLinkListFormatter	= new CollectionFormatter<>(
		LIST_SEPARATOR, new HeaderClauseFormatter(), "");

	private Converter<String, EE>									eeFormatter					= new EEFormatter();
	private Converter<String, Collection<? extends String>>			runReposFormatter			= new CollectionFormatter<>(
		LIST_SEPARATOR, aQute.bnd.osgi.Constants.EMPTY_HEADER);
	private Workspace												workspace;
	private IDocument												document;

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
		converters.put(aQute.bnd.osgi.Constants.PRIVATEPACKAGE, listConverter);
		converters.put(aQute.bnd.osgi.Constants.CLASSPATH, listConverter);
		converters.put(Constants.EXPORT_PACKAGE, exportPackageConverter);
		converters.put(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
		converters.put(Constants.IMPORT_PACKAGE, importPatternConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNFRAMEWORK, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNFW, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.SUB, listConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNPROPERTIES, propertiesConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNVM, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNPROGRAMARGS, stringConverter);
		// converters.put(BndConstants.RUNVMARGS, stringConverter);
		converters.put(aQute.bnd.osgi.Constants.TESTSUITES, listConverter);
		converters.put(aQute.bnd.osgi.Constants.TESTCASES, listConverter);
		converters.put(aQute.bnd.osgi.Constants.PLUGIN, headerClauseListConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNREQUIRES, requirementListConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNEE, eeConverter);
		converters.put(aQute.bnd.osgi.Constants.RUNREPOS, listConverter);
		// converters.put(BndConstants.RESOLVE_MODE, resolveModeConverter);
		converters.put(Constants.BUNDLE_BLUEPRINT, headerClauseListConverter);
		converters.put(Constants.INCLUDE_RESOURCE, listConverter);
		converters.put(Constants.INCLUDERESOURCE, listConverter);
		converters.put(Constants.STANDALONE, headerClauseListConverter);

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
		formatters.put(aQute.bnd.osgi.Constants.PRIVATEPACKAGE, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.CLASSPATH, stringListFormatter);
		formatters.put(Constants.EXPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, headerClauseListFormatter);
		formatters.put(Constants.IMPORT_PACKAGE, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNFRAMEWORK, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNFW, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.SUB, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNPROPERTIES, propertiesFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNVM, newlineEscapeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNPROGRAMARGS, newlineEscapeFormatter);
		// formatters.put(BndConstants.RUNVMARGS, newlineEscapeFormatter);
		// formatters.put(BndConstants.TESTSUITES, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.TESTCASES, stringListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.PLUGIN, headerClauseListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNREQUIRES, requirementListFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNEE, eeFormatter);
		formatters.put(aQute.bnd.osgi.Constants.RUNREPOS, runReposFormatter);
		// formatters.put(BndConstants.RESOLVE_MODE, resolveModeFormatter);
		formatters.put(Constants.BUNDLE_BLUEPRINT, headerClauseListFormatter);
		formatters.put(Constants.INCLUDE_RESOURCE, stringListFormatter);
		formatters.put(Constants.INCLUDERESOURCE, stringListFormatter);
		formatters.put(Constants.STANDALONE, standaloneLinkListFormatter);
	}

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

	public BndEditModel(Project project) throws IOException {
		this(project.getWorkspace());
		this.project = project;
		File propertiesFile = project.getPropertiesFile();
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
		return doGetObject(aQute.bnd.osgi.Constants.RUNSYSTEMPACKAGES, exportPackageConverter);
	}

	public void setSystemPackages(List<? extends ExportedPackage> packages) {
		List<ExportedPackage> oldPackages = getSystemPackages();
		doSetObject(aQute.bnd.osgi.Constants.RUNSYSTEMPACKAGES, oldPackages, packages, headerClauseListFormatter);
	}

	public List<String> getClassPath() {
		return doGetObject(aQute.bnd.osgi.Constants.CLASSPATH, listConverter);
	}

	public void setClassPath(List<? extends String> classPath) {
		List<String> oldClassPath = getClassPath();
		doSetObject(aQute.bnd.osgi.Constants.CLASSPATH, oldClassPath, classPath, stringListFormatter);
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
		return doGetObject(aQute.bnd.osgi.Constants.DSANNOTATIONS, listConverter);
	}

	public void setDSAnnotationPatterns(List<? extends String> patterns) {
		List<String> oldValue = getDSAnnotationPatterns();
		doSetObject(aQute.bnd.osgi.Constants.DSANNOTATIONS, oldValue, patterns, stringListFormatter);
	}

	public List<ServiceComponent> getServiceComponents() {
		return doGetObject(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
	}

	public void setServiceComponents(List<? extends ServiceComponent> components) {
		List<ServiceComponent> oldValue = getServiceComponents();
		doSetObject(aQute.bnd.osgi.Constants.SERVICE_COMPONENT, oldValue, components, headerClauseListFormatter);
	}

	public List<ImportPattern> getImportPatterns() {
		return doGetObject(Constants.IMPORT_PACKAGE, importPatternConverter);
	}

	public void setImportPatterns(List<? extends ImportPattern> patterns) {
		List<ImportPattern> oldValue = getImportPatterns();
		doSetObject(Constants.IMPORT_PACKAGE, oldValue, patterns, headerClauseListFormatter);
	}

	public List<VersionedClause> getBuildPath() {
		return doGetObject(aQute.bnd.osgi.Constants.BUILDPATH, buildPathConverter);
	}

	public List<VersionedClause> getTestPath() {
		return doGetObject(aQute.bnd.osgi.Constants.TESTPATH, buildPathConverter);
	}

	public void setBuildPath(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		doSetObject(aQute.bnd.osgi.Constants.BUILDPATH, oldValue, paths, headerClauseListFormatter);
	}

	public void setTestPath(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getTestPath();
		doSetObject(aQute.bnd.osgi.Constants.TESTPATH, oldValue, paths, headerClauseListFormatter);
	}

	@Deprecated
	public List<VersionedClause> getBuildPackages() {
		return doGetObject(aQute.bnd.osgi.Constants.BUILDPACKAGES, buildPackagesConverter);
	}

	@Deprecated
	public void setBuildPackages(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPackages();
		doSetObject(aQute.bnd.osgi.Constants.BUILDPACKAGES, oldValue, paths, headerClauseListFormatter);
	}

	public List<VersionedClause> getRunBundles() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNBUNDLES, clauseListConverter);
	}

	public void setRunBundles(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getRunBundles();
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

	public Map<String, String> getRunProperties() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNPROPERTIES, propertiesConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunProperties(java.util.Map)
	 */
	public void setRunProperties(Map<String, String> props) {
		Map<String, String> old = getRunProperties();
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

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#getRunProgramArgs()
	 */
	public String getRunProgramArgs() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNPROGRAMARGS, stringConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see bndtools.editor.model.IBndModel#setRunProgramArgs(java.lang.String)
	 */
	public void setRunProgramArgs(String args) {
		String old = getRunProgramArgs();
		doSetObject(aQute.bnd.osgi.Constants.RUNPROGRAMARGS, old, args, newlineEscapeFormatter);
	}

	@SuppressWarnings("deprecation")
	public List<String> getTestSuites() {
		List<String> testCases = doGetObject(aQute.bnd.osgi.Constants.TESTCASES, listConverter);
		testCases = testCases != null ? testCases : Collections.emptyList();

		List<String> testSuites = doGetObject(aQute.bnd.osgi.Constants.TESTSUITES, listConverter);
		testSuites = testSuites != null ? testSuites : Collections.emptyList();

		List<String> result = new ArrayList<>(testCases.size() + testSuites.size());
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

	public List<String> getDistro() {
		return doGetObject(aQute.bnd.osgi.Constants.DISTRO, listConverter);
	}

	public void setDistro(List<String> distros) {
		List<String> old = getPluginPath();
		doSetObject(aQute.bnd.osgi.Constants.DISTRO, old, distros, stringListFormatter);
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
		assert (Constants.RUNFRAMEWORK_SERVICES.equals(clause.toLowerCase()
			.trim()) || Constants.RUNFRAMEWORK_NONE.equals(
				clause.toLowerCase()
					.trim()));
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

	public List<Requirement> getRunBlacklist() {
		return doGetObject(aQute.bnd.osgi.Constants.RUNBLACKLIST, requirementListConverter);
	}

	public void setRunBlacklist(List<Requirement> requires) {
		List<Requirement> oldValue = getRunBlacklist();
		doSetObject(aQute.bnd.osgi.Constants.RUNBLACKLIST, oldValue, requires, requirementListFormatter);
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
		return doGetObject(aQute.bnd.osgi.Constants.BUNDLE_BLUEPRINT, headerClauseListConverter);
	}

	public void setBundleBlueprint(List<HeaderClause> bundleBlueprint) {
		List<HeaderClause> old = getPlugins();
		doSetObject(aQute.bnd.osgi.Constants.BUNDLE_BLUEPRINT, old, bundleBlueprint, headerClauseListFormatter);
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

	public void setIncludeResource(List<String> newEntries) {
		List<String> resourceEntries1 = getEntries(Constants.INCLUDERESOURCE, listConverter);
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

		if (hasIncludeResourceInstruction()) {
			resourceEntries1.addAll(addedEntries);
			setEntries(resourceEntries1, Constants.INCLUDERESOURCE);
		} else {
			resourceEntries2.addAll(addedEntries);
			setEntries(resourceEntries2, Constants.INCLUDE_RESOURCE);
		}
	}

	public void addIncludeResource(String resource) {
		String key = hasIncludeResourceInstruction() ? Constants.INCLUDERESOURCE : Constants.INCLUDE_RESOURCE;
		List<String> entries = getEntries(key, listConverter);
		entries.add(resource);
		setEntries(entries, key);
	}

	private boolean hasIncludeResourceInstruction() {
		return properties.containsKey(Constants.INCLUDERESOURCE);
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
		Processor parent = null;

		if ((isProjectFile() && project != null) || (project instanceof Run))
			parent = project;
		else if (getBndResource() != null) {
			parent = Workspace.getRun(getBndResource());
			if (parent == null) {
				parent = new Processor();
				parent.setProperties(getBndResource(), getBndResource().getParentFile());
			}
		}

		Processor result;
		if (parent == null)
			result = new Processor();
		else
			result = new Processor(parent);

		StringBuilder sb = new StringBuilder();

		for (Entry<String, String> e : changesToSave.entrySet()) {
			sb.append(e.getKey())
				.append(": ")
				.append(e.getValue())
				.append("\n\n");
		}
		UTF8Properties p = new UTF8Properties();
		p.load(new StringReader(sb.toString()));

		result.getProperties()
			.putAll(properties);
		result.getProperties()
			.putAll(p);
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
}
