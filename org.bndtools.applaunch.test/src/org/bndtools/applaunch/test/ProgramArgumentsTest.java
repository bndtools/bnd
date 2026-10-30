package org.bndtools.applaunch.test;

import static org.bndtools.applaunch.test.Locations.APPLICATION;
import static org.bndtools.applaunch.test.Locations.PRODUCT;
import static org.bndtools.applaunch.test.Locations.PRODUCT_NAME;
import static org.bndtools.applaunch.test.Locations.path;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bndtools.applaunch.test.app.ApplicationState;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * Verifies that the Eclipse runtime options given as {@code -runprogramargs}
 * in {@code programargs.bndrun} are applied by {@code org.bndtools.applaunch}
 * like the Eclipse launcher applies them: runtime options become framework
 * properties and locations, the remaining arguments reach the application.
 */
@ExtendWith({
	BundleContextExtension.class, ServiceExtension.class, SoftAssertionsExtension.class
})
public class ProgramArgumentsTest {
	static final long		TIMEOUT	= 20_000;

	@InjectSoftAssertions
	SoftAssertions			softly;

	@InjectBundleContext
	BundleContext			context;

	// Waits until the application has been started by the launcher
	@InjectService(timeout = TIMEOUT)
	ApplicationState		state;

	@InjectService(timeout = TIMEOUT)
	IApplicationContext		applicationContext;

	@InjectService(timeout = TIMEOUT)
	EnvironmentInfo			environment;

	@InjectService(timeout = TIMEOUT, filter = "(type=osgi.instance.area)")
	Location				instanceLocation;

	@InjectService(timeout = TIMEOUT, filter = "(type=osgi.user.area)")
	Location				userLocation;

	@InjectService(timeout = TIMEOUT, filter = "(type=osgi.configuration.area)")
	Location				configurationLocation;

	@InjectService(timeout = TIMEOUT, filter = "(type=osgi.install.area)")
	Location				installLocation;

	@Test
	public void applicationRunsOnMainThread() {
		softly.assertThat(state.threadName())
			.isEqualTo("main");
		softly.assertThat(state.applicationContext())
			.isSameAs(applicationContext);
	}

	@Test
	public void applicationArgumentsExcludeRuntimeOptions() {
		softly.assertThat(state.applicationArgs())
			.containsExactly("-clearPersistedState", "-perspective", "some.perspective", "--custom=value",
				"file.txt");
	}

	@Test
	public void commandLineArgumentsArePublished() {
		String[] all = environment.getCommandLineArgs();
		String[] framework = environment.getFrameworkArgs();
		String[] application = environment.getNonFrameworkArgs();

		softly.assertThat(application)
			.containsExactly("-application", APPLICATION, "-product", PRODUCT, "-clearPersistedState",
				"-perspective", "some.perspective", "--custom=value", "file.txt");
		softly.assertThat(framework)
			.containsSubsequence("-os", "aix", "-ws", "motif", "-arch", "ppc64", "-nl", "de_DE", "-nlExtensions",
				"@collation=phonebook", "-clean", "-consoleLog", "-noExit", "-debug", "-dev", "bin/", "-data")
			.containsSubsequence("-user")
			.containsSubsequence("-configuration")
			.containsSubsequence("-launcher")
			.containsSubsequence("-install", "-noSplash", "-showSplash", "splash.bmp", "-vm")
			.endsWith("-vmargs", "-Xmx1g")
			.doesNotContain(application);
		softly.assertThat(all)
			.startsWith("-os", "aix")
			.endsWith("-vmargs", "-Xmx1g")
			.hasSize(framework.length + application.length);
	}

	@Test
	public void platformOptionsBecomeProperties() {
		softly.assertThat(context.getProperty("osgi.os"))
			.isEqualTo("aix");
		softly.assertThat(context.getProperty("osgi.ws"))
			.isEqualTo("motif");
		softly.assertThat(context.getProperty("osgi.arch"))
			.isEqualTo("ppc64");
		softly.assertThat(context.getProperty("osgi.nl"))
			.isEqualTo("de_DE");
		softly.assertThat(context.getProperty("osgi.nl.extensions"))
			.isEqualTo("@collation=phonebook");
		softly.assertThat(environment.getOS())
			.isEqualTo("aix");
		softly.assertThat(environment.getWS())
			.isEqualTo("motif");
		softly.assertThat(environment.getOSArch())
			.isEqualTo("ppc64");
		softly.assertThat(environment.getNL())
			.isEqualTo("de_DE");
	}

	@Test
	public void flagOptionsBecomeProperties() {
		softly.assertThat(context.getProperty("osgi.clean"))
			.isEqualTo("true");
		softly.assertThat(context.getProperty("eclipse.consoleLog"))
			.isEqualTo("true");
		softly.assertThat(context.getProperty("osgi.noShutdown"))
			.isEqualTo("true");
		softly.assertThat(context.getProperty("osgi.debug"))
			.isEmpty();
		softly.assertThat(context.getProperty("osgi.dev"))
			.isEqualTo("bin/");
	}

	@Test
	public void launcherOptionsBecomeProperties() {
		softly.assertThat(context.getProperty("eclipse.launcher"))
			.endsWith("/generated/eclipse");
		softly.assertThat(context.getProperty("eclipse.vm"))
			.endsWith("/generated/java");
		softly.assertThat(context.getProperty("eclipse.vmargs"))
			.isEqualTo("-Xmx1g\n");
	}

	@Test
	public void applicationAndProductAreSelected() {
		softly.assertThat(context.getProperty("eclipse.application"))
			.isEqualTo(APPLICATION);
		softly.assertThat(context.getProperty("eclipse.product"))
			.isEqualTo(PRODUCT);
		softly.assertThat(applicationContext.getBrandingId())
			.isEqualTo(PRODUCT);
		softly.assertThat(applicationContext.getBrandingName())
			.isEqualTo(PRODUCT_NAME);
		softly.assertThat(applicationContext.getBrandingApplication())
			.isEqualTo(APPLICATION);
	}

	@Test
	public void dataSetsInstanceLocation() throws Exception {
		softly.assertThat(instanceLocation.isSet())
			.isTrue();
		softly.assertThat(path(instanceLocation))
			.endsWith("/generated/instance");
		softly.assertThat(context.getProperty("osgi.instance.area"))
			.endsWith("/generated/instance/");
	}

	@Test
	public void userSetsUserLocation() throws Exception {
		softly.assertThat(userLocation.isSet())
			.isTrue();
		softly.assertThat(path(userLocation))
			.endsWith("/generated/user");
		softly.assertThat(context.getProperty("osgi.user.area"))
			.endsWith("/generated/user/");
	}

	@Test
	public void locationsFixedAtFrameworkStartAreKept() throws Exception {
		softly.assertThat(configurationLocation.isSet())
			.isTrue();
		softly.assertThat(path(configurationLocation))
			.doesNotEndWith("/generated/configuration");
		softly.assertThat(context.getProperty("osgi.configuration.area"))
			.doesNotContain("/generated/configuration");
		softly.assertThat(installLocation.isSet())
			.isTrue();
		softly.assertThat(path(installLocation))
			.doesNotEndWith("/generated/install");
		softly.assertThat(context.getProperty("osgi.install.area"))
			.doesNotContain("/generated/install");
	}
}
