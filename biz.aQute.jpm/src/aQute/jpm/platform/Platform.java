package aQute.jpm.platform;

import java.io.*;
import java.util.*;

public abstract class Platform {
	static Platform	platform;
	public static Platform getPlatform() {
		if (platform == null) {

			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
				platform = new Windows();
			else if (osName.startsWith("mac"))
				platform = new MacOS();
			else
				platform = new Linux();
		}
		return platform;
	}

	public abstract File getGlobal();
	public abstract File getLocal();

	abstract public void link(String value, File file) throws IOException;

	abstract public void unlink(String command) throws IOException;
	
	abstract public void shell(String initial) throws IOException;
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		formatter.format("Name                %s\n", getName());
		formatter.format("Local               %s\n", getLocal());
		formatter.format("Global               %s\n", getGlobal());
		return sb.toString();
	}

	abstract public String getName();
}
