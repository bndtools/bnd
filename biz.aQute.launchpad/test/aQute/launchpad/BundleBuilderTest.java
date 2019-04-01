package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Dictionary;

import org.junit.Test;
import org.osgi.framework.Bundle;

import aQute.bnd.build.Workspace;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

public class BundleBuilderTest {

	static {
		/*
		 * requires new support from the driver so the workspace can be removed
		 * later since it takes a bit of time to startup.
		 */
		try {
			Workspace w = Workspace.findWorkspace(IO.work);
		} catch (Exception e) {
			Exceptions.duck(e);
		}
	}

	LaunchpadBuilder builder = new LaunchpadBuilder().runfw("org.apache.felix.framework");

	@Test
	public void testInherit() throws Exception {
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
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
				.workspace()
				.start();
			testHeader(bundle, "Workspace", "true");
			testHeader(bundle, "Project", null);
			testHeader(bundle, "Inherit", null);
			testHeader(bundle, "LaunchPadInheritTest", "Workspace");
		}
		try (Launchpad lp = builder.create()) {

			Bundle bundle = lp.bundle()
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

		assertThat(headers.get(key)).isEqualTo(value);
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
}
