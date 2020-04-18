package org.bndtools.builder.indexer;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.AbstractBuildListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import bndtools.central.Central;

public class BuiltBundleIndexer extends AbstractBuildListener {

	private final ILogger		logger			= Logger.getLogger(BuiltBundleIndexer.class);

	@Override
	public void builtBundles(final IProject project, IPath[] paths) {
		try {
			Central.refreshWorkspaceResourcesRepository();
		} catch (Exception e) {
			logger.logError("Failed to update workspace index.", e);
		}
	}

}
