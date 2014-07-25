package org.bndtools.builder.decorator.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.bndtools.builder.NewBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;

import bndtools.central.Central;

/**
 * A decorator for {@link IPackageFragment}s that adds an icon if the package is exported by the bundle manifest.
 *
 * @author duckAsteroid
 */
public class PackageDecorator extends LabelProvider implements ILightweightLabelDecorator {

    private final ImageDescriptor exportedIcon = AbstractUIPlugin.imageDescriptorFromPlugin(NewBuilder.PLUGIN_ID, "icons/plus-decorator.png");
    private final ImageDescriptor excludedIcon = AbstractUIPlugin.imageDescriptorFromPlugin(NewBuilder.PLUGIN_ID, "icons/excluded_ovr.gif");

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof IPackageFragment) {

            IPackageFragment pkg = (IPackageFragment) element;

            IJavaProject javaProject = pkg.getJavaProject();
            IProject project = javaProject.getProject();
            boolean schedule = true;

            try {
                Collection<IResource> sourceFolders = Central.getSourceFolderModel(project);
                if (sourceFolders == null) {
                    return;
                }

                IResource sourceFolderResource = pkg.getParent().getResource();
                if (!sourceFolders.contains(sourceFolderResource)) {
                    schedule = false;
                    return;
                }

                Map<String, ? extends Collection<Version>> exports = Central.getExportedPackageModel(project);
                if (exports == null) {
                    return;
                }

                schedule = false;

                String pkgName = pkg.getElementName();
                Collection<Version> versions = exports.get(pkgName);
                if (versions != null) {
                    decoration.addOverlay(exportedIcon);
                    if (versions.isEmpty())
                        decoration.addSuffix(" " + Collections.singletonList(Version.emptyVersion).toString());
                    else
                        decoration.addSuffix(" " + versions.toString());
                }

                boolean isSourcePkg = isSourcePackage(pkg);
                boolean emptyPkg = isEmptyPackage(pkg);
                Collection<String> contained = Central.getContainedPackageModel(project);
                if (isSourcePkg && !emptyPkg && (contained == null || !contained.contains(pkgName))) {
                    decoration.addOverlay(excludedIcon);
                    decoration.addSuffix(" <excluded>");
                }
            } finally {
                if (schedule) {
                    //
                    // We no schedule this just for every build
                    // which should run whenever there is a potential change
                    //
                    //                    ExportedPackageDecoratorJob.scheduleForProject(project);
                }
            }
        }
    }

    private boolean isEmptyPackage(IPackageFragment pkg) {
        try {
            return !pkg.containsJavaResources();
        } catch (JavaModelException e) {
            return false;
        }
    }

    private boolean isSourcePackage(IPackageFragment pkg) {
        try {
            return pkg.getKind() == IPackageFragmentRoot.K_SOURCE;
        } catch (JavaModelException e) {
            return false;
        }
    }
}
