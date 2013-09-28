package aQute.launcher.pre;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;

public class JpmLauncher {

	public static void main(String args[]) throws Exception {
		
//		if (args.length == 1 && args[0].equals("--jpminstall")) {
//			System.err.println("does not work yet");
//			installjpm();
//		}
		try {
			Class< ? > cl = JpmLauncher.class.getClassLoader().loadClass("aQute.launcher.Launcher");
			Method m = cl.getMethod("main", new Class< ? >[] {
				String[].class
			});
			m.invoke(null, new Object[] {
				args
			});
			return;
		}
		catch (ClassNotFoundException e) {}
		catch (NoSuchMethodException eee) {}
		catch (Exception ee) {
			throw ee;
		}
		
		

		System.err.println("This jar requires installation by jpm, invoke this command with the option --jpminstall.\n"
				+ "This will install jpm and then then use jpm to install this jar. You can read more about jpm4j\n"
				+ "on https://www.jpm4j.org");
	}

	 static void installjpm() throws IOException {
		Process exec = Runtime.getRuntime().exec("jpm version");
		String version = collect( exec.getInputStream());
		if ( exec.exitValue() == -1 ) {
			System.out.println("No jpm installed, installing jpm (requires sudo)");
			File tmpjpm = File.createTempFile("jpm", ".jar");
			URL url = new URL("https://github.com/jpm4j/jpm4j.installers/raw/master/dist/biz.aQute.jpm.run.jar ");

			InputStream in = url.openStream();
			copy(tmpjpm, in);

			Runtime.getRuntime().exec("java -jar " + tmpjpm.getAbsolutePath() + " init");
		} else {
			System.out.println("detected jpm version " + version);
		}
		
		
		
		
	}

	private static String collect(InputStream inputStream) throws IOException {
		try {
			StringBuilder sb = new StringBuilder();
			InputStreamReader rdr = new InputStreamReader(inputStream);
			int c;
			while ( (c=rdr.read())>=0)
				sb.append((char)c);
			
			return sb.toString();
		} finally {
			inputStream.close();
		}
	}

	private static void copy(File tmpjpm, InputStream in) throws FileNotFoundException, IOException {
		try {
			OutputStream out = new FileOutputStream(tmpjpm);
			try {
				byte[] buffer = new byte[200000];
				int size;
				while ( (size=in.read(buffer)) > 0)
					out.write(buffer,0,size);
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}
}
