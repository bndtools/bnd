package org.bndtools.applaunch.test;

import static org.bndtools.applaunch.test.Locations.APPLICATION;
import static org.bndtools.applaunch.test.Locations.PRODUCT;
import static org.bndtools.applaunch.test.Locations.PRODUCT_NAME;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bndtools.applaunch.test.app.ApplicationState;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * Verifies a launch without any program arguments ({@code systemproperties.bndrun}):
 * the application is selected through the {@code eclipse.product} framework
 * property and the command line published to Eclipse is empty, not absent.
 */
@ExtendWith({
	BundleContextExtension.class, ServiceExtension.class, SoftAssertionsExtension.class
})
public class SystemPropertiesTest {
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

	@Test
	public void applicationRunsOnMainThread() {
		softly.assertThat(state.threadName())
			.isEqualTo("main");
		softly.assertThat(state.applicationArgs())
			.isEmpty();
	}

	@Test
	public void commandLineIsEmpty() {
		softly.assertThat(environment.getCommandLineArgs())
			.isEmpty();
		softly.assertThat(environment.getFrameworkArgs())
			.isEmpty();
		softly.assertThat(environment.getNonFrameworkArgs())
			.isEmpty();
	}

	@Test
	public void productSelectsApplication() {
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
	public void runPropertiesAreVisible() {
		softly.assertThat(context.getProperty("applaunch.test.marker"))
			.isEqualTo("systemproperties");
		softly.assertThat(context.getProperty("eclipse.application"))
			.as("no -application argument and no eclipse.application property")
			.isNull();
	}
}
