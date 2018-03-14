/*******************************************************************************
 * Copyright (c) 2012 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.PlatformUI;

import bndtools.release.nl.Messages;

public class ReleaseWorkspaceHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {

            if (ReleaseHelper.getReleaseRepositories().length == 0) {
                Activator.message(Messages.noReleaseRepos);
                return null;
            }

            if (!PlatformUI.getWorkbench()
                .saveAllEditors(true)) {
                return null;
            }

            WorkspaceAnalyserJob job = new WorkspaceAnalyserJob(null);
            job.setRule(ResourcesPlugin.getWorkspace()
                .getRoot());
            job.schedule();

        } catch (Exception e) {
            throw new ExecutionException(e.getMessage(), e);
        }

        return null;
    }
}
