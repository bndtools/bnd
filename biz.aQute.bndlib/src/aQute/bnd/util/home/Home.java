package aQute.bnd.util.home;

import java.io.File;

import aQute.lib.io.IO;

/**
 * Manages the home directory of bnd. The home directory can be set with an
 * environment variable `HOME` or a System property `bnd.home.dir`. If not set,
 * it will revert to `~/.bnd`.
 */
public class Home {
	public static final String	USER_HOME_BND_DEFAULT		= "~/.bnd";
	public static final String	USER_HOME_BND_SYSTEM_PROP	= "bnd.home.dir";

	public static final File	BND_HOME;

	static {
		File thome = null;
		for (String path : new String[] {
			IO.getenv("BND_HOME"), System.getProperty(USER_HOME_BND_SYSTEM_PROP)
		}) {
			if (path != null) {
				thome = new File(path);
				break;
			}
		}
		if (thome == null)
			thome = IO.getFile(IO.home, ".bnd");
		thome.mkdirs();
		BND_HOME = thome;
	}

	/**
	 * This led to real bad practice of concatening file paths.
	 *
	 * @return the absolute path to the bnd home directory
	 */
	@Deprecated
	public static final String getUserHomeBnd() {
		return BND_HOME.getAbsolutePath();
	}

	public static File getUserHomeBnd(String relative) {
		return IO.getFile(BND_HOME, relative);
	}

}
