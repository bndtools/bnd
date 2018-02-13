package aQute.bnd.main;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.DownloadBlocker;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.differ.DiffImpl;
import aQute.bnd.differ.RepositoryElement;
import aQute.bnd.header.Attrs;
import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.maven.support.MavenRemoteRepository;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.maven.ToDependencyPom;
import aQute.bnd.service.repository.ResourceRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.MultiMap;
import aQute.lib.collections.SortedList;
import aQute.lib.deployer.FileRepo;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.OptionArgument;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.cryptography.SHA1;
import aQute.libg.glob.Glob;

public class RepoCommand {
	private final static Logger	logger	= LoggerFactory.getLogger(RepoCommand.class);
	final static JSONCodec codec = new JSONCodec();

	@Description("Access to the repositories. Provides a number of sub commands to manipulate the repository "
			+ "(see repo help) that provide access to the installed repos for the current project.")
	@Arguments(arg = {
			"sub-cmd", "..."
	})
	interface repoOptions extends Options {
		@Description("Workspace (a standalone bndrun file or a sbdirectory of a workspace (default is the cwd)")
		String workspace();

		@Description("Add a File Repository")
		Collection<String> filerepo();

		@Description("Include the maven repository")
		boolean maven();

		@Description("Specify a project")
		@OptionArgument("<path>")
		String project();

		@Description("Include the cache repository")
		boolean cache();

		@Description("Override the name of the release repository (-releaserepo)")
		Glob release();

	}

	final bnd						bnd;
	final repoOptions				opts;
	final RepositoryPlugin			writable;
	final List<RepositoryPlugin>	repos	= new ArrayList<>();
	final Workspace					workspace;

	/**
	 * Called from the command line
	 * 
	 * @param bnd
	 * @param opts
	 * @throws Exception
	 */
	public RepoCommand(bnd bnd, repoOptions opts) throws Exception {
		this.opts = opts;
		this.bnd = bnd;

		this.workspace = bnd.getWorkspace(opts.workspace());
		if (workspace == null)
			throw new IllegalArgumentException("Cannot find workspace from " + opts.workspace());

		// We can include the maven repository

		if (opts.maven()) {
			logger.debug("maven");
			MavenRemoteRepository maven = new MavenRemoteRepository();
			maven.setProperties(new Attrs());
			maven.setReporter(bnd);
			repos.add(maven);
		}

		// Repos given by the --repo option

		if (opts.filerepo() != null) {
			for (String r : opts.filerepo()) {
				logger.debug("file repo {}", r);
				FileRepo repo = new FileRepo();
				repo.setReporter(bnd);
				File location = bnd.getFile(r);
				repo.setLocation(location.getAbsolutePath());
				repos.add(repo);
			}
		}

		// If no repos are set
		if (repos.isEmpty()) {
			logger.debug("getting project repos");
			Project p = bnd.getProject(opts.project());

			if (p != null) {
				repos.addAll(p.getWorkspace().getRepositories());
			} else {
				Workspace w = bnd.getWorkspace((File) null);
				if (w != null) {
					repos.addAll(w.getRepositories());
				}
			}
		}
		logger.debug("repos {}", repos);

		// Clean up and find first writable
		RepositoryPlugin w = null;
		for (Iterator<RepositoryPlugin> rp = repos.iterator(); rp.hasNext();) {
			RepositoryPlugin rpp = rp.next();

			// Check for the cache
			if (!opts.cache() && rpp.getName().equals("cache")) {
				rp.remove();
			}
			if (w == null && rpp.canWrite()) {
				if (opts.release() == null || opts.release().matcher(rpp.getName()).matches())
					w = rpp;
			}
		}
		this.writable = w;
		logger.debug("writable {}", w);

		List<String> args = opts._arguments();
		if (args.size() == 0) {
			// Default command
			_repos(null);
		} else {
			// Other commands
			String cmd = args.remove(0);
			String help = opts._command().execute(this, cmd, args);
			if (help != null) {
				bnd.out.print(help);
			}
		}
	}

