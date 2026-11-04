package org.bndtools.builder.validate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class JavaVersionsValidatorTest {

	@Test
	public void javaVersionsNormalizeLegacyPrefix() {
		assertThat(JavaVersionsValidator.sameJavaVersion("8", "1.8")).isTrue();
		assertThat(JavaVersionsValidator.sameJavaVersion("17", "1.8")).isFalse();
		assertThat(JavaVersionsValidator.sameJavaVersion("8", null)).isFalse();
	}
}
