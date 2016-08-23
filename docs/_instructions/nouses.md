---
layout: default
class: Project
title: -nouses  BOOLEAN
summary: Do not generate uses directives on the exported packages. 
---

	/**
	 * Add the uses clauses. This method iterates over the exports and cal
	 * 
	 * @param exports
	 * @param uses
	 * @throws MojoExecutionException
	 */
	void doUses(Packages exports, Map<PackageRef,List<PackageRef>> uses, Packages imports) {
		if (isTrue(getProperty(NOUSES)))
			return;

		for (Iterator<PackageRef> i = exports.keySet().iterator(); i.hasNext();) {
			PackageRef packageRef = i.next();
			String packageName = packageRef.getFQN();
			setProperty(CURRENT_PACKAGE, packageName);
			try {
				doUses(packageRef, exports, uses, imports);
			}
			finally {
				unsetProperty(CURRENT_PACKAGE);
			}

		}
	}

