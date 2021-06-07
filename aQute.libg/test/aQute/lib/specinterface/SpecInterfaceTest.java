package aQute.lib.specinterface;

import static aQute.lib.specinterface.SpecInterface.getOptions;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import aQute.lib.io.IO;

public class SpecInterfaceTest {

	interface Foo {
		List<String> _arguments();

		boolean foo();

		File bar();

		Optional<Set<File>> xfiles();

		Optional<Set<File>> yfiles();

		Map<String, String> _properties();
	}

	@Test
	public void testSimple() throws Exception {
		Foo options = getOptions(Foo.class,
			Arrays.asList("foo", "-fb", "bar", "arg", "--yfiles", "testresources/fileset/**", "foo=bar"), IO.work)
				.instance();
		assertThat(options.foo()).isTrue();
		assertThat(options.bar()
			.getName()).isEqualTo("bar");

		assertThat(options.xfiles()).isNotPresent();
		assertThat(options.yfiles()
			.get()).hasSize(10);

		List<String> args = options._arguments();
		assertThat(args).contains("arg")
			.contains("foo")
			.hasSize(2);

		Map<String, String> props = options._properties();
		assertThat(props).isNotNull()
			.containsEntry("foo", "bar");
	}
}
