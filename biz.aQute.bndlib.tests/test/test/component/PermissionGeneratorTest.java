package test.component;

import static aQute.bnd.test.BndTestCase.assertOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.PermissionGenerator;
import aQute.bnd.osgi.Resource;

public class PermissionGeneratorTest {
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

		Set<String> permissions = new HashSet<>();
		String line = null;
		while ((line = r.readLine()) != null) {
			if (!line.isEmpty()) {
				System.err.println("Permission read: " + line);
				assertThat(permissions).as("Found duplicate permission: %s", line)
					.doesNotContain(line);
				permissions.add(line);
			}
		}

		tmpFile.delete();

		return permissions;
	}

	private static Set<String> filterAndSubtract(Set<String> input, String regex) {
		Set<String> result = new HashSet<>();
		Pattern pattern = Pattern.compile(regex);
		for (Iterator<String> it = input.iterator(); it.hasNext();) {
			String string = it.next();
			Matcher matcher = pattern.matcher(string);
			if (matcher.matches()) {
				String selected = matcher.group(1);
				result.add(selected);
				it.remove();
			}
		}
		return result;
	}

	private static void assertNothingLeft(Set<String> permissions) {
		assertThat(permissions).as("No other permissions expected")
			.isEmpty();
	}

	private static void assertPackageAvailable(Set<String> permissions) {
		Set<String> importedPackages = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.PackagePermission \"([^\"]+)\" \"import\"\\)$");
		Set<String> exportedPackages = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.PackagePermission \"([^\"]+)\" \"exportonly,import\"\\)$");

		/* @formatter:off */
		assertThat(importedPackages).containsExactlyInAnyOrder(
			"aQute.bnd.differ",
            "aQute.bnd.header",
            "aQute.bnd.osgi",
            "aQute.bnd.service.diff",
            "aQute.bnd.test",
            "aQute.bnd.version",
            "aQute.lib.filter",
            "aQute.lib.io",
            "aQute.lib.xml",
            "aQute.service.reporter",
            "javax.xml.namespace",
            "javax.xml.parsers",
            "javax.xml.xpath",
            "org.assertj.core.api",
            "org.junit.jupiter.api",
            "org.osgi.framework",
            "org.osgi.service.component",
            "org.osgi.service.log",
            "org.w3c.dom",
            "org.xml.sax");
		assertThat(exportedPackages).containsExactlyInAnyOrder("test.api");
		/* @formatter:on */
	}

	private static void assertServicesAvailable(Set<String> permissions) {
		Set<String> registeredServices = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.ServicePermission \"([^\"]+)\" \"register\"\\)$");
		Set<String> requiredServices = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.ServicePermission \"([^\"]+)\" \"get\"\\)$");

		/* @formatter:off */
		assertThat(registeredServices).as("Registered services")
			.containsExactlyInAnyOrder(
				"java.io.Serializable",
				"java.lang.Object",
				"java.lang.Runnable",
				"org.osgi.service.component.ComponentFactory");
		assertThat(requiredServices).as("Required services")
			.containsExactlyInAnyOrder("org.osgi.service.log.LogService");
		/* @formatter:on */
	}

	private static void assertCapabilitiesAvailable(Set<String> permissions) {
		Set<String> requiredCapabilities = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.CapabilityPermission \"([^\"]+)\" \"require\"\\)$");
		Set<String> providedCapabilities = filterAndSubtract(permissions,
			"^\\(org.osgi.framework.CapabilityPermission \"([^\"]+)\" \"provide\"\\)$");

		assertThat(providedCapabilities).as("Provided capabilities")
			.containsExactlyInAnyOrder("osgi.service");
		assertThat(requiredCapabilities).as("Required capabilities")
			.containsExactlyInAnyOrder("osgi.service");
	}

	private static void assertAdminAvailable(Set<String> permissions) {
		Set<String> adminCapabilities = filterAndSubtract(permissions, "^\\((org.osgi.framework.AdminPermission)\\)$");
		assertThat(adminCapabilities).as("Admin capabilities")
			.hasSize(1);
	}

	@Test
	public void testJustPackages() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;packages}");
		assertPackageAvailable(permissions);
		assertNothingLeft(permissions);
	}

	@Test
	public void testJustServices() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;services}");
		assertServicesAvailable(permissions);
		assertNothingLeft(permissions);
	}

	@Test
	public void testJustCapabilities() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;capabilities}");
		assertCapabilitiesAvailable(permissions);
		assertNothingLeft(permissions);
	}

	@Test
	public void testJustAdmin() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;admin}");
		assertAdminAvailable(permissions);
		assertNothingLeft(permissions);
	}

	@Test
	public void testConcatenatedPermissionsNoAdmin() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor(
			"${permissions;packages}${permissions;services}${permissions;capabilities}");
		assertPackageAvailable(permissions);
		assertServicesAvailable(permissions);
		assertCapabilitiesAvailable(permissions);
		assertNothingLeft(permissions);
	}

	@Test
	public void testInlinePermissionsNoAdmin() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;packages;services;capabilities}");
		assertPackageAvailable(permissions);
		assertServicesAvailable(permissions);
		assertCapabilitiesAvailable(permissions);
		assertNothingLeft(permissions);
	}

	@Test
	public void testInlinePermissions() throws Exception {
		Set<String> permissions = getPermissionsGeneratedFor("${permissions;packages;services;capabilities;admin}");
		assertPackageAvailable(permissions);
		assertServicesAvailable(permissions);
		assertCapabilitiesAvailable(permissions);
		assertAdminAvailable(permissions);
		assertNothingLeft(permissions);
	}

	@Test
	public void testCustomCapabilityParsing() throws Exception {
		Builder b = new Builder();
		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=*)\"");
		Set<String> services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).containsExactlyInAnyOrder("*");

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(!(objectClass=*))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).isEmpty();

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(!(objectClass=test.Helper))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).isEmpty();

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=test.*)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).containsExactlyInAnyOrder("test.*");

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(|(objectClass=test.*)(objectClass=test2.*))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).containsExactlyInAnyOrder("test.*", "test2.*");

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(|(objectClass=test.*)(!(objectClass=test2.*)))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).containsExactlyInAnyOrder("test.*");

		// In the following cases, there is no way to determine the permissions
		// needed, so nothing is generated. In these cases it is up to the user
		// to generate the permission
		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=test2.*Helper)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).isEmpty();

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(objectClass=test*.*)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).isEmpty();

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(other=prop)\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).isEmpty();

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(&(objectClass=test.*)(objectClass=*Helper))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).isEmpty();

		b.setProperty("Require-Capability", "osgi.service;filter:=\"(!(&(objectClass=test.*)(objectClass=*Helper)))\"");
		services = PermissionGenerator.getReferencedServices(b);
		assertThat(services).isEmpty();
	}
}
