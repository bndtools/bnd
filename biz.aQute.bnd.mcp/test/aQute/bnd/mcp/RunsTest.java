package aQute.bnd.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunsTest {
	@TempDir
	Path tmp;

	@Test
	void runFilesMustStayInsideWorkspace() throws Exception {
		Path workspace = tmp.resolve("ws");
		Files.createDirectories(workspace.resolve("cnf"));
		Files.writeString(workspace.resolve("cnf/build.bnd"), "");
		Path outside = tmp.resolve("outside.bndrun");
		Files.writeString(outside, "-runfw: org.apache.felix.framework\n");

		try (Runs runs = new Runs()) {
			Runs.ResolveResult result = runs.resolve(new Runs.ResolveSpec(workspace.toString(), outside.toString(), false));

			assertThat(result.diagnostics().errors())
				.singleElement()
				.asString()
				.contains("Not a workspace .bndrun file");
		}
	}

	@Test
	void stoppingUnknownRunIsIdempotentButStatusReportsUnknownRun() {
		try (Runs runs = new Runs()) {
			Runs.RunResult stopped = runs.stop(new Runs.StopSpec("missing"));
			Runs.RunResult status = runs.status(new Runs.StatusSpec("missing"));

			assertThat(stopped.state()).isEqualTo("STOPPED");
			assertThat(stopped.diagnostics().errors()).isEmpty();
			assertThat(status.state()).isEqualTo("UNKNOWN");
			assertThat(status.diagnostics().errors()).singleElement().asString().contains("Unknown run ID");
		}
	}

	@Test
	void readinessDefaultsToHttpAndValidatesInput() {
		assertThat(Runs.Readiness.of(null, null).mode).isEqualTo("http");
		assertThat(Runs.Readiness.of(" NONE ", null).mode).isEqualTo("none");
		assertThat(Runs.Readiness.of("console", "Started").pattern.pattern()).isEqualTo("Started");
		assertThatThrownBy(() -> Runs.Readiness.of("console", " ")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("readyPattern");
		assertThatThrownBy(() -> Runs.Readiness.of("tcp", null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("tcp");
	}

	@Test
	void invalidReadinessIsReportedBeforeAnyResourceIsCreated() throws Exception {
		Path workspace = tmp.resolve("ws");
		Files.createDirectories(workspace.resolve("cnf"));
		Files.writeString(workspace.resolve("cnf/build.bnd"), "");
		Path bndrun = Files.writeString(workspace.resolve("app.bndrun"), "");

		try (Runs runs = new Runs()) {
			Runs.RunResult result = runs.start(new Runs.StartSpec(workspace.toString(), bndrun.toString(), 100, "tcp", null));

			assertThat(result.state()).isEqualTo("FAILED");
			assertThat(result.diagnostics().errors()).singleElement().asString().contains("Unknown readiness 'tcp'");
		}
	}

	@Test
	void consoleReadinessMatchesLinesAcrossAppendChunks() {
		Runs.RingBuffer console = new Runs.RingBuffer(3, Pattern.compile("Started in \\d+ ms"));

		console.append("boot\nStarted in 4");
		assertThat(console.matched()).isFalse();
		console.append("2 ms\nmore\nmore\nmore\n");

		assertThat(console.matched()).isTrue();
		assertThat(console.snapshot()).hasSize(3);
	}
}
