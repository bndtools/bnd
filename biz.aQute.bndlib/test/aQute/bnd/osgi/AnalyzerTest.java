package aQute.bnd.osgi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.providertype.TestProviderInheritance;
import aQute.bnd.osgi.providertype.packg.TestProviderInheritancePackage;
import aQute.lib.io.IO;

class AnalyzerTest {

	/**
	 * Test provider type inheritance and package
	 */

	@Test
	public void testProviderInheritance() throws Exception {
		try (Builder outer = new Builder()) {
			try (Builder builder = new Builder()) {
				builder.addClasspath(IO.getFile("bin_test"));
				builder.setProperty("-includepackage", "aQute.bnd.osgi.providertype.*");
				builder.build();
				assertThat(builder.check()).isTrue();

				assertThat(isProvider(builder, TestProviderInheritance.Top.class)).isTrue();
				assertThat(isProvider(builder, TestProviderInheritance.Middle.class)).isTrue();
				assertThat(isProvider(builder, TestProviderInheritance.Bottom.class)).isTrue();
				assertThat(isProvider(builder, TestProviderInheritance.None.class)).isFalse();

				assertThat(isProvider(builder, TestProviderInheritancePackage.Top.class)).isTrue();
				assertThat(isProvider(builder, TestProviderInheritancePackage.Middle.class)).isTrue();
				assertThat(isProvider(builder, TestProviderInheritancePackage.Bottom.class)).isTrue();
				assertThat(isProvider(builder, TestProviderInheritancePackage.None.class)).isTrue();
			}
		}
	}

	private boolean isProvider(Builder builder, Class<?> type) {
		return builder.isProvider(builder.getTypeRefFromFQN(type.getName()));
	}

}
