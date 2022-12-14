package bndtools.launch;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bndtools.api.launch.LaunchConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.osgi.service.component.annotations.Component;

import aQute.lib.strings.Strings;
import bndtools.launch.api.AbstractLaunchShortcut;

@Component(service = ILaunchShortcut2.class)
public class JUnitShortcut extends AbstractLaunchShortcut {

	public JUnitShortcut() {
		super(LaunchConstants.LAUNCH_ID_OSGI_JUNIT);
	}

	/*
	 * This is called when the launch starts in the editor. If a method or type
	 * is selected in a Java Editor, we will test that type/method.
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
					element = input.getAdapter(IJavaElement.class);
				}

				if (element != null) {
					launchJavaElements(Collections.singletonList(element), mode);
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
	protected void launchJavaElements(List<IJavaElement> elements, String mode) throws CoreException {
		assert elements != null && elements.size() > 0;

		IProject targetProject = elements.get(0)
			.getJavaProject()
			.getProject();

		ILaunchConfigurationWorkingCopy wc = findLaunchConfigWC(null, targetProject);

		if (wc == null) {
			IPath projectPath = targetProject.getFullPath()
				.makeRelative();
			wc = createConfiguration(projectPath, targetProject);
		}

		if (wc != null) {
			customise(elements, wc);
			DebugUITools.launch(wc.doSave(), mode);
		}
	}

	@Override
	protected ILaunchConfiguration findLaunchConfig(IPath targetPath, IProject targetProject) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = findLaunchConfigWC(targetPath, targetProject);
		if (wc != null) {
			wc.doSave();
		}
		return wc;
	}

	protected ILaunchConfigurationWorkingCopy findLaunchConfigWC(IPath targetPath, IProject targetProject)
		throws CoreException {
		ILaunchConfiguration launch = super.findLaunchConfig(targetPath, targetProject);
		if (launch == null) {
			return null;
		}
		ILaunchConfigurationWorkingCopy wc = launch.getWorkingCopy();
		wc.removeAttribute(OSGiJUnitLaunchDelegate.ORG_BNDTOOLS_TESTNAMES);
		return wc;
	}

	/*
	 * From the Java element, we traverse over all the children of the given
	 * element until we hit a Type. Types are then added if they are either a
	 * JUnit 3 (implements junit.framework.Test) or JUnit 4 (one of the methods
	 * has an org.junit annotation)
	 */
	private static void customise(List<IJavaElement> elements, ILaunchConfigurationWorkingCopy config)
		throws JavaModelException {

		assert elements != null;

		Set<String> testNames = new HashSet<>();
		for (IJavaElement element : elements) {
			gatherTests(testNames, element, config);
		}
		if (!testNames.isEmpty()) {
			config.setAttribute(OSGiJUnitLaunchDelegate.ORG_BNDTOOLS_TESTNAMES, Strings.join(" ", testNames));
		}
	}

	private static String getParameterString(IMethod method) {
		StringBuilder builder = new StringBuilder(128);
		builder.append('(');

		// If we don't have parameters there's no need to build an AST
		if (method.getNumberOfParameters() > 0) {
			// In the source, the parameters may have their simple names. But
			// for biz.aQute.tester.junit-platform to recognise the parameters,
			// it needs to be passed in the fully-qualified parameter names. The
			// JavaModel (ie, IMethod) does not resolve the parameters into
			// their fully-qualified type names, so we need to create an AST
			// with resolved bindings to do this.

			// Fetch the Java model compilation unit
			ICompilationUnit cUnit = method.getCompilationUnit();
			// Parse the source
			// (the language specification version should suit your needs)
			ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
			parser.setSource(cUnit);
			// Need the bindings to get the fully-qualified names.
			parser.setResolveBindings(true);
			// Fetch the AST node for the compilation unit
			ASTNode unitNode = parser.createAST(null);
			// It should be an AST compilation unit
			if (unitNode instanceof CompilationUnit) {
				CompilationUnit sourceUnit = (CompilationUnit) unitNode;
				try {
					ISourceRange sr = method.getNameRange();
					NodeFinder nf = new NodeFinder(sourceUnit, sr.getOffset(), sr.getLength());
					ASTNode elementNode = nf.getCoveringNode();
					while (elementNode != null && !(elementNode instanceof MethodDeclaration)) {
						elementNode = elementNode.getParent();
					}
					if (elementNode != null) {
						MethodDeclaration methodDec = (MethodDeclaration) elementNode;
						IMethodBinding binding = methodDec.resolveBinding();
						builder.append(Stream.of(binding.getParameterTypes())
							.map(ITypeBinding::getErasure)
							.map(ITypeBinding::getQualifiedName)
							.collect(Collectors.joining(",")));
					}
				} catch (JavaModelException e) {}
			}
		}
		return builder.append(')')
			.toString();
	}

	/*
	 * Recursive method that traverses the children of a Java Element and
	 * gathers test case types.
	 */
	private static void gatherTests(Set<String> testNames, IJavaElement element, ILaunchConfigurationWorkingCopy config)
		throws JavaModelException {

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
					testNames.add(type.getFullyQualifiedName());
				}
			}
				break;

			/*
			 * Normally we do not traverse to methods but we can call the
			 * customise with a method
			 */
			case IJavaElement.METHOD : {
				IMethod method = (IMethod) element;
				String typeName = ((IType) method.getParent()).getFullyQualifiedName();
				String methodName = method.getElementName();

				testNames.add(typeName + ":" + methodName + getParameterString(method));
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

		if (!type.isClass() || Flags.isAbstract(flags) || Flags.isPrivate(flags))
			return false;

		//
		// One of the super classes/interfaces must be a Junit class/interface
		//
		ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
		// JUnit 3 test - class must be public
		if (Flags.isPublic(flags)) {
			for (IType z : hierarchy.getAllSupertypes(type)) {
				if (z.getFullyQualifiedName()
					.startsWith("junit."))
					return true;
			}
		}

		if (isJUnit(type))
			return true;

		for (IType z : hierarchy.getAllSupertypes(type)) {
			if (isJUnit(z, flags))
				return true;
		}

		return false;
	}

	/*
	 * Check if any of the methods has an annotation from the org.junit package
	 */
	private static boolean isJUnit(IType z) throws JavaModelException {
		return isJUnit(z, z.getFlags());
	}

	private static boolean isJUnit(IType z, int typeFlags) throws JavaModelException {
		for (IMethod m : z.getMethods()) {
			for (IAnnotation annotation : m.getAnnotations()) {
				IType declaringType = m.getDeclaringType();
				if (declaringType != null) {
					String[][] names = declaringType.resolveType(annotation.getElementName());
					if (names != null) {
						for (String[] pair : names) {
							if (pair[0].startsWith("org.junit.")) {
								final int flags = m.getFlags();
								// Jupiter allows public, protected &
								// package-protected methods & classes,
								// JUnit 4 only public
								if ((pair[0].startsWith("org.junit.jupiter.") && !Flags.isPrivate(flags))
									|| (Flags.isPublic(flags) && Flags.isPublic(typeFlags))) {
									return true;
								}
							}
						}
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
			ITextSelection selection = (ITextSelection) editor.getSelectionProvider()
				.getSelection();
			if (selection != null)
				return ((ICompilationUnit) elem).getElementAt(selection.getOffset());
		}
		return null;
	}
}
