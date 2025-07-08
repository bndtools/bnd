---
layout: default
title: -upto VERSION
class: Project
summary: |
   Specify the highest compatibility version, will disable any incompatible features added after this version.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-upto: 2.3.1`

- Pattern: `(\d{1,10})(\.(\d{1,10})(\.(\d{1,10})(\.([-\w]+))?)?)?`

<!-- Manual content from: ext/upto.md --><br /><br />

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
