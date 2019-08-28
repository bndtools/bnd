package bndtools.internal.pkgselection;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class JavaSearchScopePackageLister implements IPackageLister {

	private final IJavaSearchScope	scope;
	private final IRunnableContext	runContext;

	public JavaSearchScopePackageLister(IJavaSearchScope scope, IRunnableContext runContext) {
		this.scope = scope;
		this.runContext = runContext;
	}

	@Override
	public String[] getPackages(boolean includeNonSource, IPackageFilter filter) throws PackageListException {
		final List<IJavaElement> packageList = new LinkedList<>();
		final SearchRequestor requestor = new SearchRequestor() {
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				IJavaElement enclosingElement = (IJavaElement) match.getElement();
				String name = enclosingElement.getElementName();
				if (name.length() > 0) { // Do not include default pkg
					packageList.add(enclosingElement);
				}
			}
		};
		final SearchPattern pattern = SearchPattern.createPattern("*", IJavaSearchConstants.PACKAGE,
			IJavaSearchConstants.DECLARATIONS, SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE);

		IRunnableWithProgress operation = monitor -> {
			try {
				new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor,
					monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};

		try {
			runContext.run(true, true, operation);
		} catch (InvocationTargetException e) {
			throw new PackageListException(e.getCause());
		} catch (InterruptedException e) {
			throw new PackageListException("Operation interrupted");
		}

		// Remove non-source and excludes
		Set<String> packageNames = new LinkedHashSet<>();
		for (Iterator<IJavaElement> iter = packageList.iterator(); iter.hasNext();) {
			boolean omit = false;
			IJavaElement element = iter.next();
			if (!includeNonSource) {
				IPackageFragment pkgFragment = (IPackageFragment) element;
				try {
					if (pkgFragment.getCompilationUnits().length == 0) {
						omit = true;
					}
				} catch (JavaModelException e) {
					throw new PackageListException(e);
				}
			}

			if (filter != null && !filter.select(element.getElementName())) {
				omit = true;
			}
			if (!omit) {
				packageNames.add(element.getElementName());
			}
		}

		return packageNames.toArray(new String[0]);
	}

}
