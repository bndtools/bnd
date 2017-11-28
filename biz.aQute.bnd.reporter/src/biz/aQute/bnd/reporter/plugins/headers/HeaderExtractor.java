package biz.aQute.bnd.reporter.plugins.headers;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.VersionDTO;
import biz.aQute.bnd.reporter.plugins.headers.dto.VersionInRangeDTO;
import biz.aQute.bnd.reporter.plugins.headers.dto.VersionRangeDTO;

abstract class HeaderExtractor {

	abstract public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter);

	abstract public String getEntryName();

	protected VersionRangeDTO toOsgiRange(final String value) {

		if (value != null && VersionRange.isOSGiVersionRange(value)) {
			return toRange(VersionRange.parseOSGiVersionRange(value));
		} else {
			return null;
		}
	}

	protected VersionRangeDTO toRange(final VersionRange range) {

		final VersionRangeDTO result = new VersionRangeDTO();

		result.floor = new VersionInRangeDTO();
		result.floor.include = range.includeLow();
		result.floor.major = range.getLow().getMajor();
		result.floor.minor = range.getLow().getMinor();
		result.floor.micro = range.getLow().getMicro();
		if (range.getLow().getQualifier() != null && !range.getLow().getQualifier().isEmpty()) {
			result.floor.qualifier = range.getLow().getQualifier();
		}

		if (!range.isSingleVersion()) {
			result.ceiling = new VersionInRangeDTO();
			result.ceiling.include = range.includeHigh();
			result.ceiling.major = range.getHigh().getMajor();
			result.ceiling.minor = range.getHigh().getMinor();
			result.ceiling.micro = range.getHigh().getMicro();
			if (range.getHigh().getQualifier() != null && !range.getHigh().getQualifier().isEmpty()) {
				result.ceiling.qualifier = range.getHigh().getQualifier();
			}
		}

		return result;
	}

	protected VersionDTO toVersion(final String value) {

		if (value != null && Version.isVersion(value)) {
			final Version version = Version.parseVersion(value);
			final VersionDTO result = new VersionDTO();

			result.major = version.getMajor();
			result.minor = version.getMinor();
			result.micro = version.getMicro();
			if (version.getQualifier() != null && !version.getQualifier().isEmpty()) {
				result.qualifier = version.getQualifier();
			}

			return result;
		} else {
			return null;
		}
	}

	protected VersionRangeDTO getDefaultRange() {

		final VersionRangeDTO range = new VersionRangeDTO();

		range.floor = new VersionInRangeDTO();
		range.floor.major = 0;
		range.floor.minor = 0;
		range.floor.micro = 0;
		range.floor.include = true;

		return range;
	}

	protected VersionDTO getDefaultVersion() {

		final VersionDTO version = new VersionDTO();

		version.major = 0;
		version.minor = 0;
		version.micro = 0;

		return version;
	}

	protected String removeSpecial(final String key) {

		String result = key;
		if (key != null) {
			while (!result.isEmpty() && !Character.isLetterOrDigit(result.charAt(0))) {
				result = result.substring(1, result.length());
			}
		}
		return result;
	}

	protected String cleanKey(final String key) {

		String result = key;
		if (key != null) {
			while (result.endsWith("~")) {
				result = result.substring(0, result.length() - 1);
			}
		}
		return result;
	}

	protected List<String> cleanKey(final Set<String> keys) {

		final List<String> result = new LinkedList<>();
		if (keys != null) {
			for (final String key : keys) {
				result.add(cleanKey(key));
			}
		}
		return result;
	}
}
