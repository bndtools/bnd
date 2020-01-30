package aQute.junit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;

public final class BundleUtils {
	public static boolean hasTests(Bundle bundle) {
		return testCases(bundle).anyMatch(s -> true);
	}

	public static boolean hasNoTests(Bundle bundle) {
		return !hasTests(bundle);
	}

	private static final Attributes.Name TESTCASES = new Attributes.Name(aQute.bnd.osgi.Constants.TESTCASES);

	public static Stream<String> testCases(Bundle bundle) {
		URL url = bundle.getEntry("META-INF/MANIFEST.MF");
		if (url == null) {
			return Stream.empty();
		}
		try (InputStream in = url.openStream()) {
			Manifest manifest = new Manifest(in);
			return testCases(manifest.getMainAttributes()
				.getValue(TESTCASES));
		} catch (IOException e) {
			return Stream.empty();
		}
	}

	private final static Pattern SIMPLE_LIST_SPLITTER = Pattern.compile("\\s*,\\s*");

	public static Stream<String> testCases(String testNames) {
		if ((testNames == null) || (testNames = testNames.trim()).isEmpty()) {
			return Stream.empty();
		}
		return SIMPLE_LIST_SPLITTER.splitAsStream(testNames)
			.filter(testName -> !testName.isEmpty());
	}
}
