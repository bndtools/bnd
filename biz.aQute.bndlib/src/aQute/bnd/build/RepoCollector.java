package aQute.bnd.build;

import java.util.Collection;
import java.util.LinkedHashSet;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;

/**
 * Helper which collects ${repo;} references used in -includeresource.
 */
public class RepoCollector extends Processor {
	private final Collection<Container>	repoRefs	= new LinkedHashSet<>();
	private final Project	project;

	public RepoCollector(Processor parent) {
		super(parent);

		while (parent != null && !(parent instanceof Project)) {
			parent = parent.getParent();
		}
		assert parent != null;
		this.project = ((Project) parent);
	}

	/**
	 * @return returns the collected repositories
	 */
	public Collection<Container> getRepoRefs() {
		// borrowed from aQute.bnd.osgi.Builder.doIncludeResources(Jar)
		// because this causes a call to the _repo() macro below
		// in which we populate this.repoRefs
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

		Collection<Container> containers = project.repoContainers(args);
		if (containers == null) {
			return null;
		}

		// actual collection
		repoRefs.addAll(containers);

		return project.repoPaths(containers);
	}





}
