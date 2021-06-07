package biz.aQute.bnd.reporter.plugins.entries.bundle;

import static biz.aQute.bnd.reporter.matcher.IsDTODeepEquals.deepEqualsTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.lib.json.JSONCodec;
import junit.framework.TestCase;

public class GogoPluginTest extends TestCase {

	public void testGogo() throws Exception {

		try (final Jar jar = new Jar("jar", "testresources/gogoEntry/source.jar");
			final Processor p = new Processor()) {

			final GogoPlugin e = new GogoPlugin();
			final Map<String, Object> result = new HashMap<>();
			e.setReporter(p);

			result.put(e.getProperties()
				.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY), e.extract(jar, Locale.forLanguageTag("und")));

			assertTrue(p.isOk());

			assertThat(result, is(deepEqualsTo(new JSONCodec().dec()
				.from(Paths.get("testresources/gogoEntry/result.json")
					.toFile())
				.get())));
		}
	}
}
