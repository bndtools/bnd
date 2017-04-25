package aQute.launcher.pre;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;

import aQute.lib.io.IOConstants;

public class JpmLauncher {
	static final int BUFFER_SIZE = IOConstants.PAGE_SIZE * 16;

	public static void main(String args[]) throws Exception {

		// if (args.length == 1 && args[0].equals("--jpminstall")) {
		// System.err.println("does not work yet");
		// installjpm();
		// }
		try {
			Class< ? > cl = JpmLauncher.class.getClassLoader().loadClass("aQute.launcher.Launcher");
			Method m = cl.getMethod("main", new Class< ? >[] {
					String[].class
			});
			m.invoke(null, new Object[] {
					args
			});
			return;
		} catch (ClassNotFoundException e) {} catch (NoSuchMethodException eee) {} catch (Exception ee) {
			throw ee;
		}

		System.err.println("This jar requires installation by jpm, invoke this command with the option --jpminstall.\n"
				+ "This will install jpm and then then use jpm to install this jar. You can read more about jpm4j\n"
				+ "on https://www.jpm4j.org");
	}

	static void installjpm() throws IOException {
		Process exec = Runtime.getRuntime().exec("jpm version");
		String version = collect(exec.getInputStream());
		if (exec.exitValue() == -1) {
			System.out.println("No jpm installed, installing jpm (requires sudo)");
			File tmpjpm = File.createTempFile("jpm", ".jar");
			URL url = new URL("https://github.com/jpm4j/jpm4j.installers/raw/master/dist/biz.aQute.jpm.run.jar ");

			copy(url.openStream(), tmpjpm);

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
			while ((c = rdr.read()) >= 0)
				sb.append((char) c);

			return sb.toString();
		} finally {
			inputStream.close();
		}
	}

	private static void copy(InputStream in, File tmpjpm) throws IOException {
		try (OutputStream out = Files.newOutputStream(tmpjpm.toPath())) {
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				out.write(buffer, 0, size);
			}
		} finally {
			in.close();
		}
	}
}
