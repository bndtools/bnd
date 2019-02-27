package aQute.launchpad;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;

import aQute.bnd.osgi.Verifier;
import aQute.lib.io.IO;

public class BundleBuilderTest {
	LaunchpadBuilder	builder;
	Launchpad			lp;
	BundleBuilder 		bb;
	
	@Rule
	public JUnitSoftAssertions	softly	= new JUnitSoftAssertions();


	@Before
	public void before() throws Exception {
		builder = new LaunchpadBuilder();
		try {
			lp = builder.runfw("org.apache.felix.framework")
				.create();
		} catch (Exception e) {
			builder.close();
		}
		bb = lp.bundle();
	}

	@After
	public void after() throws Exception {
		IO.close(bb);
		IO.close(lp);
		IO.close(builder);
	}

	@Test
	public void bundleBuilder_shouldAllowManualOverrideOfBSN() throws Exception {
		softly.assertThatCode(() -> bb.bundleSymbolicName("my.test.name"))
			.as("set")
			.doesNotThrowAnyException();
		softly.assertThat(bb.install()
			.getSymbolicName())
			.as("bsn")
			.isEqualTo("my.test.name");
	}

	@Test
	public void bundleBuilder_shouldCreateUniqueLegalBSN() throws Exception {
		final Bundle b1 = bb.install();
		softly.assertThat(b1.getSymbolicName())
			.as("bsn 1")
			.isNotBlank()
			.matches(Verifier.SYMBOLICNAME);
		// Check that we don't get the same name.
		final Bundle b2 = lp.bundle()
			.install();
		softly.assertThat(b2.getSymbolicName())
			.as("bsn 2")
			.isNotBlank()
			.matches(Verifier.SYMBOLICNAME)
			.isNotEqualTo(b1.getSymbolicName());
	}

	@Test
	public void bundleBuilder_shouldNotAllowManualOverride_withInvalidBSN() throws Exception {
		for (String invalid : new String[] {
				null, "my&test.name", "   ", ""
			}) {
				softly.assertThatThrownBy(() -> bb.bundleSymbolicName(invalid))
					.as("bsn: '" + invalid + '\'')
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(invalid == null ? "null" : invalid);
		}
	}
}
