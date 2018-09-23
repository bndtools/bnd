package test.configurator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class TestConfigurator {
	@org.junit.Test
	public void testInclResReqOther() throws Exception {

		try (final Builder builder = new Builder()) {

			builder.setProperty("-includeresource",
				"OSGI-INF/other/configuration.json=testresources/configurator/configurator/configurator.json");
			builder.setProperty("-configurator", "from;active=false,target;dir=OSGI-INF/other");
			final Jar jar = builder.build();

			assertAll(jar, Arrays.asList("OSGI-INF/other/configuration.json"),
				Arrays.asList("configurations:List<String>=OSGI-INF/other",
					"(osgi.extender=osgi.configurator)(version>=1.0.0"),
				null);

		}
	}

	private void assertAll(Jar jar, List<String> existingResources, List<String> reqCapContains,
		List<String> reqCapNotContains) throws Exception {

		assertNotNull(jar);
		if (existingResources != null) {

			for (String res : existingResources) {

				assertNotNull(jar.getResource(res));
			}
		}

		Manifest manifest = jar.getManifest();
		assertNotNull(manifest);

		Attributes mainAttributes = manifest.getMainAttributes();
		assertNotNull(mainAttributes);

		if (reqCapContains != null && !reqCapContains.isEmpty()) {
			String valReqCap = mainAttributes.getValue(Constants.REQUIRE_CAPABILITY);
			assertNotNull(valReqCap);
			if (reqCapContains != null) {

				for (String cont : reqCapContains) {

					assertTrue(valReqCap.contains(cont));
				}
			}

			if (reqCapNotContains != null) {

				for (String notCont : reqCapNotContains) {

					assertFalse(valReqCap.contains(notCont));
				}
			}
		}
	}

	@org.junit.Test
	public void testInclResReqConfNotMatched() throws Exception {

		try (final Builder builder = new Builder()) {

			builder.setProperty("-includeresource",
				"OSGI-INF/other/configuration.json=testresources/configurator/configurator/configurator.json");
			builder.setProperty("-configurator", "from;active=false");

			final Jar jar = builder.build();

			assertAll(jar, Arrays.asList("OSGI-INF/other/configuration.json"), null,
				Arrays.asList("(osgi.extender=osgi.configurator)"));
		}
	}

	@org.junit.Test
	public void testNoInstruction() throws Exception {

		try (final Builder builder = new Builder()) {

			final Jar jar = builder.build();

			assertAll(jar, null, null,
				Arrays.asList("configurations:List<String>=", "(osgi.extender=osgi.configurator)"));

		}
	}

	@org.junit.Test
	public void testDefault() throws Exception {

		try (final Builder builder = new Builder()) {
			builder.setBase(new File("testresources/configurator"));
			builder.setProperty("-configurator", "");
			final Jar jar = builder.build();

			assertAll(jar, Arrays.asList("OSGI-INF/configurator/configurator.json"),
				Arrays.asList("(osgi.extender=osgi.configurator)(version>=1.0.0"), null);
		}
	}

	@org.junit.Test
	public void testAllAttrs() throws Exception {

		try (final Builder builder = new Builder()) {

			builder.setProperty("-configurator",
				"from;base=testresources/configurator;includes=other/*.json;excludes=**/*2.json;active=true,target;dir=OSGI-INF/other,requirement;active=true;version=2");
			final Jar jar = builder.build();

			assertEquals(null, jar.getResource("OSGI-INF/other/cfg2.json"));

			assertAll(jar, Arrays.asList("OSGI-INF/other/cfg1.json"),
				Arrays.asList("configurations:List<String>=OSGI-INF/other",
					"(osgi.extender=osgi.configurator)(version>=2.0.0"),
				null);
		}
	}

}
