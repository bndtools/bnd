package aQute.bnd.shade;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.renamer.ClassFileRenamer;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Shader;
import aQute.lib.io.IO;

public class ShaderTest {

	@Test
	public void testSimple() throws Exception {
		File in = IO.getFile("testresources/shade/osgi.jar");
		File out = IO.getFile("generated/out.jar");
		try (Jar jin = new Jar(in)) {
			Shader.shade(jin, new Parameters("org.osgi.framework.*;prefix:=foo"), null);

			// Source code

			assertThat(jin.getResource("OSGI-OPT/src/foo/org/osgi/framework/Bundle.java")).isNotNull();
			assertThat(jin.getResource("OSGI-OPT/src/foo/org/osgi/framework/BundleContext.java")).isNotNull();

			jin.write(out);
		}

		try (URLClassLoader urlc = new URLClassLoader(new URL[] {
			out.toURI()
				.toURL()
		}, null)) {

			Class<?> bundle = urlc.loadClass("foo.org.osgi.framework.Bundle");
			Class<?> bundleContext = urlc.loadClass("foo.org.osgi.framework.Bundle");
			Class<?> serviceEvent = urlc.loadClass("foo.org.osgi.framework.ServiceEvent");
			Class<?> logentry = urlc.loadClass("org.osgi.service.log.LogEntry");
			assertThat(logentry.getClassLoader()).isEqualTo(urlc);
			Class<?> returnType = logentry.getMethod("getBundle")
				.getReturnType();
			assertThat(returnType).isEqualTo(bundle);

		}
	}

	@Test
	public void testHashedPackage() throws Exception {
		File in = IO.getFile("testresources/shade/osgi.jar");
		File out = IO.getFile("generated/out.jar");
		String pack;
		try (Jar jin = new Jar(in)) {
			Map<String, String> shade = Shader.shade(jin, new Parameters("org.osgi.framework.*"), null);
			System.out.println(shade);
			assertThat(shade).hasSize(1)
				.containsKey("org/osgi/framework/");
			pack = shade.get("org/osgi/framework/")
				.replace('/', '.');

			assertThat(jin.getResource("OSGI-OPT/src/" + pack.replace('.', '/') + "Bundle.java")).isNotNull();
			assertThat(jin.getResource("OSGI-OPT/src/" + pack.replace('.', '/') + "BundleContext.java")).isNotNull();

			jin.write(out);
		}
		try (URLClassLoader urlc = new URLClassLoader(new URL[] {
			out.toURI()
				.toURL()
		}, null)) {

			Class<?> bundle = urlc.loadClass(pack + "Bundle");
			Class<?> bundleContext = urlc.loadClass(pack + "Bundle");
			Class<?> serviceEvent = urlc.loadClass(pack + "ServiceEvent");
			Class<?> logentry = urlc.loadClass("org.osgi.service.log.LogEntry");
			assertThat(logentry.getClassLoader()).isEqualTo(urlc);
			Class<?> returnType = logentry.getMethod("getBundle")
				.getReturnType();
			assertThat(returnType).isEqualTo(bundle);

		}
	}

	@Test
	public void testRenameClass() throws IOException {
		ClassFile input = ClassFile
			.parseInputStream(new FileInputStream(IO.getFile("testresources/shade/ShadeA.clazz")));
		System.out.println(input.toString()
			.replace(',', '\n'));

		Optional<ClassFile> rename = ClassFileRenamer.rename(input, name -> {
			if (name.startsWith("aQute/bnd/shade/")) {
				name = "foo" + name.substring(15);
				System.out.println(name);
			}
			return name;
		});
		assertThat(rename).isPresent();
		ClassFile cf = rename.get();
		System.out.println(cf.toString()
			.replace(',', '\n'));
	}

	@Test
	public void testRecordAttribute() throws Exception {
		try (InputStream stream = IO.stream(new File("testresources/record/MinMax.class"))) {
			ClassFile cf = ClassFile.parseClassFile(new DataInputStream(stream));
			ClassFile clazz = ClassFileRenamer.rename(cf, s -> {
					if (s.startsWith("java/lang/"))
						return "foo" + s.substring(9);
					return s;
				})
				.get();
			assertThat(clazz.major_version).isGreaterThanOrEqualTo(Clazz.JAVA.OpenJDK15.getMajor());

			assertThat(clazz.constant_pool.stream()
				.anyMatch("java/lang/Object"::equals)).isFalse();
		}
	}

	@Test
	public void testBuilder() throws Exception {
		try (Builder b = new Builder()) {

			b.addClasspath(IO.getFile("testresources/shade/osgi.jar"));
			b.setProperty("-sourcepath", "src");
			b.setProperty("-sources", "true");
			b.setProperty("-privatepackage", "org.osgi.service.*");
			b.setProperty("-conditionalpackage", "org.osgi.framework.*;prefix:=foo");
			b.setProperty("-shade", "${-conditionalpackage}");
			Jar jar = b.build();
			assertThat(b.check()).isTrue();

			assertThat(jar.getResources()
				.keySet()).contains("OSGI-OPT/src/foo/org/osgi/framework/ServiceEvent.java")
					.contains("foo/org/osgi/framework/ServiceEvent.class")
					.contains("foo/org/osgi/framework/packageinfo");
		}

	}

}
