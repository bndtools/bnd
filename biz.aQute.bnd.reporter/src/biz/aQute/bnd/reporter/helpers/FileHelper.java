package biz.aQute.bnd.reporter.helpers;

import java.io.File;

/**
 * Some {@code File} utility functions.
 */
public class FileHelper {

	/**
	 * @param file the file we want its name, may be {@code null}
	 * @return the name of the file in arguments without its extension, never
	 *         {@code null}
	 */
	public static String getName(final File file) {
		if (file != null) {
			final int pos = file.getName()
				.lastIndexOf(".");
			if (pos <= 0) {
				return file.getName();
			} else {
				return file.getName()
					.substring(0, pos);
			}
		} else {
			return "";
		}
	}

	/**
	 * @param file the file we want its extension, may be {@code null}
	 * @return the extension of the file in arguments, never {@code null}
	 */
	public static String getExtension(final File file) {
		if (file != null) {
			final int n = file.getName()
				.lastIndexOf('.');
			if (n <= 0) {
				return "";
			} else {
				return file.getName()
					.substring(n + 1);
			}
		} else {
			return "";
		}
	}

	/**
	 * @param file the file for which we are looking for a specific sibling
	 *            file, may be {@code null}
	 * @param availableExtensions an {@code array} of possible extensions, may
	 *            be {@code null}
	 * @return a file which is in the same folder as the file in argument and
	 *         has the same name but with a different extension and that its
	 *         extension (of the returned file) is in the availableExtensions
	 *         array (case insensitive), or null if not found.
	 */
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
							if (!fileExtension.equalsIgnoreCase(otherExtension)
								&& ArrayHelper.containsIgnoreCase(availableExtensions, otherExtension)) {
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
