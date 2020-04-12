package test.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.model.EE;

public class EETest {

	@Test
	public void highestFromTargetVersion() throws Exception {
		assertThat(EE.highestFromTargetVersion("1.0")).contains(EE.OSGI_Minimum_1_0);
		assertThat(EE.highestFromTargetVersion("1.1")).contains(EE.JRE_1_1);
		assertThat(EE.highestFromTargetVersion("1.2")).contains(EE.J2SE_1_2);
		assertThat(EE.highestFromTargetVersion("1.3")).contains(EE.J2SE_1_3);
		assertThat(EE.highestFromTargetVersion("1.4")).contains(EE.J2SE_1_4);
		assertThat(EE.highestFromTargetVersion("1.5")).contains(EE.J2SE_1_5);
		assertThat(EE.highestFromTargetVersion("1.6")).contains(EE.JavaSE_1_6);
		assertThat(EE.highestFromTargetVersion("1.7")).contains(EE.JavaSE_1_7);
		assertThat(EE.highestFromTargetVersion("1.8")).contains(EE.JavaSE_1_8);
		assertThat(EE.highestFromTargetVersion("1.9")).isEmpty();
		assertThat(EE.highestFromTargetVersion("9")).contains(EE.JavaSE_9_0);
		assertThat(EE.highestFromTargetVersion("10")).contains(EE.JavaSE_10_0);
		assertThat(EE.highestFromTargetVersion("11")).contains(EE.JavaSE_11_0);
		assertThat(EE.highestFromTargetVersion("1.11")).isEmpty();
		assertThat(EE.highestFromTargetVersion("12")).contains(EE.JavaSE_12_0);
		assertThat(EE.highestFromTargetVersion("13")).contains(EE.JavaSE_13_0);
		assertThat(EE.highestFromTargetVersion("14")).contains(EE.JavaSE_14_0);
		assertThat(EE.highestFromTargetVersion("15")).contains(EE.JavaSE_15_0);
	}

	@Test
	public void getEEFromJvm() throws Exception {
		assertThat(EE.highestFromTargetVersion(System.getProperty("java.version"))).hasValueSatisfying( //
			new Condition<>(ee -> ee.compareTo(EE.JavaSE_1_8) >= 0, "At least JavaSE-1.8"));
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
