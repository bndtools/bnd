package test.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.assertj.core.api.Condition;
import org.junit.Test;

import aQute.bnd.build.model.EE;

public class EETest {

	@Test
	public void highestFromTargetVersion() throws Exception {
		assertThat(EE.highestFromTargetVersion("1.0")
			.orElse(null)).isEqualTo(EE.OSGI_Minimum_1_0);
		assertThat(EE.highestFromTargetVersion("1.1")
			.orElse(null)).isEqualTo(EE.JRE_1_1);
		assertThat(EE.highestFromTargetVersion("1.2")
			.orElse(null)).isEqualTo(EE.J2SE_1_2);
		assertThat(EE.highestFromTargetVersion("1.3")
			.orElse(null)).isEqualTo(EE.J2SE_1_3);
		assertThat(EE.highestFromTargetVersion("1.4")
			.orElse(null)).isEqualTo(EE.J2SE_1_4);
		assertThat(EE.highestFromTargetVersion("1.5")
			.orElse(null)).isEqualTo(EE.J2SE_1_5);
		assertThat(EE.highestFromTargetVersion("1.6")
			.orElse(null)).isEqualTo(EE.JavaSE_1_6);
		assertThat(EE.highestFromTargetVersion("1.7")
			.orElse(null)).isEqualTo(EE.JavaSE_1_7);
		assertThat(EE.highestFromTargetVersion("1.8")
			.orElse(null)).isEqualTo(EE.JavaSE_1_8);
		assertThat(EE.highestFromTargetVersion("1.9")
			.orElse(null)).isEqualTo(null);
		assertThat(EE.highestFromTargetVersion("9")
			.orElse(null)).isEqualTo(EE.JavaSE_9_0);
		assertThat(EE.highestFromTargetVersion("10")
			.orElse(null)).isEqualTo(EE.JavaSE_10_0);
		assertThat(EE.highestFromTargetVersion("11")
			.orElse(null)).isEqualTo(EE.JavaSE_11_0);
		assertThat(EE.highestFromTargetVersion("1.11")
			.orElse(null)).isEqualTo(null);
		assertThat(EE.highestFromTargetVersion("12")
			.orElse(null)).isEqualTo(EE.JavaSE_12_0);
		assertThat(EE.highestFromTargetVersion("13")
			.orElse(null)).isEqualTo(EE.JavaSE_13_0);
	}

	@Test
	public void getEEFromJvm() throws Exception {
		assertThat(EE.highestFromTargetVersion(System.getProperty("java.version"))).is( //
			new Condition<>(ee -> ee.get()
				.compareTo(EE.JavaSE_1_8) >= 0, "At least JavaSE-1.8"));
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
