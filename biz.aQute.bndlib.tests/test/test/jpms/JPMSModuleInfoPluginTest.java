package test.jpms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.File;
import java.util.Arrays;

import org.junit.Test;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.classfile.ModuleMainClassAttribute;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

public class JPMSModuleInfoPluginTest {

	@Test
	public void moduleWithOptionsIgnore() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.JPMS_MODULE_INFO_OPTIONS, "java.json.bind;ignore=true");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.j.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/j"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(3)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"));
		}
	}

	@Test
	public void moduleWithOptionsStatic() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.JPMS_MODULE_INFO_OPTIONS, "java.json.bind;static=true");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.j.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/j"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json.bind"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_STATIC_PHASE);
		}
	}

	@Test
	public void moduleWithOptionsMapModuleNameStatic() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.JPMS_MODULE_INFO_OPTIONS,
				"java.enterprise;substitute=geronimo-jcdi_2.0_spec;static=true");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.j.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/j"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("java.enterprise"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.enterprise"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(
					ModuleAttribute.Require.ACC_TRANSITIVE | ModuleAttribute.Require.ACC_STATIC_PHASE);
		}
	}

	@Test
	public void moduleWithOptionsMapModuleName() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.JPMS_MODULE_INFO_OPTIONS,
				"java.enterprise;substitute=geronimo-jcdi_2.0_spec");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.j.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/j"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("java.enterprise"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.enterprise"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);
		}
	}

	@Test
	public void moduleWithAllDynamicImportsOrOptionalIsStatic() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.j.*");
			b.setProperty(Constants.IMPORT_PACKAGE,
				"javax.json.bind;resolution:=dynamic,javax.json.bind.serializer;resolution:=optional,*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/j"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json.bind"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_STATIC_PHASE);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);
		}
	}

	@Test
	public void moduleWithAllDynamicImportsIsStatic_2() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.i.*");
			b.setProperty(Constants.IMPORT_PACKAGE, "javax.json.bind.*;resolution:=dynamic,*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/i"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json.bind"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_STATIC_PHASE);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);
		}
	}

	@Test
	public void moduleWithAllDynamicImportsIsStatic() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.i.*");
			b.setProperty(Constants.IMPORT_PACKAGE, "!javax.json.bind.*,*");
			b.setProperty(Constants.DYNAMICIMPORT_PACKAGE, "javax.json.bind.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/i"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json.bind"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_STATIC_PHASE);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);
		}
	}

	@Test
	public void moduleWithAllResolutionOptionalImportsIsStatic() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;version='1.2.7-module'");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.i.*");
			b.setProperty(Constants.IMPORT_PACKAGE, "javax.json.bind.*;resolution:=optional,*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/i"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json.bind"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_STATIC_PHASE);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);
		}
	}

	@Test
	public void moduleWithNoImportsIsStatic() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;modules='java.management';version='1.2.7-module'");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.i.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/i"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(5)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("java.management"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.management"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_STATIC_PHASE);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);
		}
	}

	@Test
	public void moduleManualConfiguration() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo.module;modules='bar';version='1.2.7-module'");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.i.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo.module");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7-module");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/i"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(5)
				.anyMatch(e -> e.requires.equals("bar"))
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json.bind"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(0);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);

			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void moduleRequiresModuleB() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.i.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json.bind-api-1.0.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/i"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"))
				.anyMatch(e -> e.requires.equals("java.json.bind"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.json.bind"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(0);

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);

			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test // (expected = AssertionError.class)
	public void moduleRequiresModuleA() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.i.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			b.addClasspath(IO.getFile("testresources/javax.json-api-1.1.3.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/i"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(3)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.anyMatch(e -> e.requires.equals("java.json"));

			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void moduleRequiresA() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.h.*");
			b.addClasspath(new File("bin_test"));
			b.addClasspath(IO.getFile("testresources/osgi.annotation-7.0.0.jar"));
			b.addClasspath(IO.getFile("testresources/geronimo-jcdi_2.0_spec-1.1.jar"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(1)
				.anyMatch(e -> e.exports.equals("test/jpms/h"));
			assertThat(moduleAttribute.provides).hasSize(0);

			assertThat(moduleAttribute.requires).hasSize(2)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("geronimo-jcdi_2.0_spec"));

			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("geronimo-jcdi_2.0_spec"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);

			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void moduleServiceProvider() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.g.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(ModuleAttribute.ACC_OPEN);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(0);
			assertThat(moduleAttribute.provides).hasSize(1)
				.allMatch(e -> e.provides.equals("java/lang/Cloneable"));
			assertThat(moduleAttribute.provides).flatExtracting(e -> Arrays.asList(e.provides_with))
				.containsExactlyInAnyOrder("test/jpms/g/Foo");

			assertThat(moduleAttribute.requires).hasSize(1)
				.anyMatch(e -> e.requires.equals("java.base"));
			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void moduleServiceConsumer() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.f.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();
			jar.getManifest()
				.write(System.out);
			assertTrue(b.check());

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(ModuleAttribute.ACC_OPEN);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(0);
			assertThat(moduleAttribute.provides).hasSize(0);
			assertThat(moduleAttribute.requires).hasSize(1)
				.anyMatch(e -> e.requires.equals("java.base"));
			assertThat(moduleAttribute.uses).hasSize(4)
				.containsExactlyInAnyOrder("java/lang/Integer", "java/lang/Long", "java/lang/Number",
					"test/jpms/f/Foo");

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void moduleOpenPackage() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.e.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(2)
				.anyMatch(e -> e.opens.equals("test/jpms/e/other"))
				.anyMatch(e -> e.opens.equals("test/jpms/e"));

			assertThat(moduleAttribute.opens).filteredOn(e -> e.opens.equals("test/jpms/e/other"))
				.allMatch(e -> e.opens_flags == 0)
				.flatExtracting(e -> Arrays.asList(e.opens_to))
				.containsExactlyInAnyOrder("other");

			assertThat(moduleAttribute.opens).filteredOn(e -> e.opens.equals("test/jpms/e"))
				.allMatch(e -> e.opens_flags == 0)
				.flatExtracting(e -> Arrays.asList(e.opens_to))
				.hasSize(0);

			assertThat(moduleAttribute.exports).hasSize(0);
			assertThat(moduleAttribute.provides).hasSize(0);
			assertThat(moduleAttribute.requires).hasSize(1)
				.anyMatch(e -> e.requires.equals("java.base"));
			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void moduleOpenFromExtenderRequirement() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.d.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(ModuleAttribute.ACC_OPEN);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(0);
			assertThat(moduleAttribute.provides).hasSize(0);
			assertThat(moduleAttribute.requires).hasSize(1)
				.anyMatch(e -> e.requires.equals("java.base"));
			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void moduleWithMainClass() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.c.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(0);
			assertThat(moduleAttribute.provides).hasSize(0);
			assertThat(moduleAttribute.requires).hasSize(1)
				.anyMatch(e -> e.requires.equals("java.base"));
			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNotNull();
			assertThat(moduleMainClassAttribute.main_class).isEqualTo("test/jpms/c/Foo");
		}
	}

	@Test
	public void moduleWithExports() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.b.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(2)
				.anyMatch(e -> e.exports.equals("test/jpms/b/other"))
				.anyMatch(e -> e.exports.equals("test/jpms/b"));

			assertThat(moduleAttribute.exports).filteredOn(e -> e.exports.equals("test/jpms/b/other"))
				.allMatch(e -> e.exports_flags == 0)
				.flatExtracting(e -> Arrays.asList(e.exports_to))
				.containsExactlyInAnyOrder("other", "bar");

			assertThat(moduleAttribute.exports).filteredOn(e -> e.exports.equals("test/jpms/b"))
				.allMatch(e -> e.exports_flags == 0)
				.flatExtracting(e -> Arrays.asList(e.exports_to))
				.hasSize(0);

			assertThat(moduleAttribute.provides).hasSize(0);
			assertThat(moduleAttribute.requires).hasSize(1)
				.anyMatch(e -> e.requires.equals("java.base"));
			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void simpleModule() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.JPMS_MODULE_INFO, "foo");
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "foo");
			b.setProperty(Constants.BUNDLE_VERSION, "1.2.7");
			b.setProperty(Constants.PRIVATEPACKAGE, "test.jpms.a.*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Resource moduleInfo = jar.getResource(Constants.MODULE_INFO_CLASS);
			assertNotNull(moduleInfo);

			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(moduleInfo.openInputStream()));

			assertThat(module_info.this_class).isEqualTo("module-info");

			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleAttribute.module_name).isEqualTo("foo");
			assertThat(moduleAttribute.module_version).isEqualTo("1.2.7");
			assertThat(moduleAttribute.module_flags).isEqualTo(0);
			assertThat(moduleAttribute.opens).hasSize(0);
			assertThat(moduleAttribute.exports).hasSize(0);
			assertThat(moduleAttribute.provides).hasSize(0);
			assertThat(moduleAttribute.requires).hasSize(1)
				.anyMatch(e -> e.requires.equals("java.base"));
			assertThat(moduleAttribute.uses).hasSize(0);

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);

			assertThat(moduleMainClassAttribute).isNull();
		}
	}

}
