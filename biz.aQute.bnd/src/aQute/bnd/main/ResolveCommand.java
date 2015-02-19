package aQute.bnd.main;

import java.util.*;

import org.osgi.resource.*;
import org.osgi.resource.Resource;
import org.osgi.service.repository.*;

import aQute.bnd.build.*;
import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.strings.*;
import biz.aQute.resolve.*;

public class ResolveCommand extends Processor {

	private bnd	bnd;

	public ResolveCommand(bnd bnd) {
		super(bnd);
		this.bnd = bnd;
		getSettings(bnd);
	}

	interface ResolveOptions extends projectOptions {
		String workspace();
	}

	public void _find(ResolveOptions options, bnd bnd) throws Exception {

		List<String> args = options._();

		for (String bndrun : args) {
			Project p = bnd.getProject(options.project());
			Workspace workspace = p == null ? bnd.getWorkspace(options.workspace()) : p.getWorkspace();

			Run run = new Run(workspace, p != null ? p.getBase() : IO.work, IO.getFile(bndrun));

			ProjectResolver pr = new ProjectResolver(run);
			addClose(pr);

			pr.resolve();

			bnd.out.println("Resolved " + run);
			for (Container c : pr.getRunBundles()) {
				bnd.out.printf("%-30s %-20s %-6s %s\n", c.getBundleSymbolicName(), c.getVersion(), c.getType(),
						c.getFile());
			}

		}

	}

	interface QueryOptions extends projectOptions {
		String workspace();
	}

	public void _query(QueryOptions options) throws Exception {
		List<String> args = options._();
		String bsn = args.remove(0);
		String version = null;
		if (!args.isEmpty())
			version = args.remove(0);

		ProjectResolver pr = new ProjectResolver(bnd.getProject(options.project()));
		addClose(pr);

		IdentityCapability resource = pr.getResource(bsn, version);

		bnd.out.printf("%-30s %-20s %s\n", resource.osgi_identity(), resource.version(), resource.description(""));
		Resource r = resource.getResource();
		FilterParser p = new FilterParser();

		if (r != null) {
			List<Requirement> requirements = resource.getResource().getRequirements(null);
			if (requirements != null && requirements.size() > 0) {
				bnd.out.println("Requirements:");
				for (Requirement req : requirements) {
					Expression parse = p.parse(req);
					bnd.out.printf("  %-20s %s\n", req.getNamespace(), parse);
				}
			}
			List<Capability> capabilities = resource.getResource().getCapabilities(null);
			if (capabilities != null && capabilities.size() > 0) {

				bnd.out.println("Capabilities:");
				for (Capability cap : capabilities) {
					Map<String,Object> attrs = new HashMap<String,Object>(cap.getAttributes());
					Object id = attrs.remove(cap.getNamespace());
					Object vv = attrs.remove("version");
					if (vv == null)
						vv = attrs.remove("bundle-version");
					bnd.out.printf("  %-20s %-40s %-20s attrs=%s dirs=%s\n", cap.getNamespace(), id, vv, attrs,
							cap.getDirectives());
				}
			}
		}
	}

	interface RepoOptions extends Options {
		String workspace();
	}

	public void _repos(RepoOptions options) throws Exception {
		Workspace ws = bnd.getWorkspace(options.workspace());
		if (ws == null) {
			error("No workspace");
			return;
		}

		List<Repository> plugins = ws.getPlugins(Repository.class);
		bnd.out.println(Strings.join("\n", plugins));
	}
}
