/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.classpath;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class ResolutionProblem {
	private final BundleDependency dependency;
	private final String message;
	private final List<RejectedExportCandidate> rejectedCandidates = new LinkedList<RejectedExportCandidate>();
	
	public ResolutionProblem(String message, BundleDependency dependency) {
		this.message = message;
		this.dependency = dependency;
	}
	
	public BundleDependency getDependency() {
		return dependency;
	}
	public String getMessage() {
		return message;
	}
	public List<RejectedExportCandidate> getRejectedCandidates() {
		return rejectedCandidates;
	}
	public void addRejectedCandidate(RejectedExportCandidate candidate) {
		rejectedCandidates.add(candidate);
	}
	public void addRejectedCandidates(Collection<? extends RejectedExportCandidate> rejections) {
		rejectedCandidates.addAll(rejections);
	}
}

class RejectedExportCandidate {
	private final ExportedBundle export;
	private final String reason;
	private final boolean cycle;

	public RejectedExportCandidate(ExportedBundle export, String reason, boolean cycle) {
		this.export = export;
		this.reason = reason;
		this.cycle = cycle;
	}
	public ExportedBundle getExportCandidate() {
		return export;
	}
	public String getReason() {
		return reason;
	}
	public boolean isCycle() {
		return cycle;
	}
}
