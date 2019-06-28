package org.bndtools.core.editors;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.text.edits.ReplaceEdit;

public class PackageInfoBaselineQuickFixProcessor implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return "package-info.java".equals(unit.getElementName());
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
		throws CoreException {
		ICompilationUnit compUnit = context.getCompilationUnit();
		IResource resource = compUnit.getResource();
		IMarker[] markers = resource.findMarkers(BndtoolsConstants.MARKER_JAVA_BASELINE, false, 1);

		for (IProblemLocation location : locations) {
			for (IMarker marker : markers) {
				int markerStart = marker.getAttribute(IMarker.CHAR_START, -1);
				int markerEnd = marker.getAttribute(IMarker.CHAR_END, -1);
				int markerLength = markerEnd - markerStart;
				if (location.getOffset() <= markerStart && markerStart < location.getOffset() + location.getLength()) {
					String newVersion = marker.getAttribute("suggestedVersion", null);
					if (newVersion != null) {
						StringBuilder quotedVersion = new StringBuilder(newVersion.trim());
						if (quotedVersion.charAt(0) != '"')
							quotedVersion.insert(0, '"');
						if (quotedVersion.charAt(quotedVersion.length() - 1) != '"')
							quotedVersion.append('"');

						CompilationUnitChange change = new CompilationUnitChange("Change package-info.java", compUnit);
						change.setEdit(new ReplaceEdit(markerStart, markerLength, quotedVersion.toString()));

						return new IJavaCompletionProposal[] {
							new ChangeCorrectionProposal("Change package version to " + newVersion, change, 1000)
						};
					}
				}
			}
		}
		return null;
	}

}
