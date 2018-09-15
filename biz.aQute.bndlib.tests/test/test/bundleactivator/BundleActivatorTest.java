package test.bundleactivator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import aQute.bnd.osgi.Builder;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class BundleActivatorTest extends TestCase {

	public static class DirectImplement implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void stop(BundleContext context) throws Exception {
			// TODO Auto-generated method stub

		}

	}

	public void testBundleActivatorVerify() throws Exception {
		assertBundleActivator(DirectImplement.class, null);
	}

	public static class InheritBundleActivator extends DirectImplement {

	}

	public void testBundleActivatorInheritVerify() throws Exception {
		assertBundleActivator(InheritBundleActivator.class, null);
	}

	public interface IndirectBundleActivator extends BundleActivator {

	}

	public static class IndirectImplementViaInterface implements IndirectBundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void stop(BundleContext context) throws Exception {
			// TODO Auto-generated method stub

		}

	}

	public void testBundleActivatorInheritViaInterfaceVerify() throws Exception {
		assertBundleActivator(IndirectImplementViaInterface.class, null);
	}

	void assertBundleActivator(Class<? extends BundleActivator> c, String filter) throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.bundleactivator");
			b.setBundleActivator(c.getName());
			b.build();
			if (filter != null)
				assertTrue(b.check(filter));
			else
				assertTrue(b.check());
			b.getJar()
				.getManifest()
				.write(System.out);
		}
	}

}
