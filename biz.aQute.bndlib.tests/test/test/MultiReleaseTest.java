package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.JPMSModule;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.resource.SupportingResource;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

public class MultiReleaseTest {

	@InjectTemporaryDirectory
	File tmp;

	@Test
	public void testBuild() throws Exception {

		MavenVersion v = new MavenVersion(System.getProperty("java.version"));
		if (v.getOSGiVersion()
			.getMajor() < 17)
			return;

		Workspace ws = getWorkspace();
		Project main = ws.getProject("multirelease.main");
		Project v9 = ws.getProject("multirelease.v9");
		Project v12 = ws.getProject("multirelease.v12");
		Project v17 = ws.getProject("multirelease.v17");

		v17.compile(false);
		v17.build();
		v12.compile(false);
		v12.build();
		v9.compile(false);
		v9.build();
		main.setProperty(Constants.JPMS_MODULE_INFO, "");
		main.setProperty(Constants.AUTOMATIC_MODULE_NAME, "foo.bar");
		main.compile(false);
		main.build();

		assertThat(v17.check()).isTrue();
		assertThat(v9.check()).isTrue();
		assertThat(main.check()).isTrue();

		assertThat(main.getFile("generated/multirelease.main.jar")).isNotNull();

		Jar jar = new Jar(main.getFile("generated/multirelease.main.jar"));

		JPMSModule jpms = new JPMSModule(jar);

		assertThat(jpms.isMultiRelease()).isTrue();

		Manifest defaultManifest = jpms.getManifest(0);
		assertThat(defaultManifest).isNotNull();
		assertThat(defaultManifest.getMainAttributes()
			.getValue(Constants.IMPORT_PACKAGE)).isEqualTo("org.osgi.framework;version=\"[1.5,2)\"");
		assertThat(defaultManifest.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY)).isEqualTo("osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"");

		Manifest manifest9 = jpms.getManifest(9);
		assertThat(manifest9).isNotNull();
		assertThat(manifest9.getMainAttributes()
			.getValue(Constants.IMPORT_PACKAGE)).isEqualTo("");
		assertThat(manifest9.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY)).isEqualTo("osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=9))\"");

