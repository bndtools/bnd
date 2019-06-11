package biz.aQute.bnd.reporter.manifest.dto;

/**
 * A representation of a version in a version range.
 */
public class VersionInRangeDTO extends VersionDTO {

	/**
	 * Indicates if the version is included in the range.
	 */
	public boolean include = false;
}
