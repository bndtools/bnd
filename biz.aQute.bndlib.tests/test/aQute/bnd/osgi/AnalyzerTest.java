package aQute.bnd.osgi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.lib.io.IO;

public class AnalyzerTest {

	@Test
	public void testdoNameSection() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.doNameSection(null, "@");
			a.doNameSection(null, "@@");
			a.doNameSection(null, "@foo@bar@");
		}
	}

	@Test
	public void multiRelease() throws Exception {
		try (Analyzer a = new Analyzer(new Jar("test"))) {
			a.setProperty("Multi-Release", "true");
			assertThat(a.check()).isTrue();
			Jar jar = a.getJar();
			String fooPath = "test/Foo.class";
			String barPath = "test/Bar.class";
			jar.putResource(fooPath,
					new FileResource(IO.getFile("testresources/mr/java8/Foo.class")));
			jar.putResource(barPath,
				new FileResource(IO.getFile("testresources/mr/java8/Bar.class")));
			jar.putVersionedResource(fooPath, 9, new FileResource(IO.getFile("testresources/mr/java9/Foo.class")));
			jar.putVersionedResource(barPath, 17, new FileResource(IO.getFile("testresources/mr/java17/Bar.class")));
			jar.setManifest(a.calcManifest());
			Manifest mainMf = jar.getManifest();
			Optional<Manifest> java9Mf = jar.getManifest(9);
			Optional<Manifest> java17Mf = jar.getManifest(17);
			assertThat(mainMf.getMainAttributes()
				.getValue(Constants.REQUIRE_CAPABILITY)).isNotNull()
					.contains("(&(osgi.ee=JavaSE)(version=1.8))");
			assertThat(java9Mf.get()
				.getMainAttributes()
				.getValue(Constants.REQUIRE_CAPABILITY)).isNotNull()
					.contains("(&(osgi.ee=JavaSE)(version=9))");
			assertThat(java17Mf.get()
				.getMainAttributes()
				.getValue(Constants.REQUIRE_CAPABILITY)).isNotNull()
					.contains("(&(osgi.ee=JavaSE)(version=17))");
		}
	}

}
