package aQute.bnd.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
class WorkspacesTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@TempDir
	Path			tmp;

	String			index;
	Workspaces		workspaces;

	@BeforeEach
	void setUp() throws IOException {
		index = Fixtures.index(tmp.resolve("fixtures"));
		workspaces = new Workspaces(List.of());
	}

	@AfterEach
	void tearDown() {
		workspaces.close();
	}

	@Test
	void listsFragmentsOfAnIndex() {
		Workspaces.FragmentList list = workspaces.fragments(List.of(index), false);

		softly.assertThat(list.diagnostics()
			.errors())
			.isEmpty();
		softly.assertThat(list.fragments())
			.extracting(Workspaces.Fragment::name)
			.containsExactly("base", "app");
		softly.assertThat(list.fragments())
			.allMatch(f -> !f.official());
		softly.assertThat(list.fragments()
			.get(1)
			.requires())
			.hasSize(1);
	}

	@Test
	void unreachableIndexIsOnlyAWarning() {
		Workspaces.FragmentList list = workspaces.fragments(List.of(tmp.resolve("missing.bnd")
			.toUri()
			.toString()), false);

		softly.assertThat(list.diagnostics()
			.errors())
			.isEmpty();
		softly.assertThat(list.diagnostics()
			.warnings())
			.singleElement()
			.asString()
			.contains("missing.bnd");
		softly.assertThat(list.fragments())
			.isEmpty();
	}

	@Test
	void createInstallsFragmentWithItsRequirements() {
		Path ws = tmp.resolve("ws");

		Workspaces.Info info = workspaces.create(new Workspaces.Spec(ws.toString(), List.of("app"), List.of(index), true));

		softly.assertThat(info.diagnostics()
			.errors())
			.isEmpty();
		softly.assertThat(info.installedFragments())
			.containsExactly("base", "app");
		softly.assertThat(ws.resolve("cnf/build.bnd"))
			.isRegularFile();
		softly.assertThat(ws.resolve("cnf/ext/base.bnd"))
			.hasContent("base.installed: true");
		softly.assertThat(info.projects())
			.containsExactly("demo");
		softly.assertThat(info.bndruns())
			.containsExactly("demo/launch.bndrun");
	}

	@Test
	void thirdPartyFragmentsNeedTrustAndNothingIsWritten() {
		Path ws = tmp.resolve("ws");

		Workspaces.Info info = workspaces.create(new Workspaces.Spec(ws.toString(), List.of("app"), List.of(index), false));

		softly.assertThat(info.diagnostics()
			.errors())
			.singleElement()
			.asString()
			.contains("trustThirdParty", "base", "app");
		softly.assertThat(ws)
			.doesNotExist();
	}

	@Test
	void unknownFragmentListsAvailableOnesAndNothingIsWritten() {
		Path ws = tmp.resolve("ws");

		Workspaces.Info info = workspaces.create(new Workspaces.Spec(ws.toString(), List.of("nope"), List.of(index), true));

		softly.assertThat(info.diagnostics()
			.errors())
			.singleElement()
			.asString()
			.contains("nope", "base", "app");
		softly.assertThat(ws)
			.doesNotExist();
	}

	@Test
	void createIsIdempotentAndKeepsUserEdits() throws IOException {
		Path ws = tmp.resolve("ws");
		Workspaces.Spec spec = new Workspaces.Spec(ws.toString(), List.of("app"), List.of(index), true);
		workspaces.create(spec);
		Files.writeString(ws.resolve("demo/bnd.bnd"), "# edited\n");

		Workspaces.Info again = workspaces.create(spec);

		softly.assertThat(again.diagnostics()
			.errors())
			.isEmpty();
		softly.assertThat(again.keptFiles())
			.contains("demo/bnd.bnd", "cnf/ext/base.bnd");
		softly.assertThat(ws.resolve("demo/bnd.bnd"))
			.hasContent("# edited");
	}

	@Test
	void createWithoutFragmentsMakesAnEmptyWorkspace() {
		Path ws = tmp.resolve("ws");

		Workspaces.Info info = workspaces.create(new Workspaces.Spec(ws.toString(), null, null, false));

		softly.assertThat(info.diagnostics()
			.errors())
			.isEmpty();
		softly.assertThat(info.projects())
			.isEmpty();
		softly.assertThat(ws.resolve("cnf/build.bnd"))
			.isRegularFile();
	}

	@Test
	void describeRejectsADirectoryWithoutCnf() {
		Workspaces.Info info = workspaces.describe(tmp.toString());

		softly.assertThat(info.diagnostics()
			.errors())
			.singleElement()
			.asString()
			.contains("Not a bnd workspace");
	}

	/**
	 * Builds an offline template index with two zip fragments: 'app' requires
	 * 'base'.
	 */
	static final class Fixtures {

		static String index(Path dir) throws IOException {
			Files.createDirectories(dir);
			URI base = zip(dir.resolve("base.zip"), Map.of("cnf/ext/base.bnd", "base.installed: true"));
			URI app = zip(dir.resolve("app.zip"), Map.of( //
				"demo/bnd.bnd", "Bundle-Version: 1.0.0\n", //
				"demo/launch.bndrun", "-runfw: org.apache.felix.framework\n"));
			Path index = dir.resolve("index.bnd");
			Files.writeString(index, "-workspace-templates: \\\n" //
				+ "  " + base + ";name=base;description=Base, \\\n" //
				+ "  " + app + ";name=app;description=App;require=\"" + base + "\"\n");
			return index.toUri()
				.toString();
		}

		private static URI zip(Path file, Map<String, String> entries) throws IOException {
			try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
				for (Map.Entry<String, String> e : entries.entrySet()) {
					// fragment archives have one root folder, like GitHub zips
					zip.putNextEntry(new ZipEntry("root/" + e.getKey()));
					zip.write(e.getValue()
						.getBytes(StandardCharsets.UTF_8));
					zip.closeEntry();
				}
			}
			return file.toUri();
		}
	}
}
