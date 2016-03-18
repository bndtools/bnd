package aQute.maven.bnd;

import java.io.File;
import java.util.concurrent.Callable;

import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.maven.repo.provider.LocalRepoWatcher;
import junit.framework.TestCase;

public class LocalRepoWatcherTest extends TestCase {
	File tmp = IO.getFile("generated/tmp");

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IO.delete(tmp);
		tmp.mkdirs();
		IO.copy(IO.getFile("testresources/mavenrepo"), tmp);
	}

	public void testBasic() throws Exception {

		LocalRepoWatcher lrw = new LocalRepoWatcher(null, tmp, new ReporterAdapter(System.out),
				new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						return null;
					}
				});
	}
}
