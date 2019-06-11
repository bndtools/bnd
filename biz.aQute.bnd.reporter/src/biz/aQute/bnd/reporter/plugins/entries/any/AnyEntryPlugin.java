package biz.aQute.bnd.reporter.plugins.entries.any;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;

/**
 * This plugin allows the user to define a report entry in a bnd property file.
 * The user must provide the {@link AnyEntryPlugin#KEY_PROPERTY} and the
 * {@link AnyEntryPlugin#VALUE_PROPERTY} properties to be effective. The
 * {@link AnyEntryPlugin#KEY_PROPERTY} will then be used as the entry name in
 * the report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.ANY_ENTRY)
public class AnyEntryPlugin implements ReportEntryPlugin<Object>, Plugin {

	final public static String			KEY_PROPERTY	= "key";
	final public static String			VALUE_PROPERTY	= "value";

	private final Map<String, String>	_properties		= new HashMap<>();

	public AnyEntryPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.ANY_ENTRY);
		_properties.put(KEY_PROPERTY, EntryNamesReference.ANY_ENTRY);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Object.class.getCanonicalName());
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
		if (_properties.containsKey(KEY_PROPERTY)) {
			_properties.put(ENTRY_NAME_PROPERTY, _properties.get(KEY_PROPERTY));
		}
	}

	@Override
	public void setReporter(final Reporter processor) {
		// not used
	}

	@Override
	public Object extract(final Object source, final Locale locale) throws Exception {
		return _properties.get(VALUE_PROPERTY);
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}
}
