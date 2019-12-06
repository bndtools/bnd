package aQute.maven.provider;

import java.io.File;
import java.util.List;

import aQute.bnd.http.HttpClient;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.maven.api.MavenScope;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import junit.framework.TestCase;

public class CentralTest extends TestCase {
	private static final String		REPO_URL	= "https://repo.maven.apache.org/maven2/";
	String							tmpName;
	File							local;

	List<MavenBackingRepository>	repo;
	MavenRepository					storage;
	ReporterAdapter					reporter	= new ReporterAdapter(System.err);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tmpName = "generated/tmp/test/" + getClass().getName() + "/" + getName();
		local = IO.getFile(tmpName + "/local");
		reporter.setTrace(true);
		Config config = new Config();
		IO.delete(local);
		local.mkdirs();
		HttpClient client = new HttpClient();
		repo = MavenBackingRepository.create(REPO_URL, reporter, local, client);
		storage = new MavenRepository(local, "central", this.repo, null, client.promiseFactory()
			.executor(), null);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		storage.close();
	}

	public void testBasic() throws Exception {
		Revision r = Program.valueOf("org.lunarray.model.extensions.descriptor", "spring")
			.version("1.0");
		POM pom = storage.getPom(r);
		assertNotNull(pom);
		System.out.println(pom.getDependencies(MavenScope.compile, true));
	}

}
