package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import aQute.launchpad.junit.LaunchpadRule;

public class JunitRuleTest {

	static LaunchpadBuilder	builder	= new LaunchpadBuilder().runfw("org.apache.felix.framework");

	@Rule
	public LaunchpadRule	lp		= new LaunchpadRule(builder);

	@Test
	public void testCheckName() throws Exception {
		deeperInCallstack();
	}

	private void deeperInCallstack() throws Exception {
		try (Launchpad fw = lp.getLaunchpad()) {
			assertThat(fw.getName()).isEqualTo("testCheckName");
			assertThat(fw.getClassName()).isEqualTo(JunitRuleTest.class.getName());
		}
	}

	@Test
	public void whenGetLaunchpadIsNotCalled_thenDontTryToCloseIt() {}
}
