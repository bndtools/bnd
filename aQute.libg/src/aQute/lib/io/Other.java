package aQute.lib.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import aQute.lib.io.IO.OS;
import aQute.lib.stringrover.StringRover;

class Other implements OS {

	@Override
	public File getBasedFile(File base, String subPath) throws IOException {
		String use;
		if (subPath.contains("..")) {
			Path normalizedPath = Path.of(subPath)
				.normalize();
			if (normalizedPath.getNameCount() > 0 && normalizedPath.getName(0)
				.equals(IO.DOTDOT)) {
				throw new IOException("io.sub.up invalid path, will escape the designated directory. path='" + subPath
					+ "', base='" + base + "', normalized='" + normalizedPath + "'");
			}
			use = normalizedPath.toString();
		} else
			use = subPath;
		return new File(base, use);
	}

	@Override
	public String getenv(String string) {
		return System.getenv(string);
	}

	@Override
	public String toSafeFileName(String string) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c < ' ')
				continue;

			sb.append(switch (c) {
				case '/', ':' -> '%';
				default -> c;
			});
		}
		return sb.toString();
	}

	@Override
	public File getFile(File base, String file) {
		return getFile0(base, file);
	}

	static File getFile0(File base, String path) {
		StringRover rover = new StringRover(path);
		if (rover.startsWith("~/")) {
			rover.increment(2);
			if (!rover.startsWith("~/")) {
				return getFile0(IO.home, rover.substring(0));
			}
		}
		if (rover.startsWith("~")) {
			return getFile0(IO.home.getParentFile(), rover.substring(1));
		}

		File f = new File(rover.substring(0));
		if (f.isAbsolute()) {
			return f;
		}

		if (base == null) {
			base = IO.work;
		}

		for (f = base.getAbsoluteFile(); !rover.isEmpty();) {
			int n = rover.indexOf('/');
			if (n < 0) {
				n = rover.length();
			}
			if ((n == 0) || ((n == 1) && (rover.charAt(0) == '.'))) {
				// case "" or "."
			} else if ((n == 2) && (rover.charAt(0) == '.') && (rover.charAt(1) == '.')) {
				// case ".."
				File parent = f.getParentFile();
				if (parent != null) {
					f = parent;
				}
			} else {
				String segment = rover.substring(0, n);
				f = new File(f, segment);
			}
			rover.increment(n + 1);
		}

		return f.getAbsoluteFile();
	}

}
