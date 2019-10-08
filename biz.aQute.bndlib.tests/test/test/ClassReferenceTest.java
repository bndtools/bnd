package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz.JAVA;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ClassReferenceTest {
	class Inner {

	}

	static {
		System.err.println(Inner.class);
	}

	/**
	 * We create a JAR with the test.classreferenc.ClassReference class. This
	 * class contains a javax.swing.Box.class reference Prior to Java 1.5, this
	 * was done in a silly way that is handled specially. After 1.5 it is a
	 * normal reference.
	 *
	 * @throws Exception
	 */

	@ParameterizedTest(name = "Check code in compilerversions/src/{arguments}")
	@ValueSource(strings = {
		"sun_1_1", //
		"sun_1_2", //
		"sun_1_3", //
		"sun_1_4", //
		"sun_1_5", //
		"sun_jsr14", //
		"sun_1_6", //
		"sun_1_7", //
		"sun_1_8", //
		"jdk_9_0", //
		"jdk_10_0", //
		"jdk_11_0", //
		"jdk_12_0", //
		"jdk_13_0", //
		"eclipse_1_1", //
		"eclipse_1_2", //
		"eclipse_1_3", //
		"eclipse_1_4", //
		"eclipse_1_5", //
		"eclipse_1_6", //
		"eclipse_1_7", //
		"eclipse_1_8", //
		"eclipse_9_0", //
		"eclipse_10_0", //
		"eclipse_11_0", //
		"eclipse_12_0", //
		"eclipse_13_0" //
	})
	@DisplayName("Class Reference Test")
	public void doit(String pkg) throws Exception {
		Properties properties = new Properties();
		properties.put("-classpath", "compilerversions/compilerversions.jar");
		System.out.println("compiler version " + pkg);
		try (Builder builder = new Builder()) {
			properties.put(Constants.EEPROFILE, "auto");
			properties.put("Export-Package", pkg);
			builder.setProperties(properties);
			Jar jar = builder.build();
			assertThat(builder.check()).isTrue();
			JAVA highestEE = builder.getHighestEE();
			Map<String, Set<String>> profiles = highestEE.getProfiles();
			if (profiles != null) {
				System.out.println("profiles" + profiles);
				jar.getManifest()
					.write(System.out);
			}

			assertThat(builder.check()).isTrue();
			Manifest manifest = jar.getManifest();
			String imports = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertThat(imports).as("Package %s contains swing ref", pkg)
				.contains("javax.swing")
				.as("Package %s should not contain ClassRef", pkg)
				.doesNotContain("ClassRef");
		}
	}
}
