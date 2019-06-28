package aQute.launchpad;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RuleTest {

	LaunchpadBuilder		builder	= new LaunchpadBuilder();

	@Rule
	public TemporaryFolder	folder	= new TemporaryFolder();

	@Before
	public void before() throws IOException {
		System.setProperty("storage", folder.newFolder()
			.getAbsolutePath());
	}

	@Test
	public void test() throws Exception {

		try (Launchpad framework = builder.runfw("org.apache.felix.framework")
			.create()) {
			assertTrue(new File(System.getProperty("storage")).isDirectory());
		}
	}
}
