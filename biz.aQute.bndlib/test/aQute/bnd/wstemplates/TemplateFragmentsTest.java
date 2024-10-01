package aQute.bnd.wstemplates;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Builder;
import aQute.bnd.result.Result;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateInfo;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateUpdater;
import aQute.bnd.wstemplates.FragmentTemplateEngine.Update;
import aQute.lib.io.IO;

class TemplateFragmentsTest {
	@TempDir
	static File	wsDir;
	@TempDir
	static File	zip;

	@Test
	void testId() {
		TemplateID defaultId = TemplateID.from("");
		assertThat(defaultId.organisation()).isEqualTo("bndtools");
		assertThat(defaultId.repository()).isEqualTo("workspace");
		assertThat(defaultId.path()).isEqualTo("");
		assertThat(defaultId.ref()).isEqualTo("master");

		TemplateID withOrg = TemplateID.from("acme");
		assertThat(withOrg.organisation()).isEqualTo("acme");
		assertThat(withOrg.repository()).isEqualTo("workspace");
		assertThat(withOrg.path()).isEqualTo("");
		assertThat(withOrg.ref()).isEqualTo("master");

		TemplateID withOrgRef = TemplateID.from("acme#main");
		assertThat(withOrgRef.organisation()).isEqualTo("acme");
		assertThat(withOrgRef.repository()).isEqualTo("workspace");
		assertThat(withOrgRef.path()).isEqualTo("");
		assertThat(withOrgRef.ref()).isEqualTo("main");

		TemplateID withOrgRepo = TemplateID.from("acme/template");
		assertThat(withOrgRepo.organisation()).isEqualTo("acme");
		assertThat(withOrgRepo.repository()).isEqualTo("template");
		assertThat(withOrgRepo.path()).isEqualTo("");
		assertThat(withOrgRepo.ref()).isEqualTo("master");

		TemplateID withOrgRepoRef = TemplateID.from("acme/template#foo/bar");
		assertThat(withOrgRepoRef.organisation()).isEqualTo("acme");
		assertThat(withOrgRepoRef.repository()).isEqualTo("template");
		assertThat(withOrgRepoRef.path()).isEqualTo("");
		assertThat(withOrgRepoRef.ref()).isEqualTo("foo/bar");

		TemplateID withOrgRepoPath = TemplateID.from("acme/template/foo/bar");
		assertThat(withOrgRepoPath.organisation()).isEqualTo("acme");
		assertThat(withOrgRepoPath.repository()).isEqualTo("template");
		assertThat(withOrgRepoPath.path()).isEqualTo("foo/bar");
		assertThat(withOrgRepoPath.ref()).isEqualTo("master");

		TemplateID withOrgRepoPathRef = TemplateID.from("acme/template/foo/bar/y#feature/bar/x");
		assertThat(withOrgRepoPathRef.organisation()).isEqualTo("acme");
		assertThat(withOrgRepoPathRef.repository()).isEqualTo("template");
		assertThat(withOrgRepoPathRef.path()).isEqualTo("foo/bar/y");
		assertThat(withOrgRepoPathRef.ref()).isEqualTo("feature/bar/x");

		TemplateID withOrgRepoPathxRef = TemplateID.from("acme/template/foo/bar/y/#feature/bar/x");
		assertThat(withOrgRepoPathxRef.organisation()).isEqualTo("acme");
		assertThat(withOrgRepoPathxRef.repository()).isEqualTo("template");
		assertThat(withOrgRepoPathxRef.path()).isEqualTo("foo/bar/y");
		assertThat(withOrgRepoPathxRef.ref()).isEqualTo("feature/bar/x");

		TemplateID other = TemplateID.from("file://z.zip");
		assertThat(other.other()).isEqualTo("file://z.zip");

		// tests a template at a specific commit SHA, which we do for 3rd party
		// templates
		// https://github.com/org/repo/tree/commitSHA/subfolder/workspace-template
		// translates to the followingg templateID:
		// org/repo/tree/commitSHA/subfolder/workspace-template/
		TemplateID commitSHAUrl = TemplateID
			.from("org/repo/subfolder/workspace-template#commitSHA");
		assertThat(commitSHAUrl.organisation()).isEqualTo("org");
		assertThat(commitSHAUrl.repository()).isEqualTo("repo");
		assertThat(commitSHAUrl.path()).isEqualTo("subfolder/workspace-template");
		assertThat(commitSHAUrl.repoUrl())
			.isEqualTo("https://github.com/org/repo/tree/commitSHA/subfolder/workspace-template");

		TemplateID defaultmasterUrl = TemplateID.from("org/repo/subfolder/workspace-template");
		assertThat(defaultmasterUrl.repoUrl())
			.isEqualTo("https://github.com/org/repo/tree/master/subfolder/workspace-template");

		TemplateID defaultMasterUrl = TemplateID.from("org/repo/subfolder/workspace-template#master");
		assertThat(defaultMasterUrl.repoUrl())
			.isEqualTo("https://github.com/org/repo/tree/master/subfolder/workspace-template");

	}

