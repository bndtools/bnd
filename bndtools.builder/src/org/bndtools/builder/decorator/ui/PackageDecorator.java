package org.bndtools.builder.decorator.ui;

import java.io.File;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.BndtoolsBuilder;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.version.Version;

/**
 * A decorator for {@link IPackageFragment}s that adds an icon if the package is exported by the bundle manifest.
 *
 * @author duckAsteroid
 */
public class PackageDecorator extends LabelProvider implements ILightweightLabelDecorator {
    private static final ILogger logger = Logger.getLogger(PackageDecorator.class);
    private static final String packageDecoratorId = "bndtools.packageDecorator";
    private static final QualifiedName packageDecoratorKey = new QualifiedName(BndtoolsBuilder.PLUGIN_ID, packageDecoratorId);
    private static final String excluded = " <excluded>";
    private final ImageDescriptor exportedIcon = AbstractUIPlugin.imageDescriptorFromPlugin(BndtoolsBuilder.PLUGIN_ID, "icons/plus-decorator.png");
    private final ImageDescriptor excludedIcon = AbstractUIPlugin.imageDescriptorFromPlugin(BndtoolsBuilder.PLUGIN_ID, "icons/excluded_ovr.gif");

    @Override
    public void decorate(Object element, IDecoration decoration) {
        try {
            IPackageFragment pkg = (IPackageFragment) element;
            if (pkg.getKind() != IPackageFragmentRoot.K_SOURCE) {
                return;
            }
            IResource pkgResource = pkg.getCorrespondingResource();
            if (pkgResource == null) {
                return;
            }
            String text = pkgResource.getPersistentProperty(packageDecoratorKey);
            if (text == null) {
                return;
            }
            if (excluded.equals(text)) {
                decoration.addOverlay(excludedIcon);
            } else {
                decoration.addOverlay(exportedIcon);
            }
            decoration.addSuffix(text);
        } catch (CoreException e) {
            logger.logError("Package Decorator error", e);
        }
    }

    public static void updateDecoration(IProject project, Project model) throws Exception {
        if (!project.isOpen()) {
            return;
        }
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null) {
            return; // project is not a java project
        }
        boolean changed = false;
        for (IPackageFragmentRoot pkgRoot : javaProject.getPackageFragmentRoots()) {
            if (pkgRoot.getKind() != IPackageFragmentRoot.K_SOURCE) {
                continue;
            }
            IResource pkgRootResource = pkgRoot.getCorrespondingResource();
            if (pkgRootResource == null) {
                continue;
            }
            File pkgRootFile = pkgRootResource.getLocation().toFile();
            if (!model.getSourcePath().contains(pkgRootFile)) {
                continue;
            }
            IJavaElement[] pkgs = pkgRoot.getChildren();
            for (IJavaElement e : pkgs) {
                IPackageFragment pkg = (IPackageFragment) e;
                if (pkg.getKind() != IPackageFragmentRoot.K_SOURCE) {
                    continue;
                }
                IResource pkgResource = pkg.getCorrespondingResource();
                if (pkgResource == null) {
                    continue;
                }
                String text = pkgResource.getPersistentProperty(packageDecoratorKey);
                String pkgName = pkg.getElementName();
                Attrs pkgAttrs = model.getExports().getByFQN(pkgName);
                if (pkgAttrs != null) {
                    String version = " " + Version.parseVersion(pkgAttrs.getVersion()).toString();
                    if (!version.equals(text)) {
                        pkgResource.setPersistentProperty(packageDecoratorKey, version);
                        changed = true;
                    }
                    continue;
                }
                if (pkg.containsJavaResources() && !model.getContained().containsFQN(pkgName)) {
                    if (!excluded.equals(text)) {
                        pkgResource.setPersistentProperty(packageDecoratorKey, excluded);
                        changed = true;
                    }
                    continue;
                }
                if (text != null) {
                    pkgResource.setPersistentProperty(packageDecoratorKey, null);
                    changed = true;
                }
            }
        }

        if (changed) {
            Display display = PlatformUI.getWorkbench().getDisplay();
            SWTConcurrencyUtil.execForDisplay(display, true, new Runnable() {
                @Override
                public void run() {
                    PlatformUI.getWorkbench().getDecoratorManager().update(packageDecoratorId);
                }
            });
        }
    }
}
