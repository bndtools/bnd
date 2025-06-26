package aQute.tester.junit.platform.test;

import org.junit.jupiter.api.Disabled;

@Disabled("JUnit does not yet work on latest versions of JUnit")
public class ActivatorJUnitPlatformLatestTest extends AbstractActivatorJUnitPlatformTest {
	public ActivatorJUnitPlatformLatestTest() {
		super("biz.aQute.tester.junit-platform.latest");
	}
}
