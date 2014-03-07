package bndtools.launch;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import aQute.lib.strings.Strings;
import bndtools.launch.api.AbstractLaunchShortcut;

public class JUnitShortcut extends AbstractLaunchShortcut {

    public JUnitShortcut() {
        super(LaunchConstants.LAUNCH_ID_OSGI_JUNIT);
    }

    /*
    * This is called when the launch starts in the editor. If a method or type is selected
    * in a Java Editor, we will test that type/method.
    */
    @Override
    public void launch(IEditorPart editor, String mode) {
        try {

            //
            // Check if a method is selected. It is more precise then the 
            // older next method that adapts the editor to a JavaElement
            // this in general returns a Compilation Unit
            //

            if (editor instanceof JavaEditor) {
                IJavaElement element = getSelectedJavaElement((JavaEditor) editor);
                if (element == null) {
                    IEditorInput input = editor.getEditorInput();
                    element = (IJavaElement) input.getAdapter(IJavaElement.class);
                }

                if (element != null) {
                    launchJavaElement(element, mode);
                    return;
                }
            }

        } catch (CoreException e) {
            e.printStackTrace();
        }
        //
        // Did not succeed to get a JavaElement, we try the original way
        //
        super.launch(editor, mode);
    }

    @Override
    protected void launchJavaElement(IJavaElement element, String mode) throws CoreException {
        IPath projectPath = element.getJavaProject().getProject().getFullPath().makeRelative();

        ILaunchConfiguration config = findLaunchConfig(projectPath);
        if (config == null) {
            ILaunchConfigurationWorkingCopy wc = createConfiguration(projectPath);
            wc.doSave();

            customise(element, wc);
            config = wc;
        } else {
            ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
            customise(element, wc);
            config = wc;
        }
        DebugUITools.launch(config, mode);
    }

    /*
     * From the Java element, we traverse over all the children of the given element 
     * until we hit a Type. Types are then added if they are either a JUnit 3 
     * (implements junit.framework.Test) or JUnit 4 (one of the methods has an org.junit 
     * annotation)
     * 
     */
    private static void customise(IJavaElement element, ILaunchConfigurationWorkingCopy config) throws JavaModelException {

        assert element != null;

        Set<String> testNames = new HashSet<String>();
        gatherTests(testNames, element, config);
        if (!testNames.isEmpty()) {
            config.setAttribute(OSGiJUnitLaunchDelegate.ORG_BNDTOOLS_TESTNAMES, Strings.join(" ", testNames));
        }
    }

    /*
     * Recursive method that traverses the children of a Java Element and gathers test case types.
     */
    private static void gatherTests(Set<String> testNames, IJavaElement element, ILaunchConfigurationWorkingCopy config) throws JavaModelException {

        assert element != null;

        if (element.isReadOnly()) // not interested in in non-source
            return;

        switch (element.getElementType()) {

        /*
         * For a type, we only have to check if it is testable.
         */
        case IJavaElement.TYPE : {
            IType type = (IType) element;
            if (isTestable(type)) {
                testNames.add((type).getFullyQualifiedName());
            }
        }
            break;

        /*
         * Normally we do not traverse to methods but we can call the customise with a method 
         */
        case IJavaElement.METHOD : {
            IMethod method = (IMethod) element;
            String typeName = ((IType) method.getParent()).getFullyQualifiedName();
            String methodName = method.getElementName();
            testNames.add(typeName + ":" + methodName);
        }
            break;

        /*
         * Default we go deeper if the element is a parent
         */
        default : {
            if (element instanceof IParent)
                for (IJavaElement type : ((IParent) element).getChildren()) {
                    gatherTests(testNames, type, config);
                }
        }
            break;
        }
    }

    /*
     * Check if this element can be tested
     */
    private static boolean isTestable(IType type) throws JavaModelException {

        assert type != null;

        int flags = type.getFlags();

        //
        // Must be a class, not abstract, and public
        //

        if (!type.isClass() || Flags.isAbstract(flags) || !Flags.isPublic(flags))
            return false;

        //
        // One of the super classes/interfaces must be a Junit class/interface
        //
        ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
        for (IType z : hierarchy.getAllSupertypes(type)) {
            if (z.getFullyQualifiedName().startsWith("junit."))
                return true;
        }

        if (isJUnit4(type))
            return true;

        for (IType z : hierarchy.getAllSuperclasses(type)) {
            if (isJUnit4(z))
                return true;
        }

        return false;
    }

    /*
    *
    * Check if any of the methods has an annotation
    * from the org.junit package
    *
    */

    private static boolean isJUnit4(IType z) throws JavaModelException {
        for (IMethod m : z.getMethods()) {
            if (Flags.isPublic(m.getFlags())) {
                for (IAnnotation annotation : m.getAnnotations()) {
                    String[][] names = m.getDeclaringType().resolveType(annotation.getElementName());
                    for (String[] pair : names) {
                        if (pair[0].contains("org.junit"))
                            return true;
                    }
                }
            }
        }

        return false;
    }

    /*
     * Helper method to find out the selected Java Element from a Java Editor
     */
    private IJavaElement getSelectedJavaElement(JavaEditor editor) throws JavaModelException {
        IJavaElement elem = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
        if (elem instanceof ICompilationUnit) {
            ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
            if (selection != null)
                return ((ICompilationUnit) elem).getElementAt(selection.getOffset());
        }
        return null;
    }
}