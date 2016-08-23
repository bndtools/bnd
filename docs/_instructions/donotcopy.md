---
layout: default
class: Project
title: -donotcopy  
summary: Set the default filters for file resources that should not be copied.
---

	/**
	 * doNotCopy The doNotCopy variable maintains a patter for files that should
	 * not be copied. There is a default {@link #DEFAULT_DO_NOT_COPY} but this
	 * ca be overridden with the {@link Constants#DONOTCOPY} property.
	 */

	public boolean doNotCopy(String v) {
		return getDoNotCopy().matcher(v).matches();
	}

	public Pattern getDoNotCopy() {
		if (xdoNotCopy == null) {
			String string = null;
			try {
				string = getProperty(DONOTCOPY, DEFAULT_DO_NOT_COPY);
				xdoNotCopy = Pattern.compile(string);
			}
			catch (Exception e) {
				error("Invalid value for %s, value is %s", DONOTCOPY, string);
				xdoNotCopy = Pattern.compile(DEFAULT_DO_NOT_COPY);
			}
		}
		return xdoNotCopy;
	}

	
	
		private void resolveFiles(File dir, FileFilter filter, boolean recursive, String path, Map<String,File> files,
			boolean flatten) {

		if (doNotCopy(dir.getName())) {
			return;
		}

		File[] fs = dir.listFiles(filter);
		for (File file : fs) {
			if (file.isDirectory()) {
				if (recursive) {
					String nextPath;
					if (flatten)
						nextPath = path;
					else
						nextPath = appendPath(path, file.getName());

					resolveFiles(file, filter, recursive, nextPath, files, flatten);
				}
				// Directories are ignored otherwise
			} else {
				String p = appendPath(path, file.getName());
				if (files.containsKey(p))
					warning(Constants.INCLUDE_RESOURCE + " overwrites entry %s from file %s", p, file);
				files.put(p, file);
			}
		}
		if (fs.length == 0) {
			File empty = new File(dir, Constants.EMPTY_HEADER);
			files.put(appendPath(path, empty.getName()), empty);
		}
	}
	