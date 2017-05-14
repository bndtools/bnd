package org.bndtools.builder.decorator.ui;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.BndtoolsBuilder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * A decorator for {@link IPackageFragment}'s that adds an icon and count if the classes within contains an OSGi
 * {@link org.osgi.service.component.annotations.Component} annotation.
 *
 * @author wodencafe
 */
public class ComponentPackageDecorator extends LabelProvider implements ILightweightLabelDecorator {
    private static final ILogger logger = Logger.getLogger(ComponentPackageDecorator.class);
    private final ImageDescriptor componentIcon = AbstractUIPlugin.imageDescriptorFromPlugin(BndtoolsBuilder.PLUGIN_ID, "icons/component_s_flip.png");

    @Override
    public void decorate(Object element, IDecoration decoration) {

        try {
            if (element instanceof IPackageFragment) {
                IPackageFragment frag = (IPackageFragment) element;
                if (!frag.getJavaProject().getProject().hasNature(BndtoolsConstants.NATURE_ID)) {
                    return;
                }
                IResource resource = (IResource) frag.getAdapter(IResource.class);
                if (countComponents(resource)) {
                    decoration.addOverlay(componentIcon);
                }

            }
        } catch (CoreException e) {
            logger.logError("Component Package Decorator error", e);
        }
    }

    private static boolean countComponents(IResource resource) {
        boolean found = false;

        try {
            if (resource.findMarkers(BndtoolsConstants.MARKER_COMPONENT, true, IResource.DEPTH_INFINITE).length > 0) {
                found = true;
            }

        } catch (CoreException e) {
            logger.logError("Component Package Decorator error", e);
        }
        return found;
    }
}
