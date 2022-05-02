package test.exporter;

import static aQute.bnd.exporter.executable.ExecutableJarExporter.EXECUTABLE_JAR;
import static aQute.bnd.osgi.Constants.JPMS_MODULE_INFO;
import static aQute.bnd.osgi.Constants.MAIN_CLASS;
import static aQute.bnd.osgi.Constants.MODULE_INFO_CLASS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.Attributes;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Run;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.classfile.ModuleMainClassAttribute;
import aQute.bnd.classfile.ModulePackagesAttribute;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.IO;

public class ExecutableExporterTest {

	@Test
	public void testExecutableExportWithJPMS() throws Exception {
		File f = IO.getFile("testresources/export-executable/executable-1.bndrun");
		assertThat(f).isFile();

		try (Run run = Run.createRun(null, f)) {
			assertThat(run).isNotNull();

			run.setProperty(JPMS_MODULE_INFO, "foo;version=3.9.8");

			Map.Entry<String, Resource> export = run.export(EXECUTABLE_JAR, null);

			assertThat(export).isNotNull();

			try (JarResource jarResource = (JarResource) export.getValue()) {
				Jar jar = jarResource.getJar();

				Map<String, Resource> resources = jar.getResources();

				assertThat(resources).containsKeys("jar/org.apache.felix.scr-2.1.12.jar",
					"jar/org.eclipse.osgi-3.17.100.jar", "jar/slf4j-api-1.7.25.jar", "jar/slf4j-simple-1.7.25.jar",
					"module-info.class");

				Attributes mainAttributes = jar.getManifest()
					.getMainAttributes();

				assertThat(mainAttributes.getValue(MAIN_CLASS))
					.isEqualTo("aQute.launcher.pre.EmbeddedLauncher");

				Resource moduleInfoResource = jar.getResource(MODULE_INFO_CLASS);

				assertThat(moduleInfoResource).isNotNull();
				ClassFile module_info;
				ByteBuffer bb = moduleInfoResource.buffer();
				if (bb != null) {
					module_info = ClassFile.parseClassFile(ByteBufferDataInput.wrap(bb));
				} else {
					try (DataInputStream din = new DataInputStream(moduleInfoResource.openInputStream())) {
						module_info = ClassFile.parseClassFile(din);
					}
				}

				ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
					.filter(ModuleAttribute.class::isInstance)
					.map(ModuleAttribute.class::cast)
					.findFirst()
					.orElse(null);

				assertThat(moduleAttribute.module_name).isEqualTo("foo");
				assertThat(moduleAttribute.module_version).isEqualTo("3.9.8");
				assertThat(moduleAttribute.module_flags).isEqualTo(32);

				assertThat( //
					Arrays.stream(moduleAttribute.requires)
						.map(e -> e.requires)
						.collect(toList()) //
				).containsExactly( //
					"java.base", "java.instrument", "java.logging", "java.management", "java.xml", "jdk.unsupported");

				ModulePackagesAttribute modulePackagesAttribute = Arrays.stream(module_info.attributes)
					.filter(ModulePackagesAttribute.class::isInstance)
					.map(ModulePackagesAttribute.class::cast)
					.findFirst()
					.orElse(null);

				assertThat(modulePackagesAttribute.packages).containsExactly("aQute/launcher/agent",
					"aQute/launcher/pre", "jar");

				ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
					.filter(ModuleMainClassAttribute.class::isInstance)
					.map(ModuleMainClassAttribute.class::cast)
					.findFirst()
					.orElse(null);

				assertThat(moduleMainClassAttribute.main_class).isEqualTo("aQute/launcher/pre/EmbeddedLauncher");
			}
		}
	}

	@Test
	public void testExecutableExport() throws Exception {
		File f = IO.getFile("testresources/export-executable/executable-1.bndrun");
		assertThat(f).isFile();

		try (Run run = Run.createRun(null, f)) {
			assertThat(run).isNotNull();

			Map.Entry<String, Resource> export = run.export(EXECUTABLE_JAR, null);

			assertThat(export).isNotNull();

			try (JarResource jarResource = (JarResource) export.getValue()) {
				Jar jar = jarResource.getJar();

				assertThat(jar.getResources()).containsKeys("jar/org.apache.felix.scr-2.1.12.jar",
					"jar/org.eclipse.osgi-3.17.100.jar", "jar/slf4j-api-1.7.25.jar", "jar/slf4j-simple-1.7.25.jar")
					.doesNotContainKeys("module-info.class");

				Attributes mainAttributes = jar.getManifest()
					.getMainAttributes();

				assertThat(mainAttributes.getValue(Constants.MAIN_CLASS))
					.isEqualTo("aQute.launcher.pre.EmbeddedLauncher");

				assertThat(jar.getModuleName()).isNull();
				assertThat(jar.getModuleVersion()).isNull();
			}
		}
	}

}
