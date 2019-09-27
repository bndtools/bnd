package bndtools.pde.target;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import bndtools.central.Central;

public class RepositoryTargetLocationPage extends BndTargetLocationPage {
	private static final String				CACHE_REPOSITORY	= "cache";

	private RepositoryTargetLocation		targetLocation;

	private final List<RepositoryPlugin>	repositories;
	private RepositoryPlugin				repository;
	private TreeViewer						bundleList;

	private Text							pluginLocationText;

	public RepositoryTargetLocationPage(ITargetDefinition targetDefinition, RepositoryTargetLocation targetLocation) {
		super("AddBndRepositoryContainer", "Add Bnd Repository", "Select a Bnd repository to be added to your target",
			targetDefinition);

		if (targetLocation != null) {
			this.targetLocation = targetLocation;
			this.repository = targetLocation.getRepository();
		}

		this.repositories = new ArrayList<>();

		Workspace workspace;
		try {
			workspace = Central.getWorkspace();
		} catch (Exception e) {
			logError(BndTargetLocation.MESSAGE_UNABLE_TO_LOCATE_WORKSPACE, e);
			return;
		}

		try {
			WorkspaceRepository workspaceRepository = workspace.getWorkspaceRepository();
			repositories.add(workspaceRepository);
			for (RepositoryPlugin repository : workspace.getPlugins(RepositoryPlugin.class)) {
				if (CACHE_REPOSITORY.equals(repository.getName()))
					continue;
				if (repository instanceof IndexProvider && !((IndexProvider) repository).getSupportedPhases()
					.contains(ResolutionPhase.build))
					continue;
				repositories.add(repository);
			}

			if (this.repository == null)
				this.repository = workspaceRepository;
		} catch (Exception e) {
			repositories.clear();
			logError(RepositoryTargetLocation.MESSAGE_UNABLE_TO_RESOLVE_REPOSITORIES, e);
		}
	}

	private String getLocation() {
		return repository != null ? repository.getLocation() : "";
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		Combo pluginsCombo = createRepositoryComboArea(composite);
		SWTFactory.createLabel(composite, "Contents: ", 2);
		bundleList = createBundleListArea(composite, 2);

		updateTarget();
		selectTargetInCombo(pluginsCombo);

		setControl(composite);
	}

	private boolean selectTargetInCombo(Combo pluginsCombo) {
		if (repository == null)
			return false;

		for (int i = 0; i < pluginsCombo.getItemCount(); i++) {
			if (pluginsCombo.getItem(i)
				.equals(repository.getName())) {
				pluginsCombo.select(i);
				return true;
			}
		}

		return false;
	}

	protected Combo createRepositoryComboArea(Composite composite) {
		String[] names = new String[repositories.size()];
		int i = 0;
		for (RepositoryPlugin plugin : repositories) {
			names[i++] = plugin.getName();
		}

		SWTFactory.createLabel(composite, "Repository: ", 1);
		final Combo pluginsCombo = SWTFactory.createCombo(composite, SWT.READ_ONLY, 1, GridData.FILL_HORIZONTAL, names);

		SWTFactory.createLabel(composite, "Location: ", 1);
		pluginLocationText = SWTFactory.createText(composite, SWT.READ_ONLY, 1, GridData.FILL_HORIZONTAL);
		pluginLocationText.setText(getLocation());

		pluginsCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				repository = repositories.get(pluginsCombo.getSelectionIndex());
				updateTarget();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				clearTarget();
			}
		});

		return pluginsCombo;
	}

	protected void updateTarget() {
		resetMessage();
		if (repositories.isEmpty()) {
			clearTarget();
			return;
		}

		try {
			bundleList.setInput(getBundles());
			pluginLocationText.setText(getLocation());
			setPageComplete(true);
		} catch (Exception e) {
			logError("Unable to list bundles for repository: " + repository.getName(), e);
			clearTarget();
		}
	}

	private void clearTarget() {
		setPageComplete(false);
		bundleList.setInput(Collections.emptySet());
		repository = null;
	}

	protected Collection<?> getBundles() throws Exception {
		List<String> bundles = new ArrayList<>();
		if (repository != null) {
			for (String bsn : repository.list("*")) {
				bundles.add(bsn + " - " + repository.versions(bsn)
					.last());
			}
		}
		if (bundles.isEmpty())
			logWarning("Repository is empty: " + repository.getName(), null);
		else
			resetMessage();
		return bundles;
	}

	@Override
	public RepositoryTargetLocation getBundleContainer() {
		if (targetLocation == null) {
			targetLocation = new RepositoryTargetLocation();
		}

		return targetLocation.setRepository(repository);
	}
}
