package aQute.xlaunchpad;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public class BeforeAfterTest {

	static LaunchpadBuilder	builder	= new LaunchpadBuilder().runfw("org.apache.felix.framework")
		.bundles("org.assertj.core, org.apache.felix.scr")
		.debug();

	@Service
	Launchpad				framework;

	@Before
	public void before() {
		System.out.println("Before");
	}

	@After
	public void after() throws Exception {
		System.out.println("After");
	}

	@Test
	public void test() {
		assertNotNull(framework.getBundleContext());
	}

	@Test(expected = Exception.class)
	public void testExceptions() throws Exception {
		throw new Exception();
	}
}
