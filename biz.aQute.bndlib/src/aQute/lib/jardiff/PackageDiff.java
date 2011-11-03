/*******************************************************************************
 * Copyright (c) 2011 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package aQute.lib.jardiff;

public interface PackageDiff extends Diff, VersionDiff, VersionRangeDiff {

	public enum PackageSeverity {
		NONE(0),
		MICRO(10),
		MINOR(20),
		MAJOR(30);
		
		private final int severity;
		private PackageSeverity(int severity) {
			this.severity = severity;
		}
		
		public int value() {
			return severity;
		}
	}

	String getPackageName();
	
	boolean isImported();
	
	boolean isExported();
		
}
