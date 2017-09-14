package org.bndtools.builder;

import java.io.File;
import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.IProjectDecorator.BndProjectInfo;
import org.bndtools.builder.decorator.ui.ComponentDecorator;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

/**
 * This class creates markers for classes that contain the {@link org.osgi.service.component.annotations.Component}
 * annotation, and stores this information in the {@link BuilderPlugin} for use by {@link ComponentDecorator}and
 * {@link ComponentPackageDecorator}.
 *
 * @author wodencafe
 */
public class ComponentMarker {

    private static final ILogger logger = Logger.getLogger(ComponentMarker.class);
    public static final String ANNOTATION_COMPONENT_PACKAGE = "org.osgi.service.component.annotations";
    public static final String ANNOTATION_COMPONENT_FQN = ANNOTATION_COMPONENT_PACKAGE + ".Component";

    public static void updateComponentMarkers(IProject project, BndProjectInfo model) throws Exception {
        try {
            if (!project.isOpen()) {
                return;
            }
            IJavaProject javaProject = JavaCore.create(project);
            if (javaProject == null) {
                return; // project is not a java project
            }

            for (IClasspathEntry cpe : javaProject.getRawClasspath()) {
                if (cpe.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
                    continue;
                }
                for (IPackageFragmentRoot pkgRoot : javaProject.findPackageFragmentRoots(cpe)) {
                    if (pkgRoot.getKind() != IPackageFragmentRoot.K_SOURCE) {
                        continue;
                    }

                    IResource pkgRootResource = pkgRoot.getCorrespondingResource();
                    if (pkgRootResource == null) {
                        continue;
                    }
                    File pkgRootFile = pkgRootResource.getLocation().toFile();
                    boolean pkgInSourcePath = model.getSourcePath().contains(pkgRootFile);
                    if (pkgInSourcePath) {
                        for (IJavaElement child : pkgRoot.getChildren()) {
                            IPackageFragment pkg = (IPackageFragment) child;
                            if (pkg.getKind() != IPackageFragmentRoot.K_SOURCE) {
                                continue;
                            }

                            if (pkg.containsJavaResources()) {
                                parseChildrenForComponents(pkg);
                            }
                        }
                    }
                }
            }

            updateComponentDecorators();

        } catch (CoreException e) {
            logger.logError("Component Marker error", e);
        }
    }

    public static void updateComponentDecorators() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IDecoratorManager idm = PlatformUI.getWorkbench().getDecoratorManager();
                idm.update("bndtools.componentDecorator");
                idm.update("bndtools.componentPackageDecorator");
            }
        });
    }

    private static void parseChildrenForComponents(IPackageFragment pkg) throws JavaModelException, CoreException {
        for (IJavaElement e : pkg.getChildren()) {
            if (e instanceof ICompilationUnit) {
                ICompilationUnit compUnit = (ICompilationUnit) e;
                if (!isComponentInImports(compUnit)) {
                    continue;
                }

                compUnit.getResource().deleteMarkers(BndtoolsConstants.MARKER_COMPONENT, true, IResource.DEPTH_ONE);
                findAndMarkComponentAnnotations(compUnit);

            }
        }
    }

    private static void findAndMarkComponentAnnotations(ICompilationUnit c) throws CoreException, JavaModelException {

        Document document = null;
        boolean found = false;

        String key = null;
        for (IType t : c.getTypes()) {
            for (IAnnotation annot : t.getAnnotations()) {
                if ("Component".equals(annot.getElementName())) {
                    if (document == null)
                        document = new Document(c.getBuffer().getContents());
                    found = true;
                    key = getNameFromComponent(annot);

                    int lineNumber;
                    try {
                        lineNumber = document.getLineOfOffset(t.getSourceRange().getOffset()) + 1;
                        String message = key == null ? "OSGi Component" : key;
                        IMarker marker = c.getResource().createMarker(BndtoolsConstants.MARKER_COMPONENT);
                        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                        marker.setAttribute(IMarker.MESSAGE, message);
                        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
                        marker.setAttribute(IMarker.LOCATION, "line " + lineNumber);

                    } catch (BadLocationException e) {
                        logger.logError("Component Marker error", e);
                        lineNumber = -1;
                    }

                }

            }
        }
        if (!found) {
            c.getResource().deleteMarkers(BndtoolsConstants.MARKER_COMPONENT, true, IResource.DEPTH_ONE);

        }

    }

    private static String getNameFromComponent(IAnnotation annot) throws JavaModelException {
        String customText = null;
        for (IMemberValuePair pair : annot.getMemberValuePairs()) {
            if ("name".equals(pair.getMemberName()) && pair.getValue() != null) {
                customText = String.valueOf(pair.getValue());
            }

        }
        return customText;
    }

    private static boolean isComponentInImports(ICompilationUnit unit) throws JavaModelException {
        boolean annotationInImports = false;
        for (IImportDeclaration importDecl : unit.getImports()) {
            annotationInImports = importDecl.getElementName().equals(ANNOTATION_COMPONENT_FQN) || importDecl.getElementName().equals(ANNOTATION_COMPONENT_PACKAGE + ".*");

            if (annotationInImports) {
                break;
            }
        }
        return annotationInImports;
    }

}
