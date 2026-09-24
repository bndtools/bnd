package org.bndtools.core.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

public class MarkerResolutionProposal implements ICompletionProposal {
	private final IMarkerResolution	resolution;
	private final IMarker				marker;

	public MarkerResolutionProposal(IMarkerResolution resolution, IMarker marker) {
		this.resolution = resolution;
		this.marker = marker;
	}

	@Override
	public void apply(IDocument document) {
		resolution.run(marker);
	}

	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		if (resolution instanceof IMarkerResolution2 res2) {
			return res2.getDescription();
		}
		return null;
	}

	@Override
	public String getDisplayString() {
		return resolution.getLabel();
	}

	@Override
	public Image getImage() {
		if (resolution instanceof IMarkerResolution2 res2) {
			return res2.getImage();
		}
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
}
