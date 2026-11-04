package org.bndtools.builder.handlers.activator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BundleActivatorErrorHandlerTest {
	private IJavaProject project;
	private IType type;
	private IClasspathEntry entry;

	@BeforeEach
	void setUp() throws Exception {
		project = mock(IJavaProject.class);
		type = mock(IType.class);
		entry = mock(IClasspathEntry.class);
		IPackageFragmentRoot root = mock(IPackageFragmentRoot.class);
		IPackageFragment fragment = mock(IPackageFragment.class);
		when(type.getCompilationUnit()).thenReturn(mock(ICompilationUnit.class));
		when(type.getJavaProject()).thenReturn(project);
		when(type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT)).thenReturn(root);
		when(root.getRawClasspathEntry()).thenReturn(entry);
		when(type.getPackageFragment()).thenReturn(fragment);
		when(fragment.getElementName()).thenReturn("example");
		when(type.isClass()).thenReturn(true);
		when(type.getFlags()).thenReturn(Flags.AccPublic);
		when(type.getMethods()).thenReturn(new IMethod[0]);
	}

	@Test
	void acceptsImplicitPublicConstructor() throws Exception {
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isTrue();
	}

	@Test
	void rejectsPrivateConstructor() throws Exception {
		constructor(Flags.AccPrivate, 0);
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isFalse();
	}

	@Test
	void rejectsConstructorWithArguments() throws Exception {
		constructor(Flags.AccPublic, 1);
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isFalse();
	}

	@Test
	void acceptsPublicNoArgumentConstructor() throws Exception {
		constructor(Flags.AccPublic, 0);
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isTrue();
	}

	@Test
	void rejectsNonStaticMemberClass() throws Exception {
		when(type.isMember()).thenReturn(true);
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isFalse();
	}

	@Test
	void rejectsTestSource() throws Exception {
		when(entry.isTest()).thenReturn(true);
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isFalse();
	}

	@Test
	void rejectsDependencySource() throws Exception {
		when(type.getJavaProject()).thenReturn(mock(IJavaProject.class));
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isFalse();
	}

	@Test
	void rejectsAbstractClass() throws Exception {
		when(type.getFlags()).thenReturn(Flags.AccPublic | Flags.AccAbstract);
		assertThat(BundleActivatorErrorHandler.isActivatorCandidate(type, project)).isFalse();
	}

	private void constructor(int flags, int parameters) throws Exception {
		IMethod method = mock(IMethod.class);
		when(method.isConstructor()).thenReturn(true);
		when(method.getFlags()).thenReturn(flags);
		when(method.getNumberOfParameters()).thenReturn(parameters);
		when(type.getMethods()).thenReturn(new IMethod[] {method});
	}
}