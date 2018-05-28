package biz.aQute.bnd.reporter.helpers;

import java.io.File;
import java.io.IOException;

final public class FileHelper {
	
	private FileHelper() {
	}
	
	public static String getName(final File file) {
		if (file != null) {
			final int pos = file.getName().lastIndexOf(".");
			if (pos <= 0) {
				return file.getName();
			} else {
				return file.getName().substring(0, pos);
			}
		} else {
			return "";
		}
	}
	
	public static String getExtension(final File file) {
		if (file != null) {
			final int n = file.getName().lastIndexOf('.');
			if (n <= 0) {
				return "";
			} else {
				return file.getName().substring(n + 1);
			}
		} else {
			return "";
		}
	}
	
	public static String getCanonicalPath(final File file) {
		try {
			return file.getCanonicalPath();
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}
	
	public static File searchSiblingWithDifferentExtension(final File file, final String[] availableExtensions) {
		File[] files = null;
		if (file != null && availableExtensions != null && availableExtensions.length > 0) {
			final File parent = file.getParentFile();
			
			if (parent != null) {
				files = parent.listFiles((other) -> {
					if (other.isFile()) {
						final String fileName = getName(file);
						final String otherName = getName(other);
						if (fileName.equalsIgnoreCase(otherName)) {
							final String fileExtension = getExtension(file);
							final String otherExtension = getExtension(other);
							if (!fileExtension.equalsIgnoreCase(otherExtension) && ArrayHelper
								.containsIgnoreCase(availableExtensions, otherExtension)) {
								return true;
							}
						}
					}
					return false;
				});
			}
		}
		if (files != null && files.length > 0) {
			return files[0];
		} else {
			return null;
		}
	}
}
