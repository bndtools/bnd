package org.bndtools.core.editors.pkginfo;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class PackageInfoEditor extends TextEditor implements IResourceChangeListener {

	private static final ILogger	LOGGER	= Logger.getLogger(PackageInfoEditor.class);

	private Image					imgTitleBase;
	private Image					imgTitleWarning;
	private Image					imgTitleError;

	private Image					titleImage;

	@Override
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		super.setInitializationData(cfig, propertyName, data);

		String strIcon = cfig.getAttribute("icon");

		// Load the icons
		ImageDescriptor baseImageDesc = strIcon != null
			? AbstractUIPlugin.imageDescriptorFromPlugin(cfig.getContributor()
				.getName(), strIcon)
			: null;
		imgTitleBase = baseImageDesc != null ? baseImageDesc.createImage() : getDefaultImage();

		ImageDescriptor imgWarningOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID,
			"icons/warning_co.gif");
		DecorationOverlayIcon warningImageDesc = new DecorationOverlayIcon(imgTitleBase, imgWarningOverlay,
			IDecoration.BOTTOM_LEFT);
		imgTitleWarning = warningImageDesc.createImage();

		ImageDescriptor imgErrorOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID,
			"icons/error_co.gif");
		DecorationOverlayIcon errorImageDesc = new DecorationOverlayIcon(imgTitleBase, imgErrorOverlay,
			IDecoration.BOTTOM_LEFT);
		imgTitleError = errorImageDesc.createImage();

		titleImage = imgTitleBase;
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setDocumentProvider(new PackageInfoDocumentProvider());
		setRulerContextMenuId("#PackageInfoRuleContext");
		setSourceViewerConfiguration(new PackageInfoSourceViewerConfiguration());

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);

		updateTitleIcon();

		IResource resource = ResourceUtil.getResource(getEditorInput());
		if (resource != null)
			resource.getWorkspace()
				.addResourceChangeListener(this);
	}

	void updateTitleIcon() {
		IResource resource = ResourceUtil.getResource(getEditorInput());
		if (resource == null)
			return;

		int severity = IMarker.SEVERITY_INFO;
		try {
			IMarker[] markers = resource.findMarkers(BndtoolsConstants.MARKER_BND_PROBLEM, true, 0);
			if (markers != null) {
				for (IMarker marker : markers)
					severity = Math.max(severity, marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO));
			}
		} catch (CoreException e) {
			LOGGER.logError("Error updating packageinfo editor title icon", e);
		}

		if (severity >= IMarker.SEVERITY_ERROR) {
			titleImage = imgTitleError;
		} else if (severity >= IMarker.SEVERITY_WARNING) {
			titleImage = imgTitleWarning;
		} else {
			titleImage = imgTitleBase;
		}
		firePropertyChange(PROP_TITLE);
	}

	@Override
	public Image getTitleImage() {
		if (titleImage != null)
			return titleImage;
		return getDefaultImage();
	}

	@Override
	public void dispose() {
		IResource resource = ResourceUtil.getResource(getEditorInput());

		super.dispose();

		if (resource != null)
			resource.getWorkspace()
				.removeResourceChangeListener(this);

		if (imgTitleBase != null)
			imgTitleBase.dispose();
		if (imgTitleWarning != null)
			imgTitleWarning.dispose();
		if (imgTitleError != null)
			imgTitleError.dispose();

	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResource resource = ResourceUtil.getResource(getEditorInput());
		IResourceDelta delta = event.getDelta();
		if (delta == null)
			return;

		IPath path = resource.getFullPath();
		delta = delta.findMember(path);
		if (delta == null)
			return;

		if ((delta.getFlags() & IResourceDelta.MARKERS) != 0)
			SWTConcurrencyUtil.execForControl(getEditorSite().getShell(), true, () -> updateTitleIcon());
	}

}
