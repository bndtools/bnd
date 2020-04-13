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
		String java_version = System.getProperty("java.version");
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

}
