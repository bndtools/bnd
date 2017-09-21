package org.bndtools.builder.decorator.ui;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.BndtoolsBuilder;
import org.bndtools.builder.ComponentMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * A decorator for {@link CompilationUnit}'s that adds an icon if the class contains an OSGi
 * {@link org.osgi.service.component.annotations.Component} annotation.
 *
 * @author wodencafe
 */
public class ComponentDecorator extends LabelProvider implements ILightweightLabelDecorator {
    private static final ILogger logger = Logger.getLogger(ComponentDecorator.class);
    private final ImageDescriptor componentIcon = AbstractUIPlugin.imageDescriptorFromPlugin(BndtoolsBuilder.PLUGIN_ID, "icons/component_s_flip.png");

    @Override
    public void decorate(Object element, IDecoration decoration) {

        try {

            if (element instanceof CompilationUnit) {

                CompilationUnit unit = (CompilationUnit) element;
                if (!isComponentInImports(unit)) {
                    return;
                }

                IPackageDeclaration[] decs = unit.getPackageDeclarations();
                if (decs != null && decs.length > 0) {
                    IPackageDeclaration dec = decs[0];
                    if (dec != null) {
                        boolean found = false;
                        String customText = null;

                        for (IMarker marker : unit.getResource().findMarkers(BndtoolsConstants.MARKER_COMPONENT, true, IResource.DEPTH_ONE)) {
                            found = true;
                            customText = marker.getAttribute(IMarker.MESSAGE).toString();
                        }

                        if (found) {
                            decoration.addOverlay(componentIcon);
                            if (customText != null) {

                                if (customText.equals("OSGi Component")) {
                                    decoration.addSuffix(" [Component]");

                                } else {
                                    decoration.addSuffix(" [" + customText + "]");

                                }
                            }

                        }

                    }
                }

            } else if (element instanceof SourceType) {
                SourceType type = (SourceType) element;

                if (!isComponentInImports(type.getCompilationUnit())) {
                    return;
                }

                boolean found = false;
                String customText = null;
                for (IMarker marker : type.getCompilationUnit().getResource().findMarkers(BndtoolsConstants.MARKER_COMPONENT, true, IResource.DEPTH_ONE)) {
                    found = true;
                    customText = marker.getAttribute(IMarker.MESSAGE).toString();
                }

                if (found) {
                    decoration.addOverlay(componentIcon);
                    if (customText != null) {

                        if (customText.equals("OSGi Component")) {
                            decoration.addSuffix(" [Component]");

                        } else {
                            decoration.addSuffix(" [" + customText + "]");

                        }
                    }

                }
            } else if (element instanceof IPackageFragment) {
                IPackageFragment frag = (IPackageFragment) element;
                IResource resource = (IResource) frag.getAdapter(IResource.class);
                if (resource != null && countComponents(resource)) {
                    decoration.addOverlay(componentIcon);
                }

            }

        } catch (CoreException e) {
            logger.logError("Component Decorator error", e);
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

    private static boolean isComponentInImports(ICompilationUnit unit) throws CoreException {
        boolean annotationInImports = false;

        if ((unit != null) && unit.exists()) {
            for (IImportDeclaration importDecl : unit.getImports()) {
                annotationInImports = importDecl.getElementName().equals(ComponentMarker.ANNOTATION_COMPONENT_FQN) || importDecl.getElementName().equals(ComponentMarker.ANNOTATION_COMPONENT_PACKAGE + ".*");
                if (annotationInImports) {
                    break;
                }
            }
        }
        return annotationInImports;
    }

}
