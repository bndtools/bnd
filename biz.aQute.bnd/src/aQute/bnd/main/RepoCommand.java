package aQute.bnd.main;

import java.io.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.differ.*;
import aQute.bnd.header.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.bnd.service.diff.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.deployer.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.json.*;

public class RepoCommand {
	final static JSONCodec	codec	= new JSONCodec();

	@Description("Access to the repositories")
	interface repoOptions extends Options {
		@Description("Add a file repository")
		Collection<String> repo();

		@Description("Include the maven repository")
		boolean maven();

		@Description("Specify a project")
		@OptionArgument("<path>")
		String project();

		@Description("Include the cache repository")
		boolean cache();
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
			MavenRemoteRepository maven = new MavenRemoteRepository();
			maven.setProperties(new Attrs());
			maven.setReporter(bnd);
			repos.add(maven);
		}

		// Repos given by the --repo option

		if (opts.repo() != null) {
			for (String r : opts.repo()) {
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

		// Clean up and find first writable
		RepositoryPlugin w = null;
		for (Iterator<RepositoryPlugin> rp = repos.iterator(); rp.hasNext();) {
			RepositoryPlugin rpp = rp.next();

			// Check for the cache
			if (!opts.cache() && rpp.getName().equals("cache")) {
				rp.remove();
			}
			if (w == null && rpp.canWrite()) {
				w = rpp;
			}
		}
		this.writable = w;

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
	interface reposOptions extends Options {}

	public void _repos(@SuppressWarnings("unused") reposOptions opts) {
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
	interface listOptions extends Options {
		boolean versions();

		String mask();

		Instruction from();
	}

	public void _list(listOptions opts) throws Exception {
		Set<String> bsns = new HashSet<String>();
		Instruction from = opts.from();
		if (from == null)
			from = new Instruction("*");

		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName()))
				bsns.addAll(repo.list(opts.mask()));
		}

		for (String bsn : new SortedList<String>(bsns)) {
			Set<Version> versions = new TreeSet<Version>();
			for (RepositoryPlugin repo : repos) {
				if (from.matches(repo.getName())) {
					List<Version> result = repo.versions(bsn);
					if (result != null)
						versions.addAll(result);
				}
			}
			bnd.out.printf("%-40s %s%n", bsn, versions);
		}
	}

	/**
	 * get a file from the repo
	 * 
	 * @param opts
	 */

	interface getOptions extends Options {
		String output();

		boolean lowest();

		Instruction from();
	}

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
				List<Version> versions = repo.versions(bsn);
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
		File file = repo.get(bsn, v.toString(), Strategy.EXACT, null);

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

	interface putOptions extends Options {

		boolean force();
	}

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

		File file = bnd.getFile(args.remove(0));
		if (!file.isFile()) {
			bnd.error("No such file %s", file);
			return;
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
				PutResult r = writable.put(new BufferedInputStream(new FileInputStream(file)), new RepositoryPlugin.PutOptions());
				bnd.trace("put %s in %s (%s) into %s", file, writable.getName(), writable.getLocation(), r.artifact);
			}
		}
		finally {
			jar.close();
		}
	}

	@Arguments(arg = {
			"newer repo", "[older repo]"
	})
	interface diffOptions extends Options {
		@Description("Serialize to JSON")
		boolean json();

		@Description("Show full diff tree (also equals)")
		boolean full();

		@Description("Formatted like diff")
		boolean diff();

		@Description("Both add and removes")
		boolean all();

		boolean remove();

		boolean added();
	}

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
	interface RefreshOptions extends Options {
		
	}
	public void _refresh(RefreshOptions opts) {
		for ( Object o : repos) {
			if ( o instanceof Refreshable) {
				bnd.trace("refresh %s", o);
				((Refreshable)o).refresh();
			}
		}
	}

}
