package aQute.bnd.annotation.cdi;

import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

public final class Constants {

	private Constants() {
	}

	public static final String	CDI_EXTENSION_PROPERTY_MACRO	= CDI_EXTENSION_PROPERTY + "=${#name}";
	public static final String	CDI_EXTENSION_TYPE				= "javax.enterprise.inject.spi.Extension";
	public static final String	NAME_MACRO						= "${#name}";
	public static final String	SERVICE_ATTRIBUTE				= "objectClass:List<String>=" + CDI_EXTENSION_TYPE;
	public static final String	SERVICE_FILTER					= "(objectClass=" + CDI_EXTENSION_TYPE + ")";
	public static final String	VERSION_MACRO					= "${if;${size;${#version}};${#version}}";

}
