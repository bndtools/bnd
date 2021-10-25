package aQute.remote.api;

import org.osgi.dto.DTO;

public class XPropertyDTO extends DTO {

	public String			name;
	public String			value;
	public XPropertyType	type;

	public enum XPropertyType {
		FRAMEWORK,
		SYSTEM
	}

}
