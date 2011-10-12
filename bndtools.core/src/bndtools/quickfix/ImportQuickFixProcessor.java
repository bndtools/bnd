package bndtools.quickfix;

import java.util.ArrayList;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

// TODO dependencies for addSearchResults
//import org.eclipse.jdt.core.dom.ArrayType;
//import org.eclipse.jdt.core.dom.Name;
//import org.eclipse.jdt.core.dom.QualifiedName;
//import org.eclipse.jdt.core.dom.SimpleType;
//import org.eclipse.jdt.core.dom.Type;

import aQute.bnd.build.Project;
import aQute.lib.osgi.Constants;
import aQute.libg.header.OSGiHeader;
import bndtools.Plugin;

public class ImportQuickFixProcessor implements IQuickFixProcessor {

    public boolean hasCorrections(ICompilationUnit unit, int problemId) {
        switch (problemId) {
        case IProblem.ForbiddenReference:
        case IProblem.ImportNotFound:
        case IProblem.IsClassPathCorrect:
            // TODO depends on addSearchResults
            // case IProblem.UndefinedType:
            // case IProblem.UndefinedName:
            return true;
        default:
            return false;
        }
    }

    public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
        try {
            HashMap<Object, IJavaCompletionProposal> results = new HashMap<Object, IJavaCompletionProposal>();

            Project project = findProject(context);

            if (project != null) {
                for (int i = 0; i < locations.length; i++) {
                    switch (locations[i].getProblemId()) {
                    case IProblem.ForbiddenReference:
                        handleImportNotFound(project, context, locations[i], results);
                        break;
                    case IProblem.ImportNotFound:
                        handleImportNotFound(project, context, locations[i], results);
                        break;
                    case IProblem.IsClassPathCorrect:
                        handleIsClassPathCorrect(project, context, locations[i], results);
                        break;
                    // TODO depends on addSearchResults
                    // case IProblem.UndefinedType:
                    // handleUndefinedType(project, context, locations[i],
                    // results);
                    // break;
                    // case IProblem.UndefinedName:
                    // handleUndefinedName(project, context, locations[i],
                    // results);
                    // break;
                    }
                }
            }

            return (IJavaCompletionProposal[]) results.values().toArray(new IJavaCompletionProposal[results.size()]);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void handleIsClassPathCorrect(Project project, final IInvocationContext context, IProblemLocation problemLocation,
            final HashMap<Object, IJavaCompletionProposal> results) {
        for (final String type : problemLocation.getProblemArguments()) {
            final String iPackage = type.substring(0, type.lastIndexOf("."));
            addPackageProposal(project, results, type, iPackage);
        }
    }

    private void handleImportNotFound(Project project, final IInvocationContext context, IProblemLocation location,
            final HashMap<Object, IJavaCompletionProposal> results) throws CoreException {
        ASTNode selectedNode = location.getCoveringNode(context.getASTRoot());
        if (selectedNode == null)
            return;

        if (selectedNode instanceof ClassInstanceCreation) {
            // check QualifiedName for search results as well -
            // happens if import package is already added but exported package has
            // been removed
            
            // TODO depends on addSearchResults
            // ClassInstanceCreation c = (ClassInstanceCreation) selectedNode;
            // Type t = c.getType();
            // Name node = findName(t);
            // if (node != null)
            // {
            // addSearchResults(node, project, results);
            // }
        } else {
            for (final String iPackage : readPackage(selectedNode, location)) {
                if (!results.containsKey(iPackage)) {
                    addPackageProposal(project, results, iPackage, iPackage);
                }
            }
        }
    }

    private void addPackageProposal(Project project, HashMap<Object, IJavaCompletionProposal> results, String key, String packageName) {
        if (!isBuildPackage(project, packageName)) {
            // for now just add a simple buildpackage proposal
            results.put(key, new AddBuildPackageProposal(project, packageName));

            // Better to trawl through available resources/projects and propose specific import packages
            // based on available exports
            // this is better as it allows for different versions on import
            // for (ExportedPackage pe : findExportsForPackage(project,
            // iPackage))
            // {
            // results.put(type, new ImportPackageProposal(pe, project));
            // }
        }
    }

    private Project findProject(IInvocationContext context) throws CoreException {
        IJavaProject project = context.getCompilationUnit().getJavaProject();
        if (project.getProject().hasNature(Plugin.BNDTOOLS_NATURE)) {
            return Plugin.getDefault().getCentral().getModel(project);
        } else {
            return null;
        }
    }

    private String[] readPackage(ASTNode selectedNode, IProblemLocation location) {
        ArrayList<String> packages = new ArrayList<String>();

        ImportDeclaration id = (ImportDeclaration) ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);

        if (id == null) {
            MethodInvocation m = (MethodInvocation) ASTNodes.getParent(selectedNode, ASTNode.METHOD_INVOCATION);

            if (m != null) {
                packages.add(readPackage(m));
                while (m.getExpression() != null && m.getExpression() instanceof MethodInvocation) {
                    m = (MethodInvocation) m.getExpression();
                    packages.add(readPackage(m));
                }
            }
        } else {
            if (id.isOnDemand()) {
                packages.add(id.getName().toString());
            } else {
                String iStr = id.getName().toString();
                packages.add(iStr.substring(0, iStr.lastIndexOf(".")));
            }
        }

        return packages.toArray(new String[packages.size()]);
    }

