package aQute.bnd.main;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.maven.provider.MavenBndRepository;
import aQute.bnd.repository.maven.provider.MbrUpdater;
import aQute.bnd.repository.maven.provider.MbrUpdater.MavenVersionResult;
import aQute.bnd.repository.maven.provider.MbrUpdater.Scope;
import aQute.bnd.version.MavenVersion;
import aQute.lib.collections.MultiMap;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.json.JSONCodec;
import aQute.lib.justif.Justif;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;

@Description("Maintain Maven Bnd Repository GAV files")
@SuppressWarnings("deprecation")
public class MbrCommand extends Processor {
	final static Pattern					SNAPSHOTLIKE_P				= Pattern
		.compile("(-[^.]+)|(.*(beta|alfa|alpha|rc).?\\d+$)", Pattern.CASE_INSENSITIVE);
	final static Predicate<MavenVersion>	notSnapshotlikePredicate	= v -> !SNAPSHOTLIKE_P.matcher(v.toString())
		.find();

	@Description("Maintain Maven Bnd Repository GAV files")
	public interface MrOptions extends Options {
		@Description("Output to json instead of human readable when possible")
		boolean json();
	}

	bnd										bnd;
	private final List<MavenBndRepository>	repositories;

	final MrOptions							options;

	public MbrCommand(bnd bnd, MrOptions options) throws Exception {
		super(bnd);
		this.bnd = bnd;
		this.options = options;
		this.repositories = getRepositories();
	}

	interface BaseOptions extends Options {
		@Description("Select the repositories by index (see list for getting the index)")
		int[] repo();

	}

	@Description("List the repositories in this workspace")
	@Arguments(arg = {})
	interface ReposOptions extends Options {

	}

	@Description("List the repositories in this workspace")
	public void _repos(ReposOptions options) throws Exception {
		for (int n = 0; n < repositories.size(); n++) {
			MavenBndRepository r = repositories.get(n);
			System.out.format("%2d %-30s %s\n", n, r.getName(), r.getIndexFile());
		}
	}

	@Description("Verify the repositories, this checks if a GAV is defined in multiple repositories or if there are multiple revisions for the same program")
	@Arguments(arg = {
		"archive-glob..."
	})

	interface VerifyOptions extends BaseOptions {}

	@Description("Verify the repositories, this checks if a GAV is defined in multiple repositories or if there are multiple revisions for the same program")
	public void _verify(VerifyOptions options) throws Exception {
		List<MavenBndRepository> repos = getRepositories(options.repo());
		List<Archive> archives = getArchives(repos, options._arguments());

		MultiMap<Archive, MavenBndRepository> overlap = new MultiMap<>();
		MultiMap<Program, Archive> revisions = new MultiMap<>();

		for (Archive archive : archives) {

			revisions.add(archive.revision.program, archive);

			for (MavenBndRepository r : repos) {
				if (r.getArchives()
					.contains(archive))
					overlap.add(archive, r);
			}
		}

		overlap.entrySet()
			.removeIf(e -> e.getValue()
				.size() < 2);
		format("Archive references in multiple repositories", overlap);

		revisions.entrySet()
			.removeIf(e -> e.getValue()
				.size() < 2);
		format("Multiple archives for a single program", revisions);
	}

	@Description("For each archive in the index, show the available higher versions")
	@Arguments(arg = {
		"archive-glob..."
	})
	interface CheckOptions extends BaseOptions {

		@Description("Specify the scope of the selected version: all, micro (max), minor (max), major (max)")
		Scope scope(Scope deflt);

		@Description("Include snapshot like versions like -SNAPSHOT, -rc1, -beta12. These are skipped for updated by default")
		boolean snapshotlike();
	}

	@Description("For each archive in the index, show the available higher versions")
	public void _check(CheckOptions options) throws Exception {
		List<MavenBndRepository> repos = getRepositories(options.repo());
		List<Archive> archives = getArchives(repos, options._arguments());

		MultiMap<Archive, MavenVersion> overlap = MbrUpdater.getUpdates(options.scope(Scope.all), repos,
			archives, options.snapshotlike());
		format("Updates available", overlap);
	}

	@Description("For each archive in the index, update to a higher version if available in the repository")
	@Arguments(arg = {
		"archive-glob..."
	})
	interface UpdateOptions extends CheckOptions {
		boolean dry();
	}

	@Description("For each archive in the index, update to a higher version if available in the repository")
	public void _update(UpdateOptions options) throws Exception {
		List<MavenBndRepository> repos = getRepositories(options.repo());
		List<Archive> archives = getArchives(repos, options._arguments());

		MultiMap<Archive, MavenVersion> updates = MbrUpdater.getUpdates(options.scope(Scope.all), repos,
			archives, options.snapshotlike());

		for (MavenBndRepository repo : repos) {
			bnd.trace("repo %s", repo.getName());

			MbrUpdater mbr = new MbrUpdater(repo);
			Map<Archive, MavenVersionResult> content = mbr.calculateUpdateRevisions(updates);
			logMavenUpdates(content);

			if (!options.dry()) {
				if (repo.getIndexFile()
					.isFile()) {
					bnd.trace("reading %s", repo.getIndexFile());
				}
				if (mbr.update(content)) {
					bnd.trace("writing %s", repo.getIndexFile());
					repo.refresh();
				}
			}
		}
	}


	private List<MavenBndRepository> getRepositories(int[] repo) {
		if (repo == null)
			return repositories;

		List<MavenBndRepository> repositories = new ArrayList<>();
		for (int n : repo) {
			System.out.println("repo # =§" + n);
			repositories.add(this.repositories.get(n));
		}
		return repositories;
	}

	private List<MavenBndRepository> getRepositories() throws Exception {
		Workspace w = bnd.getWorkspace();
		if (w == null) {
			error("Not in a workspace");
			return Collections.emptyList();
		}
		return w.getRepositories()
			.stream()
			.filter(r -> r instanceof MavenBndRepository)
			.map(MavenBndRepository.class::cast)
			.collect(Collectors.toList());
	}

	private void format(String title, MultiMap<?, ?> map) throws Exception {
		if (options.json()) {
			new JSONCodec().enc()
				.indent("  ")
				.to((OutputStream) bnd.out)
				.put(map)
				.flush();
		} else {
			if (map.isEmpty())
				return;

			Justif j = new Justif(140, 50, 60, 70, 80, 90, 100, 110);
			j.formatter()
				.format("%n## %60s%n", title);
			j.table(map, "");
			bnd.out.println(j.wrap());
		}
	}

	private List<Archive> getArchives(List<MavenBndRepository> repos, List<String> list) {
		Instructions selection = new Instructions();
		if (list != null) {
			list.forEach(member -> selection.put(new Instruction(member + "*"), null));
		}

		return repos.stream()
			.parallel()
			.map(MavenBndRepository::getArchives)
			.flatMap(Collection::stream)
			.filter(archive -> selection.matches(archive.toString()))
			.collect(Collectors.toList());
	}


	private void logMavenUpdates(Map<Archive, MavenVersionResult> content) {
		content.entrySet()
			.forEach(e -> {
				Archive archive = e.getKey();
				MavenVersionResult versionResult = e.getValue();

				if (versionResult.mavenVersionAvailable()) {
					bnd.out.format(" %-70s %20s -> %s%n", archive.getRevision().program, archive.getRevision().version,
						versionResult.mavenVersion());
				}

			});
	}
}
