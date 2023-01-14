package aQute.bnd.build.classindex;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.classindex.pa.Abc;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.repository.maven.provider.MavenBndRepository;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;

@ExtendWith(SoftAssertionsExtension.class)
public class ClassIndexTest {
	private static final File	home	= IO.getFile("testresources/classindex");
	@InjectTemporaryDirectory
	File						testDir;
	private Workspace			ws;
	@InjectSoftAssertions
	SoftAssertions				softly;

	@BeforeEach
	public void setUp() throws Exception {
		IO.copy(new File(home, "ws"), testDir);

		ws = Workspace.getWorkspace(testDir);

		MavenBndRepository mbr = new MavenBndRepository();
		Map<String, String> map = Map.of("releaseUrl", "https://repo.maven.apache.org/maven2/", //
			"readOnly", "true", //
			"source", IO.collect(IO.getFile(home, "test-1.mvn")));

		mbr.setProperties(map);
		mbr.setRegistry(ws);
		mbr.setReporter(ws);
		ws.addBasicPlugin(mbr);
	}

	@AfterEach
	public void tearDown() {
		IO.close(ws);
	}

	private Map<String, List<BundleId>> search(String string) throws Exception {
		return ws.search(string)
			.unwrap();
	}

	private Map<String, List<BundleId>> search(String pkg, String clazz) throws Exception {
		return ws.search(pkg, clazz)
			.unwrap();
	}

	@Test
	public void classIndex_partialFQN_Test() throws Exception {

		softly.assertThat(search("org.osgi.framework.FooBar"))
			.as("missing class")
			.isEmpty();

		Map<String, List<BundleId>> fqn = search("org.osgi.framework.BundleContext");

		softly.assertThat(fqn)
			.as("with fqn")
			.isNotEmpty();

		softly.assertThat(search("org.osgi.frame.BundleContext"))
			.as("with fuzzy fqn")
			.isEmpty();

		Map<String, List<BundleId>> simple = search("BundleContext");
		softly.assertThat(simple)
			.as("with simple name")
			.isNotEmpty();

		Map<String, List<BundleId>> packg = search("org.osgi.framework");
		softly.assertThat(packg)
			.as("with package")
			.isNotEmpty();

		// This test assumes that the bundles come back in the same order.
		// This assumption holds true for now but may not in the future.
		// However, using "isEqualTo" allows us to test the key as well
		// and not just the values.
		softly.assertThat(fqn)
			.as("fqn and simple return the same")
			.isEqualTo(simple);
		softly.assertThat(fqn.values())
			.as("fqn and packg return the same values (key is different)")
			.containsExactlyInAnyOrderElementsOf(packg.values());
		Map<String, List<BundleId>> fuzzy = search("org.osgi.frame");
		softly.assertThat(fuzzy)
			.as("fuzzy match")
			.isEqualTo(packg);
	}

	static final String LISTENER_INFO = "org.osgi.framework.hooks.service.ListenerHook.ListenerInfo";

	@Test
	public void classIndex_nestedClasses_test() throws Exception {
		Map<String, List<BundleId>> simple = search("BundleContext");
		softly.assertThat(simple)
			.as("with simple name")
			.isNotEmpty();

		Map<String, List<BundleId>> nested = search(LISTENER_INFO);
		softly.assertThat(nested.values())
			.as("fqn of nested type")
			.containsAll(simple.values());
		softly.assertThat(nested.keySet())
			.as("keys")
			.containsExactly(LISTENER_INFO);

		Map<String, List<BundleId>> nestedDirectSearch = search("org.osgi.framework.hooks.service",
			"ListenerHook.ListenerInfo");
		softly.assertThat(nestedDirectSearch)
			.as("direct search nested type")
			.isEqualTo(nested);
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

	final static String CLASS = Descriptors.fqnToBinary(Abc.class.getCanonicalName()) + ".class";

	@Test
	public void classIndex_workspaceClasses_Test() throws Exception {
		Project pr = ws.createProject("test");
		File bin = pr.getSrcOutput();
		File dst = new File(bin, CLASS);
		IO.mkdirs(dst);
		File parentBin_test = new File("bin_test");
		IO.copy(new File(parentBin_test, CLASS), dst);

		pr.setExportPackage(Abc.class.getPackage()
			.getName());
		pr.build();

		String bsn = pr.getBsns()
			.iterator()
			.next();
		List<BundleId> id = Collections.singletonList(new BundleId(bsn, pr.getVersion(bsn)));
		Map<String, List<BundleId>> expected = new MultiMap<>(true);
		expected.put(Abc.class.getName(), id);

		softly.assertThat(search("aQute.bnd.build.classindex.pb", "Abc"))
			.as("missing class")
			.isEmpty();

		softly.assertThat(search("aQute.bnd.build.classindex.pa", "Abc"))
			.as("split match")
			.isEqualTo(expected);

		softly.assertThat(search("aQute.bnd.build.classindex.pa.Abc"))
			.as("partial FQN")
			.isEqualTo(expected);

		softly.assertThat(search("Abc"))
			.as("simple name")
			.isEqualTo(expected);
	}
}
