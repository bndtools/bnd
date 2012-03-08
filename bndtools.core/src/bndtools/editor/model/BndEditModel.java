/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.editor.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.osgi.framework.Constants;

import aQute.libg.header.Attrs;
import aQute.libg.version.Version;
import bndtools.BndConstants;
import bndtools.api.EE;
import bndtools.api.IPersistableBndModel;
import bndtools.api.Requirement;
import bndtools.api.ResolveMode;
import bndtools.editor.model.conversions.ClauseListConverter;
import bndtools.editor.model.conversions.CollectionFormatter;
import bndtools.editor.model.conversions.Converter;
import bndtools.editor.model.conversions.DefaultBooleanFormatter;
import bndtools.editor.model.conversions.DefaultFormatter;
import bndtools.editor.model.conversions.EnumConverter;
import bndtools.editor.model.conversions.EnumFormatter;
import bndtools.editor.model.conversions.HeaderClauseFormatter;
import bndtools.editor.model.conversions.HeaderClauseListConverter;
import bndtools.editor.model.conversions.MapFormatter;
import bndtools.editor.model.conversions.NewlineEscapedStringFormatter;
import bndtools.editor.model.conversions.NoopConverter;
import bndtools.editor.model.conversions.PropertiesConverter;
import bndtools.editor.model.conversions.PropertiesEntryFormatter;
import bndtools.editor.model.conversions.SimpleListConverter;
import bndtools.editor.model.conversions.VersionedClauseConverter;
import bndtools.model.clauses.ExportedPackage;
import bndtools.model.clauses.HeaderClause;
import bndtools.model.clauses.ImportPattern;
import bndtools.model.clauses.ServiceComponent;
import bndtools.model.clauses.VersionedClause;
import bndtools.types.Pair;

/**
 * A model for a Bnd file. In the first iteration, use a simple Properties
 * object; this will need to be enhanced to additionally record formatting, e.g.
 * line breaks and empty lines, and comments.
 *
 * @author Neil Bartlett
 */
public class BndEditModel implements IPersistableBndModel {

    public static final String LINE_SEPARATOR = " \\\n\t";
	public static final String LIST_SEPARATOR = ",\\\n\t";

	private static final String ISO_8859_1 = "ISO-8859-1"; //$NON-NLS-1$

	@SuppressWarnings("deprecation")
    private static final String[] KNOWN_PROPERTIES = new String[] {
		Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_VERSION,
		Constants.BUNDLE_ACTIVATOR,
		Constants.EXPORT_PACKAGE,
		Constants.IMPORT_PACKAGE,
		aQute.lib.osgi.Constants.PRIVATE_PACKAGE,
		aQute.lib.osgi.Constants.SOURCES,
		aQute.lib.osgi.Constants.VERSIONPOLICY,
		aQute.lib.osgi.Constants.SERVICE_COMPONENT,
		aQute.lib.osgi.Constants.CLASSPATH,
		aQute.lib.osgi.Constants.BUILDPATH,
		aQute.lib.osgi.Constants.BUILDPACKAGES,
		aQute.lib.osgi.Constants.RUNBUNDLES,
		aQute.lib.osgi.Constants.RUNPROPERTIES,
		aQute.lib.osgi.Constants.SUB,
		BndConstants.RUNFRAMEWORK,
		aQute.lib.osgi.Constants.RUNVM,
		BndConstants.RUNVMARGS,
		BndConstants.TESTSUITES,
		aQute.lib.osgi.Constants.TESTCASES,
		aQute.lib.osgi.Constants.PLUGIN,
		BndConstants.RUNREQUIRE,
		BndConstants.RUNEE,
		BndConstants.RUNREPOS,
		BndConstants.RESOLVE_MODE
	};

	public static final String BUNDLE_VERSION_MACRO = "${" + Constants.BUNDLE_VERSION + "}";

	private final Map<String, Converter<? extends Object, String>> converters = new HashMap<String, Converter<? extends Object,String>>();
	private final Map<String, Converter<String, ? extends Object>> formatters = new HashMap<String, Converter<String, ? extends Object>>();
	private final DataModelHelper obrModelHelper = new DataModelHelperImpl();

