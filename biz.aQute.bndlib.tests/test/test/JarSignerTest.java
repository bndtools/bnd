package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
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
		assertEquals("1.0", manifest.getMainAttributes()
			.getValue("Manifest-Version"));
		assertNotNull(manifest.getAttributes("WEB-INF/classes/org/osgi/framework/BundleContext.class"));
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
			System.err.println(Processor.join(b.getErrors(), "\n"));
			assertEquals(1, b.getErrors()
				.size());
			assertEquals(0, b.getWarnings()
				.size());
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
		properties.put("digestalg", "SHA-1");
		signer.setProperties(properties);

		Jar jar = new Jar(IO.getFile("testresources/test.jar"));
		Set<String> names = new HashSet<>(jar.getResources()
			.keySet());
		names.remove("META-INF/MANIFEST.MF");
		try (Builder b = new Builder()) {
			b.setJar(jar);
			signer.sign(b, "test");
			System.err.println(Processor.join(b.getErrors(), "\n"));
			System.err.println(Processor.join(b.getWarnings(), "\n"));
			assertEquals(0, b.getErrors()
				.size());
			assertEquals(0, b.getWarnings()
				.size());
			assertNotNull(jar.getResource("META-INF/TEST.SF"));
			Manifest m = jar.getManifest();

			// Should have added 2 new resources: TEST.SF and TEST.DSA/RSA
			assertEquals(names.size(), b.getJar()
				.getResources()
				.size() - 3);

			Attributes a = m.getAttributes("aQute/rendezvous/DNS.class");
			assertNotNull(a);
			assertEquals("G0/1CIZlB4eIVyY8tU/ZfMCqZm4=", a.getValue("SHA-1-Digest"));

			// Check if all resources are named
			for (String name : names) {
				System.err.println("name: " + name);
				assertNotNull(m.getAttributes(name));
			}
		}
	}

}
