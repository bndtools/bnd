package bnd.test;

import static org.junit.Assert.assertNull;
import static org.osgi.framework.FrameworkUtil.getBundle;

import org.junit.Test;

public class FailingBndTest {

	@Test
	public void test() {
		assertNull(getBundle(FailingBndTest.class).getBundleContext());
	}
	
}
