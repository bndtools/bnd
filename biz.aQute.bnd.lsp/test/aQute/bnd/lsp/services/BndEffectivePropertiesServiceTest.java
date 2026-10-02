package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BndEffectivePropertiesServiceTest {
	@TempDir
	Path temporary;

	@Test
	void unsavedSnapshotReplacesDeletedPropertiesAndLoadsIncludes() throws Exception {
		Files.createDirectories(temporary.resolve("cnf"));
		Files.writeString(temporary.resolve("cnf/build.bnd"), "inherited: parent\noverride: parent\n");
		Path project = Files.createDirectory(temporary.resolve("project"));
		Files.writeString(project.resolve("included.bnd"), "included: disk\n");
		Path target = project.resolve("bnd.bnd");
		String saved = "deleted: stale\noverride: local\n";
		Files.writeString(target, saved);
		String text = "-include: included.bnd\nadded: fresh\nexpanded: ${added}\n";
		var result = new BndEffectivePropertiesService().evaluate(target.toFile(), text, 2, true, false);
		Map<String, String> values = result.rows().stream()
			.collect(Collectors.toMap(BndEffectivePropertiesService.Row::key, BndEffectivePropertiesService.Row::value));
		assertThat(values).containsEntry("added", "fresh").containsEntry("expanded", "fresh")
			.containsEntry("inherited", "parent").containsEntry("override", "parent")
			.containsEntry("included", "disk").doesNotContainKey("deleted");
		assertThat(Files.readString(target)).isEqualTo(saved);
		assertThat(result.documentVersion()).isEqualTo(2);
		assertThat(result.dependencies()).contains(project.resolve("included.bnd").toFile().toURI().toString());
	}

	@Test
	void mergesOnlyInstructionsAndSupportedHeaders() throws Exception {
		Path target = temporary.resolve("launch.bndrun");
		Files.writeString(target, "");
		String text = "-standalone: true\nvalue: fresh\n-test: alpha\n-test.extra: beta\n"
			+ "ordinary.one: ${value}\nordinary.two: second\n";
		var service = new BndEffectivePropertiesService();
		var merged = service.evaluate(target.toFile(), text, 1, true, true);
		assertThat(merged.rows()).anySatisfy(row -> {
			assertThat(row.key()).isEqualTo("-test");
			assertThat(row.value()).isEqualTo("alpha,beta");
		});
		assertThat(merged.rows()).extracting(BndEffectivePropertiesService.Row::key)
			.contains("ordinary.one", "ordinary.two").doesNotContain("-test.extra");
		var raw = service.evaluate(target.toFile(), text, 1, false, true);
		assertThat(raw.merged()).isFalse();
		assertThat(raw.rows()).anySatisfy(row -> {
			assertThat(row.key()).isEqualTo("ordinary.one");
			assertThat(row.value()).isEqualTo("${value}");
		});
	}

	@Test
	void subBundleAndWorkspaceSnapshotsKeepTheirContext() throws Exception {
		Files.createDirectories(temporary.resolve("cnf/ext"));
		Path build = temporary.resolve("cnf/build.bnd");
		Files.writeString(build, "workspace.value: disk\n");
		Path project = Files.createDirectory(temporary.resolve("project"));
		Files.writeString(project.resolve("bnd.bnd"), "-sub: *.bnd\nproject.value: inherited\n");
		Path sub = project.resolve("sub.bnd");
		Files.writeString(sub, "deleted: disk\n");
		var service = new BndEffectivePropertiesService();
		var result = service.evaluate(sub.toFile(), "new: ${project.value}\n", 1, true, false);
		assertThat(result.rows()).anySatisfy(row -> {
			assertThat(row.key()).isEqualTo("new");
			assertThat(row.value()).isEqualTo("inherited");
		});
		assertThat(result.rows()).extracting(BndEffectivePropertiesService.Row::key).doesNotContain("deleted");
		var workspace = service.evaluate(build.toFile(), "workspace.value: unsaved\n", 2, true, false);
		assertThat(workspace.rows()).anySatisfy(row -> {
			assertThat(row.key()).isEqualTo("workspace.value");
			assertThat(row.value()).isEqualTo("unsaved");
		});
		assertThat(Files.readString(build)).isEqualTo("workspace.value: disk\n");
	}
}
