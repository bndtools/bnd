package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Optional;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.JPMSModule;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;

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
		Project v17 = ws.getProject("multirelease.v17");

		v17.compile(false);
		v17.build();
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
			.getValue(Constants.IMPORT_PACKAGE))
				.isEqualTo("org.osgi.framework;version=\"[1.5,2)\",org.osgi.service.condpermadmin;version=\"[1.1,2)\"");
		assertThat(manifest9.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY)).isEqualTo("osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=9))\"");

		Manifest manifest17 = jpms.getManifest(17);
		assertThat(manifest17).isNotNull();
		assertThat(manifest17.getMainAttributes()
			.getValue(Constants.IMPORT_PACKAGE)).isEqualTo(
				"java.io,java.lang,org.osgi.framework;version=\"[1.5,2)\",org.osgi.service.condpermadmin;version=\"[1.1,2)\",org.osgi.service.startlevel;version=\"[1.1,2)\"");
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

		somefile = jpms.findResource("somefile", 15);
		assertThat(somefile).isPresent();
		assertThat(IO.collect(somefile.get()
			.openInputStream())).isEqualTo("9");

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

	Workspace getWorkspace() throws Exception {
		File file = IO.getFile("testresources/ws-multirelease");
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}
}
