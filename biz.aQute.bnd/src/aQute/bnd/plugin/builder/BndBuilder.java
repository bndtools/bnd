package aQute.bnd.plugin.builder;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import aQute.bnd.plugin.*;
import aQute.bnd.plugin.popup.actions.*;
import aQute.lib.osgi.*;

public class BndBuilder extends IncrementalProjectBuilder {

	class DeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				checkBnd(resource);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				checkBnd(resource);
				break;
			}
			// return true to continue visiting children.
			return true;
		}
	}

	class ResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			checkBnd(resource);
			// return true to continue visiting children.
			return true;
		}
	}

	public static final String	BUILDER_ID	= "biz.aQute.bnd.BndBuilder";

	private static final String	MARKER_TYPE	= "biz.aQute.bnd.xmlProblem";

	private void addMarker(IFile file, String message, int lineNumber,
			int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	void checkBnd(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".bnd")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);
			try {
				Builder builder = MakeBundle.setBuilder(Activator.getDefault(),
						resource.getProject(), file.getLocation().toFile());
				try {
					builder.build();
					builder.close();
				} catch (Exception e1) {
					addMarker(file, "Unexpected exception: " + e1, 1,
							Status.ERROR);
				}
				for (Iterator<String> i = builder.getErrors().iterator(); i.hasNext();) {
					addMarker(file, i.next(), 1, Status.ERROR);
				}
				for (Iterator<String> i = builder.getWarnings().iterator(); i.hasNext();) {
					addMarker(file, i.next(), 1, Status.WARNING);
				}
			} catch (Exception e) {
				addMarker(file, "Really bad exception: " + e, 1,
						Status.ERROR);
			}
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
			getProject().accept(new ResourceVisitor());
		} catch (CoreException e) {
		}
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new DeltaVisitor());
	}
}
