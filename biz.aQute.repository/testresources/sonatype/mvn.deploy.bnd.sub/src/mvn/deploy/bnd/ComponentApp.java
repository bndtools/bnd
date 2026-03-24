package io.klib.mvn.deploy.bnd;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * OSGi component that prints deployment information upon activation.
 * <p>
 * When activated, this component logs a timestamped message indicating deployment to Maven Central,
 * and prints the Bundle-Version from the JAR manifest if available.
 * </p>
 * <p>
 * This class demonstrates how to access OSGi bundle metadata at runtime
 * using the OSGi Bundle API.
 * </p>
 *
 * @author Peter Kirschner
 */
@Component(
		immediate = true
)

public class ComponentApp {

	@Activate
	public void activate(BundleContext context) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
		String timestamp = LocalDateTime.now().format(formatter);
		System.out.println("Hello OSGi world - build by bnd and deployed at Maven Central on " + timestamp);
		System.out.println("Bundle-Version: " + readBundleVersion(context));
	}

	private String readBundleVersion(BundleContext context) {
		try {
			// Get the current bundle from the bundle context
			Bundle bundle = context.getBundle();
			
			// Get the Bundle-Version from the bundle headers
			String version = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
			
			if (version != null && !version.trim().isEmpty()) {
				return version;
			} else {
				return "Bundle-Version not found in bundle headers.";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "Error reading bundle version: " + e.getMessage();
		}
	}

}
