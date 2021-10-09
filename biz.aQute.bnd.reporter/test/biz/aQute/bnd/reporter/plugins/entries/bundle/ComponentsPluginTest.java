package biz.aQute.bnd.reporter.plugins.entries.bundle;

import static biz.aQute.bnd.reporter.matcher.IsDTODeepEquals.deepEqualsTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.lib.json.JSONCodec;

public class ComponentsPluginTest {

	@Test
	public void testComponents() throws Exception {

		try (final Jar jar = new Jar("jar", "testresources/componentsEntry/source.jar");
			final Processor p = new Processor();) {

			final ComponentsPlugin e = new ComponentsPlugin();
			final Map<String, Object> result = new HashMap<>();
			e.setReporter(p);

			result.put(e.getProperties()
				.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY), e.extract(jar, Locale.forLanguageTag("und")));

			assertTrue(p.isOk());
			assertThat(result, is(deepEqualsTo(new JSONCodec().dec()
				.from(Paths.get("testresources/componentsEntry/result.json")
					.toFile())
				.get())));
		}
	}
}
