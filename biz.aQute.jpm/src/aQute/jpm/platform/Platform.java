package aQute.jpm.platform;

import java.io.*;
import java.util.*;

import aQute.libg.reporter.*;

public abstract class Platform {
	static Platform	platform;
	static Runtime runtime = Runtime.getRuntime();
	Reporter reporter;
	
	public static Platform getPlatform(Reporter reporter) {
		
		if (platform == null) {

			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
				platform = new Windows();
			else if (osName.startsWith("mac"))
				platform = new MacOS();
			else
				platform = new Linux();
			platform.reporter = reporter;
		}
		return platform;
	}

	public abstract File getGlobal();
	public abstract File getLocal();

	abstract public void createCommand(String value, File file) throws Exception;

	abstract public void deleteCommand(String command) throws Exception;
	
	abstract public void shell(String initial) throws Exception;
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		formatter.format("Name                %s\n", getName());
		formatter.format("Local               %s\n", getLocal());
		formatter.format("Global              %s\n", getGlobal());
		return sb.toString();
	}

	abstract public String getName();
	
	abstract public void uninstall();

	public int run(String args) throws Exception {
		return runtime.exec(args).waitFor();
//		Command c = new Command();
//		c.add(args.split("\\s+"));
//		c.setTimeout(15, TimeUnit.SECONDS);
//		c.setReporter(reporter);
//		StringBuffer out = new StringBuffer(); // is synced
//		int result = c.execute(out, out);
//		if ( result != 0)
//			throw new RuntimeException("process failed: " + out);
//		return out.toString();
	}



	abstract public void createService(File base, File[] path, String main, String... args) throws IOException, Exception;
}
