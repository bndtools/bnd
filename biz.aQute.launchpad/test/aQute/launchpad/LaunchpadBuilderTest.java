package aQute.launchpad;

import static aQute.launchpad.LaunchpadBuilder.projectTestSetup;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import aQute.bnd.service.specifications.RunSpecification;

public class LaunchpadBuilderTest {
	LaunchpadBuilder			builder;

	@Rule
	public JUnitSoftAssertions	softly	= new JUnitSoftAssertions();

	@Before
	public void before() throws Exception {
		builder = new LaunchpadBuilder();
	}

	@After
	public void after() throws Exception {
		builder.close();
	}

	@Test
	public void newInstance_deepClonesProjectSetup() {
		final RunSpecification local = builder.local;
		softly.assertThat(local)
			.as("local")
			.isNotSameAs(projectTestSetup);

		softly.assertThat(local.runbundles)
			.as("runbundles")
			.isNotSameAs(projectTestSetup.runbundles);
		softly.assertThat(local.runpath)
			.as("runpath")
			.isNotSameAs(projectTestSetup.runpath)
			.isEmpty();
		softly.assertThat(local.extraSystemCapabilities)
			.as("extraSystemCapabilities")
			.isNotSameAs(projectTestSetup.extraSystemCapabilities);
		softly.assertThat(local.extraSystemPackages)
			.as("extraSystemPackages")
			.isNotSameAs(projectTestSetup.extraSystemPackages);
		softly.assertThat(local.properties)
			.as("properties")
			.isNotSameAs(projectTestSetup.properties);
		softly.assertThat(local.errors)
			.as("errors")
			.isNotSameAs(projectTestSetup.errors);
		softly.assertThat(local.runfw)
			.as("runfw")
			.isNotSameAs(projectTestSetup.runfw);
	}

	@Test
	public void newInstance_copiesProjectTestSetup() {
		softly.assertThat(builder.local)
			.isEqualToComparingFieldByField(projectTestSetup);
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
			softly.assertThat(builder.local.properties.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA))
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
			softly.assertThat(builder.local.properties.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA))
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
}