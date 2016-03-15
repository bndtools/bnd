package aQute.maven.repo;

import java.io.File;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.url.BasicAuthentication;
import aQute.lib.io.IO;
import aQute.maven.repo.provider.RemoteRepo;
import biz.aQute.http.testservers.HttpTestServer.Config;
import junit.framework.TestCase;

public class NexusTest extends TestCase {

	File		local	= IO.getFile("generated/local");
	RemoteRepo	repo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = new Config();
		IO.delete(local);
		local.mkdirs();
		HttpClient httpClient = new HttpClient();
		httpClient.addURLConnectionHandler(new BasicAuthentication("deployment", "deployment123", Workspace.log));
		repo = new RemoteRepo(httpClient, "http://localhost:8081/nexus/content/repositories/snapshots/");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testShutup() {

	}

	// public void testBasic() throws Exception {
	// File localFoobar = IO.getFile(this.local, "some.jar");
	// File localFoobarSha1 = IO.getFile(this.local, "some.jar.sha1");
	// File localFoobarMD5 = IO.getFile(this.local, "some.jar.md5");
	// String path = "foo/bar/1-SNAPSHOT/bar-1-SNAPSHOT.jar";
	//
	// repo.delete(path);
	//
	// localFoobar.getParentFile().mkdirs();
	//
	// assertFalse(repo.fetch(path, localFoobar));
	//
	// IO.store("bla", localFoobar);
	// repo.store(localFoobar, path);
	//
	// localFoobar.delete();
	// assertTrue(repo.fetch(path, localFoobar));
	// }
}
