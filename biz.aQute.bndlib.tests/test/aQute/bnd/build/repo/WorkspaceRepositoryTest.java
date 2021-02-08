package aQute.bnd.build.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.repository.maven.provider.MavenBndRepository;
import aQute.lib.exceptions.ConsumerWithException;
import aQute.lib.io.IO;
import aQute.libg.map.MAP;
import aQute.libg.map.MAP.MAPX;

public class WorkspaceRepositoryTest {
	public static final String	TMPDIR		= "generated/tmp/test";
	@Rule
	public final TestName		testName	= new TestName();
	private File				testDir;
	private File				home		= IO.getFile("testresources/build-repo");

	@Before
	public void setUp() throws IOException {
		testDir = new File(TMPDIR, getClass().getName() + "/" + testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);
	}

	@Test
	public void findprovidersMacroTest() throws Exception {

		test(ws -> {
			String services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)"
			});
			System.out.println(services);
			Parameters parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).containsKeys("org.apache.felix.scr", "org.apache.felix.http.jetty");
			assertThat(parsed.get("org.apache.felix.scr")).containsKey("version");
			assertThat(parsed.get("org.apache.felix.http.jetty")).containsKey("version");

		});
	}

	@Test
	public void findprovidersMacroTestWithStrategy() throws Exception {

		test(ws -> {
			Project project = ws.getProject("p1");
			assertThat(project).isNotNull();
			File[] build = project.build();
			assertThat(build).isNotNull()
				.isNotEmpty();
			assertThat(build[0]).hasName("p1.jar");
			String services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)", "ALL"
			});
			System.out.println(services);
			Parameters parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).containsKeys("org.apache.felix.scr", "org.apache.felix.http.jetty", "p1");
			assertThat(parsed.get("org.apache.felix.scr")).containsKey("version");
			assertThat(parsed.get("org.apache.felix.http.jetty")).containsKey("version");
			assertThat(parsed.get("p1")).containsKey("version");

			services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)", "REPOS"
			});
			System.out.println(services);
			parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).containsKeys("org.apache.felix.scr", "org.apache.felix.http.jetty")
				.doesNotContainKey("p1");
			assertThat(parsed.get("org.apache.felix.scr")).containsKey("version");
			assertThat(parsed.get("org.apache.felix.http.jetty")).containsKey("version");

			services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)", "WORKSPACE"
			});
			System.out.println(services);
			parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).doesNotContainKeys("org.apache.felix.scr", "org.apache.felix.http.jetty")
				.containsKey("p1");
			assertThat(parsed.get("p1")).containsKey("version");

			services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)", "all"
			});
			System.out.println(services);
			parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).containsKeys("org.apache.felix.scr", "org.apache.felix.http.jetty", "p1");
			assertThat(parsed.get("org.apache.felix.scr")).containsKey("version");
			assertThat(parsed.get("org.apache.felix.http.jetty")).containsKey("version");
			assertThat(parsed.get("p1")).containsKey("version");

			services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)", "repos"
			});
			System.out.println(services);
			parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).containsKeys("org.apache.felix.scr", "org.apache.felix.http.jetty")
				.doesNotContainKey("p1");
			assertThat(parsed.get("org.apache.felix.scr")).containsKey("version");
			assertThat(parsed.get("org.apache.felix.http.jetty")).containsKey("version");

			services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)", "workspace"
			});
			System.out.println(services);
			parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).doesNotContainKeys("org.apache.felix.scr", "org.apache.felix.http.jetty")
				.containsKey("p1");
			assertThat(parsed.get("p1")).containsKey("version");
		});
	}

	@Test
	public void findprovidersMacroTestWithWrongStrategy() throws Exception {

		test(ws -> {
			String services = ws._findproviders(new String[] {
				"findproviders", "osgi.service", "(objectClass=*)", "WRONG"
			});
			System.out.println(services);
			Parameters parsed = OSGiHeader.parseHeader(services);
			assertThat(parsed).containsKeys("org.apache.felix.scr", "org.apache.felix.http.jetty");
			assertThat(parsed.get("org.apache.felix.scr")).containsKey("version");
			assertThat(parsed.get("org.apache.felix.http.jetty")).containsKey("version");
			assertThat(ws.getErrors()).anyMatch(error -> error.contains("WRONG"));
		});
	}

	@Test
	public void findprovidersWithStrategyAndNoFilterWithRunRequiresTest() throws Exception {

		test(ws -> {

			Project project = ws.getProject("p1");
			assertThat(project).isNotNull();
			File[] build = project.build();
			assertThat(build).isNotNull()
				.isNotEmpty();
			assertThat(build[0]).hasName("p1.jar");

			Run run = Run.createRun(ws, null);
			run.setProperty("my.plugins", "${findproviders;osgi.service;;WORKSPACE}");
			run.setProperty("-runrequires.extra",
				"${template; my.plugins;osgi.identity;filter:='(osgi.identity=${@})'}");

			String runrequires = run.mergeProperties("-runrequires");
			assertThat(run.check()).isTrue();

			assertThat(runrequires).contains("osgi.identity;filter:='(osgi.identity=p1)'");

		});
	}

	@Test
	public void findprovidersWithRunRequiresTest() throws Exception {

		test(ws -> {
			Run run = Run.createRun(ws, null);
			run.setProperty("my.plugins", "${findproviders;osgi.service}");
			run.setProperty("-runrequires.extra",
				"${template; my.plugins;osgi.identity;filter:='(osgi.identity=${@})'}");

			String runrequires = run.mergeProperties("-runrequires");
			assertThat(run.check()).isTrue();

			assertThat(runrequires).contains(
				"osgi.identity;filter:='(osgi.identity=com.h2database)',osgi.identity;filter:='(osgi.identity=org.apache.aries.async)',osgi.identity;filter:='(osgi.identity=org.apache.felix.configadmin)',osgi.identity;filter:='(osgi.identity=org.apache.felix.coordinator)',osgi.identity;filter:='(osgi.identity=org.apache.felix.eventadmin)',osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.runtime)',osgi.identity;filter:='(osgi.identity=org.apache.felix.http.jetty)',osgi.identity;filter:='(osgi.identity=org.apache.felix.log)',osgi.identity;filter:='(osgi.identity=org.apache.felix.metatype)',osgi.identity;filter:='(osgi.identity=org.apache.felix.scr)',osgi.identity;filter:='(osgi.identity=org.eclipse.ecf.osgi.services.remoteserviceadmin)'");

		});
	}

	void test(ConsumerWithException<Workspace> consumer) throws Exception {
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
			consumer.accept(ws);
		}
	}
}
