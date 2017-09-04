package aQute.bnd.metadata;

import java.util.Map;
import java.util.Objects;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.metadata.dto.VersionDTO;
import aQute.bnd.metadata.dto.VersionRangeDTO;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

abstract class HeaderExtractor extends Extractor {

	final private String	_name;
	final private boolean	_allowDuplicateAttribute;

	public HeaderExtractor(String name, boolean allowDuplicateAttribute) {

		Objects.requireNonNull(name, "name");

		_name = name;
		_allowDuplicateAttribute = allowDuplicateAttribute;
	}

	final public String getName() {
		return _name;
	}

	final public boolean allowDuplicateAttribute() {
		return _allowDuplicateAttribute;
	}

	abstract public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders,
			Jar jar);

	abstract public void verify(ManifestHeadersDTO dto) throws Exception;

	@Override
	final protected void error(String error) throws Exception {

		throw new Exception("Header description error: " + error);
	}

	final protected String checkRange(VersionRangeDTO r) {

		if (r.includeCeiling == null) {

			return "invalid interval";
		}

		if (r.includeFloor == null) {

			return "invalid interval";
		}

		if (r.floor == null || r.floor.major == null) {

			return "invalid floor";
		}

		if (r.ceiling == null && r.includeCeiling) {

			return "invalid ceiling interval";
		}

		if (r.ceiling != null && r.ceiling.major == null) {

			return "invalid ceiling";
		}

		return null;
	}

	protected VersionRangeDTO toOsgiRange(String value) {

		if (value != null && VersionRange.isOSGiVersionRange(value)) {

			VersionRange range = VersionRange.parseOSGiVersionRange(value);
			VersionRangeDTO result = new VersionRangeDTO();

			result.includeFloor = range.includeLow();
			result.includeCeiling = range.includeHigh();
			result.floor = new VersionDTO();

			result.floor.major = range.getLow().getMajor();
			result.floor.minor = range.getLow().getMinor();
			result.floor.micro = range.getLow().getMicro();
			result.floor.qualifier = range.getLow().getQualifier();

			if (!range.isSingleVersion()) {

				result.ceiling = new VersionDTO();

				result.ceiling.major = range.getHigh().getMajor();
				result.ceiling.minor = range.getHigh().getMinor();
				result.ceiling.micro = range.getHigh().getMicro();
				result.ceiling.qualifier = range.getHigh().getQualifier();
			}

			return result;

		} else {

			return null;
		}
	}

	protected VersionRangeDTO toRange(String value) {

		if (value != null && VersionRange.isVersionRange(value)) {

			VersionRange range = VersionRange.parseVersionRange(value);
			VersionRangeDTO result = new VersionRangeDTO();

			result.includeFloor = range.includeLow();
			result.includeCeiling = range.includeHigh();
			result.floor = new VersionDTO();

			result.floor.major = range.getLow().getMajor();
			result.floor.minor = range.getLow().getMinor();
			result.floor.micro = range.getLow().getMicro();
			result.floor.qualifier = range.getLow().getQualifier();

			if (!range.isSingleVersion()) {

				result.ceiling = new VersionDTO();

				result.ceiling.major = range.getHigh().getMajor();
				result.ceiling.minor = range.getHigh().getMinor();
				result.ceiling.micro = range.getHigh().getMicro();
				result.ceiling.qualifier = range.getHigh().getQualifier();
			}

			return result;

		} else {

			return null;
		}
	}

	protected VersionDTO toVersion(String value) {

		if (value != null && Version.isVersion(value)) {

			Version version = Version.parseVersion(value);
			VersionDTO result = new VersionDTO();

			result.major = version.getMajor();
			result.minor = version.getMinor();
			result.micro = version.getMicro();
			result.qualifier = version.getQualifier();

			return result;

		} else {

			return null;
		}
	}

	protected VersionRangeDTO getDefaultRange() {

		VersionRangeDTO range = new VersionRangeDTO();

		range.includeFloor = true;
		range.includeCeiling = false;
		range.floor = new VersionDTO();

		range.floor.major = 0;
		range.floor.minor = 0;
		range.floor.micro = 0;

		return range;
	}

	protected VersionDTO getDefaultVersion() {

		VersionDTO version = new VersionDTO();

		version.major = 0;
		version.minor = 0;
		version.micro = 0;

		return version;
	}
}
