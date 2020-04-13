package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.JAVA;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.FileTree;

public class ClassReferenceTest {
	@ParameterizedTest(name = "Check code in compilerversions/src/{arguments}")
	@ArgumentsSource(CompilerVersionsArgumentsProvider.class)
	@DisplayName("Class Reference Test")
	public void classReferences(String pkg) throws Exception {
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

	static class CompilerVersionsArgumentsProvider implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			FileTree tree = new FileTree();
			List<File> files = tree.getFiles(new File("compilerversions/src"), "*");
			return files.stream()
				.filter(File::isDirectory)
				.map(File::getName)
				.map(Arguments::of);
		}
	}

	@ParameterizedTest(name = "Validate EE exists for {arguments}")
	@ArgumentsSource(JAVAArgumentsProvider.class)
	@DisplayName("Validate an EE exists for each JAVA")
	public void checkEEFor(Clazz.JAVA java) throws Exception {
		EE ee = EE.parse(java.getEE());
		assertThat(ee).isNotNull();
	}

	static class JAVAArgumentsProvider implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			Clazz.JAVA[] values = Clazz.JAVA.values();
			return Arrays.stream(values, 0, values.length - 1)
				.map(Arguments::of);
		}
	}
}
