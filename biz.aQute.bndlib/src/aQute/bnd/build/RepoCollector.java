package aQute.bnd.build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;

/**
 * Helper which collects ${repo;} references used in -includeresource.
 */
public class RepoCollector extends Processor {
	private final Project				project;
	private final Collection<Container>	repoRefs	= new LinkedHashSet<>();

	public RepoCollector(Processor parent) {
		super(parent);

		while (parent != null && !(parent instanceof Project)) {
			parent = parent.getParent();
		}
		assert parent != null;
		this.project = ((Project) parent);
	}

	/**
	 * Note: This method has side-effects since it does the actual collection.
	 * Consider storing and reusing the result for performance instead of
	 * calling it repeatedly.
	 *
	 * @return returns the collected repositories referenced in ${repo}-macros
	 *         used in the project's .bnd
	 */
	public Collection<Container> repoRefs() {
		// borrowed from aQute.bnd.osgi.Builder.doIncludeResources(Jar)
		// because this causes a call to the _repo() macro below
		// in which we populate this.repoRefs in the #add() method
		decorated(Constants.INCLUDERESOURCE);
		return repoRefs;
	}

	/**
	 * the ${repo} macro, based on {@link Project#_repo(String[])} but here we
	 * do the actual collection.
	 */
	public String _repo(String[] args) throws Exception {
		if (args.length < 2) {
			return null;
		}

		Collection<Container> containers = repoContainers(args);
		if (containers == null) {
			return null;
		}

		// actual collection
		repoRefs.addAll(containers);

		return repoPaths(containers);
	}

	Collection<Container> repoContainers(String[] args) throws Exception {
		String spec = args[1];
		String version = args.length > 2 ? args[2] : null;
		Strategy strategy = args.length == 4 ? Strategy.parse(args[3]) : Strategy.HIGHEST;

		if (strategy == null) {
			project.msgs.InvalidStrategy(Project._repoHelp, args);
			return null;
		}

		Parameters bsns = new Parameters(spec, this);
		Collection<Container> containers = new LinkedHashSet<>();

		for (Entry<String, Attrs> entry : bsns.entrySet()) {
			String bsn = removeDuplicateMarker(entry.getKey());
			Map<String, String> attrs = entry.getValue();
			Container container = project.getBundle(bsn, version, strategy, attrs);
			if (container.getError() != null) {
				project.error("${repo} macro refers to an artifact %s-%s (%s) that has an error: %s", bsn, version,
					strategy,
					container.getError());
			} else {
				add(containers, container);
			}
		}
		return containers;
	}

	private void add(Collection<Container> containers, Container container) throws Exception {
		if (container.getType() == Container.TYPE.LIBRARY) {
			List<Container> members = container.getMembers();
			for (Container sub : members) {
				add(containers, sub);
			}
		} else {
			if (container.getError() == null) {
				containers.add(container);
			} else {

				if (isPedantic()) {
					warning("Could not expand repo path request: %s ", container);
				}
			}

		}
	}

	String repoPaths(Collection<Container> containers) {
		List<String> paths = new ArrayList<>(containers.size());
		for (Container container : containers) {

			if (container.getError() == null) {
				paths.add(IO.absolutePath(container.getFile()));
			} else {
				paths.add("<<${repo} = " + container.getBundleSymbolicName() + "-" + container.getVersion() + " : "
					+ container.getError() + ">>");
			}
		}

		return join(paths);
	}

	@Override
	public void close() throws IOException {
		repoRefs.clear();
		super.close();
	}
}
