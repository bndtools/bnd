package aQute.bnd.build.classindex;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
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
	public static final String			TMPDIR		= "generated/tmp/test";
	private static final File			home		= IO.getFile("testresources/classindex");
	private File						testDir;
	private Workspace					ws;

	@Rule
	public final TestName				testName	= new TestName();

	@Rule
	public final JUnitSoftAssertions	softly		= new JUnitSoftAssertions();

	@Before
	public void setUp() throws Exception {
		testDir = new File(TMPDIR, getClass().getName() + "/" + testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);

		IO.copy(new File(home, "ws"), testDir);

		ws = Workspace.getWorkspace(testDir);

		MavenBndRepository mbr = new MavenBndRepository();
		MAPX<String, String> map = MAP.$("releaseUrl", "https://repo1.maven.org/maven2/")
			.$("readOnly", "true")
			.$("source", IO.collect(IO.getFile(home, "test-1.mvn")));

		mbr.setProperties(map);
		mbr.setRegistry(ws);
		mbr.setReporter(ws);
		ws.addBasicPlugin(mbr);
	}

	@After
	public void tearDown() {
		IO.close(ws);
	}

	@Test
	public void classIndex_partialFQN_Test() throws Exception {

		Map<String, List<BundleId>> foobar = ws.search("org.osgi.framework.FooBar")
			.unwrap();
		softly.assertThat(foobar)
			.as("missing class")
			.isEmpty();

		Map<String, List<BundleId>> fqn = ws.search("org.osgi.framework.BundleContext")
			.unwrap();
		Map<String, List<BundleId>> simple = ws.search("BundleContext")
			.unwrap();
		Map<String, List<BundleId>> packg = ws.search("org.osgi.framework")
			.unwrap();

		softly.assertThat(fqn)
			.as("with fqn")
			.isNotEmpty();
		softly.assertThat(simple)
			.as("with simple name")
			.isNotEmpty();
		softly.assertThat(packg)
			.as("with package")
			.isNotEmpty();
		softly.assertThat(fqn.values())
			.as("fqn and simple return the same")
			.containsAll(simple.values());
		softly.assertThat(fqn.values())
			.as("fqn and packg return the same")
			.containsAll(packg.values());
	}

	@Test
	public void classIndex_qualifiedSearch_Test() throws Exception {

		Map<String, List<BundleId>> foobar = ws.search("org.osgi.framework", "FooBar")
			.unwrap();
		softly.assertThat(foobar)
			.as("missing class")
			.isEmpty();

		Map<String, List<BundleId>> bc = ws.search("org.osgi.framework", "BundleContext")
			.unwrap();

		Map<String, List<BundleId>> fqBC = ws.search("org.osgi.framework.BundleContext")
			.unwrap();

		Map<String, List<BundleId>> nonBC = ws.search("org.osgi", "framework.BundleContext")
			.unwrap();

		softly.assertThat(bc)
			.as("with match")
			.isNotEmpty()
			.isEqualTo(fqBC);
		softly.assertThat(nonBC)
			.as("with no match")
			.isEmpty();
	}
}