		Manifest manifest12 = jpms.getManifest(12);
		assertThat(manifest12).isNotNull();
		assertThat(manifest12.getMainAttributes()
			.getValue(Constants.IMPORT_PACKAGE)).isEqualTo("org.osgi.service.url;version=\"[1.0,2)\"");
		assertThat(manifest12.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY)).isEqualTo("osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=12))\"");

		Manifest manifest17 = jpms.getManifest(17);
		assertThat(manifest17).isNotNull();
		assertThat(manifest17.getMainAttributes()
			.getValue(Constants.IMPORT_PACKAGE))
				.isEqualTo("org.osgi.service.startlevel;version=\"[1.1,2)\",org.osgi.service.url;version=\"[1.0,2)\"");
		assertThat(manifest17.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY)).isEqualTo(
				"fake;filter:=\"(&(fake=fake)(version>=1.2.3)(!(version>=2.0.0)))\",osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=17))\"");

		/*
		 * check finding a resource from low to high
		 */

		Optional<Resource> somefile = jpms.findResource("somefile", -1);
		assertThat(somefile).isPresent();
		assertThat(IO.collect(somefile.get()
			.openInputStream())).isEqualTo("9");

		somefile = jpms.findResource("somefile", Integer.MAX_VALUE);
		assertThat(somefile).isPresent();
		assertThat(IO.collect(somefile.get()
			.openInputStream())).isEqualTo("17");

		somefile = jpms.findResource("somefile", 11);
		assertThat(somefile).isPresent();
		assertThat(IO.collect(somefile.get()
			.openInputStream())).isEqualTo("9");

		somefile = jpms.findResource("somefile", 13);
		assertThat(somefile).isPresent();
		assertThat(IO.collect(somefile.get()
			.openInputStream())).isEqualTo("12");

		assertThat(jpms.getModuleName()).isPresent()
			.get()
			.isEqualTo("foo.bar");

		Resource m0 = jpms.getResource(0, "module-info.class");

		Resource mi9 = jpms.getResource(9, "module-info.class");
		assertThat(mi9).isNotNull()
			.isNotEqualTo(m0);
		assertThat(getModuleName(mi9)).isEqualTo("foo.bar");

		Resource mi17 = jpms.getResource(17, "module-info.class");
		assertThat(mi17).isNotNull()
			.isNotEqualTo(mi9)
			.isNotEqualTo(m0);
		assertThat(getModuleName(mi17)).isEqualTo("foo.bar");

	}

	private String getModuleName(Resource mi) {
		return Clazz.parse(mi)
			.getAttribute(ModuleAttribute.class)
			.map(ma -> ma.module_name)
			.orElse(null);
	}

	/**
	 * Check a multi-release analysis. The gson jar has the module in the
	 * versioned/9 directory. So it has no module name in the root.
	 */

	@Test
	public void testMultiReleaseAnalysis() throws Exception {
		Jar outer = new Jar(IO.getFile("jar/multi-release-gson-2.9.1.jar"));
		JPMSModule m = new JPMSModule(outer);

		assertThat(outer.getModuleName()).isEqualTo("com.google.gson");
		assertThat(outer.getModuleVersion()).isEqualTo("2.9.1");
		assertThat(m.getModuleName()).isPresent()
			.get()
			.isEqualTo("com.google.gson");
		assertThat(m.getModuleVersion()).isPresent()
			.get()
			.isEqualTo("2.9.1");

		assertThat(m.getModuleName()).isPresent()
			.get()
			.isEqualTo("com.google.gson");

		try (Builder source = new Builder()) {
			source.setProperty(Constants.JPMS_MULTI_RELEASE, "true"); // set by
																		// default
																		// in
																		// workspace
																		// model
			source.setProperty("-includeresource", "@jar/multi-release-gson-2.9.1.jar");
			source.setProperty("-exportcontents", "*");

			Jar build = source.build();

			assertThat(source.check()).isTrue();

			JPMSModule m2 = new JPMSModule(build);
			assertThat(m2.isMultiRelease()).isTrue();
			assertThat(build.getManifest()
				.getMainAttributes()
				.getValue(JPMSModule.MULTI_RELEASE_HEADER)).isEqualTo("true");
		}

	}

	@Test
	public void testMultiReleaseIndexing() throws Exception {

		File file = IO.getFile("jar/multi-release-ok.jar");

		ResourceBuilder rb = new ResourceBuilder();
		boolean identity = rb.addFile(file);
		assertThat(identity).isTrue();
		SupportingResource r = rb.build();
		assertThat(r.hasIdentity()).isTrue();

		testResource(r, "(&(osgi.wiring.package=org.osgi.framework)(version>=1.5.0)(!(version>=2.0.0)))",
			"(&(osgi.ee=JavaSE)(&(version>=1.8.0)(!(version>=9.0.0))))");
		assertThat(r.getSupportingResources()).hasSize(3);

		testResource(r.getSupportingResources()
			.get(0), null, "(&(osgi.ee=JavaSE)(&(version>=9.0.0)(!(version>=12.0.0))))");
		testResource(r.getSupportingResources()
			.get(1), "(&(osgi.wiring.package=org.osgi.service.url)(version>=1.0.0)(!(version>=2.0.0)))",
			"(&(osgi.ee=JavaSE)(&(version>=12.0.0)(!(version>=17.0.0))))");
		testResource(r.getSupportingResources()
			.get(2),
			"(&(osgi.wiring.package=org.osgi.service.startlevel)(version>=1.1.0)(!(version>=2.0.0))),(&(osgi.wiring.package=org.osgi.service.url)(version>=1.0.0)(!(version>=2.0.0)))",
			"(&(osgi.ee=JavaSE)(version>=17.0.0))");

		// check the expansion of the SupportingResources
		ResourcesRepository repo = new ResourcesRepository();
		repo.add(r);
		assertThat(repo.getResources()).hasSize(4);

		// check the expansion of the SupportingResource & roundtrip through XML

		XMLResourceGenerator xg = new XMLResourceGenerator();
		xg.resource(r);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		xg.save(bout);
		String s = new String(bout.toByteArray(), StandardCharsets.UTF_8);
		System.out.println(s);
		List<org.osgi.resource.Resource> l = XMLResourceParser
			.getResources(new ByteArrayInputStream(bout.toByteArray()), new URI(""));
		assertThat(l).hasSize(4);

	}

	final static org.osgi.framework.Version lowest = new org.osgi.framework.Version("0");

	private void testResource(org.osgi.resource.Resource r, String imports, String requires) {
		testFilters(r, PackageNamespace.PACKAGE_NAMESPACE, Strings.split(imports));
		testFilters(r, ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, Strings.split(requires));

		Capability capability = r.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE)
			.get(0);
		assertThat(capability.getAttributes()
			.get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE)).isEqualTo(lowest);
		assertThat(capability.getAttributes()
			.get(BundleNamespace.BUNDLE_NAMESPACE)).isEqualTo("multirelease.main");

	}

	private void testFilters(org.osgi.resource.Resource r, String namespace, List<String> split) {
		List<String> filters = r.getRequirements(namespace)
			.stream()
			.map(rq -> {
				System.out.println(rq);
				return rq;
			})
			.map(rq -> rq.getDirectives()
				.get("filter"))
			.filter(Objects::nonNull)
			.sorted()
			.collect(Collectors.toList());

		assertThat(filters).isEqualTo(split);
	}

	@Test
	public void testPlainBuilder() throws Exception {
		try (Builder builder = new Builder()) {
			builder.setProperty(Constants.JPMS_MODULE_INFO, "");
			builder.setProperty("-includeresource", """
				sun_1_8/=compilerversions/src/sun_1_8/, \
				META-INF/versions/9/jdk_9_0/=compilerversions/src/jdk_9_0/, \
				META-INF/versions/11/jdk_11_0/=compilerversions/src/jdk_11_0/, \
				META-INF/versions/17/jdk_17/=compilerversions/src/jdk_17/, \
				META-INF/versions/19/jdk_19/=compilerversions/src/jdk_19/, \
				""");

			Jar jar = builder.build();
			assertThat(builder.check()).isTrue();

			assertThat(jar.getResource("META-INF/versions/9/OSGI-INF/MANIFEST.MF")).isNotNull();
			assertThat(jar.getResource("META-INF/versions/9/module-info.class")).isNotNull();

			assertThat(jar.getResource("META-INF/versions/11/OSGI-INF/MANIFEST.MF")).isNotNull();
			assertThat(jar.getResource("META-INF/versions/11/module-info.class")).isNotNull();

			assertThat(jar.getResource("META-INF/versions/17/OSGI-INF/MANIFEST.MF")).isNotNull();
			assertThat(jar.getResource("META-INF/versions/17/module-info.class")).isNotNull();

			assertThat(jar.getResource("META-INF/versions/19/OSGI-INF/MANIFEST.MF")).isNotNull();
			assertThat(jar.getResource("META-INF/versions/19/module-info.class")).isNotNull();

		}
	}

	Workspace getWorkspace() throws Exception {
		File file = IO.getFile("testresources/ws-multirelease");
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}
}
