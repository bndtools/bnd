package bndtools.m2e;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import bndtools.m2e.MavenImplicitProjectRepository.RepositoryHolder;

public class MavenImplicitProjectRepositoryTest {

	@Test
	public void waitsForInitialRepository() throws Exception {
		RepositoryHolder<Object> holder = new RepositoryHolder<>();
		Object repository = new Object();
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		try {
			executor.schedule(() -> holder.set(repository), 100, TimeUnit.MILLISECONDS);
			assertSame(repository, holder.get());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void returnsLatestRepository() {
		RepositoryHolder<Object> holder = new RepositoryHolder<>();
		Object first = new Object();
		Object second = new Object();

		holder.set(first);
		assertSame(first, holder.get());

		holder.set(second);
		assertSame(second, holder.get());
	}
}
