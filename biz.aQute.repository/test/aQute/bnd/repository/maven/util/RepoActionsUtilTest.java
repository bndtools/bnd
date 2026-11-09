package aQute.bnd.repository.maven.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.util.promise.Promises;

import aQute.maven.api.Archive;
import aQute.maven.provider.TrustedChecksums.TrustedChecksumException;

public class RepoActionsUtilTest {

	private static final Archive ARCHIVE = Archive.valueOf("org.example:artifact:1.0");

	@TempDir
	File tmp;

	@Test
	void sourcesWithoutTrustedChecksumDisableOnlyTheSourcesAction() throws Exception {
		File binary = new File(tmp, "artifact-1.0.jar");
		binary.createNewFile();
		Map<String, Runnable> map = new LinkedHashMap<>();
		map.put("Other", () -> {});

		RepoActionsUtil.addSources(ARCHIVE, archive -> {
			if ("sources".equals(archive.classifier)) {
				throw new TrustedChecksumException("no checksum entry for " + archive, null);
			}
			return Promises.resolved(binary);
		}, map);

		assertThat(map).containsKeys("Other", "-Add Sources")
			.doesNotContainKey("Add Sources");
	}

	@Test
	void binaryLookupThatThrowsDisablesTheSourcesAction() throws Exception {
		Map<String, Runnable> map = new LinkedHashMap<>();

		RepoActionsUtil.addSources(ARCHIVE, archive -> {
			throw new TrustedChecksumException("no checksum entry for " + archive, null);
		}, map);

		assertThat(map).containsOnlyKeys("-Add Sources");
	}
}
