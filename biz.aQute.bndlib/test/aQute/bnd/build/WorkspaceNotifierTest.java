package aQute.bnd.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import aQute.bnd.build.api.BuildInfo;
import aQute.bnd.build.api.OnWorkspace;

public class WorkspaceNotifierTest {

	final Workspace			ws	= Workspace.createDefaultWorkspace();
	final WorkspaceNotifier	wsn	= new WorkspaceNotifier(ws);

	public WorkspaceNotifierTest() throws Exception {
		wsn.mute = false;
	}

	@Test
	public void testSimple() throws InterruptedException {
		OnWorkspace on = wsn.on("testSimple");
		Semaphore s = new Semaphore(0);

		on.initial((ws) -> {
			s.release();
		});
		Awaitility.with()
			.until(s::tryAcquire);

		wsn.initialized();
		Awaitility.with()
			.until(s::tryAcquire);

		on.projects(l -> {
			s.release();
		});
		assertThat(s.tryAcquire(500, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(s.availablePermits()).isEqualTo(0);

		wsn.projects(Collections.emptyList());

		Awaitility.with()
			.until(s::tryAcquire);
	}

	@Test
	public void testProjects() throws InterruptedException {
		OnWorkspace on = wsn.on("testProject");
		Semaphore s1 = new Semaphore(0);
		Semaphore s2 = new Semaphore(0);
		Semaphore s3 = new Semaphore(0);

		on.projects(l -> {
			s1.release();
		});
		assertThat(s1.tryAcquire(500, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(s1.availablePermits()).isEqualTo(0);

		wsn.projects(Collections.emptyList());

		Awaitility.with()
			.until(s1::tryAcquire);

		on.projects(l -> {
			s2.release();
		});
		Awaitility.with()
			.until(s2::tryAcquire);

		wsn.initialized();
		on.projects(l -> {
			s3.release();
		});
		assertThat(s3.tryAcquire(500, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(s3.availablePermits()).isEqualTo(0);
	}

	@Test
	public void testBuild() throws InterruptedException {
		OnWorkspace on = wsn.on("testBuild");
		Semaphore s1 = new Semaphore(0);
		Semaphore s2 = new Semaphore(0);
		Semaphore s3 = new Semaphore(0);
		BuildInfo buildInfo = Mockito.mock(BuildInfo.class);

		on.build(l -> {
			s1.release();
		});
		assertThat(s1.tryAcquire(500, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(s1.availablePermits()).isEqualTo(0);

		wsn.build(buildInfo);

		Awaitility.with()
			.until(s1::tryAcquire);

		on.build(l -> {
			s2.release();
		});
		Awaitility.with()
			.until(s2::tryAcquire);

		wsn.initialized();
		on.build(l -> {
			s3.release();
		});
		assertThat(s3.tryAcquire(500, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(s3.availablePermits()).isEqualTo(0);
	}

	@Test
	public void testRepos() throws InterruptedException {
		OnWorkspace on = wsn.on("testRepos");
		Semaphore s1 = new Semaphore(0);
		Semaphore s2 = new Semaphore(0);
		Semaphore s3 = new Semaphore(0);

		on.repositoriesReady(l -> {
			s1.release();
		});
		assertThat(s1.tryAcquire(500, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(s1.availablePermits()).isEqualTo(0);

		wsn.repos(Collections.emptyList());

		Awaitility.with()
			.until(s1::tryAcquire);

		on.repositoriesReady(l -> {
			s2.release();
		});
		Awaitility.with()
			.until(s2::tryAcquire);

		wsn.initialized();
		on.repositoriesReady(l -> {
			s3.release();
		});
		assertThat(s3.tryAcquire(500, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(s3.availablePermits()).isEqualTo(0);
	}
}