	/**
	 * List the repos
	 */
	@Arguments(arg = {})
	@Description("List the current repositories")
	interface reposOptions extends Options {}

	@Description("List the current repositories")
	public void _repos(@SuppressWarnings("unused") reposOptions opts) {
		int n = 1;
		for (RepositoryPlugin repo : repos) {
			String location = "";
			try {
				location = repo.getLocation();
			} catch (Throwable e) {
				// Ignore
			}
			bnd.out.printf("%03d: %-20s %4s %-20s %s%n", n++, repo.getName(), repo.canWrite() ? "r/w" : "r/o",
					Descriptors.getShortName(repo.getClass().getName()), location);
		}
	}

	/**
	 * List the content of the repos
	 */
	@Description("List all artifacts from the current repositories with their versions")
	@Arguments(arg = {})
	interface listOptions extends Options {

		@Description("Do not list the versions, just the bsns")
		boolean noversions();

		@Description("Optional search term for the list of bsns (given to the repo)")
		String query();

		@Description("A glob expression on the source repo, default is all repos")
		Instruction from();
	}

	@Description("List all artifacts from the current repositories with their versions")
	public void _list(listOptions opts) throws Exception {
		logger.debug("list");
		Set<String> bsns = new HashSet<>();
		Instruction from = opts.from();
		if (from == null)
			from = new Instruction("*");

		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName()))
				bsns.addAll(repo.list(opts.query()));
		}
		logger.debug("list {}", bsns);

		for (String bsn : new SortedList<>(bsns)) {
			if (!opts.noversions()) {
				Set<Version> versions = new TreeSet<>();
				for (RepositoryPlugin repo : repos) {
					logger.debug("get {} from {}", bsn, repo);
					if (from.matches(repo.getName())) {
						SortedSet<Version> result = repo.versions(bsn);
						if (result != null)
							versions.addAll(result);
					}
				}
				bnd.out.printf("%-40s %s%n", bsn, versions);
			} else {
				bnd.out.printf("%s%n", bsn);
			}
		}
	}

	@Description("Get an artifact from a repository.")
	@Arguments(arg = {
			"bsn", "[range]"
	})
	interface getOptions extends Options {
		@Description("Where to store the artifact")
		String output();

		@Description("")
		boolean lowest();

		Instruction from();
	}

	/**
	 * get a file from the repo
	 * 
	 * @param opts
	 */
	@Description("Get an artifact from a repository.")
	public void _get(getOptions opts) throws Exception {
		Instruction from = opts.from();
		if (from == null)
			from = new Instruction("*");

		List<String> args = opts._arguments();
		if (args.isEmpty()) {
			bnd.error("Get needs at least a bsn");
			return;
		}

		String bsn = args.remove(0);
		String range = null;

		if (!args.isEmpty()) {
			range = args.remove(0);
			if (!args.isEmpty()) {
				bnd.error("Extra args %s", args);
			}
		}

		VersionRange r = new VersionRange(range == null ? "0" : range);
		Map<Version,RepositoryPlugin> index = new HashMap<>();

		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName())) {
				SortedSet<Version> versions = repo.versions(bsn);
				if (versions != null)
					for (Version v : versions) {
						if (r.includes(v))
							index.put(v, repo);
					}
			}
		}

		SortedList<Version> l = new SortedList<>(index.keySet());
		if (l.isEmpty()) {
			bnd.out.printf("No versions found for %s%n", bsn);
			return;
		}

		Version v;
		if (opts.lowest())
			v = l.first();
		else
			v = l.last();

		RepositoryPlugin repo = index.get(v);
		File file = repo.get(bsn, v, null);

		File dir = bnd.getBase();
		String name = file.getName();

		if (opts.output() != null) {
			File f = bnd.getFile(opts.output());
			if (f.isDirectory())
				dir = f;
			else {
				dir = f.getParentFile();
				name = f.getName();
			}
		}
		IO.mkdirs(dir);
		IO.copy(file, new File(dir, name));
	}

	/**
	 * put
	 */

	@Description("Put an artifact into the repository after it has been verified.")
	@Arguments(arg = {
			"<jar>..."
	})
	interface putOptions extends Options {
		@Description("Put in repository even if verification fails (actually, no verification is done).")
		boolean force();
	}

	@Description("Put an artifact into the repository after it has been verified.")
	public void _put(putOptions opts) throws Exception {
		if (writable == null) {
			bnd.error("No writable repository in %s", repos);
			return;
		}

		List<String> args = opts._arguments();
		if (args.isEmpty()) {
			bnd.out.println("Writable repo is " + writable.getName() + " (" + writable.getLocation() + ")");
			return;
		}

		nextArgument: while (args.size() > 0) {
			boolean delete = false;
			String source = args.remove(0);
			File file = bnd.getFile(source);
			if (!file.isFile()) {
				file = File.createTempFile("jar", ".jar");
				delete = true;
				try {
					IO.copy(new URL(source).openStream(), file);
				} catch (Exception e) {
					bnd.error("No such file %s", source);
					continue nextArgument;
				}
			}

			logger.debug("put {}", file);

			try (Jar jar = new Jar(file)) {
				String bsn = jar.getBsn();
				if (bsn == null) {
					bnd.error("File %s is not a bundle (it has no bsn) ", file);
					return;
				}

				logger.debug("bsn {} version {}", bsn, jar.getVersion());

				if (!opts.force()) {
					Verifier v = new Verifier(jar);
					v.setTrace(true);
					v.setExceptions(true);
					v.verify();
					bnd.getInfo(v);
				}

				if (bnd.isOk()) {
					PutResult r = writable.put(new BufferedInputStream(IO.stream(file)),
							new RepositoryPlugin.PutOptions());
					logger.debug("put {} in {} ({}) into {}", source, writable.getName(), writable.getLocation(),
							r.artifact);
				}
			}
			if (delete)
				IO.delete(file);
		}
	}

	@Arguments(arg = {
			"newer repo", "[older repo]"
	})
	@Description("Show the diff tree of a single repo or compare 2  repos. A diff tree is a "
			+ "detailed tree of all aspects of a bundle, including its packages, types, methods, "
			+ "fields, and modifiers.")
	interface diffOptions extends Options {
		@Description("Serialize to JSON")
		boolean json();

		@Description("Show full diff tree (also wen entries are equal)")
		boolean full();

		@Description("Formatted like diff")
		boolean diff();

		@Description("Both add and removes")
		boolean all();

		@Description("Just removes (no additions)")
		boolean remove();

		@Description("Just additions (no removes)")
		boolean added();
	}

	@Description("Diff jars (or show tree)")
	public void _diff(diffOptions options) throws UnsupportedEncodingException, IOException, Exception {

		List<String> args = options._arguments();
		String newer = args.remove(0);
		String older = args.size() > 0 ? args.remove(0) : null;

		RepositoryPlugin rnewer = findRepo(newer);
		RepositoryPlugin rolder = older == null ? null : findRepo(older);

		if (rnewer == null) {
			bnd.messages.NoSuchRepository_(newer);
			return;
		}
		if (older != null && rolder == null) {
			bnd.messages.NoSuchRepository_(newer);
			return;
		}

		PrintWriter pw = IO.writer(bnd.out, UTF_8);
		Tree tNewer = RepositoryElement.getTree(rnewer);
		if (rolder == null) {
			if (options.json())
				codec.enc().to(pw).put(tNewer.serialize()).flush();
			else
				DiffCommand.show(pw, tNewer, 0);
		} else {
			Tree tOlder = RepositoryElement.getTree(rolder);
			Diff diff = new DiffImpl(tNewer, tOlder);
			MultiMap<String,String> map = new MultiMap<>();
			for (Diff bsn : diff.getChildren()) {

				for (Diff version : bsn.getChildren()) {
					if (version.getDelta() == Delta.UNCHANGED)
						continue;

					if (options.remove() == false && options.added() == false
							|| (options.remove() //
									&& version.getDelta() == Delta.REMOVED)
							|| (options.added() && version.getDelta() == Delta.ADDED)) {

						map.add(bsn.getName(), version.getName());
					}
				}
			}

			if (options.json())
				codec.enc().to(pw).put(map).flush();
			else if (!options.diff())
				bnd.printMultiMap(map);
			else
				DiffCommand.show(pw, diff, 0, !options.full());
		}
		pw.flush();
	}

	private RepositoryPlugin findRepo(String name) {
		for (RepositoryPlugin repo : repos) {
			if (repo.getName().equals(name))
				return repo;
		}
		return null;
	}

	@Description("Refresh refreshable repositories")
	@Arguments(arg = {})
	interface RefreshOptions extends Options {

	}

	@Description("Refresh refreshable repositories")
	public void _refresh(RefreshOptions opts) throws Exception {
		for (Object o : repos) {
			if (o instanceof Refreshable) {
				logger.debug("refresh {}", o);
				((Refreshable) o).refresh();
			}
		}
	}

	@Description("Displays a sorted set of versions for a given bsn that can be found in the current repositories.")
	@Arguments(arg = "bsn")
	interface VersionsOptions extends Options {

	}

	@Description("Displays a list of versions for a given bsn that can be found in the current repositories.")
	public void _versions(VersionsOptions opts) throws Exception {
		TreeSet<Version> versions = new TreeSet<>();
		String bsn = opts._arguments().remove(0);
		for (RepositoryPlugin repo : repos) {
			versions.addAll(repo.versions(bsn));
		}
		bnd.out.println(versions);
	}

	/**
	 * Copy
	 */
	@Arguments(arg = {
			"source", "dest"
	})
	interface CopyOptions extends projectOptions {

		@Description("A stanalone bndrun file")
		String standalone();

		@Description("Do not really copy but trace the steps")
		boolean dry();
	}

	public void _copy(CopyOptions options) throws Exception {

		List<String> args = options._arguments();

		String srcName = args.remove(0);
		String dstName = args.remove(0);
		RepositoryPlugin source = workspace.getRepository(srcName);
		RepositoryPlugin dest = workspace.getRepository(dstName);

		if (source == null) {
			bnd.error("No such source repository: %s, available repos %s", srcName, workspace.getRepositories());
		}

		if (dest == null) {
			bnd.error("No such destination repository: %s, available repos %s", dstName, workspace.getRepositories());
		} else if (!dest.canWrite())
			bnd.error("Destination repository cannot write: %s", dest);

		if (!bnd.isOk() || source == null || dest == null) {
			return;
		}

		logger.debug("src = {} -> {}", srcName, source);
		logger.debug("dst = {} -> {}", dstName, dest);

		@SuppressWarnings("unused")
		class Spec {
			DownloadBlocker	src;
			DownloadBlocker	dst;
			String			bsn;
			Version			version;
			public byte[]	digest;
		}

		List<Spec> sources = new ArrayList<>();

		//
		// Get the repo contents, using background downloads
		//

		for (String bsn : source.list(null)) {
			for (Version version : source.versions(bsn)) {
				logger.debug("src: {} {}", bsn, version);

				Spec spec = new Spec();
				spec.bsn = bsn;
				spec.version = version;
				spec.src = new DownloadBlocker(bnd);

				File src = source.get(bsn, version, null, spec.src);
				if (src == null) {
					bnd.error("No such entry: %s-%s", bsn, version);
				} else {
					spec.dst = findMatchingVersion(dest, bsn, version);
					sources.add(spec);
				}
			}
		}

		//
		// Verify they all exist and are valid to download
		//

		for (Spec spec : sources) {
			String reason = spec.src.getReason();
			if (reason != null) {
				bnd.error("Failed to find %s because: %s", spec.src.getFile(), reason);
			}

			File src = spec.src.getFile();
			if (!src.isFile()) {
				bnd.error("Not a valid file %s", spec.src.getFile());
			}
			spec.digest = SHA1.digest(src).digest();
		}

		//
		// See if we can prune the list by diffing
		//
		ResourceRepository resources = null;
		if (dest instanceof ResourceRepository)
			resources = (ResourceRepository) dest;

		nextFile: for (Iterator<Spec> i = sources.iterator(); i.hasNext();) {
			Spec spec = i.next();

			if (resources != null) {
				ResourceDescriptor rd = resources.getResourceDescriptor(spec.digest);
				if (rd != null)
					// Already exists
					continue nextFile;
			}

			// TODO Diff
		}

		if (!bnd.isOk())
			return;

		for (Spec spec : sources) {
			File src = spec.src.getFile();

			if (!options.dry()) {
				try (InputStream fin = IO.stream(src)) {
					PutResult put = dest.put(fin, null);
					if (put.digest != null) {
						if (!Arrays.equals(spec.digest, put.digest)) {
							bnd.error("Digest error in upload %s", src);
						}
					}
				} catch (Exception e) {
					bnd.exception(e, "Exception %s in upload %s", e, src);
				}
			}
		}

		for (String bsn : source.list(null)) {
			for (Version version : source.versions(bsn)) {
				System.out.println(bsn + ";version=" + version);
			}
		}
	}

	@Description("Create a POM out of a bnd repository")
	@Arguments(arg = {
			"repo", "name"
	})
	interface PomOptions extends Options {

		@Description("The parent of the pom (default none.xml)")
		String parent();

		@Description("Use the dependency management section")
		boolean dependencyManagement();

		@Description("Output file")
		String output();
	}

	/**
	 * Read a repository and turn all bundles that have a pom into a dependency
	 * POM
	 * 
	 * @throws Exception
	 */
	public void _topom(PomOptions opts) throws Exception {
		List<String> args = opts._arguments();

		String repoName = args.remove(0);
		String name = args.remove(0);

		RepositoryPlugin source = workspace.getRepository(repoName);

		if (source == null) {
			bnd.error("No such source repository: %s, available repos %s", repoName, workspace.getRepositories());
			return;
		}

		if (!(source instanceof ToDependencyPom)) {
			bnd.error("The repository %s cannot generate a dependency pom", source);
			return;
		}

		String sout = opts.output();
		if (sout == null)
			sout = "console";

		OutputStream out;
		if ("console".equals(sout))
			out = System.out;
		else {
			File f = bnd.getFile(sout);
			out = IO.outputStream(f);
		}
		try {
			ToDependencyPom r = (ToDependencyPom) source;
			aQute.bnd.service.maven.PomOptions po = new aQute.bnd.service.maven.PomOptions();
			po.dependencyManagement = opts.dependencyManagement();
			po.parent = opts.parent();
			po.gav = name;
			r.toPom(out, po);
		} finally {
			out.close();
		}
	}

	private DownloadBlocker findMatchingVersion(RepositoryPlugin dest, String bsn, Version version) throws Exception {
		Version floor = version.getWithoutQualifier();
		Version ceiling = new Version(floor.getMajor() + 1, 0, 0);
		VersionRange range = new VersionRange(true, floor, ceiling, false);
		SortedSet<Version> versions = dest.versions(bsn);
		if (versions == null || versions.isEmpty())
			return null;

		for (Version v : range.filter(versions)) {
			// First one is highest
			// TODO Diff
		}
		return null;
	}

}
