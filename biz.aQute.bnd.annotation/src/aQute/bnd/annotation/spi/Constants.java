package aQute.bnd.annotation.spi;

public final class Constants {

	private Constants() {}

	public static final String	SERVICELOADER_PROCESSOR	= "osgi.serviceloader.processor";
	public static final String	SERVICELOADER_REGISTRAR	= "osgi.serviceloader.registrar";
	public static final String	SERVICELOADER_VERSION	= "1.0.0";

	/**
	 * Joins to nothing if the list is empty
	 */
	public static final String	ATTRIBUTE_MACRO			= "${sjoin;\\;;${#attribute}}";

	public static final String	REGISTER_MACRO			= "register:=${#register}";

	public static final String	SERVICE_MACRO			= "objectClass:List<String>=\"${#value}\"";

	public static final String	VALUE_MACRO				= "${#value}";

}
