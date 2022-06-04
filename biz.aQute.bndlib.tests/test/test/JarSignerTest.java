package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.signing.JartoolSigner;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.lib.io.NonClosingInputStream;
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
		Predicate<String> signingResource = name -> Jar.METAINF_SIGNING_P.matcher(name)
			.matches();
		Condition<String> signingResourceCondition = new Condition<>(signingResource, "signing resource");
		assertThat(jar.getResources()
			.keySet()).as("jar entry names")
				.haveExactly(0, signingResourceCondition);
		try (Builder builder = new Builder()) {
			builder.setJar(jar);
			signer.sign(builder, "test");
			assertThat(builder.getErrors()).isEmpty();
			assertThat(builder.getWarnings()).isEmpty();

			// Should have added 2 new resources: TEST.SF and TEST.DSA/RSA/EC
			assertThat(jar.getResources()
				.keySet()).as("jar entry names")
					.haveExactly(2, signingResourceCondition);

			// Check that each resources is digested
			Manifest manifest = jar.getManifest();
			Name digestKey = new Name(properties.get("digestalg") + "-Digest");
			for (String name : jar.getResources()
				.keySet()) {
				if (JarFile.MANIFEST_NAME.equals(name) || signingResource.test(name)) {
					continue;
				}
				assertThat(manifest.getAttributes(name)).as("%s for %s", digestKey, name)
					.containsKey(digestKey);
			}
			assertThat(manifest.getAttributes("aQute/rendezvous/DNS.class"))
				.as("%s for %s", digestKey, "aQute/rendezvous/DNS.class")
				.containsEntry(digestKey, "BMyZnHUVh1dDzBZSzaEyjRAZU+3pygawaasUDYLGEJ0=");

			// Check that JarInputStream can verify
			try (JarInputStream jin = new JarInputStream(new JarResource(jar).openInputStream());
				InputStream in = new NonClosingInputStream(jin)) {
				for (JarEntry entry; (entry = jin.getNextJarEntry()) != null;) {
					String name = entry.getName();
					if (entry.isDirectory() || signingResource.test(name)) {
						continue;
					}
					IO.drain(in); // must read complete entry first
					CodeSigner[] codeSigners = entry.getCodeSigners();
					Certificate[] certificates = entry.getCertificates();
					assertThat(codeSigners).as("%s for %s", "CodeSigners", name)
						.isNotEmpty()
						.doesNotContainNull();
					assertThat(certificates).as("%s for %s", "Certificates", name)
						.isNotEmpty()
						.doesNotContainNull();
				}
			}
		}
	}

}
