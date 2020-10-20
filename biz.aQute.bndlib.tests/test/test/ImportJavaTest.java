package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.jar.Manifest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;

public class ImportJavaTest {
	private Builder builder;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		builder = new Builder();
		builder.addClasspath(new File("bin_test"));
		builder.setProperty("Bundle-Name", testInfo.getTestMethod()
			.map(Method::getName)
			.get());
	}

	@AfterEach
	public void testDown() throws Exception {
		builder.close();
	}

	@Test
	public void import_java_all() throws Exception {
		builder.setProperty("Import-Package", "org.osgi.framework;version=\"[1.9,2)\",*");
		builder.setProperty("-includepackage", "test.importjava");
		builder.setProperty("-noimportjava", "false");
		Jar jar = builder.build();
		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		manifest.write(System.err);
		Domain d = Domain.domain(manifest);
		Parameters imports = d.getImportPackage();
		assertThat(imports).containsOnlyKeys("org.osgi.framework", "java.io", "java.lang", "java.lang.invoke",
			"java.lang.reflect", "java.util", "java.util.function", "java.util.stream");
	}

	@Test
	public void import_java_some() throws Exception {
		builder.setProperty("Import-Package", "org.osgi.framework;version=\"[1.9,2)\",java.util.*,!java.*,*");
		builder.setProperty("-includepackage", "test.importjava");
		Jar jar = builder.build();
		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		manifest.write(System.err);
		Domain d = Domain.domain(manifest);
		Parameters imports = d.getImportPackage();
		assertThat(imports).containsOnlyKeys("org.osgi.framework", "java.util", "java.util.function",
			"java.util.stream");
	}

	@Test
	public void import_java_none() throws Exception {
		builder.setProperty("Import-Package", "org.osgi.framework;version=\"[1.9,2)\",!java.*,*");
		builder.setProperty("-includepackage", "test.importjava");
		Jar jar = builder.build();
		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		manifest.write(System.err);
		Domain d = Domain.domain(manifest);
		Parameters imports = d.getImportPackage();
		assertThat(imports).containsOnlyKeys("org.osgi.framework");
	}

	@Test
	public void no_import_java() throws Exception {
		builder.setProperty("Import-Package", "org.osgi.framework;version=\"[1.9,2)\",*");
		builder.setProperty("-includepackage", "test.importjava");
		builder.setProperty("-noimportjava", "true");
		Jar jar = builder.build();
		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		manifest.write(System.err);
		Domain d = Domain.domain(manifest);
		Parameters imports = d.getImportPackage();
		assertThat(imports).containsOnlyKeys("org.osgi.framework");
	}

	@Test
	public void import_java_old_framework() throws Exception {
		builder.setProperty("Import-Package", "org.osgi.framework;version=\"[1.8,2)\",*");
		builder.setProperty("-includepackage", "test.importjava");
		builder.setProperty("-noimportjava", "false");
		Jar jar = builder.build();
		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		manifest.write(System.err);
		Domain d = Domain.domain(manifest);
		Parameters imports = d.getImportPackage();
		assertThat(imports).containsOnlyKeys("org.osgi.framework");
	}

	@Test
	public void import_java_all_java11() throws Exception {
		builder.setProperty("-classpath", "compilerversions/compilerversions.jar");
		builder.setProperty("-includepackage", "test.importjava,jdk_11_0");
		builder.setProperty("-noimportjava", "false");

		Jar jar = builder.build();
		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		manifest.write(System.err);
		Domain d = Domain.domain(manifest);
		Parameters imports = d.getImportPackage();
		assertThat(imports).containsOnlyKeys("java.io", "java.lang", "java.lang.invoke", "java.lang.reflect",
			"java.util", "java.util.function", "java.util.stream", "javax.swing");
	}

	@Test
	public void no_import_java_java11() throws Exception {
		builder.setProperty("-classpath", "compilerversions/compilerversions.jar");
		builder.setProperty("-includepackage", "test.importjava,jdk_11_0");
		builder.setProperty("-noimportjava", "true");

		Jar jar = builder.build();
		assertTrue(builder.check());
		Manifest manifest = jar.getManifest();
		manifest.write(System.err);
		Domain d = Domain.domain(manifest);
		Parameters imports = d.getImportPackage();
		assertThat(imports).containsOnlyKeys("javax.swing");
	}

}
