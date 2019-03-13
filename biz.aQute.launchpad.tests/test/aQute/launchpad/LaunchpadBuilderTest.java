package aQute.launchpad;

import static aQute.launchpad.LaunchpadBuilder.projectTestSetup;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import aQute.bnd.service.specifications.RunSpecification;

public class LaunchpadBuilderTest {
	LaunchpadBuilder	builder;

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
}
