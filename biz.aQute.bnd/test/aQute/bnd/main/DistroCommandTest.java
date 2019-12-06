package aQute.bnd.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.main.RemoteCommand.DistroOptions;
import aQute.bnd.main.RemoteCommand.RemoteOptions;
import aQute.bnd.main.testlib.MockRegistry;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.getopt.CommandLine;
import aQute.lib.io.IO;
import biz.aQute.resolve.BndResolver;
import biz.aQute.resolve.ResolveProcess;
import biz.aQute.resolve.ResolverLogger;
import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class DistroCommandTest extends TestCase {

	private Framework	framework;
	private File		tmp;

	private String getTestName() {
		return getClass().getName() + "/" + getName();
	}

	@Override
	protected void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp/test/" + getTestName());
		IO.delete(tmp);
		tmp.mkdirs();

		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class, this.getClass()
			.getClassLoader());

		FrameworkFactory ff = sl.iterator()
			.next();
		Map<String, String> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, new File(tmp, "fwstorage").getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.slf4j");
		framework = ff.newFramework(configuration);
		framework.init();
		framework.start();
		BundleContext context = framework.getBundleContext();

		String[] bundles = {
			"../biz.aQute.remote/generated/biz.aQute.remote.agent.jar",
			"testdata/bundles/com.liferay.dynamic.data.mapping.taglib.jar",
			"testdata/bundles/com.liferay.item.selector.taglib.jar", "testdata/bundles/org.apache.felix.log-1.2.0.jar",
			"testdata/bundles/org.apache.felix.log-1.0.1.jar"
		};

		List<Bundle> toBeStarted = new ArrayList<>();

		for (String bundle : bundles) {
			String location = "reference:" + IO.getFile(bundle)
				.toURI()
				.toString();
			Bundle b = context.installBundle(location);
			toBeStarted.add(b);
		}
		for (Bundle b : toBeStarted) {
			if (b.getHeaders()
				.get(Constants.FRAGMENT_HOST) == null)
				b.start();
		}
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		framework.stop();
		framework.waitForStop(10000);
		super.tearDown();
	}

	public void testMultiplePackageVersionsKeepsHighest() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.jar");

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);

		try (RemoteCommand rc = new RemoteCommand(bnd, remoteOptions)) {
			rc._distro(distroOptions);
		}

		assertTrue(distro.exists());

		ResourceBuilder builder = new ResourceBuilder();

		try (Jar jar = new Jar(distro)) {
			Domain manifest = Domain.domain(jar.getManifest());

			builder.addManifest(manifest);

			Resource resource = builder.build();

			assertEquals("1.4.0", resource.getCapabilities("osgi.wiring.package")
				.stream()
				.map(Capability::getAttributes)
				.filter(atts -> ((String) atts.get("osgi.wiring.package")).equals("org.osgi.service.log"))
				.findAny()
				.map(atts -> atts.get("version"))
				.map(String::valueOf)
				.get());
		}
	}

	public void testMultipleCapabilitiesPerNamespace() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.jar");

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);

		try (RemoteCommand rc = new RemoteCommand(bnd, remoteOptions)) {
			rc._distro(distroOptions);
		}

		assertTrue(distro.exists());

		ResourceBuilder builder = new ResourceBuilder();

		try (Jar jar = new Jar(distro)) {
			Domain manifest = Domain.domain(jar.getManifest());

			builder.addManifest(manifest);

			Resource resource = builder.build();
			verifyResource(resource);
		}
	}

	public void testXmlOutput() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.xml").getAbsoluteFile();

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-x");
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);
		try (RemoteCommand rc = new RemoteCommand(bnd, remoteOptions)) {
			rc._distro(distroOptions);
		}

		assertTrue(distro.exists());

		try (XMLResourceParser parser = new XMLResourceParser(distro)) {
			List<Resource> resources = parser.parse();
			assertThat(resources).hasSize(1);

			Resource resource = resources.get(0);
			assertThat(resource.getRequirements(null)).isEmpty();
			verifyResource(resource);
		}
	}

	public void verifyResource(Resource resource) {
		List<Capability> extenderCaps = resource.getCapabilities(ExtenderNamespace.EXTENDER_NAMESPACE);

		int jspTaglibCapabilityCount = 0;
		for (Capability capability : extenderCaps) {
			Map<String, Object> attributes = capability.getAttributes();
			if ("jsp.taglib".equals(attributes.get(ExtenderNamespace.EXTENDER_NAMESPACE))) {
				jspTaglibCapabilityCount++;
			}
		}
		assertEquals(2, jspTaglibCapabilityCount);

		List<Capability> eeCaps = resource
			.getCapabilities(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);

		assertTrue(eeCaps.size() > 0);

		Capability javaSECap = null;
		for (Capability capability : eeCaps) {
			Map<String, Object> attributes = capability.getAttributes();
			if ("JavaSE".equals(attributes.get(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE))) {
				javaSECap = capability;
			}
		}
		assertNotNull(javaSECap);
		@SuppressWarnings("null")
		Map<String, Object> attributes = javaSECap.getAttributes();
		assertTrue(attributes.containsKey("version"));
		@SuppressWarnings("unchecked")
		List<Version> versions = (List<Version>) attributes.get("version");
		assertTrue(versions.size() > 1);
		assertTrue(versions.contains(new Version("1.7.0")));
		assertTrue(versions.contains(new Version("1.6.0")));
		assertTrue(versions.contains(new Version("1.5.0")));
		assertTrue(versions.contains(new Version("1.4.0")));
		assertTrue(versions.contains(new Version("1.3.0")));
	}

	public void testResolveAgainstDistro() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.jar");

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);

		try (RemoteCommand rc = new RemoteCommand(bnd, remoteOptions)) {
			rc._distro(distroOptions);
		}

		assertTrue(distro.exists());

		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			Processor model = new Processor();

			model.setProperty("-distro", distro.getAbsolutePath() + ";version=file");
			model.setProperty("-runfw", "org.eclipse.osgi");
			model.setProperty("-runrequires",
				"osgi.wiring.package;filter:='(osgi.wiring.package=com.liferay.dynamic.data.mapping.taglib.servlet.taglib)'");

			Map<Resource, List<Wire>> requiredResources = process.resolveRequired(model, null, registry,
				new BndResolver(logger), Collections.emptyList(), logger);

			assertEquals(1, requiredResources.size());
		}
	}

	public void testDistroJarLastModified() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.jar");

		if (distro.exists()) {
			assertTrue(distro.delete());
		}

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);

		try (RemoteCommand rc = new RemoteCommand(bnd, remoteOptions)) {
			rc._distro(distroOptions);
		}

		assertTrue(distro.exists());

		assertTrue(distro.lastModified() > 0);

		//
		// Verify that we can parse the XML
		// inside the JAR
		//
		try (Jar jar = new Jar(distro)) {
			assertThat(jar.getResource("OSGI-OPT/obr.xml")).isNotNull();
			List<Resource> resources = XMLResourceParser.getResources(jar.getResource("OSGI-OPT/obr.xml")
				.openInputStream(), IO.work.toURI());
			assertThat(resources).hasSize(1);
		}
	}

	public void testDistroJarNotResolvable() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.jar");

		if (distro.exists()) {
			assertTrue(distro.delete());
		}

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);

		try (RemoteCommand rc = new RemoteCommand(bnd, remoteOptions)) {
			rc._distro(distroOptions);
		}

		assertTrue(distro.exists());

		Domain domain = Domain.domain(distro);

		Parameters providedCapabilities = domain.getProvideCapability();

		assertTrue(providedCapabilities.containsKey("osgi.unresolvable"));

		Parameters requiredCapabilities = domain.getRequireCapability();

		assertTrue(requiredCapabilities.containsKey("osgi.unresolvable"));

		Attrs attrs = requiredCapabilities.get("osgi.unresolvable");

		assertEquals("(&(must.not.resolve=*)(!(must.not.resolve=*)))", attrs.get("filter:"));
	}
}
