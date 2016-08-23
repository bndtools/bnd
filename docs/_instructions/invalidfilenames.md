---
layout: default
class: Project
title: -invalidfilenames  
summary:  Specify file/directory names that should not be used because they are not portable.
---

	/**
	 * Verify of the path names in the JAR are valid on all OS's (mainly
	 * windows)
	 */
	void verifyPathNames() {
		if (!since(About._2_3))
			return;

		Set<String> invalidPaths = new HashSet<String>();
		Pattern pattern = ReservedFileNames;
		setProperty("@", ReservedFileNames.pattern());
		String p = getProperty(INVALIDFILENAMES);
		unsetProperty("@");
		if (p != null) {
			try {
				pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
			}
			catch (Exception e) {
				SetLocation error = error("%s is not a valid regular expression %s: %s", INVALIDFILENAMES,
						e.getMessage(), p);
				error.context(p).header(INVALIDFILENAMES);
				return;
			}
		}

		Set<String> segments = new HashSet<String>();
		for (String path : dot.getResources().keySet()) {
			String parts[] = path.split("/");
			for (String part : parts) {
				if (segments.add(part) && pattern.matcher(part).matches()) {
					invalidPaths.add(path);
				}
			}
		}

		if (invalidPaths.isEmpty())
			return;

		error("Invalid file/directory names for Windows in JAR: %s. You can set the regular expression used with %s, the default expression is %s",
				invalidPaths, INVALIDFILENAMES, ReservedFileNames.pattern());
	}



	public final static Pattern	ReservedFileNames				= Pattern
																		.compile(
																				"CON(\\..+)?|PRN(\\..+)?|AUX(\\..+)?|CLOCK$|NUL(\\..+)?|COM[1-9](\\..+)?|LPT[1-9](\\..+)?|"
																						+ "\\$Mft|\\$MftMirr|\\$LogFile|\\$Volume|\\$AttrDef|\\$Bitmap|\\$Boot|\\$BadClus|\\$Secure|"
																						+ "\\$Upcase|\\$Extend|\\$Quota|\\$ObjId|\\$Reparse",
																				Pattern.CASE_INSENSITIVE);
