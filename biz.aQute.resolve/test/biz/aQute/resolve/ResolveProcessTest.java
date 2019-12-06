package biz.aQute.resolve;

import static org.osgi.framework.Version.parseVersion;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_FILTER_DIRECTIVE;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.lib.io.IO;
import junit.framework.TestCase;
import test.lib.MockRegistry;

public class ResolveProcessTest extends TestCase {

	private final class ResourceComparator implements Comparator<Resource> {
		@Override
		public int compare(Resource o1, Resource o2) {
			Map<String, Object> a1 = o1.getCapabilities(IDENTITY_NAMESPACE)
				.get(0)
				.getAttributes();
			Map<String, Object> a2 = o2.getCapabilities(IDENTITY_NAMESPACE)
				.get(0)
				.getAttributes();
			String n1 = (String) a1.get(IDENTITY_NAMESPACE);
			String n2 = (String) a2.get(IDENTITY_NAMESPACE);

			int diff = n1.compareTo(n2);
			if (diff == 0) {
				diff = ((Version) a1.get(CAPABILITY_VERSION_ATTRIBUTE))
					.compareTo((Version) a2.get(CAPABILITY_VERSION_ATTRIBUTE));
			}
			return diff;
		}
	}

	public void testResolveRequired() throws Exception {
		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			registry.addPlugin(getIndex("testdata/repo7/index.xml"));

			Processor model = new Processor();

			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires",
				"osgi.extender;filter:='(&(osgi.extender=osgi.component)(version>=1.3)(!(version>=2)))'");

			Map<Resource, List<Wire>> requiredResources = process.resolveRequired(model, null, registry,
				new BndResolver(logger), Collections.emptyList(), logger);

			Collection<Resource> optionalResources = process.getOptionalResources();

			assertEquals(1, requiredResources.size());
			assertEquals(3, optionalResources.size());

			SortedSet<Resource> set = new TreeSet<>(new ResourceComparator());

			set.addAll(optionalResources);

			Iterator<Resource> it = set.iterator();

