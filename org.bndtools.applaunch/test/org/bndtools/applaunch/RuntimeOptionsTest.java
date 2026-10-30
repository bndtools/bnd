package org.bndtools.applaunch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(SoftAssertionsExtension.class)
public class RuntimeOptionsTest {

	@InjectSoftAssertions
	SoftAssertions softly;

	@Test
	public void noArguments() {
		RuntimeOptions options = RuntimeOptions.parse(new String[0]);

		softly.assertThat(options.allArgs())
			.isEmpty();
		softly.assertThat(options.frameworkArgs())
			.isEmpty();
		softly.assertThat(options.applicationArgs())
			.isEmpty();
		softly.assertThat(options.properties())
			.isEmpty();
		softly.assertThat(options.initialize())
			.isFalse();
	}

	@Test
	public void applicationArgumentsArePassedThrough() {
		String[] args = {
			"-application", "my.app", "-product", "my.product", "-feature", "my.feature", "-clearPersistedState",
			"-perspective", "my.perspective", "-pluginCustomization", "plugin_customization.ini", "-noRegistryCache",
			"-noLazyRegistryCacheLoading", "-registryMultiLanguage", "-showLocation", "--custom=value", "file.txt"
		};

		RuntimeOptions options = RuntimeOptions.parse(args);

		softly.assertThat(options.allArgs())
			.isSameAs(args);
		softly.assertThat(options.frameworkArgs())
			.isEmpty();
		softly.assertThat(options.applicationArgs())
			.containsExactly(args);
		softly.assertThat(options.properties())
			.isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
		"-clean, osgi.clean, true", //
		"-consoleLog, eclipse.consoleLog, true", //
		"-noExit, osgi.noShutdown, true", //
		"-CLEAN, osgi.clean, true", //
		"-consolelog, eclipse.consoleLog, true"
	})
	public void flagSetsProperty(String flag, String property, String value) {
		RuntimeOptions options = RuntimeOptions.parse(new String[] {
			"-application", "my.app", flag, "appArg"
		});

		softly.assertThat(options.frameworkArgs())
			.containsExactly(flag);
		softly.assertThat(options.applicationArgs())
			.containsExactly("-application", "my.app", "appArg");
		softly.assertThat(options.properties())
			.containsExactly(entry(property, value));
	}

	@ParameterizedTest
	@CsvSource({
		"-debug, osgi.debug", //
		"-dev, osgi.dev", //
		"-console, osgi.console"
	})
	public void optionalValueAsFlag(String option, String property) {
		RuntimeOptions last = RuntimeOptions.parse(new String[] {
			"-application", "my.app", option
		});
		softly.assertThat(last.frameworkArgs())
			.containsExactly(option);
		softly.assertThat(last.applicationArgs())
			.containsExactly("-application", "my.app");
		softly.assertThat(last.properties())
			.containsExactly(entry(property, ""));

		RuntimeOptions followedByOption = RuntimeOptions.parse(new String[] {
			option, "-clean"
		});
		softly.assertThat(followedByOption.frameworkArgs())
			.containsExactly(option, "-clean");
		softly.assertThat(followedByOption.applicationArgs())
			.isEmpty();
		softly.assertThat(followedByOption.properties())
			.containsExactly(entry(property, ""), entry("osgi.clean", "true"));
	}

	@ParameterizedTest
	@CsvSource({
		"-debug, .options, osgi.debug", //
		"-dev, 'bin/,target/classes', osgi.dev", //
		"-console, 7777, osgi.console", //
		"-console, localhost:7777, osgi.console"
	})
	public void optionalValueWithValue(String option, String value, String property) {
		RuntimeOptions options = RuntimeOptions.parse(new String[] {
			option, value, "appArg"
		});

		softly.assertThat(options.frameworkArgs())
			.containsExactly(option, value);
		softly.assertThat(options.applicationArgs())
			.containsExactly("appArg");
		softly.assertThat(options.properties())
			.containsExactly(entry(property, value));
	}

	@ParameterizedTest
	@CsvSource({
		"-os, linux, osgi.os", //
		"-ws, gtk, osgi.ws", //
		"-arch, x86_64, osgi.arch", //
		"-nl, en_US, osgi.nl", //
		"-nlExtensions, '@collation=phonebook;calendar=hebrew', osgi.nl.extensions", //
		"-configuration, /tmp/configuration, osgi.configuration.area", //
		"-data, /tmp/workspace, osgi.instance.area", //
		"-user, /tmp/user, osgi.user.area", //
		"-launcher, /opt/eclipse/eclipse, eclipse.launcher", //
		"-install, /opt/eclipse, osgi.install.area", //
		"-framework, file:/opt/eclipse/plugins/org.eclipse.osgi.jar, osgi.framework", //
		"-vm, /opt/java/bin/java, eclipse.vm", //
		"-OS, linux, osgi.os", //
		"-NLEXTENSIONS, '@collation=phonebook', osgi.nl.extensions"
	})
	public void valueSetsProperty(String option, String value, String property) {
		RuntimeOptions options = RuntimeOptions.parse(new String[] {
			"-application", "my.app", option, value, "appArg"
		});

		softly.assertThat(options.frameworkArgs())
			.containsExactly(option, value);
		softly.assertThat(options.applicationArgs())
			.containsExactly("-application", "my.app", "appArg");
		softly.assertThat(options.properties())
			.containsExactly(entry(property, value));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"-os", "-ws", "-arch", "-nl", "-nlExtensions", "-configuration", "-data", "-user", "-launcher", "-install",
		"-framework", "-vm", "-endSplash", "-protect"
	})
	public void valueOptionWithoutValueIsLeftToApplication(String option) {
		RuntimeOptions last = RuntimeOptions.parse(new String[] {
			"-application", "my.app", option
		});
		softly.assertThat(last.frameworkArgs())
			.isEmpty();
		softly.assertThat(last.applicationArgs())
			.containsExactly("-application", "my.app", option);
		softly.assertThat(last.properties())
			.isEmpty();

		RuntimeOptions followedByOption = RuntimeOptions.parse(new String[] {
			option, "-clean"
		});
		softly.assertThat(followedByOption.frameworkArgs())
			.containsExactly("-clean");
		softly.assertThat(followedByOption.applicationArgs())
			.containsExactly(option);
		softly.assertThat(followedByOption.properties())
			.containsExactly(entry("osgi.clean", "true"));
	}

	@Test
	public void initializeIsConsumed() {
		RuntimeOptions options = RuntimeOptions.parse(new String[] {
			"-initialize", "-application", "my.app"
		});

		softly.assertThat(options.initialize())
			.isTrue();
		softly.assertThat(options.frameworkArgs())
			.containsExactly("-initialize");
		softly.assertThat(options.applicationArgs())
			.containsExactly("-application", "my.app");
		softly.assertThat(options.properties())
			.isEmpty();
	}

	@Test
	public void launcherOnlyOptionsAreConsumedWithoutProperty() {
		RuntimeOptions options = RuntimeOptions.parse(new String[] {
			"-noSplash", "-showSplash", "splash.bmp", "-endSplash", "endsplash.sh", "-protect", "master",
			"--launcher.appendVmargs", "--launcher.overrideVmargs", "-showSplash", "-application", "my.app"
		});

		softly.assertThat(options.frameworkArgs())
			.containsExactly("-noSplash", "-showSplash", "splash.bmp", "-endSplash", "endsplash.sh", "-protect",
				"master", "--launcher.appendVmargs", "--launcher.overrideVmargs", "-showSplash");
		softly.assertThat(options.applicationArgs())
			.containsExactly("-application", "my.app");
		softly.assertThat(options.properties())
			.isEmpty();
	}

	@Test
	public void vmargsConsumeTheRemainingArguments() {
		RuntimeOptions options = RuntimeOptions.parse(new String[] {
			"-application", "my.app", "-vmargs", "-Xmx1g", "-Dfoo=bar", "-clean", "appArg"
		});

		softly.assertThat(options.frameworkArgs())
			.containsExactly("-vmargs", "-Xmx1g", "-Dfoo=bar", "-clean", "appArg");
		softly.assertThat(options.applicationArgs())
			.containsExactly("-application", "my.app");
		softly.assertThat(options.properties())
			.containsExactly(entry("eclipse.vmargs", "-Xmx1g\n-Dfoo=bar\n-clean\nappArg\n"));
	}

	@Test
	public void eclipseLaunchCommandLine() {
		String[] args = {
			"-os", "linux", "-ws", "gtk", "-arch", "x86_64", "-nl", "de_DE", "-nlExtensions",
			"@collation=phonebook", "-clean", "-consoleLog", "-noExit", "-debug", "-dev", "bin/", "-console",
			"-data", "/tmp/ws", "-user", "/tmp/user", "-configuration", "/tmp/cfg", "-launcher", "/opt/eclipse/eclipse",
			"-install", "/opt/eclipse", "-noSplash", "-showSplash", "splash.bmp", "-vm", "/opt/java/bin/java",
			"-application", "my.app", "-product", "my.product", "-clearPersistedState", "-perspective",
			"my.perspective", "--custom=value", "file.txt", "-vmargs", "-Xmx1g"
		};

		RuntimeOptions options = RuntimeOptions.parse(args);

		softly.assertThat(options.allArgs())
			.isSameAs(args);
		softly.assertThat(options.frameworkArgs())
			.containsExactly("-os", "linux", "-ws", "gtk", "-arch", "x86_64", "-nl", "de_DE", "-nlExtensions",
				"@collation=phonebook", "-clean", "-consoleLog", "-noExit", "-debug", "-dev", "bin/", "-console",
				"-data", "/tmp/ws", "-user", "/tmp/user", "-configuration", "/tmp/cfg", "-launcher",
				"/opt/eclipse/eclipse", "-install", "/opt/eclipse", "-noSplash", "-showSplash", "splash.bmp", "-vm",
				"/opt/java/bin/java", "-vmargs", "-Xmx1g");
		softly.assertThat(options.applicationArgs())
			.containsExactly("-application", "my.app", "-product", "my.product", "-clearPersistedState",
				"-perspective", "my.perspective", "--custom=value", "file.txt");
		softly.assertThat(options.properties())
			.containsExactly(entry("osgi.os", "linux"), entry("osgi.ws", "gtk"), entry("osgi.arch", "x86_64"),
				entry("osgi.nl", "de_DE"), entry("osgi.nl.extensions", "@collation=phonebook"),
				entry("osgi.clean", "true"), entry("eclipse.consoleLog", "true"), entry("osgi.noShutdown", "true"),
				entry("osgi.debug", ""), entry("osgi.dev", "bin/"), entry("osgi.console", ""),
				entry("osgi.instance.area", "/tmp/ws"), entry("osgi.user.area", "/tmp/user"),
				entry("osgi.configuration.area", "/tmp/cfg"), entry("eclipse.launcher", "/opt/eclipse/eclipse"),
				entry("osgi.install.area", "/opt/eclipse"), entry("eclipse.vm", "/opt/java/bin/java"),
				entry("eclipse.vmargs", "-Xmx1g\n"));
		softly.assertThat(options.initialize())
			.isFalse();
	}

	@Test
	public void propertiesAreUnmodifiable() {
		Map<String, String> properties = RuntimeOptions.parse(new String[] {
			"-clean"
		})
			.properties();

		softly.assertThatThrownBy(() -> properties.put("osgi.dev", ""))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"@none", "@noDefault", " @NONE ", "@nodefault"
	})
	public void symbolicLocationsLeaveLocationUnset(String value) throws Exception {
		assertThat(RuntimeOptions.toLocationURL(value)).isNull();
	}

	@Test
	public void locationFromPath() throws Exception {
		File workspace = new File("generated/workspace").getAbsoluteFile();

		URL relative = RuntimeOptions.toLocationURL("generated/workspace");
		URL absolute = RuntimeOptions.toLocationURL(workspace.getPath());
		URL fileUrl = RuntimeOptions.toLocationURL("file:" + workspace.getPath());

		softly.assertThat(relative)
			.isEqualTo(workspace.toURI()
				.toURL());
		softly.assertThat(absolute)
			.isEqualTo(workspace.toURI()
				.toURL());
		softly.assertThat(fileUrl)
			.isEqualTo(workspace.toURI()
				.toURL());
	}

	@Test
	public void locationFromSymbolicPrefix() throws Exception {
		File home = new File(System.getProperty("user.home"), "workspace");
		File dir = new File(System.getProperty("user.dir"), "workspace");

		softly.assertThat(RuntimeOptions.toLocationURL("@user.home/workspace"))
			.isEqualTo(home.toURI()
				.toURL());
		softly.assertThat(RuntimeOptions.toLocationURL("@user.dir/workspace"))
			.isEqualTo(dir.toURI()
				.toURL());
	}

	@Test
	public void locationFromUrl() throws Exception {
		assertThat(RuntimeOptions.toLocationURL("http://localhost:8080/workspace/"))
			.isEqualTo(URI.create("http://localhost:8080/workspace/")
				.toURL());
	}
}
