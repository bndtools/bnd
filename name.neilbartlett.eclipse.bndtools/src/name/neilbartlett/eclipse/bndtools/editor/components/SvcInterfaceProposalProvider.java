package name.neilbartlett.eclipse.bndtools.editor.components;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.utils.JavaContentProposal;

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
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.plugin.Activator;

public class SvcInterfaceProposalProvider implements IContentProposalProvider {
	
	private final IJavaProject javaProject;
	private final IRunnableContext context;
	
	public SvcInterfaceProposalProvider(IJavaProject javaProject) {
		this(javaProject, null);
	}

	public SvcInterfaceProposalProvider(IJavaProject javaProject, IRunnableContext context) {
		this.javaProject = javaProject;
		this.context = context;
	}

	public IContentProposal[] getProposals(String contents, int position) {
		final String prefix = contents.substring(0, position).toLowerCase();
		
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject });
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
			if(context == null) {
				runnable.run(new NullProgressMonitor());
			} else {
				context.run(false, false, runnable);
			}
		} catch (InvocationTargetException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error searching for Java types.", e.getTargetException()));
			return new IContentProposal[0];
		} catch (InterruptedException e) {
			// Reset interrupted status and return empty
			Thread.currentThread().interrupt();
			return new IContentProposal[0];
		}
		
		return result.toArray(new IContentProposal[result.size()]);
	}

}
