package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.signing.JartoolSigner;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.libg.generics.Create;

@SuppressWarnings({
	"resource", "restriction"
})
public class JarSignerTest {

	@Test
	public void testNoManifest(@InjectTemporaryDirectory
	File tmpdir) throws Exception {
		Builder b = new Builder();
		b.setProperty("jarsigner", "jarsigner");
		b.setProperty("-sign", "test");
		b.setProperty(Constants.PLUGIN, JartoolSigner.class.getName()
			+ ";keystore=testresources/keystore;keypass=testtest;storepass=testtest;sigfile=test");
		b.setProperty("-nomanifest", "true");
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "WEB-INF/classes=@jar/osgi.jar");

		Jar jar = b.build();
		File tmp = new File(tmpdir, "xyztmp.jar");

		jar.write(tmp);

		Jar jar2 = new Jar(tmp);
		Manifest manifest = jar2.getManifest();
		assertThat(manifest.getMainAttributes()).containsEntry(Name.MANIFEST_VERSION, "1.0");
		assertThat(jar2.getResources()).containsKeys("META-INF/TEST.SF", "META-INF/TEST.EC");

		assertThat(manifest.getAttributes("WEB-INF/classes/org/osgi/framework/BundleContext.class")).isNotNull();
	}

	@Test
	public void testError() throws Exception {
		JartoolSigner signer = new JartoolSigner();
		Map<String, String> properties = Create.map();
		properties.put("keystore", "testresources/keystore");
		properties.put("keypass", "testtest");
		properties.put("storepass", "notvalid");
		signer.setProperties(properties);

		try (Builder b = new Builder()) {
			b.setTrace(true);
			Jar jar = new Jar(IO.getFile("testresources/test.jar"));
			b.setJar(jar);
			signer.sign(b, "test");
			assertThat(b.getErrors()).hasSize(1);
			assertThat(b.getWarnings()).isEmpty();
		}
	}

	@Test
	public void testSimple() throws Exception {
		JartoolSigner signer = new JartoolSigner();
		Map<String, String> properties = Create.map();
		properties.put("keystore", "testresources/keystore");
		properties.put("keypass", "testtest");
		properties.put("storepass", "testtest");
		properties.put("sigFile", "test");
		properties.put("digestalg", "SHA-256");
		signer.setProperties(properties);

		Jar jar = new Jar(IO.getFile("testresources/test.jar"));
		Set<String> names = new HashSet<>(jar.getResources()
			.keySet());
		names.remove("META-INF/MANIFEST.MF");
		try (Builder b = new Builder()) {
			b.setJar(jar);
			signer.sign(b, "test");
			assertThat(b.getErrors()).isEmpty();
			assertThat(b.getWarnings()).isEmpty();
			assertThat(jar.getResources()).containsKeys("META-INF/TEST.SF", "META-INF/TEST.EC");
			Manifest m = jar.getManifest();

			// Should have added 2 new resources: TEST.SF and TEST.DSA/RSA/EC
			assertThat(b.getJar()
				.getResources()).hasSize(names.size() + 3);

			Name digestKey = new Name(properties.get("digestalg") + "-Digest");
			assertThat(m.getAttributes("aQute/rendezvous/DNS.class")).containsEntry(digestKey,
				"BMyZnHUVh1dDzBZSzaEyjRAZU+3pygawaasUDYLGEJ0=");

			// Check if all resources are named
			for (String name : names) {
				assertThat(m.getAttributes(name)).containsKey(digestKey);
			}
		}
	}

}
