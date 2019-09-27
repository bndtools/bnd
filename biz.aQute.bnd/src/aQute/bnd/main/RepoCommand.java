package aQute.bnd.main;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
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
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.maven.ToDependencyPom;
import aQute.bnd.service.progress.ProgressToOutput;
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
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA1;
import aQute.libg.glob.Glob;

public class RepoCommand {
	private final static Logger	logger	= LoggerFactory.getLogger(RepoCommand.class);
	final static JSONCodec		codec	= new JSONCodec();

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

			if (p != null && opts.workspace() == null) {
				repos.addAll(p.getWorkspace()
					.getRepositories());
				repos.add(p.getWorkspace()
					.getWorkspaceRepository());
			} else {
				if (workspace != null) {
					repos.addAll(workspace.getRepositories());
					repos.add(workspace.getWorkspaceRepository());
				}
			}
		}
		logger.debug("repos {}", repos);

		// Clean up and find first writable
		RepositoryPlugin w = null;
		for (Iterator<RepositoryPlugin> rp = repos.iterator(); rp.hasNext();) {
			RepositoryPlugin rpp = rp.next();

			// Check for the cache
			if (!opts.cache() && "bnd-cache".equals(rpp.getName())) {
				rp.remove();
			}
			if (w == null && rpp.canWrite()) {
				if (opts.release() == null || opts.release()
					.matcher(rpp.getName())
					.matches())
					w = rpp;
			}
		}
		this.writable = w;
		logger.debug("writable {}", w);

		List<String> args = opts._arguments();
		if (args.isEmpty()) {
			// Default command
			_repos(null);
		} else {
			// Other commands
			String cmd = args.remove(0);
			String help = opts._command()
				.execute(this, cmd, args);
			if (help != null) {
				bnd.out.print(help);
			}
		}
		if (workspace != null)
			bnd.getInfo(workspace);
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
				Descriptors.getShortName(repo.getClass()
					.getName()),
				location);
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
		Set<String> bsns = new HashSet<>();
		Instruction from = opts.from();
		if (from == null)
			from = new Instruction("*");

		logger.debug("repos list {}", from);

		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName())) {
				bsns.addAll(repo.list(opts.query()));
			}
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

		if (bsn.indexOf(':') > 0 && range != null) {
			for (RepositoryPlugin repo : repos) {
				File file = repo.get(bsn, Version.parseVersion(range), null);
				if (file != null) {
					bnd.progress("maven artifact %s", file);
					copyit(opts, file);
					return;
				}
			}
		}

		VersionRange r = new VersionRange(range == null ? "0" : range);
		Map<Version, RepositoryPlugin> index = new HashMap<>();

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

		copyit(opts, file);
	}

	private void copyit(getOptions opts, File file) throws IOException {
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
					try (Verifier v = new Verifier(jar)) {
						v.setTrace(true);
						v.setExceptions(true);
						v.verify();
						bnd.getInfo(v);
					}
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
				codec.enc()
					.to(pw)
					.put(tNewer.serialize())
					.flush();
			else
				DiffCommand.show(pw, tNewer, 0);
		} else {
			Tree tOlder = RepositoryElement.getTree(rolder);
			Diff diff = new DiffImpl(tNewer, tOlder);
			MultiMap<String, String> map = new MultiMap<>();
			for (Diff bsn : diff.getChildren()) {

				for (Diff version : bsn.getChildren()) {
					if (version.getDelta() == Delta.UNCHANGED)
						continue;

					if (options.remove() == false && options.added() == false || (options.remove() //
						&& version.getDelta() == Delta.REMOVED)
						|| (options.added() && version.getDelta() == Delta.ADDED)) {
						map.add(bsn.getName(), version.getName());
					}
				}
			}

			if (options.json())
				codec.enc()
					.to(pw)
					.put(map)
					.flush();
			else if (!options.diff())
				bnd.printMultiMap(map);
			else
				DiffCommand.show(pw, diff, 0, !options.full());
		}
		pw.flush();
	}

	private RepositoryPlugin findRepo(String name) {
		for (RepositoryPlugin repo : repos) {
			if (repo.getName()
				.equals(name))
				return repo;
		}
		return null;
	}

	@Description("Refresh refreshable repositories")
	@Arguments(arg = {})
	interface RefreshOptions extends Options {
		boolean quiet();
	}

	@Description("Refresh refreshable repositories")
	public void _refresh(RefreshOptions opts) throws Exception {
		ProgressToOutput progressToOutput = new ProgressToOutput(bnd.out, null);
		if (!opts.quiet()) {
			workspace.addBasicPlugin(progressToOutput);
		}

		for (Object o : repos) {
			if (o instanceof Refreshable) {
				logger.debug("refresh {}", o);
				((Refreshable) o).refresh();
			}
		}
		progressToOutput.clear();
	}

	@Description("Displays a sorted set of versions for a given bsn that can be found in the current repositories.")
	@Arguments(arg = "bsn")
	interface VersionsOptions extends Options {

	}

	@Description("Displays a list of versions for a given bsn that can be found in the current repositories.")
	public void _versions(VersionsOptions opts) throws Exception {
		TreeSet<Version> versions = new TreeSet<>();
		String bsn = opts._arguments()
			.remove(0);
		for (RepositoryPlugin repo : repos) {
			versions.addAll(repo.versions(bsn));
		}
		bnd.out.println(versions);
	}

	/**
	 * Copy
	 */
	@Arguments(arg = {
		"source", "dest", "bsn[:version]..."
	})
	interface CopyOptions extends projectOptions {

		@Description("A stanalone bndrun file")
		String standalone();

		@Description("Do not really copy but trace the steps")
		boolean dry();

		String[] filter();

		boolean quiet(boolean deflt);

		boolean force();
	}

	@SuppressWarnings("unused")
	class Spec {
		DownloadBlocker	src;
		DownloadBlocker	dst;
		String			bsn;
		Version			version;
		public byte[]	digest;
	}

	public void _copy(CopyOptions options) throws Exception {

		List<String> args = options._arguments();
		ProgressToOutput progressToOutput = new ProgressToOutput(bnd.out, null);
		if (!options.quiet(false)) {
			workspace.addBasicPlugin(progressToOutput);
		}

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

		if (options.filter() != null) {
			for (String path : options.filter()) {
				File file = bnd.getFile(path);
				if (!file.isFile()) {
					bnd.error("Filter file %s does not exist", file);
				} else {
					Files.readAllLines(file.toPath())
						.stream()
						.map(String::trim)
						.filter(s -> !s.isEmpty() && !s.startsWith("#"))
						.forEach(args::add);
				}
			}
		}
		String instructions = Strings.join(args);
		Instructions filter = new Instructions(instructions);

		logger.debug("src = {} -> {}", srcName, source);
		logger.debug("dst = {} -> {}", dstName, dest);
		logger.debug("instructions {}", instructions);

		List<Spec> sources = new ArrayList<>();

		//
		// Get the repo contents, using background downloads
		//
		Map<String, Spec> found = new TreeMap<>();

		for (String bsn : source.list(null)) {
			for (Version version : source.versions(bsn)) {
				String gav = bsn + ":" + version;
				logger.debug("src: '{}'", gav);

				if (filter.matches(gav)) {

					Spec spec = new Spec();
					spec.bsn = bsn;
					spec.version = version;
					spec.src = new DownloadBlocker(bnd);
					found.put(gav, spec);

					File src = source.get(bsn, version, null, spec.src);
					if (src == null) {
						bnd.error("No such entry: %s-%s", bsn, version);
					} else {
						spec.dst = findMatchingVersion(dest, bsn, version);
						sources.add(spec);
					}
				} else {
					logger.debug("skip: '{}'", gav);
					found.put(gav, null);
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
			spec.digest = SHA1.digest(src)
				.digest();
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

		if (!options.force() && !bnd.isOk())
			return;

		for (Spec spec : sources) {
			File src = spec.src.getFile();
			if (isBundle(src)) {
				copyIt(options.dry(), dest, spec, src);
			} else {
				bnd.warning("Not a bundle %s %s %s", spec.bsn, spec.version, src);
			}
		}
		progressToOutput.clear();

		for (Map.Entry<String, Spec> e : found.entrySet()) {
			Spec spec = e.getValue();
			String bsn = e.getKey()
				.split(":")[0];
			String version = e.getKey()
				.split(":")[1];
			if (spec != null) {
				String src = spec.src != null ? (spec.src.getReason() == null ? "ok" : spec.src.getReason()) : "no src";
				String dst = spec.dst != null ? (spec.dst.getReason() == null ? "ok" : spec.dst.getReason()) : "no dst";
				bnd.out.printf(" %-60s %-30s %-20s %-20s%n", spec.bsn, spec.version, src, dst);
			} else {
				bnd.out.printf(" %-60s %-30s %-20s %-20s%n", bsn, version, "skip", "-");
			}
		}
	}

	private boolean isBundle(File src) throws IOException {
		Domain domain = Domain.domain(src);
		Entry<String, Attrs> bundleSymbolicName = domain.getBundleSymbolicName();
		return bundleSymbolicName != null;
	}

	private void copyIt(boolean dry, RepositoryPlugin dest, Spec spec, File src) throws ZipException, IOException {
		if (src.getName()
			.endsWith(".zip")
			|| src.getName()
				.endsWith(".par")) {
			try (ZipFile zipFile = new ZipFile(src)) {
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String name = entry.getName();
					int n = name.lastIndexOf('.');
					String ext = name.substring(n);

					if (".jar".equals(ext) || ".par".equals(ext)) {
						try (InputStream in = zipFile.getInputStream(entry)) {
							Path tmpFile = Files.createTempFile("zip", ext);
							try {
								IO.copy(in, tmpFile);
								copyit(dry, dest, spec, tmpFile.toFile());
							} finally {
								Files.delete(tmpFile);
							}
						}
					} else {
						bnd.progress("skip %s", name);
					}

				}
			}

		} else {
			copyit(dry, dest, spec, src);
		}
	}

	private void copyit(boolean dry, RepositoryPlugin dest, Spec spec, File src) {
		if (!dry) {
			try (InputStream fin = IO.stream(src)) {
				PutOptions po = new RepositoryPlugin.PutOptions();
				po.context = workspace;

				PutResult put = dest.put(fin, po);
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
		Version ceiling = floor.bumpMajor();
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

	@Description("")
	interface SyncOptions extends Options {
		String[] source();

		String dest();

		String workspace();

		String[] gavs();
	}

	public void _sync(SyncOptions opts) throws Exception {
		Workspace ws = bnd.getWorkspace(opts.workspace());
		if (ws == null) {
			bnd.error("No such workspace %s", opts.workspace());
			return;
		}

		List<RepositoryPlugin> sources = new ArrayList<>();
		String dest = null;
		if (opts.dest() == null) {
			dest = ws.getProperty(Constants.RELEASEREPO);
			bnd.progress("Using release repo %s", dest);
		}

		if (dest == null) {
			bnd.error("No destination repo set. Either --dest <dst> or the %s property in the workspace file",
				Constants.RELEASEREPO);
			return;
		}

		RepositoryPlugin destRepo = ws.getRepository(dest);
		if (destRepo == null) {
			bnd.error("No destination repo %s found", dest);
			return;
		}

		for (String src : opts.source()) {
			RepositoryPlugin srcRepo = ws.getRepository(src);
			if (srcRepo == null) {
				bnd.error("No source repo %s found", src);
			} else {
				bnd.progress("Using src repo %s", srcRepo);
				sources.add(srcRepo);
			}
		}
		if (sources.isEmpty()) {
			bnd.progress("Using all repositories as source");
			sources.addAll(ws.getRepositories());
		}
		if (sources.isEmpty()) {
			bnd.error("No source repos found");
			return;
		}
		sources.remove(destRepo);

		copyGavs(opts, sources, destRepo);

		List<String> args = opts._arguments();
		while (!args.isEmpty()) {
			String bsn = args.remove(0);
			String range = "0";
			if (!args.isEmpty() && VersionRange.isVersionRange(args.get(0))) {
				range = args.remove(0);
			}
			copy(sources, destRepo, bsn, range);

		}

	}

	private void copyGavs(SyncOptions opts, List<RepositoryPlugin> sources, RepositoryPlugin destRepo)
		throws IOException {
		for (String gav : opts.gavs()) {
			File file = bnd.getFile(gav);
			if (!file.isFile()) {
				bnd.error("GAV file specied %s but cannot find", file);
				continue;
			}
			List<String> lines = Files.readAllLines(file.toPath());
			for (String line : lines) {

				line = line.trim();

				if (line.isEmpty())
					continue;

				if (line.startsWith("#"))
					continue;

				int n = line.lastIndexOf(':');
				if (n <= 0) {
					bnd.error("Not a valid GAV %s", line);
					continue;
				}

				String pregav = gav.substring(0, n);
				String version = gav.substring(n + 1);

				copy(sources, destRepo, pregav, version);

			}

		}
	}

	private void copy(List<RepositoryPlugin> sources, RepositoryPlugin destRepo, String bsn, String range) {
		File f = find(sources, bsn, range);
		if (f == null) {
			// already reported reason
			return;

		} else {
			try (FileInputStream in = new FileInputStream(f)) {
				destRepo.put(in, null);
			} catch (Exception e) {
				bnd.exception(e, "While copying %s to %s: %s", f, destRepo, e.getMessage());
			}
		}
	}

	private File find(List<RepositoryPlugin> sources, String bsn, String range) {
		try {
			VersionRange vr = VersionRange.parseVersionRange(range);

			TreeSet<Version> all = new TreeSet<>();

			for (RepositoryPlugin rp : sources) {
				SortedSet<Version> versions = rp.versions(bsn);
				all.addAll(versions);
			}
			if (all.isEmpty()) {
				bnd.error("No content found at all for %s;version=%s", bsn, range);
			}

			List<Version> list = new ArrayList<>(all);
			Collections.reverse(list);

			for (Version v : list) {
				if (vr.includes(v)) {
					for (RepositoryPlugin rp : sources) {
						File file;
						file = rp.get(bsn, v, null);
						if (file != null)
							return file;
					}
				}
			}
			bnd.error("No file found for %s;version=%s. Available: %s", bsn, range, list);
			return null;
		} catch (Exception e) {
			bnd.exception(e, "While getting %s", bsn);
			return null;
		}
	}

	interface IndexOptions extends Options {
		@Description("Optional search term for the list of bsns (given to the repo)")
		String query();

		@Description("A glob expression on the source repo, default is all repos")
		Instruction from();

		@Description("Output file (will be compressed)")
		String output();

		@Description("The name of the output file. If not set will show on the console")
		String name(String deflt);

		@Description("No output")
		boolean quiet();
	}

	public void _index(IndexOptions opts) throws Exception {
		ProgressToOutput progressToOutput = new ProgressToOutput(bnd.out, null);
		if (!opts.quiet()) {
			workspace.addBasicPlugin(progressToOutput);
		}

		Set<String> bsns = new HashSet<>();
		Instruction from = opts.from();
		if (from == null)
			from = new Instruction("*");

		logger.debug("repos list {}", from);

		ResourcesRepository all = new ResourcesRepository();
		Set<Requirement> wildcard = Collections.singleton(ResourceUtils.createWildcardRequirement());

		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName())) {
				if (repo instanceof Repository) {
					Repository repository = (Repository) repo;
					Collection<Collection<Capability>> findProviders = repository.findProviders(wildcard)
						.values();
					Set<Resource> resources = ResourceUtils.getAllResources(repository);
					all.addAll(resources);
					bnd.progress("repo %s %s resources", from, resources.size());
				} else {
					bnd.warning("selected repo that was not an OSGi repository %s", from);
				}
			}
		}

		XMLResourceGenerator gen = new XMLResourceGenerator();
		gen.repository(all);
		gen.name(opts.name("untitled"));
		if (opts.output() == null) {
			gen.indent(2);
			gen.save(bnd.out);
		} else {
			File file = bnd.getFile(opts.output());
			file.getParentFile()
				.mkdirs();
			gen.compress();
			gen.save(file);
		}
		progressToOutput.clear();
	}
}
