package aQute.junit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import junit.framework.TestCase;

/**
 * Verify that all bundles are resolved
 */
public class UnresolvedTester extends TestCase {
	BundleContext					context;
	private final static Pattern	IP_P	= Pattern
		.compile(" \\(&\\(osgi.wiring.package=([^)]+)\\)\\(version>=([^)]+)\\)\\(!\\(version>=([^)]+)\\)\\)\\)");

	public void setBundleContext(BundleContext context) {
		this.context = context;
	}

	@SuppressWarnings("deprecation")
	public void testAllResolved() {
		assertNotNull("Expected a Bundle Context", context);
		StringBuilder sb = new StringBuilder();

		for (Bundle b : context.getBundles()) {
			if (b.getState() == Bundle.INSTALLED && b.getHeaders()
				.get(aQute.bnd.osgi.Constants.FRAGMENT_HOST) == null) {
				try {
					b.start();
				} catch (BundleException e) {
					sb.append(b.getSymbolicName())
						.append(" [")
						.append(b.getBundleId())
						.append("];")
						.append(b.getVersion())
						.append("\n");
					sb.append("    ")
						.append(e.getMessage())
						.append("\n\n");
					System.err.println(e.getMessage());
				}
			}
		}
		Matcher matcher = IP_P.matcher(sb);
		String out = matcher
			.replaceAll("\n\n         " + aQute.bnd.osgi.Constants.IMPORT_PACKAGE + ": $1;version=[$2,$3)\n");
		assertTrue("Unresolved bundles\n" + out, sb.length() == 0);
	}
}
