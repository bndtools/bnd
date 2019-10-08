package test.component;

import static aQute.bnd.test.BndTestCase.assertOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.PermissionGenerator;
import aQute.bnd.osgi.Resource;
import junit.framework.TestCase;

public class PermissionGeneratorTest extends TestCase {
	private static Set<String> getPermissionsGeneratedFor(String permissionFile) throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*_basic");
		b.setProperty("Private-Package", "test.component");
		b.setProperty("Export-Package", "test.api");
		b.setProperty("-includeresource.resourceprops", "resource.props;literal=\"\"");

		File tmpFile = File.createTempFile("bndtest", "permissions.perm");
		FileWriter fileWriter = new FileWriter(tmpFile);
		fileWriter.write(permissionFile);
		fileWriter.close();
		b.setProperty("-includeresource", "{OSGI-INF/permissions.perm=" + tmpFile.getAbsolutePath() + "}");

		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		Resource resource = jar.getResource("OSGI-INF/permissions.perm");
		BufferedReader r = new BufferedReader(new InputStreamReader(resource.openInputStream()));

		Set<String> permissions = new TreeSet<>();
		String line = null;
		while ((line = r.readLine()) != null) {
			if (!line.isEmpty()) {
				assertTrue("Found duplicate permission: " + line, permissions.add(line));
				System.err.println("Permission read: " + line);
			}
		}

		tmpFile.delete();

		return permissions;
	}

	private static Set<String> filterAndSubtract(Set<String> input, String regex) {
		Set<String> result = new TreeSet<>();
		Pattern pattern = Pattern.compile(regex);
		for (Iterator<String> it = input.iterator(); it.hasNext();) {
			String string = it.next();
			Matcher matcher = pattern.matcher(string);
			if (matcher.matches()) {
				it.remove();
				result.add(matcher.group(1));
			}
		}
		return result;
	}

	private static void assertNotingLeft(Set<String> permissions) {
		assertEquals("No other permissions expected", Collections.emptySet(), permissions);
	}

	private static void assertPackageAvailable(Set<String> permissions) {
		Set<String> importedPackages = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.PackagePermission \"([^\"]+)\" \"import\"\\)$");
		Set<String> exportedPackages = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.PackagePermission \"([^\"]+)\" \"export\"\\)$");

		/* @formatter:off */
		assertThat(importedPackages).containsExactly(
			"aQute.bnd.differ",
            "aQute.bnd.header",
            "aQute.bnd.osgi",
            "aQute.bnd.service.diff",
            "aQute.bnd.test",
            "aQute.bnd.version",
            "aQute.lib.filter",
            "aQute.lib.io",
            "aQute.service.reporter",
            "javax.xml.namespace",
            "javax.xml.parsers",
            "javax.xml.xpath",
            "junit.framework",
            "org.assertj.core.api",
            "org.osgi.framework",
            "org.osgi.service.component",
            "org.osgi.service.log",
            "org.w3c.dom",
            "org.xml.sax");
		assertThat(exportedPackages).containsExactly("test.api");
		/* @formatter:on */
	}

	private static void assertServicesAvailable(Set<String> permissions) {
		Set<String> registeredServices = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.ServicePermission \"([^\"]+)\" \"register\"\\)$");
		Set<String> requiredServices = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.ServicePermission \"([^\"]+)\" \"get\"\\)$");

		/* @formatter:off */
		assertEquals("Registered services",
				new TreeSet<>(Arrays.asList("java.lang.Runnable",
						"java.io.Serializable",
						"java.lang.Object")),
						registeredServices);
		assertEquals("Required services",
				new TreeSet<>(Arrays.asList("org.osgi.service.log.LogService")),
						requiredServices);
		/* @formatter:on */
	}

	private static void assertCapabilitiesAvailable(Set<String> permissions) {
		Set<String> requiredCapabilities = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.CapabilityPermission \"([^\"]+)\" \"require\"\\)$");
		Set<String> providedCapabilities = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.CapabilityPermission \"([^\"]+)\" \"provide\"\\)$");

		/* @formatter:off */
		assertEquals("Provided capabilities",
				new TreeSet<>(Arrays.asList("osgi.service")),
				providedCapabilities);
		assertEquals("Required capabilities",
				new TreeSet<>(Arrays.asList("osgi.service")),
						requiredCapabilities);
		/* @formatter:on */
	}

	private static void assertAdminAvailable(Set<String> permissions) {
		Set<String> adminCapabilities = filterAndSubtract(permissions, "^\\((org.osgi.framework.AdminPermission)\\)$");
		assertEquals("Admin capabilities", 1, adminCapabilities.size());
	}

	public static void testJustPackages() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;packages}");
		assertPackageAvailable(permissions);
		assertNotingLeft(permissions);
	}

	public static void testJustServices() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;services}");
		assertServicesAvailable(permissions);
		assertNotingLeft(permissions);
	}

	public static void testJustCapabilities() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;capabilities}");
		assertCapabilitiesAvailable(permissions);
		assertNotingLeft(permissions);
	}

	public static void testJustAdmin() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;admin}");
		assertAdminAvailable(permissions);
		assertNotingLeft(permissions);
	}

	public static void testConcatenatedPermissionsNoAdmin() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor(
			"${permissions;packages}${permissions;services}${permissions;capabilities}");
		assertPackageAvailable(permissions);
		assertServicesAvailable(permissions);
		assertCapabilitiesAvailable(permissions);
		assertNotingLeft(permissions);
	}

	public static void testInlinePermissionsNoAdmin() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;packages;services;capabilities}");
		assertPackageAvailable(permissions);
		assertServicesAvailable(permissions);
		assertCapabilitiesAvailable(permissions);
		assertNotingLeft(permissions);
	}

	public static void testInlinePermissions() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;packages;services;capabilities;admin}");
		assertPackageAvailable(permissions);
		assertServicesAvailable(permissions);
		assertCapabilitiesAvailable(permissions);
		assertAdminAvailable(permissions);
		assertNotingLeft(permissions);
	}

	public static void testCustomCapabilityParsing() throws Exception {
		Builder b = new Builder();
		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=*)\"");
		Set<String> services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.singleton("*"), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(!(objectClass=*))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.emptySet(), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(!(objectClass=test.Helper))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.emptySet(), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=test.*)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.singleton("test.*"), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(|(objectClass=test.*)(objectClass=test2.*))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(new HashSet<>(Arrays.asList("test.*", "test2.*")), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(|(objectClass=test.*)(!(objectClass=test2.*)))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.singleton("test.*"), services);

		// In the following cases, there is no way to determine the permissions
		// needed, so nothing is generated. In these cases it is up to the user
		// to generate the permission
		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=test2.*Helper)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.emptySet(), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=test*.*)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.emptySet(), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(other=prop)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.emptySet(), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(&(objectClass=test.*)(objectClass=*Helper))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.emptySet(), services);

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(!(&(objectClass=test.*)(objectClass=*Helper)))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertEquals(Collections.emptySet(), services);
	}
}
