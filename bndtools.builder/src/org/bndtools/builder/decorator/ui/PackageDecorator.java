package org.bndtools.builder.decorator.ui;

import java.io.File;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.IProjectDecorator.BndProjectInfo;
import org.bndtools.builder.BndtoolsBuilder;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
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

    public static void updateDecoration(IProject project, BndProjectInfo model) throws Exception {
        if (!project.isOpen()) {
            return;
        }
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null) {
            return; // project is not a java project
        }
        boolean changed = false;
        for (IClasspathEntry cpe : javaProject.getRawClasspath()) {
            if (cpe.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
                continue;
            }
            for (IPackageFragmentRoot pkgRoot : javaProject.findPackageFragmentRoots(cpe)) {
                assert pkgRoot.getKind() == IPackageFragmentRoot.K_SOURCE;
                IResource pkgRootResource = pkgRoot.getCorrespondingResource();
                if (pkgRootResource == null) {
                    continue;
                }
                File pkgRootFile = pkgRootResource.getLocation().toFile();
                boolean pkgInSourcePath = model.getSourcePath().contains(pkgRootFile);
                for (IJavaElement child : pkgRoot.getChildren()) {
                    IPackageFragment pkg = (IPackageFragment) child;
                    assert pkg.getKind() == IPackageFragmentRoot.K_SOURCE;
                    IResource pkgResource = pkg.getCorrespondingResource();
                    if (pkgResource == null) {
                        continue;
                    }
                    String text = pkgResource.getPersistentProperty(packageDecoratorKey);
                    if (pkgInSourcePath) {
                        String pkgName = pkg.getElementName();

                        // Decorate if exported package
                        Attrs pkgAttrs = model.getExports().getByFQN(pkgName);
                        if (pkgAttrs != null) {
                            StringBuilder sb = new StringBuilder(" ").append(Version.parseVersion(pkgAttrs.getVersion()));
                            pkgAttrs = model.getImports().getByFQN(pkgName);
                            if (pkgAttrs != null) {
                                String versionRange = pkgAttrs.getVersion();
                                if (versionRange != null) {
                                    sb.append('\u2194').append(versionRange);
                                }
                            }
                            String version = sb.toString();
                            if (!version.equals(text)) {
                                pkgResource.setPersistentProperty(packageDecoratorKey, version);
                                changed = true;
                            }
                            continue;
                        }

                        // Decorate if non-empty, non-contained package
                        if (pkg.containsJavaResources() && !model.getContained().containsFQN(pkgName)) {
                            if (!excluded.equals(text)) {
                                pkgResource.setPersistentProperty(packageDecoratorKey, excluded);
                                changed = true;
                            }
                            continue;
                        }
                    }

                    // Clear decoration
                    if (text != null) {
                        pkgResource.setPersistentProperty(packageDecoratorKey, null);
                        changed = true;
                    }
                }
            }
        }

        // If decoration change, update display
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
