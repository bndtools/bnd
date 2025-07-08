---
layout: default
class: Project
title: -upto VERSION 
summary: Specify the highest compatibility version, will disable any incompatible features added after this version. 
---

/**
	 * This method is about compatibility. New behavior can be conditionally
	 * introduced by calling this method and passing what version this behavior
	 * was introduced. This allows users of bnd to set the -upto instructions to
	 * the version that they want to be compatible with. If this instruction is
	 * not set, we assume the latest version.
	 */

	Version	upto	= null;

	public boolean since(Version introduced) {
		if (upto == null) {
			String uptov = getProperty(UPTO);
			if (uptov == null) {
				upto = Version.HIGHEST;
				return true;
			}
			if (!Version.VERSION.matcher(uptov).matches()) {
				error("The %s given version is not a version: %s", UPTO, uptov);
				upto = Version.HIGHEST;
				return true;
			}

			upto = new Version(uptov);
		}
		return upto.compareTo(introduced) >= 0;
	}