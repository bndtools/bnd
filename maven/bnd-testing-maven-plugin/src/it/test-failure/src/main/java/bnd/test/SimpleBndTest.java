package bnd.test;

import static org.junit.Assert.assertNotNull;
import static org.osgi.framework.FrameworkUtil.getBundle;

import org.junit.Test;

public class SimpleBndTest {

	@Test
	public void test() {
		assertNotNull(getBundle(SimpleBndTest.class).getBundleContext());
	}
	
}
