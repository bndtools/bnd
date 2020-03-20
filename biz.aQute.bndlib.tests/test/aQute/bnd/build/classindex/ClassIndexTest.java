package aQute.bnd.build.classindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.repository.maven.provider.MavenBndRepository;
import aQute.lib.io.IO;
import aQute.libg.map.MAP;
import aQute.libg.map.MAP.MAPX;

public class ClassIndexTest {
	public static final String	TMPDIR		= "generated/tmp/test";
	@Rule
	public final TestName		testName	= new TestName();
	private File				testDir;
	private File				home		= IO.getFile("testresources/classindex");

	@Before
	public void setUp() throws IOException {
		testDir = new File(TMPDIR, getClass().getName() + "/" + testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);
	}

	@Test
	public void classIndexTest() throws Exception {
		IO.copy(new File(home, "ws"), testDir);

		try (Workspace ws = Workspace.getWorkspace(testDir)) {
			MavenBndRepository mbr = new MavenBndRepository();
			MAPX<String, String> map = MAP.$("releaseUrl", "https://repo1.maven.org/maven2/")
				.$("readOnly", "true")
				.$("source", IO.collect(IO.getFile(home, "test-1.mvn")));

			mbr.setProperties(map);
			mbr.setRegistry(ws);
			mbr.setReporter(ws);
			ws.addBasicPlugin(mbr);

			Map<String, List<BundleId>> foobar = ws.search("org.osgi.framework.FooBar")
				.unwrap();
			assertThat(foobar).isEmpty();

			Map<String, List<BundleId>> fqn = ws.search("org.osgi.framework.BundleContext")
				.unwrap();
			Map<String, List<BundleId>> simple = ws.search("BundleContext")
				.unwrap();
			Map<String, List<BundleId>> packg = ws.search("org.osgi.framework")
				.unwrap();

			assertThat(fqn).isNotEmpty();
			assertThat(simple).isNotEmpty();
			assertThat(packg).isNotEmpty();
			assertThat(fqn.values()).containsAll(simple.values());
			assertThat(fqn.values()).containsAll(packg.values());
		}
	}
}
