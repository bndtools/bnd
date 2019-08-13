package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.jar.Manifest;

import org.junit.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA1;

public class LibDirectiveTest {

	@Test
	public void testLibDirective() throws Exception {
		File asmjar = new File("jar/asm.jar");
		try (Builder b = new Builder()) {
			b.setProperty("Bundle-ClassPath", ".");
			// -includeresource from the classpath
			b.setProperty("-includeresource", "lib/asm.jar=asm.jar;lib:=true");
			b.addClasspath(asmjar);
			b.build();
			assertThat(b.check()).isTrue();
			Jar jar = b.getJar();
			assertThat(jar).isNotNull();
			Manifest m = jar.getManifest();
			assertThat(m).isNotNull();
			String bcp = m.getMainAttributes()
				.getValue(Constants.BUNDLE_CLASSPATH);
			assertThat(bcp).isNotNull();
			assertThat(Strings.split(bcp)).containsExactly(".", "lib/asm.jar");
			Resource resource = jar.getResource("lib/asm.jar");
			assertThat(resource).isNotNull();
			assertThat(SHA1.digest(asmjar)).isEqualTo(SHA1.digest(resource.openInputStream()));
		}
	}

	@Test
	public void testLibDirectiveWithDefaultedBundleClassPath() throws Exception {
		File asmjar = new File("jar/asm.jar");
		try (Builder b = new Builder()) {
			// -includeresource from a file
			b.setProperty("-includeresource", "lib/=jar/asm.jar;lib:=true");
			b.build();
			assertThat(b.check()).isTrue();
			Jar jar = b.getJar();
			assertThat(jar).isNotNull();
			Manifest m = jar.getManifest();
			assertThat(m).isNotNull();
			String bcp = m.getMainAttributes()
				.getValue(Constants.BUNDLE_CLASSPATH);
			assertThat(bcp).isNotNull();
			assertThat(Strings.split(bcp)).containsExactly(".", "lib/asm.jar");
			Resource resource = jar.getResource("lib/asm.jar");
			assertThat(resource).isNotNull();
			assertThat(SHA1.digest(asmjar)).isEqualTo(SHA1.digest(resource.openInputStream()));
		}
	}

}
