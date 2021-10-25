package aQute.remote.api;

import org.osgi.dto.DTO;

public class XPackageDTO extends DTO {

	public String		name;
	public String		version;
	public XpackageType	type;

	public enum XpackageType {
		EXPORT,
		IMPORT
	}

}
