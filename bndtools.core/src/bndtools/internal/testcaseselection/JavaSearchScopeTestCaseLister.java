package bndtools.internal.testcaseselection;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import bndtools.internal.pkgselection.SearchUtils;

public class JavaSearchScopeTestCaseLister implements ITestCaseLister {

	private final IJavaSearchScope	scope;
	private final IRunnableContext	runContext;

	public JavaSearchScopeTestCaseLister(IJavaSearchScope scope, IRunnableContext runContext) {
		this.scope = scope;
		this.runContext = runContext;
	}

	@Override
	public String[] getTestCases(boolean includeNonSource, ITestCaseFilter filter) throws TestCaseListException {
		final List<IJavaElement> testCaseList = new LinkedList<>();

		search(Arrays.asList("junit.framework.TestCase", "junit.framework.TestSuite"), testCaseList); //$NON-NLS-1$ //$NON-NLS-2$

		// Remove non-source and excludes
		Set<String> testCaseNames = new LinkedHashSet<>();
		for (Iterator<IJavaElement> iter = testCaseList.iterator(); iter.hasNext();) {
			boolean omit = false;
			IJavaElement element = iter.next();
			try {

				IType type = (IType) element.getAncestor(IJavaElement.TYPE);
				if (Flags.isAbstract(type.getFlags())) {
					omit = true;
				}

				if (!includeNonSource) {
					IPackageFragment pkgFragment = (IPackageFragment) element
						.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
					if (pkgFragment.getCompilationUnits().length == 0) {
						omit = true;
					}
				}

			} catch (JavaModelException e) {
				throw new TestCaseListException(e);
			}
			String className = getClassName(element);
			if (filter != null && !filter.select(className)) {
				omit = true;
			}
			if (!omit) {
				testCaseNames.add(className);
			}
		}

		return testCaseNames.toArray(new String[0]);
	}

	private static String getClassName(IJavaElement element) {

		IPackageFragment pkgFragment = (IPackageFragment) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		String packageName = pkgFragment.getElementName();
		if (packageName.length() > 0) {
			return packageName + '.' + element.getElementName();
		}
		return element.getElementName();
	}

	public void search(List<String> types, final List<IJavaElement> testCaseList) throws TestCaseListException {

		IRunnableWithProgress operation = new SearchOperation(types, testCaseList, scope);
		try {
			runContext.run(true, true, operation);
		} catch (InvocationTargetException e) {
			throw new TestCaseListException(e.getCause());
		} catch (InterruptedException e) {
			throw new TestCaseListException(Messages.JavaSearchScopeTestCaseLister_2);
		}
	}

	private static class SearchOperation implements IRunnableWithProgress {

		final List<String>			types;
		final List<IJavaElement>	testCaseList;
		final IJavaSearchScope		scope;

		public SearchOperation(List<String> types, List<IJavaElement> testCaseList, IJavaSearchScope scope) {
			this.types = types;
			this.testCaseList = testCaseList;
			this.scope = scope;
		}

		private void search(List<String> types, final List<IJavaElement> testCaseList, IProgressMonitor monitor)
			throws TestCaseListException {
			for (String type : types) {
				List<String> newTypes = search(type, testCaseList, monitor);
				if (!newTypes.isEmpty()) {
					search(newTypes, testCaseList, monitor);
				}
			}
		}

		private List<String> search(String type, final List<IJavaElement> testCaseList, IProgressMonitor monitor)
			throws TestCaseListException {

			SearchPattern pattern = SearchPattern.createPattern(type, IJavaSearchConstants.CLASS,
				IJavaSearchConstants.IMPLEMENTORS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);

			final List<String> typesFound = new ArrayList<>();

			SearchRequestor requestor = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					IJavaElement enclosingElement = (IJavaElement) match.getElement();
					if (!testCaseList.contains(enclosingElement)) {
						typesFound.add(getClassName(enclosingElement));
					}
					testCaseList.add(enclosingElement);
				}
			};
			try {
				new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor,
					monitor);
			} catch (CoreException e) {
				throw new TestCaseListException(e);
			}
			return typesFound;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				search(types, testCaseList, monitor);
			} catch (TestCaseListException e) {
				throw new InvocationTargetException(e);
			}
		}
	}
}
