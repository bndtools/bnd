package aQute.bnd.remote.junit;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BeforeAfterTest {

	JUnitFrameworkBuilder builder;
	JUnitFramework			framework;

	@Before
	public void before() {
		builder = new JUnitFrameworkBuilder();
		framework = builder.runfw("org.apache.felix.framework")
			.create();
	}

	@After
	public void after() throws Exception {
		builder.close();
	}

	@Test
	public void test() {
		assertNotNull(framework.getBundleContext());
	}
}
