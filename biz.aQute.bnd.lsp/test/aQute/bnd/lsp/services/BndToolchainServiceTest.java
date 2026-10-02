package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

public class BndToolchainServiceTest {

	@Test
	public void testNormalizeVersion() {
		assertThat(BndToolchainService.normalizeVersion("1.8")).isEqualTo(OptionalInt.of(8));
		assertThat(BndToolchainService.normalizeVersion("8")).isEqualTo(OptionalInt.of(8));
		assertThat(BndToolchainService.normalizeVersion("11")).isEqualTo(OptionalInt.of(11));
		assertThat(BndToolchainService.normalizeVersion("17")).isEqualTo(OptionalInt.of(17));
		assertThat(BndToolchainService.normalizeVersion("1.5")).isEqualTo(OptionalInt.of(5));
		assertThat(BndToolchainService.normalizeVersion("invalid")).isEqualTo(OptionalInt.empty());
	}

	@Test
	public void testCompatibility() {
		BndToolchainService service = new BndToolchainService();

		// JDK 17 with 1.8 target -> compatible
		assertThat(service.isCompatible(17, "1.8")).isTrue();
		assertThat(service.isCompatible(17, "17")).isTrue();

		// JDK 21 dropped source/target 7 and below
		assertThat(service.isCompatible(21, "1.7")).isFalse();
		assertThat(service.isCompatible(21, "1.6")).isFalse();
		assertThat(service.isCompatible(21, "1.8")).isTrue();
		assertThat(service.isCompatible(21, "21")).isTrue();
	}
}
