package biz.aQute.bnd.reporter.plugins.entries.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;

/**
 * This plugin extracts the file name of the base of a Processor.
 */
@BndPlugin(name = "entry." + EntryNamesReference.FILE_NAME)
public class FileNamePlugin implements ReportEntryPlugin<Processor>, Plugin {

	private final Map<String, String> _properties = new HashMap<>();

	public FileNamePlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.FILE_NAME);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Processor.class.getCanonicalName());
	}

	@Override
	public Object extract(final Processor processor, final Locale locale) throws Exception {
		Objects.requireNonNull(processor, "processor");

		return processor.getBase() != null ? processor.getBase()
			.getName() : null;
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
