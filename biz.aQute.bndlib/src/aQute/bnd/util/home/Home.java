package aQute.bnd.util.home;

public class Home {
	public static final String	USER_HOME_BND_DEFAULT		= "~/.bnd";
	public static final String	USER_HOME_BND_SYSTEM_PROP	= "bnd.home.dir";

	public static final String getUserHomeBnd() {
		return System.getProperty(USER_HOME_BND_SYSTEM_PROP, USER_HOME_BND_DEFAULT);
	}

}
