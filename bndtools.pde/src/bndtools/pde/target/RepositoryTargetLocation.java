package bndtools.pde.target;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import bndtools.central.Central;

public class RepositoryTargetLocation extends BndTargetLocation {
	static final String			TYPE									= "BndRepositoryLocation";

	static final String			MESSAGE_UNABLE_TO_RESOLVE_REPOSITORIES	= "Unable to resolve Bnd repository plugins";

	static final String			ELEMENT_REPOSITORY						= "repository";
	static final String			ATTRIBUTE_REPOSITORY_NAME				= "name";

	private String				repositoryName;
	private RepositoryPlugin	repository;

	public RepositoryTargetLocation() {
		super(TYPE, "database.png");
	}

	public RepositoryTargetLocation setRepository(String repositoryName) {
		this.repositoryName = repositoryName;
		this.repository = null;
		clearResolutionStatus();
		return this;
	}

	public RepositoryTargetLocation setRepository(RepositoryPlugin repository) {
		this.repositoryName = repository.getName();
		this.repository = repository;
		clearResolutionStatus();
		return this;
	}

	public RepositoryPlugin getRepository() {
		return repository;
	}

	@Override
	public String getText(Object element) {
		return repositoryName;
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, ITargetLocation targetLocation) {
		RepositoryTargetLocationWizard wizard = new RepositoryTargetLocationWizard();
		wizard.setTarget(target);
		wizard.setTargetLocation(this);
		return wizard;
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
		throws CoreException {
		resolveRepository();

		try {
			List<TargetBundle> bundles = new ArrayList<>();

			List<String> bsns = repository.list("*");
			monitor.beginTask("Resolving Bundles", bsns.size());

			int i = 0;
			for (String bsn : bsns) {
				Version version = repository.versions(bsn)
					.last();
				File download = repository.get(bsn, version, new HashMap<String, String>(),
					new RepositoryPlugin.DownloadListener[] {});
				try {
					bundles.add(new TargetBundle(download));
				} catch (Exception e) {
					throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
						"Invalid plugin in repository: " + bsn + " @ " + getLocation(false), e));
				}

				if (monitor.isCanceled())
					return null;
				monitor.worked(++i);
			}

			monitor.done();

			return bundles.toArray(new TargetBundle[0]);
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, MESSAGE_UNABLE_TO_RESOLVE_BUNDLES, e));
		}
	}

	private void resolveRepository() throws CoreException {
		Workspace workspace;
		try {
			workspace = Central.getWorkspace();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, MESSAGE_UNABLE_TO_LOCATE_WORKSPACE, e));
		}

		try {
			if (repositoryName.equals(workspace.getWorkspaceRepository()
				.getName())) {
				this.repository = workspace.getWorkspaceRepository();
			} else {
				for (RepositoryPlugin repository : workspace.getPlugins(RepositoryPlugin.class))
					if (repositoryName.equalsIgnoreCase(repository.getName()))
						this.repository = repository;
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, MESSAGE_UNABLE_TO_RESOLVE_REPOSITORIES, e));
		}

		if (this.repository == null)
			throw new CoreException(
				new Status(IStatus.ERROR, PLUGIN_ID, "Unable to locate the named repository: " + repositoryName));
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve)
			resolveRepository();
		return repository != null ? repository.getLocation() : "";
	}

	@Override
	protected void serialize(Document document, Element locationElement) {
		Element repositoryElement = document.createElement(ELEMENT_REPOSITORY);
		repositoryElement.setAttribute(ATTRIBUTE_REPOSITORY_NAME, repositoryName);
		locationElement.appendChild(repositoryElement);
	}

	public static class Factory extends BndTargetLocationFactory {
		public Factory() {
			super(TYPE);
		}

		@Override
		public ITargetLocation getTargetLocation(Element locationElement) throws CoreException {
			NodeList children = locationElement.getChildNodes();

			for (int i = 0; i < children.getLength(); ++i) {
				Node node = children.item(i);

				if (isElement(node, ELEMENT_REPOSITORY)) {
					String name = ((Element) node).getAttribute(ATTRIBUTE_REPOSITORY_NAME);

					return new RepositoryTargetLocation().setRepository(name);
				}
			}

			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "No repository name specified"));
		}
	}
}
