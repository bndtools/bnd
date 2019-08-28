package bndtools.editor.pkgpatterns;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.lib.exceptions.Exceptions;
import bndtools.javamodel.IJavaSearchContext;
import bndtools.utils.CachingContentProposalProvider;

public class PkgPatternsProposalProvider extends CachingContentProposalProvider {
	private static final ILogger		logger	= Logger.getLogger(PkgPatternsProposalProvider.class);

	private final IJavaSearchContext	searchContext;

	public PkgPatternsProposalProvider(IJavaSearchContext searchContext) {
		this.searchContext = searchContext;
	}

	@Override
	protected Collection<? extends IContentProposal> doGenerateProposals(String contents, int position) {
		String prefix = contents.substring(0, position);

		final int replaceFromPos;
		if (prefix.startsWith("!")) { //$NON-NLS-1$
			prefix = prefix.substring(1);
			replaceFromPos = 1;
		} else {
			replaceFromPos = 0;
		}

		Comparator<PkgPatternProposal> comparator = (o1, o2) -> {
			int result = o1.getPackageFragment()
				.getElementName()
				.compareTo(o2.getPackageFragment()
					.getElementName());
			if (result == 0) {
				result = Boolean.valueOf(o1.isWildcard())
					.compareTo(Boolean.valueOf(o2.isWildcard()));
			}
			return result;
		};
		final TreeSet<PkgPatternProposal> result = new TreeSet<>(comparator);

		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {
			searchContext.getJavaProject()
		});
		final SearchPattern pattern = SearchPattern.createPattern("*" + prefix + "*", IJavaSearchConstants.PACKAGE,
			IJavaSearchConstants.DECLARATIONS, SearchPattern.R_PATTERN_MATCH);
		final SearchRequestor requestor = new SearchRequestor() {
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				IPackageFragment pkg = (IPackageFragment) match.getElement();
				// Reject the default package and any package starting with
				// "java." since these cannot be imported
				if (pkg.isDefaultPackage() || pkg.getElementName()
					.startsWith("java."))
					return;

				result.add(new PkgPatternProposal(pkg, false, replaceFromPos));
				result.add(new PkgPatternProposal(pkg, true, replaceFromPos));
			}
		};
		IRunnableWithProgress runnable = monitor -> {
			try {
				new SearchEngine().search(pattern, new SearchParticipant[] {
					SearchEngine.getDefaultSearchParticipant()
				}, scope, requestor, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};

		try {
			IRunnableContext runContext = searchContext.getRunContext();
			if (runContext != null) {
				runContext.run(false, false, runnable);
			} else {
				runnable.run(new NullProgressMonitor());
			}
			return result;
		} catch (InvocationTargetException e) {
			logger.logError("Error searching for packages.",
				Exceptions.unrollCause(e, InvocationTargetException.class));
			return Collections.emptyList();
		} catch (InterruptedException e) {
			Thread.currentThread()
				.interrupt();
			return Collections.emptyList();
		}
	}

	@Override
	protected boolean match(String contents, int position, IContentProposal proposal) {
		final String prefix = contents.substring(0, position);
		return ((PkgPatternProposal) proposal).getPackageFragment()
			.getElementName()
			.toLowerCase()
			.indexOf(prefix.toLowerCase()) > -1;
	}

}
