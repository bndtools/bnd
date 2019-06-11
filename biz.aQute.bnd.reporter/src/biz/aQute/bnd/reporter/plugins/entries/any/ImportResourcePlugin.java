package biz.aQute.bnd.reporter.plugins.entries.any;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.helpers.ArrayHelper;
import biz.aQute.bnd.reporter.helpers.FileHelper;
import biz.aQute.bnd.reporter.service.resource.converter.ResourceConverterPlugin;

/**
 * This plugin allows to add a local or remote file to the report. The file will
 * be converted into DTO. The {@link ImportResourcePlugin#URL_PROPERTY} property
 * must be set and the {@link ImportResourcePlugin#TYPE_PROPERTY} can be set if
 * the file type is not obvious (derived from its extension).
 */
@BndPlugin(name = "entry." + EntryNamesReference.IMPORT_FILE)
public class ImportResourcePlugin implements ReportEntryPlugin<Object>, Plugin, RegistryPlugin {

	final public static String			URL_PROPERTY	= "url";
	final public static String			TYPE_PROPERTY	= "type";

	private Reporter					_reporter;
	private Registry					_registry;
	private final Map<String, String>	_properties		= new HashMap<>();

	public ImportResourcePlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.IMPORT_FILE);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Object.class.getCanonicalName());
	}

	@Override
	public Object extract(final Object source, final Locale locale) throws Exception {
		try (InputStream is = resolveFile()) {
			try {
				return convertFile(is, getType());
			} catch (final Exception e) {
				_reporter.exception(e, "Failed to convert the imported file at %s", getUrl());
			}
		} catch (final Exception e) {
			_reporter.exception(e, "Failed to import the file at %s", getUrl());
		}
		return null;
	}

	private InputStream resolveFile() throws Exception {
		if (getUrl() != null) {
			final File file = IO.getFile(getUrl());
			if (file.isFile()) {
				try {
					return new FileInputStream(file);
				} catch (final IOException e) {
					_reporter.exception(e, "Failed to import the file at %s", getUrl());
				}
			} else {
				try {
					return Resource.fromURL(new URL(getUrl()))
						.openInputStream();
				} catch (final IOException e) {
					_reporter.exception(e, "Failed to import the file at %s", getUrl());
				}
			}
		}
		return new ByteArrayInputStream(new byte[0]);
	}

	private Object convertFile(final InputStream inputStream, final String extension) throws Exception {
		for (final ResourceConverterPlugin deserializer : _registry.getPlugins(ResourceConverterPlugin.class)) {
			if (ArrayHelper.containsIgnoreCase(deserializer.getHandledExtensions(), extension)) {
				return deserializer.extract(inputStream);
			}
		}
		_reporter.error("Unable to convert the imported file %s of type %s, available types are: %s", getUrl(),
			getType(), getAvailableConverterExtensions());
		return null;
	}

	private String[] getAvailableConverterExtensions() {
		return _registry.getPlugins(ResourceConverterPlugin.class)
			.stream()
			.flatMap(r -> Arrays.stream(r.getHandledExtensions()))
			.collect(Collectors.toList())
			.toArray(new String[0]);
	}

	private String getUrl() {
		return _properties.getOrDefault(URL_PROPERTY, null);
	}

	private String getType() {
		return _properties.getOrDefault(TYPE_PROPERTY, null);
	}

	@Override
	public void setRegistry(final Registry registry) {
		_registry = registry;
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);

		if (getUrl() != null) {
			final File f = IO.getFile(getUrl());
			if (!map.containsKey(ENTRY_NAME_PROPERTY)) {
				_properties.put(ENTRY_NAME_PROPERTY, FileHelper.getName(f));
			}
			if (getType() == null) {
				_properties.put(TYPE_PROPERTY, FileHelper.getExtension(f));
			}
		}
	}

	@Override
	public void setReporter(final Reporter processor) {
		_reporter = processor;
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}
}