	@Test
	void testRemote() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);
			File a = makeJar("a.zip", """
				-includeresource prefix/cnf/build.bnd;literal="# a\\n"
				""");

			// use an archived repository
			Result<List<TemplateInfo>> result = tfs.read("-workspace-templates " + a.toURI() + ";name=a;description=A,"
				+ "bndtools/workspace-templates/gradle#567648ff425693b27b191bd38ace7c9c10539c2d;name=b;description=B");

			assertThat(result.isOk()).describedAs(result.toString())
				.isTrue();

			List<TemplateInfo> infos = result.unwrap();
			assertThat(infos).hasSize(2);
			assertThat(infos.remove(0)
				.name()).isEqualTo("a");

			TemplateUpdater updater = tfs.updater(wsDir, infos);
			updater.commit();

			assertThat(IO.getFile(wsDir, "cnf/build.bnd")).isFile();
			assertThat(new File(wsDir, "gradle/wrapper")).isDirectory();
			if (!IO.isWindows()) {
				assertThat(new File(wsDir, "gradlew")).isFile()
					.isExecutable();
			}
			assertThat(new File(wsDir, "gradle.properties")).isFile();
			assertThat(new File(wsDir, "readme.md").isFile()).isFalse();
		}

	}

	@Test
	void test() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);
			File a = makeJar("a.zip", """
				-includeresource workspace-master/cnf/build.bnd;literal="# a\\n"
				""");
			File b = makeJar("b.zip", """
				-includeresource workspace-master/cnf/build.bnd;literal="# b\\n"
				""");

			Result<List<TemplateInfo>> result = tfs.read(
				"-workspace-templates " + a.toURI() + ";name=a;description=A," + b.toURI() + ";name=b;description=B");

			assertThat(result.isOk()).describedAs(result.toString())
				.isTrue();

			List<TemplateInfo> infos = result.unwrap();
			assertThat(infos).hasSize(2);

			TemplateUpdater updater = tfs.updater(wsDir, infos);
			updater.commit();

			assertThat(IO.getFile(wsDir, "cnf/build.bnd")).isFile();
			assertThat(IO.collect(IO.getFile(wsDir, "cnf/build.bnd"))).isEqualTo("# a\n# b\n");

			File build = IO.getFile(wsDir, "cnf/build.bnd");

			Map<File, List<Update>> updaters = updater.updaters();
			assertThat(updaters).containsKey(build);
			assertThat(updaters.get(build)).hasSize(2);

			updater.commit();

			assertThat(build).isFile();
		}

	}

	private static Workspace getworkSpace() throws Exception {
		File f = IO.getFile(wsDir, "cnf/build.bnd");
		f.getParentFile()
			.mkdirs();
		IO.store("#test\n", f);
		Workspace workspace = Workspace.getWorkspace(wsDir);
		workspace.addBasicPlugin(new HttpClient());
		return workspace;
	}

	static File makeJar(String name, String spec) throws Exception {
		File props = new File(zip, "props");
		File jar = new File(zip, name);
		IO.store(spec, props);
		try (Builder b = new Builder()) {
			b.setProperties(props);
			b.build();
			assertThat(b.check());
			b.getJar()
				.removePrefix("META-INF");
			b.getJar()
				.setManifest((Manifest) null);
			b.getJar()
				.write(jar);
		}
		return jar;
	}

}
