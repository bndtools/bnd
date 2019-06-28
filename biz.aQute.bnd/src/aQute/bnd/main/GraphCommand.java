package aQute.bnd.main;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.fileset.FileSet;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.strings.Strings;

public class GraphCommand implements AutoCloseable {

	final bnd		bnd;
	final Workspace	workspace;

	@Description("Provide access to a dependency graph")
	interface GraphOptions extends Options {

	}

	GraphCommand(bnd bnd, GraphOptions options) throws Exception {
		this.bnd = bnd;
		this.workspace = bnd.getWorkspace();
	}

	@Description("Find the roots in a set of bundles. A root is a resource that is present but not dependent on by any other resource in the set")
	@Arguments(arg = "filespec...")
	interface RootOptions extends Options {

	}

	@Description("Find the roots in a set of bundles. A root is a resource that is present but not dependent on by any other resource in the set")
	public void _roots(RootOptions options) {

		FileSet set = new FileSet(bnd.getBase(), Strings.join(options._arguments()));
		Set<File> files = set.getFiles();
		if (files.isEmpty()) {
			bnd.warning("No matching files found for %s", set);
			return;
		}

		List<Resource> resources = new SimpleIndexer().reporter(bnd)
			.files(files)
			.getResources();

		ResourcesRepository r = new ResourcesRepository(resources);
		Set<Resource> roots = new HashSet<>(resources);

		for (Resource resource : resources) {
			for (Requirement requirement : resource.getRequirements(null)) {
				List<Capability> capabilities = r.findProvider(requirement);
				Set<Resource> requiredResources = ResourceUtils.getResources(capabilities);
				roots.removeAll(requiredResources);
			}
		}

		for (Resource root : roots) {
			bnd.out.println(root);
		}
	}

	@Override
	public void close() throws Exception {}

}
