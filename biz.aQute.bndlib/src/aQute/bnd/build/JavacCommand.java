package aQute.bnd.build;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.libg.command.Command;

/**
 * Builds the common {@code javac} command used to compile a project.
 * <p>
 * The command includes the configured Java release, source and target versions,
 * compiler profile, debug information, deprecation settings, additional Java
 * options, and boot class path. The appropriate use of {@code --release} is
 * determined from the project configuration and the capabilities of the
 * configured {@code javac} executable.
 * </p>
 * <p>
 * This class also validates combinations of compiler options that are
 * incompatible with {@code --release} and determines the version of the
 * configured {@code javac} executable when required.
 * </p>
 * <p>
 * This class is package-private and is used by {@link Project} to construct the
 * Java compiler command.
 * </p>
 */
class JavacCommand {

	private final static Logger					logger				= LoggerFactory.getLogger(JavacCommand.class);
	private static final Map<String, Integer>	javacVersions		= new ConcurrentHashMap<>();
	private static final Pattern				javacVersionPattern	= Pattern.compile("javac\\s+(\\d+)(?:\\.(\\d+))?");
	private final Project						p;

	JavacCommand(Project p) {
		this.p = p;
	}

	Command getCommonJavac(boolean test) throws Exception {
		Command javac = new Command();
		String javacExecutable = p.getJavaExecutable("javac");
		javac.add(javacExecutable);
		String target = p.getProperty(Constants.JAVAC_TARGET, "1.6");
		String profile = p.getProperty(Constants.JAVAC_PROFILE, "");
		String source = p.getProperty(Constants.JAVAC_SOURCE, "1.6");
		String debug = p.getProperty("javac.debug");
		if ("on".equalsIgnoreCase(debug) || "true".equalsIgnoreCase(debug))
			debug = "vars,source,lines";

		Parameters options = new Parameters(p.getProperty("java.options"), p);

		boolean deprecation = Processor.isTrue(p.getProperty("java.deprecation"));
		Collection<Container> bcp = Container.flatten(p.getBootclasspath());

		javac.add("-encoding", "UTF-8");

		boolean hasReleaseOption = hasReleaseOption(options);
		boolean hasSourceTargetOption = hasSourceTargetOption(options);
		OptionalInt release = getJavacRelease(javacExecutable, source, target, profile, bcp, options);
		validateJavacReleaseOptions(release.isPresent(), hasReleaseOption, hasSourceTargetOption, profile,
			bcp.isEmpty());
		if (release.isPresent()) {
			javac.add("--release", Integer.toString(release.getAsInt()));
		} else if (!hasReleaseOption) {
			javac.add("-source", source);
			javac.add("-target", target);
		}

		if (!profile.isEmpty())
			javac.add("-profile", profile);

		if (deprecation)
			javac.add("-deprecation");

		if (test || debug == null) {
			javac.add("-g:source,lines,vars");
		} else {
			javac.add("-g:" + debug);
		}

		javac.addAll(options.keyList());

		StringBuilder bootclasspath = new StringBuilder();
		String bootclasspathDel = "-Xbootclasspath/p:";

		for (Container c : bcp) {
			bootclasspath.append(bootclasspathDel)
				.append(IO.absolutePath(c.getFile()));
			bootclasspathDel = File.pathSeparator;
		}

		if (bootclasspath.length() != 0) {
			javac.add(bootclasspath.toString());
		}
		return javac;
	}

	private OptionalInt getJavacRelease(String javacExecutable, String source, String target, String profile,
		Collection<Container> bootclasspath, Parameters options) {
		String configuredRelease = p.getProperty(Constants.JAVAC_RELEASE);
		boolean hasReleaseOption = hasReleaseOption(options);
		if (hasReleaseOption) {
			logger.debug("Using explicit --release from java.options");
		}
		if (configuredRelease != null || !profile.isEmpty() || !bootclasspath.isEmpty() || hasReleaseOption) {
			return getJavacRelease(configuredRelease, source, target, profile, bootclasspath.isEmpty(),
				hasReleaseOption, 0);
		}
		return getJavacRelease(null, source, target, profile, true, false, getJavacVersion(javacExecutable));
	}

	static OptionalInt getJavacRelease(String configuredRelease, String source, String target, String profile,
		boolean emptyBootclasspath, boolean hasReleaseOption, int javacVersion) {
		if (configuredRelease != null) {
			return configuredRelease.isEmpty() ? OptionalInt.empty() : parseJavaRelease(configuredRelease);
		}
		if (!profile.isEmpty() || !emptyBootclasspath || hasReleaseOption) {
			return OptionalInt.empty();
		}
		OptionalInt sourceRelease = parseJavaRelease(source);
		OptionalInt targetRelease = parseJavaRelease(target);
		if (sourceRelease.isEmpty() || !sourceRelease.equals(targetRelease) || sourceRelease.getAsInt() < 8
			|| javacVersion < 9) {
			return OptionalInt.empty();
		}
		return sourceRelease;
	}

	static void validateJavacReleaseOptions(boolean releaseSelected, boolean hasReleaseOption,
		boolean hasSourceTargetOption, String profile, boolean emptyBootclasspath) {
		if ((releaseSelected || hasReleaseOption)
			&& (hasSourceTargetOption || !profile.isEmpty() || !emptyBootclasspath)) {
			throw new IllegalArgumentException(
				"javac --release cannot be combined with source/target options, javac.profile, or a bootclasspath");
		}
	}

	private static boolean hasReleaseOption(Parameters options) {
		return hasOption(options, "--release");
	}

	private static boolean hasSourceTargetOption(Parameters options) {
		return hasOption(options, "--source", "-source", "--target", "-target");
	}

	private static boolean hasOption(Parameters options, String... names) {
		return options.keyList()
			.stream()
			.anyMatch(option -> Arrays.stream(names)
				.anyMatch(name -> option.equals(name) || option.startsWith(name + "=")));
	}

	private static OptionalInt parseJavaRelease(String value) {
		String normalized = value.trim();
		if (normalized.matches("1\\.\\d+")) {
			normalized = normalized.substring(2);
		}
		try {
			return OptionalInt.of(Integer.parseInt(normalized));
		} catch (NumberFormatException e) {
			return OptionalInt.empty();
		}
	}

	private static int getJavacVersion(String javacExecutable) {
		String key = IO.absolutePath(new File(javacExecutable));
		return javacVersions.computeIfAbsent(key, ignored -> probeJavacVersion(javacExecutable));
	}

	private static int probeJavacVersion(String javacExecutable) {
		Command javac = new Command(javacExecutable);
		javac.add("-version");
		javac.setTimeout(5, TimeUnit.SECONDS);
		StringBuilder stdout = new StringBuilder();
		StringBuilder stderr = new StringBuilder();
		try {
			if (javac.execute(stdout, stderr) == 0) {
				java.util.regex.Matcher matcher = javacVersionPattern.matcher(stdout.append(stderr));
				if (matcher.find()) {
					return "1".equals(matcher.group(1)) ? Integer.parseInt(matcher.group(2))
						: Integer.parseInt(matcher.group(1));
				}
			}
		} catch (Exception e) {
			logger.debug("Unable to determine javac version for {}", javacExecutable, e);
		}
		return Runtime.version()
			.feature();
	}

}
