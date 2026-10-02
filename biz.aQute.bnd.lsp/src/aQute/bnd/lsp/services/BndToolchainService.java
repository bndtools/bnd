package aQute.bnd.lsp.services;

import java.io.File;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.libg.command.Command;

public class BndToolchainService {
	private static final Logger						logger			= LoggerFactory
		.getLogger(BndToolchainService.class);
	private static final Pattern					JAVAC_VERSION_P	= Pattern
		.compile("javac\\s+(\\d+)(?:\\.(\\d+))?", Pattern.CASE_INSENSITIVE);

	private final Map<String, OptionalInt>			versionCache	= new ConcurrentHashMap<>();

	public boolean isJdk(File javaHome) {
		if (javaHome == null || !javaHome.isDirectory()) {
			return false;
		}

		File bin = new File(javaHome, "bin");
		if (!bin.isDirectory()) {
			return false;
		}

		File javac = new File(bin, isWindows() ? "javac.exe" : "javac");
		return javac.isFile() && javac.canExecute();
	}

	public OptionalInt probeJavacVersion(String javacExecutable) {
		if (javacExecutable == null || javacExecutable.isEmpty()) {
			return OptionalInt.empty();
		}

		return versionCache.computeIfAbsent(javacExecutable, exe -> {
			Command cmd = new Command();
			cmd.add(exe);
			cmd.add("-version");
			cmd.setTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS);

			StringBuilder stdout = new StringBuilder();
			StringBuilder stderr = new StringBuilder();

			try {
				int exitCode = cmd.execute(stdout, stderr);
				String output = (stdout.length() > 0 ? stdout : stderr).toString();
				Matcher m = JAVAC_VERSION_P.matcher(output);
				if (m.find()) {
					String v1 = m.group(1);
					String v2 = m.group(2);
					if ("1".equals(v1) && v2 != null) {
						return OptionalInt.of(Integer.parseInt(v2));
					} else if (v1 != null) {
						return OptionalInt.of(Integer.parseInt(v1));
					}
				}
				logger.debug("Could not parse javac version from output: {}", output);
			} catch (Exception e) {
				logger.debug("Failed to probe javac version for {}", exe, e);
			}

			// Fallback to runtime feature version
			return OptionalInt.of(Runtime.version()
				.feature());
		});
	}

	public boolean isCompatible(int javacVersion, String requiredSourceOrTarget) {
		if (requiredSourceOrTarget == null || requiredSourceOrTarget.isEmpty()) {
			return true;
		}

		OptionalInt req = normalizeVersion(requiredSourceOrTarget);
		if (!req.isPresent()) {
			return true;
		}

		int reqVer = req.getAsInt();

		// Modern javac (>=21) dropped source/target 7 and below
		if (javacVersion >= 21 && reqVer < 8) {
			return false;
		}

		return javacVersion >= reqVer;
	}

	public static OptionalInt normalizeVersion(String v) {
		if (v == null || v.isEmpty()) {
			return OptionalInt.empty();
		}
		String trimmed = v.trim();
		if (trimmed.startsWith("1.") && trimmed.length() > 2) {
			trimmed = trimmed.substring(2);
		}
		try {
			return OptionalInt.of(Integer.parseInt(trimmed));
		} catch (NumberFormatException e) {
			return OptionalInt.empty();
		}
	}

	private static boolean isWindows() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase()
			.contains("win");
	}
}
