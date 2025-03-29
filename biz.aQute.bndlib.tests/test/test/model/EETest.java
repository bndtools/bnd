package test.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;

public class EETest {

	@ParameterizedTest(name = "Check ee {1} label {0}")
	@ArgumentsSource(EEVersionsArgumentsProvider.class)
	@DisplayName("Test EE.highestFromTargetVersion")
	public void highestFromTargetVersion(String label, EE ee) throws Exception {
		assertThat(EE.highestFromTargetVersion(label)).contains(ee);
	}

	static class EEVersionsArgumentsProvider implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			EE[] values = EE.values();
			return Arrays.stream(values, 0, values.length - 1)
				.filter(ee -> ee.getCapabilityName()
					.indexOf('/') < 0)
				.map(ee -> Arguments.of(ee.getVersionLabel(), ee));
		}
	}

	@Test
	public void highestFromTargetVersionOther() throws Exception {
		assertThat(EE.highestFromTargetVersion("1.0")).contains(EE.OSGI_Minimum_1_0);
		assertThat(EE.highestFromTargetVersion("1.9")).isEmpty();
		assertThat(EE.highestFromTargetVersion("1.11")).isEmpty();
	}

	@Test
	public void getEEFromJvm() throws Exception {
		String java_version = System.getProperty("java.specification.version");
		Version version = MavenVersion.parseMavenString(java_version)
			.getOSGiVersion();
		assertThat(EE.highestFromTargetVersion(java_version)).hasValueSatisfying( //
			new Condition<>(ee -> ee.getCapabilityVersion()
				.getMajor() == version.getMajor(), "EE capability version same version as JDK"));
	}

	@Test
	public void failsWithNonVersionInput() throws Exception {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> EE.highestFromTargetVersion("sillyinput"))
			.withMessage("Invalid syntax for version: sillyinput");
	}

	@Test
	public void failsWithNullInput() throws Exception {
		assertThatExceptionOfType(NullPointerException.class).isThrownBy( //
			() -> EE.highestFromTargetVersion(null));
	}

	@ParameterizedTest(name = "Validate JAVA exists for {arguments}")
	@ArgumentsSource(EEsArgumentsProvider.class)
	@DisplayName("Validate a JAVA exists for each EE")
	public void checkJAVAFor(EE ee) throws Exception {
		assertThat(Clazz.JAVA.values()).anyMatch(j -> j.getEE()
			.equals(ee.getEEName()));
	}

	@ParameterizedTest(name = "Validate Compatible EEs exist for {arguments}")
	@ArgumentsSource(EEsArgumentsProvider.class)
	@DisplayName("Validate Compatible EEs exist for each EE")
	public void checkEEHasCompatible(EE ee) throws Exception {
		if (ee == EE.JRE_1_1) {
			// skip JRE_1_1
			return;
		}
		EE[] compatible = ee.getCompatible();
		assertThat(compatible).isNotEmpty();
	}

	@ParameterizedTest(name = "Validate release target for {arguments}")
	@ArgumentsSource(EEsArgumentsProvider.class)
	@DisplayName("Validate release target for each EE")
	public void checkEEHasValidRelease(EE ee) throws Exception {
		switch (ee) {
			case OSGI_Minimum_1_0 :
			case OSGI_Minimum_1_1 :
			case OSGI_Minimum_1_2 :
			case J2SE_1_2 :
			case J2SE_1_3 :
			case J2SE_1_4 :
			case J2SE_1_5 :
			case JRE_1_1 :
				assertThat(ee.getReleaseTarget()).isEmpty();
				break;
			case JavaSE_1_6 :
				assertThat(ee.getReleaseTarget()).hasValue(6);
				break;
			case JavaSE_1_7 :
				assertThat(ee.getReleaseTarget()).hasValue(7);
				break;
			case JavaSE_1_8 :
			case JavaSE_compact1_1_8 :
			case JavaSE_compact2_1_8 :
			case JavaSE_compact3_1_8 :
				assertThat(ee.getReleaseTarget()).hasValue(8);
				break;

			default :
				assertThat(ee.getReleaseTarget()).hasValue(ee.getCapabilityVersion()
					.getMajor());
				break;
		}
	}

	@ParameterizedTest(name = "Validate Packages exist for {arguments}")
	@ArgumentsSource(EEsArgumentsProvider.class)
	@DisplayName("Validate Packages exist for each EE")
	public void checkEEHasPackages(EE ee) throws Exception {
		if (ee == EE.JRE_1_1) {
			// skip JRE_1_1
			return;
		}
		assertThat(ee.getPackages()).isNotEmpty();
	}

	@ParameterizedTest(name = "Validate Modules exist for {arguments}")
	@ArgumentsSource(ModularEEsArgumentsProvider.class)
	@DisplayName("Validate Modules exist for each EE beyond Java 8")
	public void checkEEHasModules(EE ee) throws Exception {
		assertThat(ee.getModules()).isNotEmpty();
	}

	@Test
	public void testEEIsJDKPackage() {
		// this package appeared in JDK 1.6+
		// see .properties in aQute.bnd.build.model
		PackageRef pck = new Descriptors().getPackageRef("javax.xml.transform.stax");
		assertThat(EE.J2SE_1_2.getPackages().containsKey(pck.getFQN())).isFalse();
		assertThat(EE.J2SE_1_5.getPackages()
			.containsKey(pck.getFQN())).isFalse();
		assertThat(EE.JavaSE_1_6.getPackages()
			.containsKey(pck.getFQN())).isTrue();
		assertThat(EE.JavaSE_17.getPackages()
			.containsKey(pck.getFQN())).isTrue();
	}

	static class EEsArgumentsProvider implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			EE[] values = EE.values();
			return Arrays.stream(values, 0, values.length - 1)
				.filter(ee -> ee.getCapabilityName()
					.indexOf('/') < 0)
				.map(Arguments::of);
		}
	}

	static class ModularEEsArgumentsProvider implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			EE[] values = EE.values();
			return Arrays.stream(values, 0, values.length - 1)
				.filter(ee -> ee.compareTo(EE.JavaSE_1_8) > 0)
				.map(Arguments::of);
		}
	}

}
