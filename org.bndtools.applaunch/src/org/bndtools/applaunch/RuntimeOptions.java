package org.bndtools.applaunch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Processes the program arguments of a bnd launch the way the Eclipse launcher
 * does. Arguments understood by the Eclipse runtime (see "The Eclipse Runtime
 * Options" in the Platform Plug-in Developer Guide) are translated into
 * framework properties and removed from the arguments handed to the Eclipse
 * application. All other arguments, for example {@code -application},
 * {@code -product} or {@code -clearPersistedState}, are left to the Eclipse
 * application layer which processes them itself.
 * <p>
 * The OSGi layer options mirror
 * {@code org.eclipse.core.runtime.adaptor.EclipseStarter}, the Main layer
 * options mirror {@code org.eclipse.equinox.launcher.Main}.
 */
final class RuntimeOptions {
	// OSGi layer options (EclipseStarter)
	static final String						ARCH				= "-arch";
	static final String						CLEAN				= "-clean";
	static final String						CONFIGURATION		= "-configuration";
	static final String						CONSOLE				= "-console";
	static final String						CONSOLE_LOG			= "-consoleLog";
	static final String						DATA				= "-data";
	static final String						DEBUG				= "-debug";
	static final String						DEV					= "-dev";
	static final String						INITIALIZE			= "-initialize";
	static final String						LAUNCHER			= "-launcher";
	static final String						NL					= "-nl";
	static final String						NL_EXTENSIONS		= "-nlExtensions";
	static final String						NO_EXIT				= "-noExit";
	static final String						OS					= "-os";
	static final String						USER				= "-user";
	static final String						WS					= "-ws";

	// Main layer options (org.eclipse.equinox.launcher.Main)
	static final String						APPEND_VMARGS		= "--launcher.appendVmargs";
	static final String						END_SPLASH			= "-endSplash";
	static final String						FRAMEWORK			= "-framework";
	static final String						INSTALL				= "-install";
	static final String						NO_SPLASH			= "-noSplash";
	static final String						OVERRIDE_VMARGS		= "--launcher.overrideVmargs";
	static final String						PROTECT				= "-protect";
	static final String						SHOW_SPLASH			= "-showSplash";
	static final String						VM					= "-vm";
	static final String						VMARGS				= "-vmargs";

	// Framework properties
	static final String						PROP_ARCH			= "osgi.arch";
	static final String						PROP_CLEAN			= "osgi.clean";
	static final String						PROP_CONFIG_AREA	= "osgi.configuration.area";
	static final String						PROP_CONSOLE		= "osgi.console";
	static final String						PROP_CONSOLE_LOG	= "eclipse.consoleLog";
	static final String						PROP_DEBUG			= "osgi.debug";
	static final String						PROP_DEV			= "osgi.dev";
	static final String						PROP_FRAMEWORK		= "osgi.framework";
	static final String						PROP_INSTALL_AREA	= "osgi.install.area";
	static final String						PROP_INSTANCE_AREA	= "osgi.instance.area";
	static final String						PROP_LAUNCHER		= "eclipse.launcher";
	static final String						PROP_NL				= "osgi.nl";
	static final String						PROP_NL_EXTENSIONS	= "osgi.nl.extensions";
	static final String						PROP_NO_SHUTDOWN	= "osgi.noShutdown";
	static final String						PROP_OS				= "osgi.os";
	static final String						PROP_USER_AREA		= "osgi.user.area";
	static final String						PROP_VM				= "eclipse.vm";
	static final String						PROP_VMARGS			= "eclipse.vmargs";
	static final String						PROP_WS				= "osgi.ws";

	// Location values, see EquinoxLocations
	private static final String				NONE				= "@none";
	private static final String				NO_DEFAULT			= "@noDefault";
	private static final String				USER_HOME			= "@user.home";
	private static final String				USER_DIR			= "@user.dir";

	private enum Kind {
		/** Never takes a value. */
		FLAG,
		/** Takes the next argument as value unless it starts with '-'. */
		OPTIONAL_VALUE,
		/** Requires a value; without one the option is left to the application. */
		VALUE
	}

	private static final class Option {
		final String	name;
		final Kind		kind;
		final String	property;
		final String	flagValue;

		Option(String name, Kind kind, String property, String flagValue) {
			this.name = name;
			this.kind = kind;
			this.property = property;
			this.flagValue = flagValue;
		}
	}

	private static final Map<String, Option>	OPTIONS	= new HashMap<>();

	static {
		flag(CLEAN, PROP_CLEAN, "true");
		flag(CONSOLE_LOG, PROP_CONSOLE_LOG, "true");
		flag(NO_EXIT, PROP_NO_SHUTDOWN, "true");
		flag(INITIALIZE, null, null);
		optional(CONSOLE, PROP_CONSOLE);
		optional(DEBUG, PROP_DEBUG);
		optional(DEV, PROP_DEV);
		value(ARCH, PROP_ARCH);
		value(CONFIGURATION, PROP_CONFIG_AREA);
		value(DATA, PROP_INSTANCE_AREA);
		value(LAUNCHER, PROP_LAUNCHER);
		value(NL, PROP_NL);
		value(NL_EXTENSIONS, PROP_NL_EXTENSIONS);
		value(OS, PROP_OS);
		value(USER, PROP_USER_AREA);
		value(WS, PROP_WS);

		flag(NO_SPLASH, null, null);
		flag(APPEND_VMARGS, null, null);
		flag(OVERRIDE_VMARGS, null, null);
		optional(SHOW_SPLASH, null);
		value(END_SPLASH, null);
		value(PROTECT, null);
		value(FRAMEWORK, PROP_FRAMEWORK);
		value(INSTALL, PROP_INSTALL_AREA);
		value(VM, PROP_VM);
	}

	private static void flag(String name, String property, String value) {
		register(new Option(name, Kind.FLAG, property, value));
	}

	private static void optional(String name, String property) {
		register(new Option(name, Kind.OPTIONAL_VALUE, property, ""));
	}

	private static void value(String name, String property) {
		register(new Option(name, Kind.VALUE, property, null));
	}

	private static void register(Option option) {
		OPTIONS.put(key(option.name), option);
	}

	private static String key(String arg) {
		return arg.toLowerCase(Locale.ROOT);
	}

	private final String[]				allArgs;
	private final String[]				frameworkArgs;
	private final String[]				applicationArgs;
	private final Map<String, String>	properties;
	private final boolean				initialize;

	private RuntimeOptions(String[] allArgs, List<String> frameworkArgs, List<String> applicationArgs,
		Map<String, String> properties, boolean initialize) {
		this.allArgs = allArgs;
		this.frameworkArgs = frameworkArgs.toArray(new String[0]);
		this.applicationArgs = applicationArgs.toArray(new String[0]);
		this.properties = Collections.unmodifiableMap(properties);
		this.initialize = initialize;
	}

	/**
	 * Splits the given program arguments into runtime (framework) arguments
	 * and application arguments.
	 *
	 * @param args the program arguments of the launch, never {@code null}
	 */
	static RuntimeOptions parse(String[] args) {
		List<String> framework = new ArrayList<>();
		List<String> application = new ArrayList<>();
		Map<String, String> properties = new LinkedHashMap<>();
		boolean initialize = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equalsIgnoreCase(VMARGS)) {
				// everything after -vmargs belongs to the VM, never to the application
				framework.add(arg);
				StringBuilder vmargs = new StringBuilder();
				while (++i < args.length) {
					framework.add(args[i]);
					vmargs.append(args[i])
						.append('\n');
				}
				properties.put(PROP_VMARGS, vmargs.toString());
				break;
			}
			Option option = OPTIONS.get(key(arg));
			if (option == null) {
				application.add(arg);
				continue;
			}
			String value = (i + 1 < args.length && !args[i + 1].startsWith("-")) ? args[i + 1] : null;
			switch (option.kind) {
				case FLAG :
					framework.add(arg);
					if (option.name.equals(INITIALIZE)) {
						initialize = true;
					}
					if (option.property != null) {
						properties.put(option.property, option.flagValue);
					}
					break;
				case OPTIONAL_VALUE :
					framework.add(arg);
					if (value != null) {
						framework.add(value);
						i++;
					}
					if (option.property != null) {
						properties.put(option.property, value == null ? option.flagValue : value);
					}
					break;
				case VALUE :
					if (value == null) {
						application.add(arg);
						break;
					}
					framework.add(arg);
					framework.add(value);
					i++;
					if (option.property != null) {
						properties.put(option.property, value);
					}
					break;
			}
		}
		return new RuntimeOptions(args, framework, application, properties, initialize);
	}

	/**
	 * Converts an Eclipse location value into a URL. Handles the symbolic
	 * {@code @user.home} and {@code @user.dir} prefixes, {@code file:} URLs,
	 * other URLs and plain file system paths.
	 *
	 * @return the URL or {@code null} for {@code @none} and {@code @noDefault}
	 *         which leave a location unset
	 */
	static URL toLocationURL(String value) throws MalformedURLException {
		String location = value.trim();
		if (location.equalsIgnoreCase(NONE) || location.equalsIgnoreCase(NO_DEFAULT)) {
			return null;
		}
		if (location.startsWith(USER_HOME)) {
			location = System.getProperty("user.home") + location.substring(USER_HOME.length());
		} else if (location.startsWith(USER_DIR)) {
			location = System.getProperty("user.dir") + location.substring(USER_DIR.length());
		}
		if (location.regionMatches(true, 0, "file:", 0, 5)) {
			return fileURL(location.substring(5));
		}
		try {
			URI uri = new URI(location);
			// a one character scheme is a Windows drive letter
			if (uri.isAbsolute() && uri.getScheme()
				.length() > 1) {
				return uri.toURL();
			}
		} catch (URISyntaxException | IllegalArgumentException | MalformedURLException e) {
			// not a URL, treat as file system path
		}
		return fileURL(location);
	}

	private static URL fileURL(String path) throws MalformedURLException {
		return new File(path).getAbsoluteFile()
			.toURI()
			.toURL();
	}

	/** All program arguments of the launch. */
	String[] allArgs() {
		return allArgs;
	}

	/** The arguments consumed by the Eclipse runtime. */
	String[] frameworkArgs() {
		return frameworkArgs;
	}

	/** The arguments passed on to the Eclipse application layer. */
	String[] applicationArgs() {
		return applicationArgs;
	}

	/** The framework properties derived from the runtime arguments. */
	Map<String, String> properties() {
		return properties;
	}

	/** {@code true} if {@code -initialize} was specified; no application runs. */
	boolean initialize() {
		return initialize;
	}
}
