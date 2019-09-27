package biz.aQute.bnd.reporter.generator;

import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Maps;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;

@BndPlugin(name = "entry.entryTest")
public class TestEntryPlugin implements ReportEntryPlugin<String>, Plugin {

	private final Map<String, String> prop = Maps.newHashMap();

	/**
	 * DOCME
	 */
	public TestEntryPlugin() {
		prop.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, "entryTest");
		prop.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, String.class.getCanonicalName());
	}

	@Override
	public Object extract(final String source, final Locale locale) throws Exception {
		return "test";
	}

	@Override
	public Map<String, String> getProperties() {
		return prop;
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		prop.putAll(map);
	}

	@Override
	public void setReporter(final Reporter processor) {
		// nothing
	}
}
