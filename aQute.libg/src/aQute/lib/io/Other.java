package aQute.lib.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import aQute.lib.io.IO.OS;

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
}
