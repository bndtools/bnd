package aQute.bnd.main;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.differ.*;
import aQute.bnd.header.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.diff.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.deployer.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.libg.glob.*;

public class RepoCommand {
	final static JSONCodec	codec	= new JSONCodec();

	@Description("Access to the repositories. Provides a number of sub commands to manipulate the repository "
			+ "(see repo help) that provide access to the installed repos for the current project.")
	@Arguments(arg = {
			"sub-cmd", "..."
	})
	interface repoOptions extends Options {
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
	final List<RepositoryPlugin>	repos	= new ArrayList<RepositoryPlugin>();

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

		// We can include the maven repository

		if (opts.maven()) {
			bnd.trace("maven");
			MavenRemoteRepository maven = new MavenRemoteRepository();
			maven.setProperties(new Attrs());
			maven.setReporter(bnd);
			repos.add(maven);
		}

		// Repos given by the --repo option

		if (opts.filerepo() != null) {
			for (String r : opts.filerepo()) {
				bnd.trace("file repo " + r);
				FileRepo repo = new FileRepo();
				repo.setReporter(bnd);
				File location = bnd.getFile(r);
				repo.setLocation(location.getAbsolutePath());
				repos.add(repo);
			}
		}

		// If no repos are set
		if (repos.isEmpty()) {
			bnd.trace("getting project repos");
			Project p = bnd.getProject(opts.project());
			if (p != null) {
				repos.addAll(p.getWorkspace().getRepositories());
			}
		}
		bnd.trace("repos " + repos);

		// Clean up and find first writable
		RepositoryPlugin w = null;
		for (Iterator<RepositoryPlugin> rp = repos.iterator(); rp.hasNext();) {
			RepositoryPlugin rpp = rp.next();

			// Check for the cache
			if (!opts.cache() && rpp.getName().equals("cache")) {
				rp.remove();
			}
			if (w == null && rpp.canWrite()) {
				if ( opts.release() == null || opts.release().matcher(rpp.getName()).matches())
					w = rpp;
			}
		}
		this.writable = w;
		bnd.trace("writable " + w);

		List<String> args = opts._();
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
	public void _repos(@SuppressWarnings("unused")
	reposOptions opts) {
		int n = 1;
		for (RepositoryPlugin repo : repos) {
			String location = "";
			try {
				location = repo.getLocation();
			}
			catch (Throwable e) {
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
		bnd.trace("list");
		Set<String> bsns = new HashSet<String>();
		Instruction from = opts.from();
		if (from == null)
			from = new Instruction("*");

		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName()))
				bsns.addAll(repo.list(opts.query()));
		}
		bnd.trace("list " + bsns);

		for (String bsn : new SortedList<String>(bsns)) {
			if (!opts.noversions()) {
				Set<Version> versions = new TreeSet<Version>();
				for (RepositoryPlugin repo : repos) {
					bnd.trace("get " + bsn + " from " + repo);
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

	/**
	 * get a file from the repo
	 * 
	 * @param opts
	 */

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

	@Description("Get an artifact from a repository.")
	public void _get(getOptions opts) throws Exception {
		Instruction from = opts.from();
		if (from == null)
			from = new Instruction("*");

		List<String> args = opts._();
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
		Map<Version,RepositoryPlugin> index = new HashMap<Version,RepositoryPlugin>();

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

		SortedList<Version> l = new SortedList<Version>(index.keySet());
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

		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Could not create directory " + dir);
		}
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

		List<String> args = opts._();
		if (args.isEmpty()) {
			bnd.out.println("Writable repo is " + writable.getName() + " (" + writable.getLocation() + ")");
			return;
		}

		nextArgument: while (args.size() > 0) {
			boolean delete=false;
			String source = args.remove(0);
			File file = bnd.getFile(source);
			if (!file.isFile()) {
				file = File.createTempFile("jar", ".jar");
				delete = true;
				try {
					IO.copy(new URL(source).openStream(), file);
				}
				catch (Exception e) {
					bnd.error("No such file %s", source);
					continue nextArgument;
				}
			}

			bnd.trace("put %s", file);

			Jar jar = new Jar(file);
			try {
				String bsn = jar.getBsn();
				if (bsn == null) {
					bnd.error("File %s is not a bundle (it has no bsn) ", file);
					return;
				}

				bnd.trace("bsn %s version %s", bsn, jar.getVersion());

				if (!opts.force()) {
					Verifier v = new Verifier(jar);
					v.setTrace(true);
					v.setExceptions(true);
					v.verify();
					bnd.getInfo(v);
				}

				if (bnd.isOk()) {
					PutResult r = writable.put(new BufferedInputStream(new FileInputStream(file)),
							new RepositoryPlugin.PutOptions());
					bnd.trace("put %s in %s (%s) into %s", source, writable.getName(), writable.getLocation(), r.artifact);
				}
			}
			finally {
				jar.close();
			}
			if ( delete)
				file.delete();
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

		List<String> _ = options._();
		String newer = _.remove(0);
		String older = _.size() > 0 ? _.remove(0) : null;

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

		PrintWriter pw = new PrintWriter(new OutputStreamWriter(bnd.out, "UTF-8"));
		Tree tNewer = RepositoryElement.getTree(rnewer);
		if (rolder == null) {
			if (options.json())
				codec.enc().to(new OutputStreamWriter(bnd.out, "UTF-8")).put(tNewer.serialize()).flush();
			else
				DiffCommand.show(pw, tNewer, 0);
		} else {
			Tree tOlder = RepositoryElement.getTree(rolder);
			Diff diff = new DiffImpl(tNewer, tOlder);
			MultiMap<String,String> map = new MultiMap<String,String>();
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
				codec.enc().to(new OutputStreamWriter(bnd.out, "UTF-8")).put(map).flush();
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
				bnd.trace("refresh %s", o);
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
		TreeSet<Version> versions = new TreeSet<Version>();
		String bsn = opts._().remove(0);
		for (RepositoryPlugin repo : repos) {
			versions.addAll(repo.versions(bsn));
		}
		bnd.out.println(versions);
	}
}
