/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.decorators;

import java.util.Set;

import name.neilbartlett.eclipse.bndtools.BndProject;
import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.builder.BndProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ExportedBundlesDecorator implements ILightweightLabelDecorator {
	
	private final ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/export_decor.gif");

	public void decorate(Object element, IDecoration decoration) {
		try {
			IResource resource = (IResource) element;
			IProject project = resource.getProject();
			if(project.isOpen()) {
				if(project.hasNature(BndProjectNature.NATURE_ID)) {
					BndProject bndProject = BndProject.create(project);
					Set<IResource> exportDirs = bndProject.getExportDirs();
					if(exportDirs != null && exportDirs.contains(resource)) {
						decoration.addOverlay(descriptor, IDecoration.TOP_LEFT);
					}
				}
			}
		} catch (CoreException e) {
			// Ignore
		}
	}

	public void addListener(ILabelProviderListener listener) {
	}
	public void dispose() {
	}
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}
	public void removeListener(ILabelProviderListener listener) {
	}
}