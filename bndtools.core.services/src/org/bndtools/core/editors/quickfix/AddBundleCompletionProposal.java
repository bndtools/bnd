package org.bndtools.core.editors.quickfix;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.lib.strings.Strings;
import bndtools.central.Central;

class AddBundleCompletionProposal extends WorkspaceJob implements IJavaCompletionProposal {

	final BundleId				bundle;
	final String				displayString;
	final int					relevance;
	final IInvocationContext	context;
	final Project				project;
	final String				pathtype;
	final String				fqn;
	final boolean				doImport;

	public AddBundleCompletionProposal(String fqn, BundleId bundle, int relevance, IInvocationContext context,
		Project project, String pathtype, boolean doImport) {
		super("Adding '" + bundle + "' to " + project + " " + pathtype);
		this.fqn = fqn;
		this.bundle = bundle;
		this.relevance = relevance;
		this.context = context;
		this.project = project;
		this.pathtype = pathtype;
		this.doImport = doImport;
		this.displayString = Strings.format("Add %s %s to %s (found %s)", bundle.getBsn(), bundle.getShortVersion(),
			pathtype, fqn);
	}

	@Override
	public void apply(org.eclipse.jface.text.IDocument document) {
		schedule();
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	@Override
	public Point getSelection(org.eclipse.jface.text.IDocument document) {
		return new Point(context.getSelectionOffset(), context.getSelectionLength());
	}

	@Override
	public String getAdditionalProposalInfo() {
		return displayString;
	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public Image getImage() {
		return Icons.image("bundle");
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public int getRelevance() {
		return relevance;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		try {
			IStatus status = Central.bndCall(() -> {

				BndEditModel model = new BndEditModel(project);
				model.load();

				switch (pathtype) {
					case Constants.TESTPATH :
						model.addPath(bundle, Constants.TESTPATH);
						break;

					case Constants.BUILDPATH :
					default :
						model.addPath(bundle, Constants.BUILDPATH);
						break;
				}

				model.saveChanges();
				Central.refreshFile(project.getPropertiesFile());
				return Status.OK_STATUS;

			}, monitor);

			String[] determine = Descriptors.determine(fqn)
				.unwrap();

			assert determine[0] != null : "We must have found a package";
			if (doImport) {
				if (determine[1] == null) {
					context.getCompilationUnit()
						.createImport(fqn + ".*", null, monitor);
				} else {
					context.getCompilationUnit()
						.createImport(fqn, null, monitor);
				}
			}
			return status;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, "bndtools.core.services",
				"Failed to add bundle " + bundle + " to " + pathtype, e);
		}
	}
}
