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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import bndtools.javamodel.IJavaMethodSearchContext;
import bndtools.utils.CachingContentProposalProvider;

public class MethodProposalProvider extends CachingContentProposalProvider {
	
	private final IJavaMethodSearchContext searchContext;
	
	public MethodProposalProvider(IJavaMethodSearchContext searchContext) {
		this.searchContext = searchContext;
	}

	@Override
    public List<IContentProposal> doGenerateProposals(String contents, int position) {
		final String prefix = contents.substring(0, position);
		final List<IContentProposal> result = new ArrayList<IContentProposal>();
		
		try {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					SubMonitor progress = SubMonitor.convert(monitor, 10);
					
					try {
						IJavaProject project = searchContext.getJavaProject();
						String targetTypeName = searchContext.getTargetTypeName();
						IType targetType = project.findType(targetTypeName, progress.newChild(1));
						
						if(targetType == null)
							return;
						
						ITypeHierarchy hierarchy = targetType.newSupertypeHierarchy(progress.newChild(5));
						IType[] classes = hierarchy.getAllClasses();
						progress.setWorkRemaining(classes.length);
						for (IType clazz : classes) {
							IMethod[] methods = clazz.getMethods();
							for (IMethod method : methods) {
								if(method.getElementName().toLowerCase().startsWith(prefix)) {
									String[] parameterTypes = method.getParameterTypes();
									// TODO check parameter type
									result.add(new MethodContentProposal(method));
								}
							}
							progress.worked(1);
						}
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			IRunnableContext runContext = searchContext.getRunContext();
			if(runContext != null) {
				runContext.run(false, false, runnable);
			} else {
				runnable.run(new NullProgressMonitor());
			}
			return result;
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
	
	@Override
	protected boolean match(String contents, int position, IContentProposal proposal) {
		final String prefix = contents.substring(0, position).toLowerCase();
		IMethod method = ((MethodContentProposal) proposal).getMethod();
		return method.getElementName().toLowerCase().startsWith(prefix);
	}
}