	private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);
	private final Properties properties = new Properties();;

	private IResource bndResource;
	private boolean projectFile;
	private final Map<String, Object> objectProperties = new HashMap<String, Object>();
	private final Map<String, String> changesToSave = new HashMap<String, String>();

	// CONVERTERS
    private Converter<List<VersionedClause>, String> buildPathConverter = new ClauseListConverter<VersionedClause>(new Converter<VersionedClause, Pair<String, Attrs>>() {
        public VersionedClause convert(Pair<String, Attrs> input) throws IllegalArgumentException {
            return new VersionedClause(input.getFirst(), input.getSecond());
        }
    });
    private Converter<List<VersionedClause>, String> buildPackagesConverter = new ClauseListConverter<VersionedClause>(new Converter<VersionedClause, Pair<String, Attrs>>() {
        public VersionedClause convert(Pair<String, Attrs> input) throws IllegalArgumentException {
            return new VersionedClause(input.getFirst(), input.getSecond());
        }
    });
    private Converter<List<VersionedClause>, String> clauseListConverter = new ClauseListConverter<VersionedClause>(new VersionedClauseConverter());
    private Converter<String, String> stringConverter = new NoopConverter<String>();
    private Converter<Boolean, String> includedSourcesConverter = new Converter<Boolean,String>() {
        public Boolean convert(String string) throws IllegalArgumentException {
            return Boolean.valueOf(string);
        }
    };
    private Converter<VersionPolicy, String> versionPolicyConverter = new Converter<VersionPolicy,String>() {
        public VersionPolicy convert(String string) throws IllegalArgumentException {
            return VersionPolicy.parse(string);
        }
    };
    Converter<List<String>, String> listConverter = SimpleListConverter.create();
    Converter<List<HeaderClause>, String> headerClauseListConverter = new HeaderClauseListConverter();
    ClauseListConverter<ExportedPackage> exportPackageConverter = new ClauseListConverter<ExportedPackage>(new Converter<ExportedPackage, Pair<String, Attrs>>() {
        public ExportedPackage convert(Pair<String, Attrs> input) {
            return new ExportedPackage(input.getFirst(), input.getSecond());
        }
    });
    Converter<List<ServiceComponent>, String> serviceComponentConverter = new ClauseListConverter<ServiceComponent>(new Converter<ServiceComponent, Pair<String, Attrs>>() {
        public ServiceComponent convert(Pair<String, Attrs> input) throws IllegalArgumentException {
            return new ServiceComponent(input.getFirst(), input.getSecond());
        }
    });
    Converter<List<ImportPattern>, String> importPatternConverter =  new ClauseListConverter<ImportPattern>(new Converter<ImportPattern, Pair<String, Attrs>>() {
        public ImportPattern convert(Pair<String, Attrs> input) throws IllegalArgumentException {
            return new ImportPattern(input.getFirst(), input.getSecond());
        }
    });

    Converter<Map<String, String>, String> propertiesConverter = new PropertiesConverter();

    Converter<List<Requirement>, String> requirementListConverter = SimpleListConverter.create(new Converter<Requirement, String>() {
        public Requirement convert(String input) throws IllegalArgumentException {
            int index = input.indexOf(":");
            if (index < 0)
                throw new IllegalArgumentException("Invalid format for OBR requirement");

            String name = input.substring(0, index);
            String filter = input.substring(index + 1);

            return new Requirement(name, filter);
        }
    });
    Converter<EE, String> eeConverter = new Converter<EE, String>() {
        public EE convert(String input) throws IllegalArgumentException {
            return EE.parse(input);
        }
    };

    Converter<ResolveMode, String> resolveModeConverter = EnumConverter.create(ResolveMode.class, ResolveMode.manual);

    // FORMATTERS
    Converter<String, Object> defaultFormatter = new DefaultFormatter();
    Converter<String, String> newlineEscapeFormatter = new NewlineEscapedStringFormatter();
    Converter<String, Boolean> defaultFalseBoolFormatter = new DefaultBooleanFormatter(false);
    Converter<String, Collection<?>> stringListFormatter = new CollectionFormatter<Object>(LIST_SEPARATOR, (String) null);
    Converter<String, Collection<? extends HeaderClause>> headerClauseListFormatter = new CollectionFormatter<HeaderClause>(LIST_SEPARATOR, new HeaderClauseFormatter(), null);
    Converter<String, Map<String, String>> propertiesFormatter = new MapFormatter(LIST_SEPARATOR, new PropertiesEntryFormatter(), null);
    Converter<String, Collection<? extends Requirement>> requirementListFormatter = new CollectionFormatter<Requirement>(LIST_SEPARATOR, new Converter<String, Requirement>() {
        public String convert(Requirement input) throws IllegalArgumentException {
            return new StringBuilder().append(input.getName()).append(':').append(input.getFilter()).toString();
        }
    }, null);
    Converter<String, EE> eeFormatter = new Converter<String, EE>() {
        public String convert(EE input) throws IllegalArgumentException {
            return input != null ? input.getEEName() : null;
        }
    };
    Converter<String,Collection<? extends String>> runReposFormatter = new CollectionFormatter<String>(LIST_SEPARATOR, aQute.lib.osgi.Constants.EMPTY_HEADER);
    Converter<String, ResolveMode> resolveModeFormatter = EnumFormatter.create(ResolveMode.class, ResolveMode.manual);

	@SuppressWarnings("deprecation")
    public BndEditModel() {
	    // register converters
        converters.put(aQute.lib.osgi.Constants.BUILDPATH, buildPathConverter);
        converters.put(aQute.lib.osgi.Constants.BUILDPACKAGES, buildPackagesConverter);
        converters.put(aQute.lib.osgi.Constants.RUNBUNDLES, clauseListConverter);
        converters.put(Constants.BUNDLE_SYMBOLICNAME, stringConverter);
        converters.put(Constants.BUNDLE_VERSION, stringConverter);
        converters.put(Constants.BUNDLE_ACTIVATOR, stringConverter);
        converters.put(BndConstants.OUTPUT, stringConverter);
        converters.put(aQute.lib.osgi.Constants.SOURCES, includedSourcesConverter);
        converters.put(aQute.lib.osgi.Constants.VERSIONPOLICY, versionPolicyConverter);
        converters.put(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, listConverter);
        converters.put(aQute.lib.osgi.Constants.CLASSPATH, listConverter);
        converters.put(Constants.EXPORT_PACKAGE, exportPackageConverter);
        converters.put(aQute.lib.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
        converters.put(Constants.IMPORT_PACKAGE, importPatternConverter);
        converters.put(BndConstants.RUNFRAMEWORK, stringConverter);
        converters.put(aQute.lib.osgi.Constants.SUB, listConverter);
        converters.put(aQute.lib.osgi.Constants.RUNPROPERTIES, propertiesConverter);
        converters.put(aQute.lib.osgi.Constants.RUNVM, stringConverter);
        converters.put(BndConstants.RUNVMARGS, stringConverter);
        converters.put(BndConstants.TESTSUITES, listConverter);
        converters.put(aQute.lib.osgi.Constants.TESTCASES, listConverter);
        converters.put(aQute.lib.osgi.Constants.PLUGIN, headerClauseListConverter);
        converters.put(BndConstants.RUNREQUIRE, requirementListConverter);
        converters.put(BndConstants.RUNEE, new NoopConverter<String>());
        converters.put(BndConstants.RUNREPOS, listConverter);
        converters.put(BndConstants.RESOLVE_MODE, resolveModeConverter);

        formatters.put(aQute.lib.osgi.Constants.BUILDPATH, headerClauseListFormatter);
        formatters.put(aQute.lib.osgi.Constants.BUILDPACKAGES, headerClauseListFormatter);
        formatters.put(aQute.lib.osgi.Constants.RUNBUNDLES, headerClauseListFormatter);
        formatters.put(Constants.BUNDLE_SYMBOLICNAME, newlineEscapeFormatter);
        formatters.put(Constants.BUNDLE_VERSION, newlineEscapeFormatter);
        formatters.put(Constants.BUNDLE_ACTIVATOR, newlineEscapeFormatter);
        formatters.put(BndConstants.OUTPUT, newlineEscapeFormatter);
        formatters.put(aQute.lib.osgi.Constants.SOURCES, defaultFalseBoolFormatter);
        formatters.put(aQute.lib.osgi.Constants.VERSIONPOLICY, defaultFormatter);
        formatters.put(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, stringListFormatter);
        formatters.put(aQute.lib.osgi.Constants.CLASSPATH, stringListFormatter);
        formatters.put(Constants.EXPORT_PACKAGE, headerClauseListFormatter);
        formatters.put(aQute.lib.osgi.Constants.SERVICE_COMPONENT, headerClauseListFormatter);
        formatters.put(Constants.IMPORT_PACKAGE, headerClauseListFormatter);
        formatters.put(BndConstants.RUNFRAMEWORK, newlineEscapeFormatter);
        formatters.put(aQute.lib.osgi.Constants.SUB, stringListFormatter);
        formatters.put(aQute.lib.osgi.Constants.RUNPROPERTIES, propertiesFormatter);
        formatters.put(aQute.lib.osgi.Constants.RUNVM, newlineEscapeFormatter);
        formatters.put(BndConstants.RUNVMARGS, newlineEscapeFormatter);
        formatters.put(BndConstants.TESTSUITES, stringListFormatter);
        formatters.put(aQute.lib.osgi.Constants.TESTCASES, stringListFormatter);
        formatters.put(aQute.lib.osgi.Constants.PLUGIN, headerClauseListFormatter);
        formatters.put(BndConstants.RUNREQUIRE, requirementListFormatter);
        formatters.put(BndConstants.RUNEE, new NoopConverter<String>());
        formatters.put(BndConstants.RUNREPOS, runReposFormatter);
        formatters.put(BndConstants.RESOLVE_MODE, resolveModeFormatter);
	}

	public void loadFrom(IDocument document) throws IOException {
		// Clear and load
		properties.clear();
		InputStream stream = new ByteArrayInputStream(document.get().getBytes(ISO_8859_1));
		properties.load(stream);
		objectProperties.clear();
		changesToSave.clear();

		// Fire property changes on all known property names
		for (String prop : KNOWN_PROPERTIES) {
			// null values for old and new forced the change to be fired
			propChangeSupport.firePropertyChange(prop, null, null);
		}
	}

	public void saveChangesTo(IDocument document) {
		for(Iterator<Entry<String,String>> iter = changesToSave.entrySet().iterator(); iter.hasNext(); ) {
			Entry<String, String> entry = iter.next();
			iter.remove();

			String propertyName = entry.getKey();
			String stringValue = entry.getValue();

			updateDocument(document, propertyName, stringValue);
		}
	}

	private static IRegion findEntry(IDocument document, String name) throws BadLocationException {
		int lineCount = document.getNumberOfLines();

		int entryStart = -1;
		int entryLength = 0;

		for(int i=0; i<lineCount; i++) {
			IRegion lineRegion = document.getLineInformation(i);
			String line = document.get(lineRegion.getOffset(), lineRegion.getLength());
			if(line.startsWith(name)) {
				entryStart = lineRegion.getOffset();
				entryLength = lineRegion.getLength();

				// Handle continuation lines, where the current line ends with a blackslash.
				while(document.getChar(lineRegion.getOffset() + lineRegion.getLength() - 1) == '\\') {
					if(++i >= lineCount) {
						break;
					}
					lineRegion = document.getLineInformation(i);
					entryLength += lineRegion.getLength() + 1; // Extra 1 is required for the newline
				}

				return new Region(entryStart, entryLength);
			}
		}

		return null;
	}

	private static void updateDocument(IDocument document, String name, String value) {
		String newEntry;
		if(value != null) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(name).append(": ").append(value);
			newEntry = buffer.toString();
		} else {
			newEntry = "";
		}

		try {
			IRegion region = findEntry(document, name);
			if(region != null) {
				// Replace an existing entry
				int offset = region.getOffset();
				int length = region.getLength();

				// If the replacement is empty, remove one extra character to the right, i.e. the following newline,
				// unless this would take us past the end of the document
				if(newEntry.length() == 0 && offset + length + 1 < document.getLength()) {
					length++;
				}
				document.replace(offset, length, newEntry);
			} else if(newEntry.length() > 0) {
				// This is a new entry, put it at the end of the file

				// Does the last line of the document have a newline? If not,
				// we need to add one.
				if(document.getLength() > 0 && document.getChar(document.getLength() - 1) != '\n')
					newEntry = "\n" + newEntry;
				document.replace(document.getLength(), 0, newEntry);
			}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<String> getAllPropertyNames() {
	    List<String> result = new ArrayList<String>(properties.size());

	    @SuppressWarnings("unchecked")
        Enumeration<String> names = (Enumeration<String>) properties.propertyNames();

	    while (names.hasMoreElements()) {
	        result.add(names.nextElement());
	    }
	    return result;
	}

    public Object genericGet(String propertyName) {
        Converter<? extends Object, String> converter = converters.get(propertyName);
        if (converter == null) converter = new NoopConverter<String>();
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
		return doGetObject(BndConstants.OUTPUT, stringConverter);
	}

	public void setOutputFile(String name) {
		doSetObject(BndConstants.OUTPUT, getOutputFile(), name, newlineEscapeFormatter);
	}

	public boolean isIncludeSources() {
		return doGetObject(aQute.lib.osgi.Constants.SOURCES, includedSourcesConverter);
	}

	public void setIncludeSources(boolean includeSources) {
		boolean oldValue = isIncludeSources();
		doSetObject(aQute.lib.osgi.Constants.SOURCES, oldValue, includeSources, defaultFalseBoolFormatter);
	}

	@Deprecated
	public VersionPolicy getVersionPolicy() throws IllegalArgumentException {
		return doGetObject(aQute.lib.osgi.Constants.VERSIONPOLICY, versionPolicyConverter);
	}

	@Deprecated
	public void setVersionPolicy(VersionPolicy versionPolicy) {
		VersionPolicy oldValue;
		try {
			oldValue = getVersionPolicy();
		} catch (IllegalArgumentException e) {
			oldValue = null;
		}
		doSetObject(aQute.lib.osgi.Constants.VERSIONPOLICY, oldValue, versionPolicy, defaultFormatter);
	}
    public List<String> getPrivatePackages() {
        return doGetObject(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, listConverter);
	}
    public void setPrivatePackages(List<? extends String> packages) {
		List<String> oldPackages = getPrivatePackages();
		doSetObject(aQute.lib.osgi.Constants.PRIVATE_PACKAGE, oldPackages, packages, stringListFormatter);
	}
    public List<ExportedPackage> getSystemPackages() {
        return doGetObject(aQute.lib.osgi.Constants.RUNSYSTEMPACKAGES, exportPackageConverter);
    }
    public void setSystemPackages(List<? extends ExportedPackage> packages) {
        List<ExportedPackage> oldPackages = getSystemPackages();
        doSetObject(aQute.lib.osgi.Constants.RUNSYSTEMPACKAGES, oldPackages, packages, headerClauseListFormatter);
    }
	public List<String> getClassPath() {
		return doGetObject(aQute.lib.osgi.Constants.CLASSPATH, listConverter);
	}
	public void addPrivatePackage(String packageName) {
		List<String> packages = getPrivatePackages();
		if(packages == null)
			packages = new ArrayList<String>();
		else
			packages = new ArrayList<String>(packages);
		packages.add(packageName);
		setPrivatePackages(packages);
	}
	public void setClassPath(List<? extends String> classPath) {
		List<String> oldClassPath = getClassPath();
		doSetObject(aQute.lib.osgi.Constants.CLASSPATH, oldClassPath, classPath, stringListFormatter);
	}
	public List<ExportedPackage> getExportedPackages() {
        return doGetObject(Constants.EXPORT_PACKAGE, exportPackageConverter);
	}
	public void setExportedPackages(List<? extends ExportedPackage> exports) {
		boolean referencesBundleVersion = false;

		if(exports != null) {
			for (ExportedPackage pkg : exports) {
				String versionString = pkg.getVersionString();
				if(versionString != null && versionString.indexOf(BUNDLE_VERSION_MACRO) > -1) {
					referencesBundleVersion = true;
				}
			}
		}
		List<ExportedPackage> oldValue = getExportedPackages();
		doSetObject(Constants.EXPORT_PACKAGE, oldValue, exports, headerClauseListFormatter);

		if(referencesBundleVersion && getBundleVersionString() == null) {
			setBundleVersion(new Version(0, 0, 0).toString());
		}
	}
	public void addExportedPackage(ExportedPackage export) {
		List<ExportedPackage> exports = getExportedPackages();
		exports = (exports == null) ? new ArrayList<ExportedPackage>() : new ArrayList<ExportedPackage>(exports);
		exports.add(export);
		setExportedPackages(exports);
	}
	public List<ServiceComponent> getServiceComponents() {
		return doGetObject(aQute.lib.osgi.Constants.SERVICE_COMPONENT, serviceComponentConverter);
	}
	public void setServiceComponents(List<? extends ServiceComponent> components) {
		List<ServiceComponent> oldValue = getServiceComponents();
		doSetObject(aQute.lib.osgi.Constants.SERVICE_COMPONENT, oldValue, components, headerClauseListFormatter);
	}
	public List<ImportPattern> getImportPatterns() {
		return doGetObject(Constants.IMPORT_PACKAGE, importPatternConverter);
	}
	public void setImportPatterns(List<? extends ImportPattern> patterns) {
		List<ImportPattern> oldValue = getImportPatterns();
		doSetObject(Constants.IMPORT_PACKAGE, oldValue, patterns, headerClauseListFormatter);
	}
	public List<VersionedClause> getBuildPath() {
		return doGetObject(aQute.lib.osgi.Constants.BUILDPATH, buildPathConverter);
	}
	public void setBuildPath(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		doSetObject(aQute.lib.osgi.Constants.BUILDPATH, oldValue, paths, headerClauseListFormatter);
	}

    public List<VersionedClause> getBuildPackages() {
        return doGetObject(aQute.lib.osgi.Constants.BUILDPACKAGES, buildPackagesConverter);
    }

    public void setBuildPackages(List<? extends VersionedClause> paths) {
        List<VersionedClause> oldValue = getBuildPackages();
        doSetObject(aQute.lib.osgi.Constants.BUILDPACKAGES, oldValue, paths, headerClauseListFormatter);
    }

	public List<VersionedClause> getRunBundles() {
		return doGetObject(aQute.lib.osgi.Constants.RUNBUNDLES, clauseListConverter);
	}
	public void setRunBundles(List<? extends VersionedClause> paths) {
		List<VersionedClause> oldValue = getBuildPath();
		doSetObject(aQute.lib.osgi.Constants.RUNBUNDLES, oldValue, paths, headerClauseListFormatter);
	}

    public List<VersionedClause> getBackupRunBundles() {
        return doGetObject(BndConstants.BACKUP_RUNBUNDLES, clauseListConverter);
    }
    public void setBackupRunBundles(List<? extends VersionedClause> paths) {
        List<VersionedClause> oldValue = getBuildPath();
        doSetObject(BndConstants.BACKUP_RUNBUNDLES, oldValue, paths, headerClauseListFormatter);
    }



	public String getRunFramework() {
	    return doGetObject(BndConstants.RUNFRAMEWORK, stringConverter);
	}
	public void setRunFramework(String clause) {
	    String oldValue = getRunFramework();
	    doSetObject(BndConstants.RUNFRAMEWORK, oldValue, clause, newlineEscapeFormatter);
	}
	public boolean isIncludedPackage(String packageName) {
		final Collection<String> privatePackages = getPrivatePackages();
		if(privatePackages != null) {
			if(privatePackages.contains(packageName))
				return true;
		}
		final Collection<ExportedPackage> exportedPackages = getExportedPackages();
		if(exportedPackages != null) {
			for (ExportedPackage pkg : exportedPackages) {
				if(packageName.equals(pkg.getName())) {
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

	public Map<String, String> getRunProperties() {
		return doGetObject(aQute.lib.osgi.Constants.RUNPROPERTIES, propertiesConverter);
	}

	/* (non-Javadoc)
     * @see bndtools.editor.model.IBndModel#setRunProperties(java.util.Map)
     */
	public void setRunProperties(Map<String, String> props) {
		Map<String, String> old = getRunProperties();
		doSetObject(aQute.lib.osgi.Constants.RUNPROPERTIES, old, props, propertiesFormatter);
	}

    /* (non-Javadoc)
     * @see bndtools.editor.model.IBndModel#getRunVMArgs()
     */
    public String getRunVMArgs() {
        return doGetObject(aQute.lib.osgi.Constants.RUNVM, stringConverter);
    }

    /* (non-Javadoc)
     * @see bndtools.editor.model.IBndModel#setRunVMArgs(java.lang.String)
     */
    public void setRunVMArgs(String args) {
        String old = getRunVMArgs();
        doSetObject(aQute.lib.osgi.Constants.RUNVM, old, args, newlineEscapeFormatter);
    }

    @SuppressWarnings("deprecation")
    public List<String> getTestSuites() {
        List<String> testCases = doGetObject(aQute.lib.osgi.Constants.TESTCASES, listConverter);
        testCases = testCases != null ? testCases : Collections.<String>emptyList();

        List<String> testSuites = doGetObject(BndConstants.TESTSUITES, listConverter);
        testSuites = testSuites != null ? testSuites : Collections.<String>emptyList();

        List<String> result = new ArrayList<String>(testCases.size() + testSuites.size());
        result.addAll(testCases);
        result.addAll(testSuites);
        return result;
    }

    @SuppressWarnings("deprecation")
    public void setTestSuites(List<String> suites) {
        List<String> old = getTestSuites();
        doSetObject(aQute.lib.osgi.Constants.TESTCASES, old, suites, stringListFormatter);
        doSetObject(BndConstants.TESTSUITES, null, null, stringListFormatter);
    }

    public List<HeaderClause> getPlugins() {
        return doGetObject(aQute.lib.osgi.Constants.PLUGIN, headerClauseListConverter);
    }

    public void setPlugins(List<HeaderClause> plugins) {
        List<HeaderClause> old = getPlugins();
        doSetObject(aQute.lib.osgi.Constants.PLUGIN, old, plugins, headerClauseListFormatter);
    }

    public List<Requirement> getRunRequire() {
        return doGetObject(BndConstants.RUNREQUIRE, requirementListConverter);
    }

    public void setRunRequire(List<Requirement> requires) {
        List<Requirement> old = getRunRequire();
        doSetObject(BndConstants.RUNREQUIRE, old, requires, requirementListFormatter);
    }

    public ResolveMode getResolveMode() {
        return doGetObject(BndConstants.RESOLVE_MODE, resolveModeConverter);
    }

    public void setResolveMode(ResolveMode mode) {
        ResolveMode old = getResolveMode();
        doSetObject(BndConstants.RESOLVE_MODE, old, mode, resolveModeFormatter);
    }

    public EE getEE() {
        return doGetObject(BndConstants.RUNEE, eeConverter);
    }

    public void setEE(EE ee) {
        EE old = getEE();
        doSetObject(BndConstants.RUNEE, old, ee, eeFormatter);
    }

    public List<String> getRunRepos() {
        return doGetObject(BndConstants.RUNREPOS, listConverter);
    }

    public void setRunRepos(List<String> repos) {
        List<String> old = getRunRepos();
        doSetObject(BndConstants.RUNREPOS, old, repos, runReposFormatter);
    }

    <R> R doGetObject(String name, Converter<? extends R, ? super String> converter) {
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
            result = null;
        }
        return result;
    }

    <T> void doSetObject(String name, T oldValue, T newValue, Converter<String, ? super T> formatter) {
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

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

    public void setBndResource(IResource bndResource) {
        this.bndResource = bndResource;
    }

    public IResource getBndResource() {
        return bndResource;
    }
}