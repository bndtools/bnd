package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Dictionary;
import java.util.function.Supplier;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;

public class BundleBuilderTest {

	static {
		// try {
		// Workspace w = Workspace.findWorkspace(IO.work);
		// } catch (Exception e) {
		// Exceptions.duck(e);
		// }
	}

	LaunchpadBuilder builder;

	@Before
	public void before() throws Exception {
		builder = new LaunchpadBuilder();
		builder.runfw("org.apache.felix.framework");
		assertThat(builder.getLocal().runfw).as("runfw")
			.isNotEmpty();
	}

	@After
	public void after() throws Exception {
		builder.close();
	}

	@Test
	public void testInherit() throws Exception {
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.header("-removeheaders.launchpadtest", "")
				.header("-require-bnd", "'(version>=4.3.0)'")
				.privatePackage("")
				.exportPackage("")
				.start();

			testHeader(bundle, "Workspace", null);
			testHeader(bundle, "Project", null);
			testHeader(bundle, "Inherit", null);
			testHeader(bundle, "LaunchPadInheritTest", null);
		}
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.header("-removeheaders.launchpadtest", "")
				.workspace()
				.start();
			testHeader(bundle, "Workspace", "true");
			testHeader(bundle, "Project", null);
			testHeader(bundle, "Inherit", null);
			testHeader(bundle, "LaunchPadInheritTest", "Workspace");
		}
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.header("-removeheaders.launchpadtest", "")
				.project()
				.privatePackage("") // remove any packages defined in the
				.exportPackage("") // project file
				.start();
			testHeader(bundle, "Workspace", "true");
			testHeader(bundle, "Project", "true");
			testHeader(bundle, "Inherit", null);
			testHeader(bundle, "LaunchPadInheritTest", "Project");
		}
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.header("-removeheaders.launchpadtest", "")
				.parent("testresources/inherit.bnd")
				.privatePackage("")
				.exportPackage("")
				.start();
			testHeader(bundle, "Workspace", null);
			testHeader(bundle, "Project", null);
			testHeader(bundle, "Inherit", "true");
			testHeader(bundle, "LaunchPadInheritTest", "Inherit");
		}
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.header("-removeheaders.launchpadtest", "")
				.parent("testresources/inherit.bnd")
				.project()
				.privatePackage("")
				.exportPackage("")
				.start();
			testHeader(bundle, "Workspace", "true");
			testHeader(bundle, "Project", "true");
			testHeader(bundle, "Inherit", "true");
			testHeader(bundle, "LaunchPadInheritTest", "Inherit");
		}
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.header("-removeheaders.launchpadtest", "")
				.parent("testresources/inherit.bnd")
				.workspace()
				.privatePackage("")
				.exportPackage("")
				.start();
			testHeader(bundle, "Workspace", "true");
			testHeader(bundle, "Project", null);
			testHeader(bundle, "Inherit", "true");
			testHeader(bundle, "LaunchPadInheritTest", "Inherit");
		}
	}

	private void testHeader(Bundle bundle, String key, String value) {
		Dictionary<String, String> headers = bundle.getHeaders();

		assertThat(headers.get(key)).isEqualToIgnoringWhitespace(value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWorkspaceNotAtEnd() throws Exception {
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.workspace()
				.workspace()
				.privatePackage("")
				.exportPackage("")
				.start();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testProjectNotAtEnd() throws Exception {
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.project()
				.project()
				.privatePackage("")
				.exportPackage("")
				.start();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParentAfterWorkspaceOrProject() throws Exception {
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.project()
				.parent("bnd.bnd")
				.privatePackage("")
				.exportPackage("")
				.start();
		}
	}

	@Test
	public void testWorkspaceOrProjectAfterParent() throws Exception {
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.parent("bnd.bnd")
				.project()
				.privatePackage("")
				.exportPackage("")
				.start();
		}
	}

	@Test
	public void testOrder() throws Exception {
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
				.parent("testresources/inherit.bnd")
				.project()
				.privatePackage("")
				.exportPackage("")
				.start();

		}
	}

	@Test
	public void differentBundles_withSameSymbolicName_shouldNotBeTheSameBundle() throws Exception {
		try (Launchpad lp = builder.create()) {
			Bundle bundle = lp.bundle()
				.bundleSymbolicName("test.bundle")
				.bundleVersion("1.2.3")
				.start();
			Bundle bundle2 = lp.bundle()
				.bundleSymbolicName("test.bundle")
				.bundleVersion("2.3.4")
				.start();
			assertThat(bundle).as("identity")
				.isNotSameAs(bundle2);
			assertThat(bundle.getBundleId()).as("id")
				.isNotEqualTo(bundle2.getBundleId());
		}
	}

	@Test
	public void location_defaultsToBSNPlusVersion() throws Exception {

		try (Launchpad lp = builder.create(); AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
			Bundle bundle = lp.bundle()
				.install();
			softly.assertThat(bundle.getLocation())
				.as("full default")
				.contains(bundle.getSymbolicName())
				.endsWith("-0");

			bundle = lp.bundle()
				.bundleSymbolicName("test.bundle")
				.install();
			softly.assertThat(bundle.getLocation())
				.as("default version")
				.contains("test.bundle")
				.endsWith("-0");

			bundle = lp.bundle()
				.bundleSymbolicName("test.bundle")
				.bundleVersion("1.2.3")
				.install();
			softly.assertThat(bundle.getLocation())
				.as("explicit version")
				.contains("test.bundle")
				.endsWith("-1.2.3");

			bundle = lp.bundle()
				.bundleSymbolicName("test.bundle")
				.bundleVersion("2.3.4")
				.location("some string")
				.install();
			softly.assertThat(bundle.getLocation())
				.as("explicit location")
				.isEqualTo("some string");
		}
	}

	@Test
	public void bsn_defaultsToLaunchpadName() throws Exception {
		try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
			try (Launchpad lp = builder.debug()
				.create()) {
				Bundle bundle1 = lp.bundle()
					.install();
				softly.assertThat(bundle1.getSymbolicName())
					.as("full default")
					.startsWith(lp.getClassName() + "." + lp.getName());

				Bundle bundle2 = lp.bundle()
					.install();

				softly.assertThat(bundle2.getSymbolicName())
					.as("full default 2")
					.startsWith(lp.getClassName() + "." + lp.getName())
					.isNotEqualTo(bundle1.getSymbolicName());
			}
			try (Launchpad lp = builder.debug()
				.create("& noncompliant name", "Unfriendly . class .& ..name")) {
				Bundle unfriendlyBundle = lp.bundle()
					.install();
				softly.assertThat(unfriendlyBundle.getSymbolicName())
					.as("unfriendly bundle")
					.startsWith("Unfriendly.class.name.noncompliantname");
			}
		}
	}

	public static class TestClass implements Supplier<Bundle> {
		@Override
		public Bundle get() {
			return FrameworkUtil.getBundle(TestClass.class);
		}
	}

	@Test
	public void addResourceWithCopy_createsCopyOfClass() throws Exception {
		try (Launchpad lp = builder.create()) {
			Bundle b = lp.bundle()
				.addResourceWithCopy(TestClass.class)
				.start();
			Class<?> inside = b.loadClass(TestClass.class.getName());
			assertThat(inside.getName()).as("className")
				.isEqualTo(TestClass.class.getName());
			// Should be unequal as they have been loaded by different class
			// loaders.
			assertThat(inside).as("class")
				.isNotEqualTo(TestClass.class);
			assertThat(inside.getClassLoader()).as("classLoader")
				.isInstanceOf(BundleReference.class);

			BundleReference br = (BundleReference) inside.getClassLoader();

			assertThat(br.getBundle()).as("bundle")
				.isSameAs(b);
			@SuppressWarnings("unchecked")
			Supplier<Bundle> s = (Supplier<Bundle>) inside.newInstance();
			Bundle suppliedBundle = s.get();
			assertThat(suppliedBundle).as("suppliedBundle")
				.isSameAs(b);
		}
	}

	@Test
	public void addResourceWithCopy_throwsIAE_onALambda() throws Exception {
		// Lambdas are synthetic and generated by the JVM at runtime. Thus there
		// is no actual class file to copy into the bundle.
		try (Launchpad lp = builder.create()) {
			Runnable r = () -> {};
			assertThatThrownBy(() -> lp.bundle()
				.addResourceWithCopy(r.getClass())).isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(r.getClass()
						.getName())
					.hasMessageContaining("synthetic");
		}
	}
}
