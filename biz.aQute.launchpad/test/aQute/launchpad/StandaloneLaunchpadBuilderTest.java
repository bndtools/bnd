package aQute.launchpad;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import aQute.lib.io.IO;

public class StandaloneLaunchpadBuilderTest {
	StandaloneLaunchpadBuilder	builder;

	@Rule
	public JUnitSoftAssertions	softly	= new JUnitSoftAssertions();

	@Before
	public void before() throws Exception {
		builder = new StandaloneLaunchpadBuilder(IO.getFile("standalone-1.bndrun"));
	}

	@After
	public void after() throws Exception {
		builder.close();
	}

	@Test
	public void set_setsLocalProperties() {
		builder.set("somekey", "somevalue");
		softly.assertThat(builder.local.properties)
			.containsEntry("somekey", "somevalue");
	}

	@Test
	public void excludePackages_filtersSystemPackages() throws Exception {
		addPackage("m.n.o.p", "3.2.1");
		addPackage("my.pkg1", "1.2");
		addPackage("my.pkg.pkg2", "1.3");
		addPackage("our.other", "2.3");
		addPackage("our.other.pkg2", "2.3");
		builder.excludeExport("my.*,our.other,*aQute*,*slf*,*bytebuddy*")
			.runfw("org.apache.felix.framework");
		try (Launchpad lp = builder.create()) {
			System.out.println(lp.getBundleContext()
				.getProperty("org.osgi.framework.system.packages.extra"));
			softly.assertThat(lp.framework.getBundleContext()
				.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA))
				.doesNotContain("my.pkg1;")
				.doesNotContain("my.pkg.pkg2;")
				.doesNotContain("our.other;")
				.contains("our.other.pkg2;")
				.contains("m.n.o.p;");
		}
	}

	@Test
	public void excludePackages_withPredicates_filtersSystemPackages() throws Exception {
		addPackage("m.n.o.p", "3.2.1");
		addPackage("my.pkg1", "1.2");
		addPackage("my.pkg.pkg2", "1.3");
		addPackage("our.other", "2.3");
		addPackage("our.other.pkg2", "2.3");
		builder.excludeExport(x -> x.startsWith("my"))
			.excludeExport(y -> y.equals("our.other"))
			.runfw("org.apache.felix.framework");
		try (Launchpad lp = builder.create()) {
			softly.assertThat(lp.framework.getBundleContext()
				.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA))
				.doesNotContain("my.pkg1;")
				.doesNotContain("my.pkg.pkg2;")
				.doesNotContain("our.other;")
				.contains("our.other.pkg2;")
				.contains("m.n.o.p;");
		}
	}

	protected void addPackage(String bsn, String version) {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("version", version);
		builder.local.extraSystemPackages.put(bsn, attrs);
	}

	// This class is necessary to promote visibility of loadClass() from
	// protected to public to allow Mockito to do verification on it.
	static public class VisibleClassLoader extends ClassLoader {
		@Override
		public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			return super.loadClass(name, resolve);
		}
	}

	@Test
	public void usingClassLoader_usesSpecifiedClassLoader_toBootstrapFramework() throws Exception {
		VisibleClassLoader loader = spy(VisibleClassLoader.class);
		builder.runfw("org.apache.felix.framework")
			.usingClassLoader(loader);

		try (Launchpad lp = builder.create()) {
			verify(loader).loadClass(eq("org.apache.felix.framework.FrameworkFactory"), anyBoolean());
		}
	}
}
