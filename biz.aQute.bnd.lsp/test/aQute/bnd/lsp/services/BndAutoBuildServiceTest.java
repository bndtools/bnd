package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.lsp.models.BndBuildStatus;

class BndAutoBuildServiceTest {
	@TempDir
	Path							temporary;

	private BndWorkspaceManager		manager;
	private ScheduledExecutorService	scheduler;
	private BndAutoBuildService		service;
	private final BlockingQueue<BndBuildStatus> statuses = new LinkedBlockingQueue<>();

	@BeforeEach
	void setUp() throws Exception {
		Files.createDirectories(temporary.resolve("cnf"));
		Files.writeString(temporary.resolve("cnf/build.bnd"), "");
		project("p", "");
		project("q", "-buildpath: p\n");
		project("r", "");

		manager = new BndWorkspaceManager();
		scheduler = Executors.newScheduledThreadPool(2);
		service = new BndAutoBuildService(manager, scheduler, 50);
		service.configure(true, true);
		service.setListener(statuses::add);
	}

	@AfterEach
	void tearDown() {
		service.close();
		scheduler.shutdownNow();
		manager.shutdown();
	}

	private void project(String name, String extra) throws Exception {
		Path dir = Files.createDirectories(temporary.resolve(name));
		Files.writeString(dir.resolve("bnd.bnd"), "Bundle-Version: 1.0.0\n-includeresource: " + name + ".txt\n" + extra);
		Files.writeString(dir.resolve(name + ".txt"), name);
	}

	private File jar(String name) {
		return temporary.resolve(name + "/generated/" + name + ".jar")
			.toFile();
	}

	/** The summary of the next finished run, ignoring progress reports. */
	private BndBuildStatus.Summary awaitRun() throws Exception {
		while (true) {
			BndBuildStatus status = statuses.poll(60, TimeUnit.SECONDS);
			assertThat(status).as("a build run")
				.isNotNull();
			if (status.summary != null) {
				return status.summary;
			}
		}
	}

	private void changed(String relative) {
		service.filesChanged(List.of(temporary.resolve(relative)
			.toFile()));
	}

	@Test
	void changedProjectAndItsDependentsAreBuilt() throws Exception {
		changed("p/p.txt");

		BndBuildStatus.Summary summary = awaitRun();

		assertThat(summary.built).containsExactly("p", "q");
		assertThat(jar("p")).exists();
		assertThat(jar("q")).exists();
		assertThat(jar("r")).doesNotExist();
	}

	@Test
	void unrelatedProjectsAreLeftAlone() throws Exception {
		changed("cnf/build.bnd");
		assertThat(awaitRun().built).containsExactlyInAnyOrder("p", "q", "r");
		long old = System.currentTimeMillis() - 60_000;
		assertThat(jar("r").setLastModified(old)).isTrue();
		assertThat(jar("q").setLastModified(old)).isTrue();

		changed("p/p.txt");

		assertThat(awaitRun().built).containsExactly("p", "q");
		assertThat(jar("r").lastModified()).isLessThanOrEqualTo(old + 1000);
	}

	@Test
	void workspaceConfigurationChecksEveryProject() throws Exception {
		Files.createDirectories(temporary.resolve("cnf/ext"));
		changed("cnf/ext/repos.bnd");

		assertThat(awaitRun().built).containsExactlyInAnyOrder("p", "q", "r");
	}

	@Test
	void buildOutputDoesNotTriggerBuilds() throws Exception {
		changed("p/generated/p.jar");
		changed("p/.settings/org.eclipse.jdt.core.prefs");
		changed("p/launch.bndrun");
		changed("cnf/cache/anything.bnd");

		assertThat(statuses.poll(500, TimeUnit.MILLISECONDS)).isNull();
		assertThat(jar("p")).doesNotExist();
	}

	@Test
	void changesAreCollectedIntoOneRun() throws Exception {
		changed("p/p.txt");
		changed("r/r.txt");
		changed("p/bnd.bnd");

		assertThat(awaitRun().built).containsExactlyInAnyOrder("p", "q", "r");
		assertThat(statuses.poll(500, TimeUnit.MILLISECONDS)).isNull();
	}

	@Test
	void switchedOffNothingIsBuilt() throws Exception {
		service.setEnabled(false);
		assertThat(statuses.poll(5, TimeUnit.SECONDS)).satisfies(s -> {
			assertThat(s.state).isEqualTo(BndBuildStatus.STATE_DISABLED);
			assertThat(s.enabled).isFalse();
		});

		changed("p/p.txt");

		assertThat(statuses.poll(500, TimeUnit.MILLISECONDS)).isNull();
		assertThat(jar("p")).doesNotExist();
	}

	@Test
	void untrustedWorkspaceIsNeverBuilt() throws Exception {
		service.configure(true, false);

		changed("p/p.txt");

		assertThat(statuses.poll(500, TimeUnit.MILLISECONDS)).isNull();
		assertThat(jar("p")).doesNotExist();
		BndBuildStatus status = service.currentStatus();
		assertThat(status.state).isEqualTo(BndBuildStatus.STATE_DISABLED);
		assertThat(status.reason).isEqualTo(BndBuildStatus.REASON_UNTRUSTED);
	}

	@Test
	void failedProjectIsReportedAndSkipsItsDependents() throws Exception {
		Files.writeString(temporary.resolve("p/bnd.bnd"), "Bundle-Version: 1.0.0\n-includeresource: missing.txt\n");

		changed("p/bnd.bnd");
		BndBuildStatus.Summary summary = awaitRun();

		assertThat(summary.failed).containsExactly("p");
		assertThat(summary.skipped).containsExactly("q");
		assertThat(summary.errors).isPositive();
	}
}
