package aQute.bnd.platform;

import java.io.*;

public abstract class Platform {
	static Platform	platform;
	public static Platform getPlatform() {
		if (platform == null) {

			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
				platform = new Windows();
			else if (osName.startsWith("macos"))
				platform = new MacOS();
			else
				platform = new Linux();
		}
		return platform;
	}

	public abstract File getGlobal();
	public abstract File getLocal();
}
