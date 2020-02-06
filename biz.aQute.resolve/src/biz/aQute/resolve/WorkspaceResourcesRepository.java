package biz.aQute.resolve;

import java.io.File;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.libg.cryptography.SHA256;

public class WorkspaceResourcesRepository extends ResourcesRepository implements WorkspaceRepositoryMarker {

	public static final String WORKSPACE_NAMESPACE = ResourceUtils.WORKSPACE_NAMESPACE;

	public WorkspaceResourcesRepository(Workspace workspace) throws Exception {
		for (Project p : workspace.getAllProjects()) {
			File[] files = p.getBuildFiles(false);
			if (files != null) {
				for (File file : files) {
					Domain manifest = Domain.domain(file);
					ResourceBuilder rb = new ResourceBuilder();
					boolean hasIdentity = rb.addManifest(manifest);
					rb.addContentCapability(file.toURI(), SHA256.digest(file)
						.asHex(), file.length(), hasIdentity ? Constants.MIME_TYPE_BUNDLE : Constants.MIME_TYPE_JAR);

					// Add a capability specific to the workspace so that we can
					// identify this fact later during resource processing.
					rb.addWorkspaceNamespace(p.getName());
					add(rb.build());
				}
			}
		}
	}

	@Override
	public String toString() {
		return "Workspace";
	}

}