			checkOptionalResource(process, it.next(), "org.apache.felix.configadmin", parseVersion("1.8.8"),
				"org.osgi.service.cm");
			checkOptionalResource(process, it.next(), "org.apache.felix.log", parseVersion("1.0.1"),
				"org.osgi.service.log");
			checkOptionalResource(process, it.next(), "org.apache.felix.metatype", parseVersion("1.1.0"),
				"org.osgi.service.metatype");
		}
	}

	public void testBigNastyResolveRequired() throws Exception {
		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			registry.addPlugin(getIndex("testdata/repo7/index.xml"));
			registry.addPlugin(getIndex("testdata/repo7/index-aries.xml"));
			registry.addPlugin(getIndex("testdata/repo7/index-gemini.xml"));
			registry.addPlugin(getIndex("testdata/repo7/index-local.xml"));

			Processor model = new Processor();

			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires", "osgi.extender;filter:='(osgi.extender=osgi.component)'");

			Map<Resource, List<Wire>> requiredResources = process.resolveRequired(model, null, registry,
				new BndResolver(logger), Collections.emptyList(), logger);

			Collection<Resource> optionalResources = process.getOptionalResources();

			assertEquals(1, requiredResources.size());
			assertEquals(13, optionalResources.size());

			SortedSet<Resource> set = new TreeSet<>(new ResourceComparator());

			set.addAll(optionalResources);

			Iterator<Resource> it = set.iterator();

			checkOptionalResource(process, it.next(), "org.apache.felix.configadmin", parseVersion("1.8.2"),
				"org.osgi.service.cm");
			checkOptionalResource(process, it.next(), "org.apache.felix.configadmin", parseVersion("1.8.8"),
				"org.osgi.service.cm");
			checkOptionalResource(process, it.next(), "org.apache.felix.gogo.command", parseVersion("0.12.0"),
				"org.osgi.service.log");
			checkOptionalResource(process, it.next(), "org.apache.felix.gogo.runtime", parseVersion("0.10.0"),
				"org.apache.felix.service.command");
			checkOptionalResource(process, it.next(), "org.apache.felix.log", parseVersion("1.0.1"),
				"org.osgi.service.log");
			checkOptionalResource(process, it.next(), "org.apache.felix.metatype", parseVersion("1.0.4"),
				"org.osgi.service.metatype");
			checkOptionalResource(process, it.next(), "org.apache.felix.metatype", parseVersion("1.1.0"),
				"org.osgi.service.metatype");
			checkOptionalResource(process, it.next(), "org.apache.felix.shell", parseVersion("1.4.2"),
				"org.osgi.service.log", "org.apache.felix.shell");
			checkOptionalResource(process, it.next(), "org.eclipse.osgi.services", parseVersion("3.1.200.v20070605"),
				"org.osgi.service.cm", "org.osgi.service.log", "org.osgi.service.metatype");
			checkOptionalResource(process, it.next(), "org.ops4j.pax.logging.pax-logging-api", parseVersion("1.4.0"),
				"org.osgi.service.log");
			checkOptionalResource(process, it.next(), "osgi.cmpn", parseVersion("4.3.1.201210102024"),
				"org.osgi.service.cm", "org.osgi.service.log", "org.osgi.service.metatype");
			checkOptionalResource(process, it.next(), "osgi.cmpn", parseVersion("5.0.0.201305092017"),
				"org.osgi.service.cm", "org.osgi.service.log", "org.osgi.service.metatype");
			checkOptionalResource(process, it.next(), "osgi.enterprise", parseVersion("4.2.0.201003190513"),
				"org.osgi.service.cm", "org.osgi.service.log", "org.osgi.service.metatype");
		}
	}

	public void testResolveFailure_1() throws Exception {
		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			registry.addPlugin(getIndex("testdata/repo7/index.xml"));

			Processor model = new Processor();

			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires", "osgi.wiring.package;filter:='(osgi.wiring.package=aQute.lib.io)'");

			try {
				process.resolveRequired(model, null, registry, new BndResolver(logger), Collections.emptyList(),
					logger);
			} catch (ResolutionException re) {
				assertNotNull(re.getCause());

				String output = ResolveProcess.format(re);

				System.out.println(output);

				String expected = "osgi.wiring.package: (osgi.wiring.package=aQute.lib.io)";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));

				return;
			}

			fail("Should have failed to resolve.");
		}
	}

	public void testResolveFailure_2() throws Exception {
		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			registry.addPlugin(getIndex("testdata/repo7/index.xml"));

			Processor model = new Processor();

			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires", "osgi.service;filter:='(objectClass=java.util.concurrent.Executor)'");

			try {
				process.resolveRequired(model, null, registry, new BndResolver(logger), Collections.emptyList(),
					logger);
			} catch (ResolutionException re) {
				assertNotNull(re.getCause());

				String output = ResolveProcess.format(re);

				System.out.println(output);

				String expected = "osgi.service: (objectClass=java.util.concurrent.Executor)";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));

				return;
			}

			fail("Should have failed to resolve.");
		}
	}

	public void testResolveFailure_3() throws Exception {
		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			registry.addPlugin(getIndex("testdata/repo7/index.xml"));

			Processor model = new Processor();

			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires",
				"osgi.enroute.endpoint;filter:='(&(osgi.enroute.endpoint=/sse/1)(&(version>=1.1.0)(!(version>=2.0.0))))'");

			try {
				process.resolveRequired(model, null, registry, new BndResolver(logger), Collections.emptyList(),
					logger);
			} catch (ResolutionException re) {
				assertNotNull(re.getCause());

				String output = ResolveProcess.format(re);

				System.out.println(output);

				String expected = "osgi.enroute.endpoint: (&(osgi.enroute.endpoint=/sse/1)(&(version>=1.1.0)(!(version>=2.0.0))))";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));

				return;
			}

			fail("Should have failed to resolve.");
		}
	}

	// transitive failure
	public void testResolveFailure_4() throws Exception {
		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			registry.addPlugin(getIndex("testdata/repo7/index-aggregate.xml"));

			Processor model = new Processor();

			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires", "osgi.identity;filter:='(osgi.identity=biz.aQute.bnd)'");

			try {
				process.resolveRequired(model, null, registry, new BndResolver(logger), Collections.emptyList(),
					logger);
			} catch (ResolutionException re) {
				assertNotNull(re.getCause());

				String output = ResolveProcess.format(re);

				System.out.println(output);

				String expected = "osgi.wiring.package=org.apache.tools.ant.types";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));

				expected = "Capabilities satisfying the following requirements could not be found:";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));
				expected = "The following requirements are optional:";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));

				return;
			}

			fail("Should have failed to resolve.");
		}
	}

	// transitive failure, no optional requirements
	public void testResolveFailure_5() throws Exception {
		ResolveProcess process = new ResolveProcess();
		try (ResolverLogger logger = new ResolverLogger()) {
			MockRegistry registry = new MockRegistry();

			registry.addPlugin(getIndex("testdata/repo7/index-aggregate.xml"));

			Processor model = new Processor();

			model.setProperty("-runfw", "org.apache.felix.framework");
			model.setProperty("-runrequires", "osgi.identity;filter:='(osgi.identity=biz.aQute.bnd)'");

			try {
				process.resolveRequired(model, null, registry, new BndResolver(logger), Collections.emptyList(),
					logger);
			} catch (ResolutionException re) {
				assertNotNull(re.getCause());

				String output = ResolveProcess.format(re, false);

				System.out.println(output);

				String expected = "osgi.wiring.package=org.apache.tools.ant.types";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));

				expected = "Capabilities satisfying the following requirements could not be found:";
				assertTrue("Doesn't contain " + expected + ": <" + output + ">", output.contains(expected));
				expected = "The following requirements are optional:";
				assertFalse("Contains " + expected + ": <" + output + ">", output.contains(expected));

				return;
			}

			fail("Should have failed to resolve.");
		}
	}

	private OSGiRepository getIndex(String location) throws Exception {
		OSGiRepository repo = new OSGiRepository();
		HttpClient httpClient = new HttpClient();
		Map<String, String> map = new HashMap<>();
		map.put("locations", IO.getFile(location)
			.toURI()
			.toString());
		map.put("name", getName());
		map.put("cache",
			new File("generated/tmp/test/cache/" + getClass().getName() + "/" + getName()).getAbsolutePath());
		repo.setProperties(map);
		Processor p = new Processor();
		p.addBasicPlugin(httpClient);
		repo.setRegistry(p);
		return repo;
	}

	private void checkOptionalResource(ResolveProcess process, Resource resource, String bsn, Version version,
		String packageName, String... morePackages) {
		Collection<Wire> reasons;
		Map<String, Object> idAttrs;
		idAttrs = resource.getCapabilities(IDENTITY_NAMESPACE)
			.get(0)
			.getAttributes();
		assertEquals(bsn, idAttrs.get(IDENTITY_NAMESPACE));
		assertEquals(version, idAttrs.get(CAPABILITY_VERSION_ATTRIBUTE));
		reasons = process.getOptionalReasons(resource);
		assertEquals(1 + morePackages.length, reasons.size());

		Iterator<Wire> iterator = reasons.iterator();

		Wire wire = iterator.next();
		assertEquals("org.apache.felix.scr", wire.getRequirer()
			.getCapabilities(IDENTITY_NAMESPACE)
			.get(0)
			.getAttributes()
			.get(IDENTITY_NAMESPACE));
		assertTrue(wire.getRequirement()
			.toString(),
			wire.getRequirement()
				.getDirectives()
				.get(REQUIREMENT_FILTER_DIRECTIVE)
				.contains(packageName));

		for (String pkg : morePackages) {
			wire = iterator.next();
			assertEquals("org.apache.felix.scr", wire.getRequirer()
				.getCapabilities(IDENTITY_NAMESPACE)
				.get(0)
				.getAttributes()
				.get(IDENTITY_NAMESPACE));
			assertTrue(wire.getRequirement()
				.toString(),
				wire.getRequirement()
					.getDirectives()
					.get(REQUIREMENT_FILTER_DIRECTIVE)
					.contains(pkg));
		}
	}

}
