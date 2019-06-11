package biz.aQute.bnd.reporter.plugins.entries.any;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import aQute.bnd.service.reporter.ReportEntryPlugin;

public class AnyEntryPluginTest {

	@Test
	public void testAnyEntryPlugin() throws Exception {
		final AnyEntryPlugin plugin = new AnyEntryPlugin();

		final Map<String, String> prop = new HashMap<>();
		prop.put(AnyEntryPlugin.KEY_PROPERTY, "testKey");
		prop.put(AnyEntryPlugin.VALUE_PROPERTY, "testValue");
		plugin.setProperties(prop);

		assertEquals("testKey", plugin.getProperties()
			.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY));
		assertEquals("testValue", plugin.extract(new Object(), Locale.forLanguageTag("und")));
	}
}
