package aQute.bnd.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class ProjectJavacTest {
	@InjectTemporaryDirectory
	File temporaryDirectory;

	@Test
	public void usesReleaseForEqualNormalizedSourceAndTarget() throws Exception {
		assertThat(release(null, "1.8", "8", "", true, false, 17)).hasValue(8);
	}

	@Test
	public void retainsLegacyArgumentsWhenReleaseIsIncompatible() throws Exception {
		assertThat(release(null, "11", "17", "", true, false, 17)).isEmpty();
	}

	@Test
	public void explicitReleaseWinsAndEmptyValueOptOuts() throws Exception {
		assertThat(release("17", "1.8", "1.8", "", true, false, 8)).hasValue(17);
		assertThat(release("", "17", "17", "", true, false, 17)).isEmpty();
	}

	@Test
	public void preservesExplicitReleaseOption() throws Exception {
		assertThat(release(null, "17", "17", "", true, true, 17)).isEmpty();
	}

	@Test
	public void doesNotCombineReleaseWithLegacyClasspathOptions() {
		org.assertj.core.api.Assertions.assertThatThrownBy(
			() -> JavacCommand.validateJavacReleaseOptions(true, false, false, "compact1", true))
			.isInstanceOf(IllegalArgumentException.class);
		org.assertj.core.api.Assertions.assertThatThrownBy(
			() -> JavacCommand.validateJavacReleaseOptions(false, true, true, "", true))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void retainsLegacyArgumentsForUnsupportedReleaseConditions() throws Exception {
		assertThat(release(null, "1.6", "1.6", "", true, false, 17)).isEmpty();
		assertThat(release(null, "abc", "abc", "", true, false, 17)).isEmpty();
		assertThat(release(null, "17", "17", "compact1", true, false, 17)).isEmpty();
		assertThat(release(null, "17", "17", "", false, false, 17)).isEmpty();
		assertThat(release(null, "17", "17", "", true, false, 8)).isEmpty();
	}

	@Test
	public void rejectsNewerApiWhenTargetingJava8() throws Exception {
		if (Runtime.version()
			.feature() < 9) {
			return;
		}

		IO.copy(IO.getFile("testresources/ws-multirelease"), temporaryDirectory);
		try (Workspace workspace = new Workspace(temporaryDirectory)) {
			Project project = workspace.getProject("multirelease.main");
			File source = project.getFile("src/hello/UsesNewerApi.java");
			IO.store("""
				package hello;
				import java.util.List;
				public class UsesNewerApi {
					List<String> values = List.of();
				}
				""", source);

			project.compile(false);

			assertThat(project.check()).isFalse();
		}
	}

	private OptionalInt release(String configuredRelease, String source, String target, String profile,
		boolean emptyBootclasspath, boolean hasReleaseOption, int javacVersion) {
		return JavacCommand.getJavacRelease(configuredRelease, source, target, profile, emptyBootclasspath,
			hasReleaseOption,
			javacVersion);
	}
}
