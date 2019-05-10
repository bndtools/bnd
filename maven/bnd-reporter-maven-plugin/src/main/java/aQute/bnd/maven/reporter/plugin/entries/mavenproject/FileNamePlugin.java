package aQute.bnd.maven.reporter.plugin.entries.mavenproject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.maven.reporter.plugin.MavenProjectWrapper;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;

/**
 * This plugin extracts the file name of the base of a maven project.
 */
@BndPlugin(name = "entry." + EntryNamesReference.FILE_NAME)
public class FileNamePlugin implements ReportEntryPlugin<MavenProjectWrapper>, Plugin {

	private final Map<String, String> _properties = new HashMap<>();

	public FileNamePlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.FILE_NAME);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, MavenProjectWrapper.class.getCanonicalName());
	}

	@Override
	public Object extract(final MavenProjectWrapper obj, final Locale locale) throws Exception {
		Objects.requireNonNull(obj, "obj");

		return obj.getProject()
			.getBasedir()
			.getName();
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public void setReporter(final Reporter processor) {
		// not used
	}
}
