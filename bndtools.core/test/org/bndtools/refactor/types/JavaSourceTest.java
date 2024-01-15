package org.bndtools.refactor.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bndtools.refactor.types.LiteralRefactorer.format;

import org.junit.jupiter.api.Test;

class JavaSourceTest {

	@Test
	void testFormatting() {
		assertThat(format("", "0", "", 4, true)).isEqualTo("0000");
		assertThat(format("", "1", "", 4, true)).isEqualTo("0001");
		assertThat(format("", "10", "", 4, true)).isEqualTo("0010");
		assertThat(format("", "200", "", 4, true)).isEqualTo("0200");
		assertThat(format("", "3000", "", 4, true)).isEqualTo("3000");

		assertThat(format("", "40000", "", 4, true)).isEqualTo("0004_0000");
		assertThat(format("", "40000", "", 4, false)).isEqualTo("4_0000");
		assertThat(format("", "500000", "", 4, true)).isEqualTo("0050_0000");
		assertThat(format("", "500000", "", 4, false)).isEqualTo("50_0000");
		assertThat(format("", "6000000", "", 4, true)).isEqualTo("0600_0000");
		assertThat(format("", "6000000", "", 4, false)).isEqualTo("600_0000");
		assertThat(format("", "70000000", "", 4, true)).isEqualTo("7000_0000");
		assertThat(format("", "70000000", "", 4, false)).isEqualTo("7000_0000");

		assertThat(format("", "800000000", "", 4, true)).isEqualTo("0008_0000_0000");
		assertThat(format("", "800000000", "", 4, false)).isEqualTo("8_0000_0000");
	}

}