    private String readPackage(MethodInvocation m) {
        return m.resolveMethodBinding().getDeclaringClass().getPackage().getName();
    }
    
    private boolean isBuildPackage(Project project, String packageName) {
        return OSGiHeader.parseHeader(project.getProperty(Constants.BUILDPACKAGES)).containsKey(packageName);
    }

    // The following code is useful for searching for classes that are not yet exported or finding packages via a name but no package
    // i.e. HttpServlet - which one? Well javax.http.servlet.HttpServlet of course but we don't have a package with which to do a search
    // so we need to find all resources that contain this class then propose their packages as quick fixes
//    private void handleUndefinedName(Project project, IInvocationContext context, IProblemLocation problem, HashMap<Object, IJavaCompletionProposal> results) {
//        Name node = findNode(context, problem);
//
//        if (node == null) {
//            return;
//        }
//        addSearchResults(node, project, results);
//    }
//
//    private void handleUndefinedType(Project project, IInvocationContext context, IProblemLocation problem, HashMap<Object, IJavaCompletionProposal> results)
//            throws CoreException {
//        Name node = findNode(context, problem);
//
//        if (node == null) {
//            return;
//        }
//        addSearchResults(node, project, results);
//    }
//
//    private void addSearchResults(Name node, Project project, HashMap<Object, IJavaCompletionProposal> results) {
//        for (SearchResult result : Search.findProviders(node.getFullyQualifiedName(), project, null)) {
//            if (!isBuildPackage(project, result.getPackageName())) {
//                String type = result.getPackageName() + "." + node.getFullyQualifiedName();
//                results.put(type, new ImportSearchResultProposal(result, node, project));
//            }
//        }
//    }
//
//    private Name findName(Type t) {
//        if (t.isSimpleType()) {
//            SimpleType st = (SimpleType) t;
//            return st.getName();
//        } else if (t.isArrayType()) {
//            ArrayType at = (ArrayType) t;
//            return findName(at.getElementType());
//        } else {
//            return null;
//        }
//    }
//
//    private Name findNode(IInvocationContext context, IProblemLocation problem) {
//        ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());
//        if (selectedNode == null) {
//            return null;
//        }
//
//        while (selectedNode.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
//            selectedNode = selectedNode.getParent();
//        }
//
//        Name node = null;
//
//        if (selectedNode instanceof Type) {
//            node = findName((Type) selectedNode);
//        } else if (selectedNode instanceof Name) {
//            node = (Name) selectedNode;
//        }
//
//        return node;
//    }
}
