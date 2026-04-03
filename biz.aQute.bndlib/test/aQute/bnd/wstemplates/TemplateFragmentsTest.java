package aQute.bnd.wstemplates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
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

	@Test
	void testRequireField() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);
			
			// Create template jars
			File base = makeJar("base.zip", """
				-includeresource workspace-master/cnf/base.txt;literal="base content\\n"
				""");
			File common = makeJar("common.zip", """
				-includeresource workspace-master/cnf/common.txt;literal="common content\\n"
				""");
			File app = makeJar("app.zip", """
				-includeresource workspace-master/cnf/app.txt;literal="app content\\n"
				""");

			// Read templates with require dependencies:
			// app requires common, common requires base
			Result<List<TemplateInfo>> result = tfs.read(
				"-workspace-templates " + 
				base.toURI() + ";name=base;description=Base," +
				common.toURI() + ";name=common;description=Common;require=" + base.toURI() + "," +
				app.toURI() + ";name=app;description=App;require=" + common.toURI());

			assertThat(result.isOk()).describedAs(result.toString())
				.isTrue();

			List<TemplateInfo> infos = result.unwrap();
			assertThat(infos).hasSize(3);
			
			// Add all templates to the engine
			infos.forEach(tfs::add);

			// Only select the 'app' template - base and common should be auto-included
			TemplateInfo appTemplate = infos.stream()
				.filter(t -> t.name().equals("app"))
				.findFirst()
				.orElseThrow();

			try (TemplateUpdater updater = tfs.updater(wsDir, List.of(appTemplate))) {
				updater.commit();
			}

			// Verify all three files are created (app and its dependencies)
			assertThat(IO.getFile(wsDir, "cnf/base.txt")).isFile();
			assertThat(IO.getFile(wsDir, "cnf/common.txt")).isFile();
			assertThat(IO.getFile(wsDir, "cnf/app.txt")).isFile();
			
			assertThat(IO.collect(IO.getFile(wsDir, "cnf/base.txt"))).isEqualTo("base content\n");
			assertThat(IO.collect(IO.getFile(wsDir, "cnf/common.txt"))).isEqualTo("common content\n");
			assertThat(IO.collect(IO.getFile(wsDir, "cnf/app.txt"))).isEqualTo("app content\n");
		}
	}

	@Test
	void testRequireMultipleDependencies() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);
			
			// Create template jars
			File pluginA = makeJar("pluginA.zip", """
				-includeresource workspace-master/cnf/pluginA.txt;literal="plugin A\\n"
				""");
			File pluginB = makeJar("pluginB.zip", """
				-includeresource workspace-master/cnf/pluginB.txt;literal="plugin B\\n"
				""");
			File main = makeJar("main.zip", """
				-includeresource workspace-master/cnf/main.txt;literal="main\\n"
				""");

			// Main requires both pluginA and pluginB
			Result<List<TemplateInfo>> result = tfs.read(
				"-workspace-templates " + 
				pluginA.toURI() + ";name=pluginA," +
				pluginB.toURI() + ";name=pluginB," +
				main.toURI() + ";name=main;require=\"" + pluginA.toURI() + "," + pluginB.toURI() + "\"");

			assertThat(result.isOk()).describedAs(result.toString())
				.isTrue();

			List<TemplateInfo> infos = result.unwrap();
			infos.forEach(tfs::add);

			// Only select main template
			TemplateInfo mainTemplate = infos.stream()
				.filter(t -> t.name().equals("main"))
				.findFirst()
				.orElseThrow();

			try (TemplateUpdater updater = tfs.updater(wsDir, List.of(mainTemplate))) {
				updater.commit();
			}

			// Verify all three files are created
			assertThat(IO.getFile(wsDir, "cnf/pluginA.txt")).isFile();
			assertThat(IO.getFile(wsDir, "cnf/pluginB.txt")).isFile();
			assertThat(IO.getFile(wsDir, "cnf/main.txt")).isFile();
		}
	}

	@Test
	void testRequireCircularDependency() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);
			
			File templateA = makeJar("templateA.zip", """
				-includeresource workspace-master/cnf/a.txt;literal="A\\n"
				""");
			File templateB = makeJar("templateB.zip", """
				-includeresource workspace-master/cnf/b.txt;literal="B\\n"
				""");

			// Create circular dependency: A requires B, B requires A
			Result<List<TemplateInfo>> result = tfs.read(
				"-workspace-templates " + 
				templateA.toURI() + ";name=A;require=" + templateB.toURI() + "," +
				templateB.toURI() + ";name=B;require=" + templateA.toURI());

			assertThat(result.isOk()).describedAs(result.toString())
				.isTrue();

			List<TemplateInfo> infos = result.unwrap();
			infos.forEach(tfs::add);

			TemplateInfo templateAInfo = infos.stream()
				.filter(t -> t.name().equals("A"))
				.findFirst()
				.orElseThrow();

			// Should handle circular dependency gracefully
			try (TemplateUpdater updater = tfs.updater(wsDir, List.of(templateAInfo))) {
				updater.commit();
			}

			// Both files should be created without infinite loop
			assertThat(IO.getFile(wsDir, "cnf/a.txt")).isFile();
			assertThat(IO.getFile(wsDir, "cnf/b.txt")).isFile();
		}
	}

	@Test
	void testRequireMissingDependency() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);
			
			File template = makeJar("template.zip", """
				-includeresource workspace-master/cnf/main.txt;literal="main\\n"
				""");

			// Template requires a non-existent template
			Result<List<TemplateInfo>> result = tfs.read(
				"-workspace-templates " + 
				template.toURI() + ";name=main;require=nonexistent/missing");

			assertThat(result.isOk()).describedAs(result.toString())
				.isTrue();

			List<TemplateInfo> infos = result.unwrap();
			infos.forEach(tfs::add);

			TemplateInfo mainTemplate = infos.get(0);

			// Should handle missing dependency gracefully (with warning log)
			try (TemplateUpdater updater = tfs.updater(wsDir, List.of(mainTemplate))) {
				updater.commit();
			}

			// Main template should still be applied
			assertThat(IO.getFile(wsDir, "cnf/main.txt")).isFile();
		}
	}

	@Test
	void testRequireDeduplication() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);
			
			File base = makeJar("base.zip", """
				-includeresource workspace-master/cnf/base.txt;literal="base\\n"
				""");
			File moduleA = makeJar("moduleA.zip", """
				-includeresource workspace-master/cnf/moduleA.txt;literal="module A\\n"
				""");
			File moduleB = makeJar("moduleB.zip", """
				-includeresource workspace-master/cnf/moduleB.txt;literal="module B\\n"
				""");

			// Both moduleA and moduleB require base
			Result<List<TemplateInfo>> result = tfs.read(
				"-workspace-templates " + 
				base.toURI() + ";name=base," +
				moduleA.toURI() + ";name=moduleA;require=" + base.toURI() + "," +
				moduleB.toURI() + ";name=moduleB;require=" + base.toURI());

			assertThat(result.isOk()).describedAs(result.toString())
				.isTrue();

			List<TemplateInfo> infos = result.unwrap();
			infos.forEach(tfs::add);

			// Select both moduleA and moduleB (both require base)
			List<TemplateInfo> selectedTemplates = infos.stream()
				.filter(t -> t.name().equals("moduleA") || t.name().equals("moduleB"))
				.collect(java.util.stream.Collectors.toList());

			try (TemplateUpdater updater = tfs.updater(wsDir, selectedTemplates)) {
				Map<File, List<Update>> updates = updater.updaters();
				
				// Base should only appear once in updates, not twice
				File baseFile = IO.getFile(wsDir, "cnf/base.txt");
				if (updates.containsKey(baseFile)) {
					assertThat(updates.get(baseFile)).hasSize(1);
				}
				
				updater.commit();
			}

			// All three files should exist
			assertThat(IO.getFile(wsDir, "cnf/base.txt")).isFile();
			assertThat(IO.getFile(wsDir, "cnf/moduleA.txt")).isFile();
			assertThat(IO.getFile(wsDir, "cnf/moduleB.txt")).isFile();
			
			// Base content should appear only once
			assertThat(IO.collect(IO.getFile(wsDir, "cnf/base.txt"))).isEqualTo("base\n");
		}
	}

	@Test
	void testFragmentZipSlip() throws Exception {
		try (Workspace w = getworkSpace()) {
			FragmentTemplateEngine tfs = new FragmentTemplateEngine(w);

			File a = makeJar("a.zip", """
				-includeresource workspace-master/cnf/build.bnd;literal="# a\\n"
				""");
			File b = makeJar("b.zip", """
				-includeresource workspace-master/cnf/build.bnd;literal="# a\\n"
				""");
			File c = makeJar("c.zip", """
				-includeresource workspace-master/cnf/build.bnd;literal="# a\\n"
				""");
			File d = makeJar("d.zip", """
				-includeresource workspace-master/cnf/build.bnd;literal="# a\\n"
				""");

			ZipSlipCreator.createZipSlip1(a); // ../
			ZipSlipCreator.createZipSlip2(b); // ../../
			ZipSlipCreator.createZipSlip3(c); // subdir/../../
			ZipSlipCreator.createZipSlip4(d); // absolute path outside wsDir

			// all except d.zip, because the leading '/' in
			// absolute path are stripped by ZipUtil.cleanPath()
			Arrays.asList(a, b, c)
				.forEach(f -> {
					assertThatException().as("Expected error for file %s", f.getName())
						.isThrownBy(() -> {
							try (TemplateUpdater updater = tfs.updater(wsDir,
							tfs.read("-workspace-templates " + f.toURI() + ";name=a;description=A")
									.unwrap())) {}
					})
						.withMessageContaining("Entry path is outside of zip file");
				});

			// d.zip with an absolute path outside is safe, because the leading
			// '/' is stripped by ZipUtil.cleanPath()
			// making this path relative
			assertThatNoException().isThrownBy(() -> {
				try (TemplateUpdater updater = tfs.updater(wsDir,
					tfs.read("-workspace-templates " + d.toURI() + ";name=a;description=A")
						.unwrap())) {
					updater.commit();

					String[] list = wsDir.list();
					assertThat(list).containsExactlyInAnyOrder("abspath.txt", "cnf");

				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
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
