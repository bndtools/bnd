package aQute.bnd.osgi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JPMSModuleTest {

	@Test
	void cleanupTest() {
		assertThat(JPMSModule.cleanupName("foo-1.0.jar")).isEqualTo("foo");
		assertThat(JPMSModule.cleanupName("bar-foo.jar")).isEqualTo("bar.foo");
		assertThat(JPMSModule.cleanupName("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"))
			.isEqualTo("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
		assertThat(JPMSModule.cleanupName("foo.jar")).isEqualTo("foo");
		assertThat(JPMSModule.cleanupName("foo")).isEqualTo("foo");
		assertThat(JPMSModule.cleanupName("foo.bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("-foo.bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("-foo.bar-")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("-foo......................bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("foo.--.bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("-------------------------foo.--.bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("-------------------------foo.-🙂-.bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("foo🙂bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName("\n\nfoo🙂bar")).isEqualTo("foo.bar");
		assertThat(JPMSModule.cleanupName(null)).isNull();
	}

}
