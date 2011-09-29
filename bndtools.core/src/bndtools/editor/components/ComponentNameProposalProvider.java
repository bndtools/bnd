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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.javamodel.IJavaSearchContext;
import bndtools.utils.CachingContentProposalProvider;
import bndtools.utils.JavaContentProposal;

public class ComponentNameProposalProvider extends CachingContentProposalProvider {

	protected static final String XML_SUFFIX = ".xml";
	private final IJavaSearchContext searchContext;

	public ComponentNameProposalProvider(IJavaSearchContext searchContext) {
		this.searchContext = searchContext;
	}

	public ComponentNameProposalProvider(final IJavaProject javaProject) {
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
		String lowerCasePrefix = contents.substring(0, position).toLowerCase();
		if(proposal instanceof ResourceProposal) {
			return ((ResourceProposal) proposal).getName().toLowerCase().startsWith(lowerCasePrefix);
		}
		if(proposal instanceof JavaContentProposal) {
			return ((JavaContentProposal) proposal).getTypeName().toLowerCase().startsWith(lowerCasePrefix);
		}
		return false;
	}

	@Override
	protected List<IContentProposal> doGenerateProposals(String contents, int position) {
		final String prefix = contents.substring(0, position);
		IJavaProject javaProject = searchContext.getJavaProject();
		final List<IContentProposal> result = new ArrayList<IContentProposal>(100);

		// Resource matches
		IProject project = javaProject.getProject();
		try {
			project.accept(new IResourceProxyVisitor() {
				public boolean visit(IResourceProxy proxy) throws CoreException {
					if(proxy.getType() == IResource.FILE) {
						if(proxy.getName().toLowerCase().startsWith(prefix) && proxy.getName().toLowerCase().endsWith(XML_SUFFIX)) {
							result.add(new ResourceProposal(proxy.requestResource()));
						}
						return false;
					}
					// Recurse into everything else
					return true;
				}
			}, 0);
		} catch (CoreException e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error searching for resources.", e));
		}

		// Class matches
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject });
		final TypeNameRequestor requestor = new TypeNameRequestor() {
			@Override
            public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
				if(!Flags.isAbstract(modifiers) && (Flags.isPublic(modifiers) || Flags.isProtected(modifiers))) {
					result.add(new JavaContentProposal(new String(packageName), new String(simpleTypeName), false));
				}
			};
		};
		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					new SearchEngine().searchAllTypeNames(null, 0, prefix.toCharArray(), SearchPattern.R_PREFIX_MATCH, IJavaSearchConstants.CLASS, scope, requestor, IJavaSearchConstants.CANCEL_IF_NOT_READY_TO_SEARCH, monitor);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		IRunnableContext runContext = searchContext.getRunContext();
		try {
			if(runContext != null) {
				runContext.run(false, false, runnable);
			} else {
				runnable.run(new NullProgressMonitor());
			}
		} catch (InvocationTargetException e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error searching for classes.", e.getTargetException()));
		} catch (InterruptedException e) {
			// Reset the interruption status and continue
			Thread.currentThread().interrupt();
		}
		return result;
	}

	ILabelProvider createLabelProvider() {
		return new ClassOrResourceLabelProvider();
	}

	private static class ResourceProposal implements IContentProposal {

		final String name;
		final String fullPath;

		public ResourceProposal(IResource resource) {
			name = resource.getName();
			fullPath = resource.getProjectRelativePath().toString();
		}
		public String getContent() {
			return fullPath;
		}
		public int getCursorPosition() {
			return name.length();
		}
		public String getDescription() {
			return null;
		}
		public String getLabel() {
			return name + " (" + fullPath + ")";
		}
		public String getName() {
			return name;
		}
	}

	private static class ClassOrResourceLabelProvider extends LabelProvider {

		private Image xmlImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/xml_file_obj.gif").createImage();
		private Image classImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/class_obj.gif").createImage();

		@Override
		public String getText(Object element) {
			return ((IContentProposal) element).getLabel();
		}

		@Override
		public Image getImage(Object element) {
			Image result = null;
			if(element instanceof ResourceProposal) {
				result = xmlImg;
			} else if(element instanceof JavaContentProposal) {
				result = classImg;
			}
			return result;
		}
		@Override
		public void dispose() {
			super.dispose();
			xmlImg.dispose();
			classImg.dispose();
		}
	}
}
