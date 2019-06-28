package biz.aQute.bnd.reporter.helpers;

import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import biz.aQute.bnd.reporter.manifest.dto.VersionDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionInRangeDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionRangeDTO;

/**
 * Helper for OSGi version.
 */
public class VersionHelper {

	/**
	 * Convert a string representation of a version range into a DTO.
	 *
	 * @param versionRange the version range to convert
	 * @return the version range DTO or {@code null} if the version range in
	 *         argument is {@code null} or is not a version range
	 */
	static public VersionRangeDTO toRange(final String versionRange) {
		if (versionRange != null && VersionRange.isOSGiVersionRange(versionRange)) {
			return toRange(VersionRange.parseOSGiVersionRange(versionRange));
		} else {
			return null;
		}
	}

	/**
	 * Convert a version range into a DTO.
	 *
	 * @param versionRange the version range to convert
	 * @return the version range DTO
	 */
	static public VersionRangeDTO toRange(final VersionRange versionRange) {
		final VersionRangeDTO result = new VersionRangeDTO();

		result.floor = new VersionInRangeDTO();
		result.floor.include = versionRange.includeLow();
		result.floor.major = versionRange.getLow()
			.getMajor();
		result.floor.minor = Integer.valueOf(versionRange.getLow()
			.getMinor());
		result.floor.micro = Integer.valueOf(versionRange.getLow()
			.getMicro());
		if (versionRange.getLow()
			.getQualifier() != null
			&& !versionRange.getLow()
				.getQualifier()
				.isEmpty()) {
			result.floor.qualifier = versionRange.getLow()
				.getQualifier();
		}

		if (!versionRange.isSingleVersion()) {
			result.ceiling = new VersionInRangeDTO();
			result.ceiling.include = versionRange.includeHigh();
			result.ceiling.major = versionRange.getHigh()
				.getMajor();
			result.ceiling.minor = Integer.valueOf(versionRange.getHigh()
				.getMinor());
			result.ceiling.micro = Integer.valueOf(versionRange.getHigh()
				.getMicro());
			if (versionRange.getHigh()
				.getQualifier() != null
				&& !versionRange.getHigh()
					.getQualifier()
					.isEmpty()) {
				result.ceiling.qualifier = versionRange.getHigh()
					.getQualifier();
			}
		}

		return result;
	}

	/**
	 * Convert a version into a DTO.
	 *
	 * @param version the version to convert
	 * @return the version DTO or {@code null} if the version in argument is
	 *         {@code null} or is not a version
	 */
	static public VersionDTO toVersion(final String version) {
		if (version != null && Version.isVersion(version)) {
			final Version v = Version.parseVersion(version);
			final VersionDTO result = new VersionDTO();

			result.major = v.getMajor();
			result.minor = Integer.valueOf(v.getMinor());
			result.micro = Integer.valueOf(v.getMicro());
			if (v.getQualifier() != null && !v.getQualifier()
				.isEmpty()) {
				result.qualifier = v.getQualifier();
			}

			return result;
		} else {
			return null;
		}
	}

	/**
	 * Create the default version range [0.0.0,inf)
	 *
	 * @return the version range
	 */
	static public VersionRangeDTO createDefaultRange() {
		final VersionRangeDTO range = new VersionRangeDTO();

		range.floor = new VersionInRangeDTO();
		range.floor.major = 0;
		range.floor.minor = Integer.valueOf(0);
		range.floor.micro = Integer.valueOf(0);
		range.floor.include = true;

		return range;
	}

	/**
	 * Create the default version 0.0.0
	 *
	 * @return the version
	 */
	static public VersionDTO createDefaultVersion() {
		final VersionDTO version = new VersionDTO();

		version.major = 0;
		version.minor = Integer.valueOf(0);
		version.micro = Integer.valueOf(0);

		return version;
	}
}
