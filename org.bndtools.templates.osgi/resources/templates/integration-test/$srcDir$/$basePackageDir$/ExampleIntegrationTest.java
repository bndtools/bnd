package $basePackageName$;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@RunWith(MockitoJUnitRunner.class)
public class ExampleIntegrationTest {

	private final BundleContext context = FrameworkUtil.getBundle(ExampleIntegrationTest.class).getBundleContext();
	
	@Before
	public void before() {
		// TODO add test setup here
	}
	
	@After
	public void after() {
		// TODO add test clear up here
	}
	
	@Test
	public void testExample() {
		// TODO implement a test here
	}
	
}