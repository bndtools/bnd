package aQute.bnd.main;

import java.io.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.service.*;
import aQute.lib.collections.*;
import aQute.lib.deployer.*;
import aQute.lib.getopt.*;
import aQute.lib.osgi.*;
import aQute.libg.header.*;
import aQute.libg.version.*;

public class RepoCommand {
	interface repoOptions extends Options {
		Collection<String> repo();

		boolean maven();

		String project();

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
			opts._command().execute(this, cmd, args);
		}
	}

	/**
	 * List the repos
	 */
	interface reposOptions extends Options {
	}

	public void _repos(reposOptions opts) {
		int n = 1;
		for (RepositoryPlugin repo : repos) {
			String location = "";
			try {
				location = repo.getLocation();
			} catch (Throwable e) {
				// Ignore
			}
			bnd.out.printf("%03d: %-20s %4s %-20s %s\n", n++, repo.getName(),
					repo.canWrite() ? "r/w" : "r/o",
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
			bnd.out.printf("%-40s %s\n", bsn, versions);
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
		
		List<String> args =opts._(); 
		if ( args.isEmpty()) {
			bnd.error("Get needs at least a bsn");
			return;
		}
		
		String bsn = args.remove(0);
		String range = null;
		
		if ( !args.isEmpty()) {
			range = args.remove(0);
			if ( !args.isEmpty()) {
				bnd.error("Extra args %s", args);
			}
		}
		
		VersionRange r = new VersionRange( range == null ? "0" : range);
		
		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName())) {
				List<Version> versions = repo.versions(bsn);
				for ( Version v : versions ) {
					if ( r.includes(v)) {
						String out = opts.output();
						if ( out == null) 
							out = ;
					}
				}
			}
		}
	}
	
	/**
	 * put
	 */
	interface putOptions extends Options {
		Instruction to();
	}
	
	public void _put(putOptions opts) throws Exception {
		Instruction to = opts.to();
		if (to == null)
			to = new Instruction("*");
		
		List<String> args =opts._(); 
		if ( args.isEmpty()) {
			bnd.error("Get needs at least a bsn");
			return;
		}

		String bsn = args.remove(0);
		String range = null;
		
		if ( !args.isEmpty()) {
			range = args.remove(0);
			if ( !args.isEmpty()) {
				bnd.error("Extra args %s", args);
			}
		}
		
		VersionRange r = new VersionRange( range == null ? "0" : range);
		
		for (RepositoryPlugin repo : repos) {
			if (from.matches(repo.getName())) {
				List<Version> versions = repo.versions(bsn);
				for ( Version v : versions ) {
					if ( r.includes(v)) {
						String out = opts.output();
						if ( out == null) 
							out = ;
					}
				}
			}
		}
	}
	
	/*
	 * { Command cmd = opts._command(); if ( ) cmd.execute(rc, arg, args) String
	 * bsn = null; String version = null;
	 * 
	 * for (; i < args.length; i++) { if ("repos".equals(args[i])) { int n = 0;
	 * for (RepositoryPlugin repo : repos) { out.printf("%3d. %s\n", n++, repo);
	 * } return; } else if ("list".equals(args[i])) { String mask = null; if (i
	 * < args.length - 1) { mask = args[++i]; } repoList(repos, mask); return; }
	 * else if ("--repo".equals(args[i]) || "-r".equals(args[i])) { String
	 * location = args[++i]; } else if ("spring".equals(args[i])) { // if (bsn
	 * == null || version == null) { //
	 * error("--bsn and --version must be set before spring command is used");
	 * // } else { // String url = String // .format(
	 * "http://www.springsource.com/repository/app/bundle/version/download?name=%s&version=%s&type=binary"
	 * , // bsn, version); // repoPut(writable, p, url, bsn, version); // }
	 * error("not supported anymore"); return; } else if ("put".equals(args[i]))
	 * { while (i < args.length - 1) { String source = args[++i]; try {
	 * 
	 * URL url = IO.toURL(source, getBase()); trace("put from %s", url);
	 * InputStream in = url.openStream(); try { Jar jar = new
	 * Jar(url.toString(), in); Verifier verifier = new Verifier(jar);
	 * verifier.verify(); getInfo(verifier); if (isOk()) { File put =
	 * writable.put(jar); trace("stored in %s", put); } } finally { in.close();
	 * } } catch (Exception e) { error("putting %s into %s, exception: %s",
	 * source, writable, e); } } return; } else if ("get".equals(args[i])) { if
	 * (i < args.length) { error("repo get requires a bsn, see repo help");
	 * return; } bsn = args[i++]; if (i < args.length) {
	 * error("repo get requires a version, see repo help"); return; } version =
	 * args[i++];
	 * 
	 * for (RepositoryPlugin repo : repos) { File f = repo.get(bsn, version,
	 * Strategy.LOWEST, null); if (f != null) { if (i < args.length) { File out
	 * = getFile(args[i++]); IO.copy(f, out); } else out.println(f);
	 * 
	 * return; } } error("cannot find %s-%s in %s", bsn, version, repos);
	 * return; } }
	 * 
	 * if (i < args.length && !"help".equals(args[i]))
	 * out.println("Unknown repo command: " + args[i]);
	 * 
	 * return; }
	 */
}
