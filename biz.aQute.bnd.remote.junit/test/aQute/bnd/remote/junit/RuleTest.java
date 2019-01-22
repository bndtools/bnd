package aQute.bnd.remote.junit;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RuleTest {

	JUnitFrameworkBuilder builder = new JUnitFrameworkBuilder();

	@Rule
	public TemporaryFolder	folder	= new TemporaryFolder();

	@Before
	public void before() throws IOException {
		System.setProperty("storage", folder.newFolder()
			.getAbsolutePath());
	}

	@Test
	public void test() throws Exception {


		try (JUnitFramework framework = builder.runfw("org.apache.felix.framework")
			.create()) {
			assertTrue(new File(System.getProperty("storage")).isDirectory());
		}
	}
}
