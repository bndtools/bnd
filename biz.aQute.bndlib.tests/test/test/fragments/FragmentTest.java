package test.fragments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;

public class FragmentTest {

	Builder init() throws IOException {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("bin_test"));
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.setExportPackage("test.fragments.imports");
		return b;
	}

	@Test
	public void simple() throws Exception {
		try (Builder b = init()) {
			b.setProperty(Constants.FRAGMENT_HOST, "osgi;bundle-version=4");
			b.build();
			assertTrue(b.check());

			assertThat(b.getImports()
				.keySet()).containsExactly(b.getPackageRef("javax.net.ssl"));
		}
	}

	@Test
	public void unmatchedVersion() throws Exception {
		try (Builder b = init()) {
			b.setProperty(Constants.FRAGMENT_HOST, "osgi;bundle-version=5");
			b.build();
			assertTrue(b.check("Host osgi=bundle-version=5"));
			assertThat(b.getImports()
				.keySet()).containsExactly(b.getPackageRef("javax.net.ssl"), b.getPackageRef("org.osgi.framework"));
		}
	}

	@Test
	public void noVersion() throws Exception {
		try (Builder b = init()) {
			b.setProperty(Constants.FRAGMENT_HOST, "osgi");
			b.build();
			assertTrue(b.check());
			assertThat(b.getImports()
				.keySet()).containsExactly(b.getPackageRef("javax.net.ssl"));
		}
	}

	@Test
	public void notAFragment() throws Exception {
		try (Builder b = init()) {
			b.build();
			assertTrue(b.check());
			assertThat(b.getImports()
				.keySet()).containsExactly(b.getPackageRef("javax.net.ssl"), b.getPackageRef("org.osgi.framework"));
		}
	}
}
