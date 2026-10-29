package aQute.bnd.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

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
}