package bndtools.utils;

import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class PathUtils {

	/**
	 * This code is copied from the {@link Path#makeRelativeTo(IPath)} method
	 * which was added in Eclipse 3.5.
	 */
	public static IPath makeRelativeTo(IPath path, IPath base) {
		// can't make relative if devices are not equal
		if (path.getDevice() != base.getDevice() && (path.getDevice() == null || !path.getDevice()
			.equalsIgnoreCase(base.getDevice())))
			return path;
		int commonLength = path.matchingFirstSegments(base);
		final int differenceLength = base.segmentCount() - commonLength;
		final int newSegmentLength = differenceLength + path.segmentCount() - commonLength;
		if (newSegmentLength == 0)
			return Path.EMPTY;
		String[] newSegments = new String[newSegmentLength];
		// add parent references for each segment different from the base
		Arrays.fill(newSegments, 0, differenceLength, ".."); //$NON-NLS-1$
		// append the segments of this path not in common with the base
		System.arraycopy(path.segments(), commonLength, newSegments, differenceLength,
			newSegmentLength - differenceLength);

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < newSegments.length; i++) {
			if (i > 0)
				builder.append('/');
			builder.append(newSegments[i]);
		}
		if (path.hasTrailingSeparator())
			builder.append('/');

		return new Path(builder.toString());
	}

}
