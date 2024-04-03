package aQute.lib.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.io.IO.OS;

class Windows implements OS {
	final static Pattern WINDOWS_BAD_FILE_NAME_P = Pattern.compile(
		"(?:(:?.*[\u0000-\u001F<>:\"|/\\\\?*].*)|\\.\\.|CON|PRN|AUX|NUL|COM\\d|COM¹|COM²|COM³|LPT\\d|LPT¹|LPT²|LPT³)(?:\\.\\w+)?",
		Pattern.CASE_INSENSITIVE);
	final static Pattern	DRIVE_P					= Pattern.compile("/?(?<drive>[a-z]:)", Pattern.CASE_INSENSITIVE);

	@Override
	public File getBasedFile(File base, String subPath) throws IOException {
		String use;
		Matcher matcher = WINDOWS_BAD_FILE_NAME_P.matcher(subPath);
		if (matcher.find()) {

			try {
				Path normalizedPath = Path.of(subPath)
					.normalize();

				if (normalizedPath.startsWith(IO.DOTDOT)) {
					throw new IOException("io.sub.up invalid path, will escape the designated directory. path='"
						+ subPath + "', base='" + base + "', normalized='" + normalizedPath + "'");
				}
				for (int i = 0; i < normalizedPath.getNameCount(); i++) {
					String segment = normalizedPath.getName(i)
						.toString();
					if (matcher.reset(segment)
						.matches()) {
						throw new IOException("io.win.sub.invalid pathcontains reserved names on windows. path='"
							+ subPath + "', base='" + base + "', pattern='" + WINDOWS_BAD_FILE_NAME_P + "'");
					}
				}
				use = normalizedPath.toString();
			} catch (java.nio.file.InvalidPathException e) {
				throw new IOException("io.win.sub.invalid pathcontains reserved names on windows. path='" + subPath
					+ "', base='" + base + "': '" + e.getMessage() + "'");
			}
		} else
			use = subPath;
		return new File(base, use);
	}

	@Override
	public String getenv(String string) {
		return IO.hc.getenv(string);
	}

	@Override
	public String toSafeFileName(String string) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c < ' ')
				continue;

			sb.append(switch (c) {
				case '<', '>', '"', '/', '\\', '|', '*', ':', '?' -> '%';
				default -> c;
			});
		}
		if (sb.length() == 0 || WINDOWS_BAD_FILE_NAME_P.matcher(sb)
			.matches())
			sb.append("_");

		return sb.toString();
	}

	@Override
	public File getFile(File base, String file) {
		file = file.replace('\\', '/');
		Matcher m = DRIVE_P.matcher(file);
		if (m.lookingAt()) {
			base = new File(m.group("drive"));
		}
		return Other.getFile0(base, file);
	}
}
