package aQute.bnd.repository.maven.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.tags.Tagged;
import aQute.bnd.service.tags.Tags;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.maven.provider.FakeNexus;

@ExtendWith(SoftAssertionsExtension.class)
public class WorkspaceTest {
	@InjectTemporaryDirectory
	File						tmp;
	File						local;
	File						remote;
	File						index;
	File						build;

	private MavenBndRepository	repo;
	private FakeNexus			fnx;
	private Workspace			workspace;

	@BeforeEach
	protected void setUp() throws Exception {
		local = IO.getFile(tmp, "local");
		remote = IO.getFile(tmp, "remote");
		index = IO.getFile(tmp, "index");
		build = IO.getFile(tmp, "workspace/cnf/build.bnd");
		remote.mkdirs();
		local.mkdirs();

		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		IO.copy(IO.getFile("testresources/mavenrepo/index.maven"), index);

		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		IO.close(fnx);
	}

	@Test
	public void testEnv() throws Exception {
		config(null);
		assertNotNull(workspace);
		assertNotNull(repo);
		System.out.println(workspace.getBase());

		// check repo tags
		// we expect all repos to be returned for the 'resolve' tag by default
		// if a repo has no tag specified
		List<Repository> repos = workspace.getPlugins(Repository.class);
		List<Repository> resolveRepos = workspace.getPlugins(Repository.class, Constants.REPOTAGS_RESOLVE);
		assertEquals(repos, resolveRepos);

		Repository repo = repos.get(0);
		assertTrue(repo instanceof Tagged);
		assertEquals(0, ((Tagged) repo).getTags()
			.size());
		assertTrue(((Tagged) repo).getTags()
			.includesAny(Constants.REPOTAGS_RESOLVE));

		// same for 'compile' tag
		List<Repository> compileRepos = workspace.getPlugins(Repository.class, Constants.REPOTAGS_RESOLVE);
		assertEquals(repos, compileRepos);

	}

	@Test
	public void testRepoWithDifferentTag() throws Exception {
		// similar as testEnv()
		// but override the tag with a different one and repeat the tests
		config(Map.of("tags", " foo,bar , a "));

		List<Repository> resolveRepos = workspace.getPlugins(Repository.class, Constants.REPOTAGS_RESOLVE);
		assertTrue(resolveRepos.isEmpty());

		List<Repository> repos = workspace.getPlugins(Repository.class);
		Repository repo = repos.get(0);
		assertTrue(repo instanceof Tagged);
		assertEquals(3, ((Tagged) repo).getTags()
			.size());

		// make sure tags are sorted consistently (alphabetically)
		assertEquals("a", new ArrayList<>(((Tagged) repo).getTags()).get(0));
		assertEquals("bar", new ArrayList<>(((Tagged) repo).getTags()).get(1));
		assertEquals("foo", new ArrayList<>(((Tagged) repo).getTags()).get(2));

	}

	void config(Map<String, String> override) throws Exception {
		Map<String, String> config = new HashMap<>();
		config.put("local", tmp.getAbsolutePath() + "/local");
		config.put("index", tmp.getAbsolutePath() + "/index");
		config.put("releaseUrl", fnx.getBaseURI() + "/repo/");

		if (override != null)
			config.putAll(override);

		try (Formatter sb = new Formatter();) {
			sb.format("-plugin.maven= \\\n");
			sb.format("  %s; \\\n", MavenBndRepository.class.getName());
			sb.format("  name=test; \\\n", MavenBndRepository.class.getName());
			sb.format("  local=%s; \\\n", config.get("local"));
			sb.format("  releaseUrl=%s; \\\n", config.get("releaseUrl"));
			sb.format("  index=%s; \\\n", config.get("index"));

			String tags = config.get("tags");
			if (tags != null && !tags.isBlank()) {
				sb.format("  tags=\"%s\"\n", tags);
			}

			build.getParentFile()
				.mkdirs();
			IO.store(sb.toString(), build);

			workspace = Workspace.getWorkspace(build.getParentFile()
				.getParentFile());
			repo = workspace.getPlugin(MavenBndRepository.class);
		}
	}

	@Test
	public void testTagDisplay() {
		assertEquals("", Tags.print(Tags.of()));
		assertEquals("", Tags.print(Tags.NO_TAGS));
		assertEquals("foo", Tags.print(Tags.of("foo")));
		assertEquals("bar,foo", Tags.print(Tags.of("foo", "bar")));
		assertEquals("-", Tags.print(Tags.of(Tagged.EMPTY_TAGS)));
		assertEquals("foo", Tags.print(Tags.of(Tagged.EMPTY_TAGS, "foo")));
		assertEquals("bar,foo", Tags.print(Tags.of(Tagged.EMPTY_TAGS, "foo", "bar")));
	}

	@Test
	public void testTags(SoftAssertions softly) {

		softly.assertThat(Tags.of()
			.includesAny("resolve"))
			.isTrue();

		softly.assertThat(Tags.of()
			.includesAny("compile"))
			.isTrue();

		softly.assertThat(Tags.of("resolve")
			.includesAny("resolve"))
			.isTrue();


		softly.assertThat(Tags.of("resolve", "compile")
			.includesAny("resolve"))
			.isTrue();

		softly.assertThat(Tags.of("resolve", "compile")
			.includesAny("compile"))
			.isTrue();

		softly.assertThat(Tags.of("resolve", "compile")
			.includesAny("compile", "resolve"))
			.isTrue();

		softly.assertThat(Tags.of("resolve", "compile")
			.includesAny("resolve", "compile"))
			.isTrue();

		softly.assertThat(Tags.of("resolve", "compile", "somethingelse")
			.includesAny("resolve", "compile"))
			.isTrue();

		// negative
		softly.assertThat(Tags.of("resolve", "nocompile")
			.includesAny("resolve"))
			.isTrue();

		softly.assertThat(Tags.of("resolve", "nocompile")
			.includesAny("compile"))
			.isFalse();

		softly.assertThat(Tags.of("noresolve", "nocompile")
			.includesAny("resolve"))
			.isFalse();

		softly.assertThat(Tags.of("noresolve", "nocompile")
			.includesAny("compile"))
			.isFalse();

	}
}
