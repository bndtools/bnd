package aQute.maven.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.maven.api.MavenScope;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;

public class CentralTest {
	private static final String		REPO_URL	= "https://repo.maven.apache.org/maven2/";
	File							local;

	List<MavenBackingRepository>	repo;
	MavenRepository					storage;
	ReporterAdapter					reporter	= new ReporterAdapter(System.err);

	@BeforeEach
	protected void setUp(@InjectTemporaryDirectory
	String tmpName) throws Exception {
		local = IO.getFile(tmpName + "/local");
		reporter.setTrace(true);
		Config config = new Config();
		local.mkdirs();
		HttpClient client = new HttpClient();
		repo = MavenBackingRepository.create(REPO_URL, reporter, local, client);
		storage = new MavenRepository(local, "central", this.repo, null, client.promiseFactory()
			.executor(), null);
	}

	@AfterEach
	protected void tearDown() throws Exception {
		storage.close();
	}

	@Test
	public void testBasic() throws Exception {
		Revision r = Program.valueOf("org.lunarray.model.extensions.descriptor", "spring")
			.version("1.0");
		POM pom = storage.getPom(r);
		assertNotNull(pom);
		System.out.println(pom.getDependencies(MavenScope.compile, true));
	}

}
