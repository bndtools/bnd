package aQute.libg.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import aQute.lib.io.IO;

public class CommandTest {

	@Test
	public void testExecution() throws Exception {
		Command c = new Command();
		c.add(IO.getJavaExecutablePath("java"));
		c.add("-Dx=\"foo bar\"");
		c.add("-version");
		assertThat(c.getArguments()).containsExactly(IO.getJavaExecutablePath("java"), "-Dx=\"foo bar\"", "-version");
		assertThat(c.execute(System.out, System.err)).isZero();
	}

	@Test
	public void testFull() throws Exception {

		Command c = new Command("app -Dx=\"foo bar\" -version");
		assertThat(c.getArguments()).containsExactly("app", "-Dx=\"foo bar\"", "-version");

		c = new Command().full("app -Dx=\"foo bar\" -version");
		assertThat(c.getArguments()).containsExactly("app", "-Dx=\"foo bar\"", "-version");
	}

	@Test
	public void testWindowsQuoting() throws Exception {
		assertThat(Command.needsWindowsQuoting("a\\\"b")).isTrue();
		assertThat(Command.windowsQuote("a\\\"b")).isEqualTo("\"a\\\\\\\"b\"");

		assertThat(Command.needsWindowsQuoting("abc")).isFalse();
		assertThat(Command.windowsQuote("abc")).isEqualTo("abc");

		assertThat(Command.needsWindowsQuoting("ab c")).isTrue();
		assertThat(Command.windowsQuote("ab c")).isEqualTo("\"ab c\"");

		assertThat(Command.needsWindowsQuoting("a\\\\b")).isTrue();
		assertThat(Command.windowsQuote("a\\\\b")).isEqualTo("\"a\\\\b\"");

		assertThat(Command.needsWindowsQuoting("a\\\\")).isTrue();
		assertThat(Command.windowsQuote("a\\\\")).isEqualTo("\"a\\\\\\\\\"");

		assertThat(Command.needsWindowsQuoting("d\"e f\"g")).isTrue();
		assertThat(Command.windowsQuote("d\"e f\"g")).isEqualTo("\"d\\\"e f\\\"g\"");

		assertThat(Command.needsWindowsQuoting("-Dlauncher.path=C:\\foo bar\\some.jar")).isTrue();
		assertThat(Command.windowsQuote("-Dlauncher.path=C:\\foo bar\\some.jar"))
			.isEqualTo("\"-Dlauncher.path=C:\\foo bar\\some.jar\"");
	}
}
