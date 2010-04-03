/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.editor.components;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import bndtools.Plugin;
import bndtools.javamodel.IJavaSearchContext;
import bndtools.utils.CachingContentProposalProvider;
import bndtools.utils.JavaContentProposal;

import aQute.bnd.plugin.Activator;

public class SvcInterfaceProposalProvider extends CachingContentProposalProvider {
	
	private IJavaSearchContext searchContext;
	
	public SvcInterfaceProposalProvider(IJavaSearchContext searchContext) {
		this.searchContext = searchContext;
	}
	
	public SvcInterfaceProposalProvider(final IJavaProject javaProject) {
		this(new IJavaSearchContext() {
			public IJavaProject getJavaProject() {
				return javaProject;
			}
			public IRunnableContext getRunContext() {
				return null;
			}
		});
	}
	
	@Override
	protected boolean match(String contents, int position, IContentProposal proposal) {
		final String prefix = contents.substring(0, position).toLowerCase();
		return ((JavaContentProposal) proposal).getTypeName().toLowerCase().startsWith(prefix);
	}
	
	@Override
	protected List<IContentProposal> doGenerateProposals(String contents, int position) {
		final String prefix = contents.substring(0, position);
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { searchContext.getJavaProject() });
		final ArrayList<IContentProposal> result = new ArrayList<IContentProposal>(100);
		final TypeNameRequestor typeNameRequestor = new TypeNameRequestor() {
			@Override
			public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
				boolean isInterface = Flags.isInterface(modifiers);
				result.add(new JavaContentProposal(new String(packageName), new String(simpleTypeName), isInterface));
			}
		};
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					new SearchEngine().searchAllTypeNames(null, 0, prefix.toCharArray(), SearchPattern.R_PREFIX_MATCH, IJavaSearchConstants.CLASS_AND_INTERFACE, scope, typeNameRequestor, IJavaSearchConstants.CANCEL_IF_NOT_READY_TO_SEARCH, monitor);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		
		try {
			if(searchContext.getRunContext() == null) {
				runnable.run(new NullProgressMonitor());
			} else {
				searchContext.getRunContext().run(false, false, runnable);
			}
		} catch (InvocationTargetException e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error searching for Java types.", e.getTargetException()));
			return Collections.emptyList();
		} catch (InterruptedException e) {
			// Reset interrupted status and return empty
			Thread.currentThread().interrupt();
			return Collections.emptyList();
		}
		return result;
	}

}
