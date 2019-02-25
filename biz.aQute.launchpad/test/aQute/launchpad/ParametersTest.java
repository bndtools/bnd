package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

public class ParametersTest {

	@Test
	public void testParameters() {
		Parameters parameters = new Parameters("   foo    ;  \tbar ; abc = \"abc  x\"; def=def,    foo; bar2 = bar2");

		assertThat(parameters.keySet()).containsExactlyInAnyOrder("foo", "bar", "foo~");

		Map<String, String> attrs = parameters.get("foo");

		assertThat(attrs).isEqualTo(parameters.get("bar"));

		assertThat(attrs.get("abc")).isEqualTo("abc  x");
		assertThat(attrs.get("def")).isEqualTo("def");

		assertThat(parameters.get("foo~")
			.get("bar2")).isEqualTo("bar2");
	}
}
