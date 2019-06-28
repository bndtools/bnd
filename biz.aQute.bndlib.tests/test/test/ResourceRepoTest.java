package test;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.bnd.resource.repository.ResourceRepositoryImpl;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.service.repository.ResourceRepository.ResourceRepositoryEvent;
import aQute.bnd.service.repository.ResourceRepository.TYPE;
import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class ResourceRepoTest extends TestCase {
	ResourceRepositoryImpl	repoImpl	= new ResourceRepositoryImpl();
	File					tmp			= new File("tmp");

	@Override
	public void setUp() throws Exception {
		IO.delete(tmp);
		tmp.mkdirs();
		repoImpl.setCache(new File(tmp, "cache"));
		repoImpl.setExecutor(Executors.newCachedThreadPool());
		File file = new File(tmp, "index.json");
		file.delete();
		repoImpl.setIndexFile(file);

	}

	@Override
	public void tearDown() throws Exception {
		IO.delete(tmp);
	}

	public void testRepositoryId() throws Exception {
		// Just basic check
		assertEquals(0, repoImpl.filter(null, null)
			.size());

		SearchableRepository.ResourceDescriptor osgi = create("jar/osgi.jar");
		assertNull(repoImpl.getResource(osgi.id));

		// Add it
		boolean add = repoImpl.add("x", osgi);
		assertNotNull(repoImpl.getResource(osgi.id));
		assertEquals(1, repoImpl.filter(null, null)
			.size());
		assertEquals(1, repoImpl.filter("x", null)
			.size());
		assertEquals(0, repoImpl.filter("y", null)
			.size());

		repoImpl.delete("y", osgi.id);
		assertEquals(1, repoImpl.filter("x", null)
			.size());

		repoImpl.add("y", osgi);
		assertEquals(1, repoImpl.filter("x", null)
			.size());
		assertEquals(1, repoImpl.filter("y", null)
			.size());
		assertEquals(1, repoImpl.filter(null, null)
			.size());

		repoImpl.delete("y", osgi.id);
		assertEquals(1, repoImpl.filter("x", null)
			.size());
		assertEquals(0, repoImpl.filter("y", null)
			.size());
		assertEquals(1, repoImpl.filter(null, null)
			.size());

		repoImpl.delete("x", osgi.id);
		assertEquals(0, repoImpl.filter("x", null)
			.size());
		assertEquals(0, repoImpl.filter("y", null)
			.size());
		assertEquals(0, repoImpl.filter(null, null)
			.size());

	}

	public void testBasic() throws Exception {
		// Just basic check
		assertEquals(0, repoImpl.filter(null, null)
			.size());

		SearchableRepository.ResourceDescriptor osgi = create("jar/osgi.jar");
		assertNull(repoImpl.getResource(osgi.id));

		// Add it
		repoImpl.add("x", osgi);

		// See if descriptor exists
		SearchableRepository.ResourceDescriptor t = repoImpl.getResourceDescriptor(osgi.id);
		assertNotNull(t);

		File ff = repoImpl.getResource(osgi.id);
		assertTrue(ff.isFile());

		//
		// Should also be in the list
		//
		List<? extends ResourceDescriptor> list = repoImpl.filter("x", null);
		assertNotNull(list);
		assertEquals(1, list.size());

		//
		// Adding it multiple times
		// is idempotent
		//
		repoImpl.add("x", osgi);
		assertEquals(1, list.size());

		repoImpl.add("y", osgi);
		assertEquals(1, list.size());

		//
		// Check we can delete the cache but this should
		// not delete the index
		//
		repoImpl.deleteCache(t.id);
		list = repoImpl.filter(null, null);
		assertNotNull(list);
		assertEquals(1, list.size());

		//
		// Check download listeners
		//
		final Semaphore s = new Semaphore(0);
		final AtomicBoolean success = new AtomicBoolean(false);

		repoImpl.getResource(t.id, new DownloadListener() {

			@Override
			public void success(File file) throws Exception {
				System.out.println("Success");
				success.set(true);
				s.release();
			}

			@Override
			public void failure(File file, String reason) throws Exception {
				System.out.println("Failure");
				success.set(false);
				s.release();
			}

			@Override
			public boolean progress(File file, int percentage) throws Exception {
				return true;
			}
		});

		s.acquire();
		assertTrue(success.get());

		repoImpl.delete(null, t.id);
		assertEquals(0, repoImpl.filter(null, null)
			.size());

	}

	public void testEvents() throws Exception {
		final AtomicInteger adds = new AtomicInteger();
		final AtomicInteger removes = new AtomicInteger();
		final AtomicInteger starts = new AtomicInteger();
		final AtomicInteger ends = new AtomicInteger();
		final AtomicInteger errors = new AtomicInteger();

		repoImpl.addListener(events -> {
			for (ResourceRepositoryEvent event : events) {
				switch (event.type) {
					case ADD :
						adds.incrementAndGet();
						break;
					case END_DOWNLOAD :
						ends.incrementAndGet();
						break;
					case ERROR :
						errors.incrementAndGet();
						break;
					case REMOVE :
						removes.incrementAndGet();
						break;
					case START_DOWNLOAD :
						starts.incrementAndGet();
						break;

					default :
						errors.incrementAndGet();
						break;
				}
			}
		});

		ResourceDescriptor rd = create("jar/osgi.jar");
		repoImpl.add("x", rd);
		assertEquals(1, adds.get());
		assertEquals(0, removes.get());
		repoImpl.delete(null, rd.id);

		assertEquals(1, adds.get());
		assertEquals(1, removes.get());

		repoImpl.add("x", rd);
		File f = repoImpl.getResource(rd.id);
		assertEquals(2, adds.get());
		assertEquals(1, starts.get());
		assertEquals(1, ends.get());
		assertEquals(0, errors.get());
	}

	public void testMultipleDownloads() throws Exception {

		final Semaphore s = new Semaphore(0);
		final AtomicInteger downloads = new AtomicInteger();

		ResourceDescriptor rd = create("jar/osgi.jar");
		repoImpl.add("x", rd);

		final Semaphore done = new Semaphore(0);

		DownloadListener l = new DownloadListener() {

			@Override
			public void success(File file) throws Exception {
				done.release();
			}

			@Override
			public void failure(File file, String reason) throws Exception {
				System.out.println("failure! " + file + " " + reason);
			}

			@Override
			public boolean progress(File file, int percentage) throws Exception {
				return false;
			}
		};

		repoImpl.addListener(events -> {
			for (ResourceRepositoryEvent event : events) {
				if (event.type == TYPE.START_DOWNLOAD) {
					System.out.println("trying to acquire s");
					s.acquire();
					System.out.println("got it");
					downloads.incrementAndGet();
				}
			}

		});
		File f1 = repoImpl.getResource(rd.id, l);
		File f2 = repoImpl.getResource(rd.id, l);
		assertFalse(f1.isFile());
		assertFalse(f2.isFile());
		assertEquals(0, downloads.get());

		s.release();

		done.acquire(2);
		assertTrue(f1.isFile());
		assertTrue(f2.isFile());
		assertTrue(f1.equals(f2));
		assertEquals(1, downloads.get());
	}

	public void testStore() throws Exception {
		assertEquals(0, repoImpl.filter(null, null)
			.size());
		repoImpl.add("x", create("jar/osgi.jar"));
		assertEquals(1, repoImpl.filter(null, null)
			.size());
		repoImpl = new ResourceRepositoryImpl();
		repoImpl.setCache(new File(tmp, "cache"));
		repoImpl.setExecutor(Executors.newCachedThreadPool());
		repoImpl.setIndexFile(new File(tmp, "index.json"));
		assertEquals(1, repoImpl.filter(null, null)
			.size());
	}

	private SearchableRepository.ResourceDescriptor create(String path) throws NoSuchAlgorithmException, Exception {
		SearchableRepository.ResourceDescriptor rd = new SearchableRepository.ResourceDescriptor();
		File f = IO.getFile(path);
		rd.id = SHA1.digest(f)
			.digest();
		rd.url = f.toURI();
		rd.description = "bla bla";
		rd.bsn = "osgi.core";
		rd.version = new Version("1.0.0");
		return rd;
	}
}
