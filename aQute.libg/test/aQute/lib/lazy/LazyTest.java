package aQute.lib.lazy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class LazyTest {

	@Test
	public void testBasic() throws Exception {
		try (Lazy<String> l = new Lazy<>(String::new)) {
			assertThat(l.isInitialized()).isFalse();
			assertThat(l.get()).isEmpty();
			assertThat(l.isInitialized()).isTrue();
			assertThat(l.map(String::isEmpty)).isTrue();
		}
	}
}
