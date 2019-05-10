package aQute.bnd.classfile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

import aQute.bnd.classfile.writer.ModuleInfoWriter;
import aQute.bnd.osgi.Clazz;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.ByteBufferDataOutput;
import aQute.lib.io.IO;

public class ModuleInfoTest {

	@Test
	public void testModuleInfoJavac() throws Exception {
		try (InputStream stream = IO.stream(new File("testresources/module-info/module1/module-info.class"))) {
			ClassFile module_info = ClassFile.parseClassFile(new DataInputStream(stream));
			assertThat(module_info.this_class).isEqualTo("module-info");
			ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst()
				.orElse(null);
			assertThat(moduleAttribute).isNotNull();

			assertThat(moduleAttribute.module_name).isEqualTo("com.example.foo");
			assertThat(moduleAttribute.module_version).isNull();

			assertThat(moduleAttribute.requires).hasSize(4)
				.anyMatch(e -> e.requires.equals("java.base"))
				.anyMatch(e -> e.requires.equals("java.logging"))
				.anyMatch(e -> e.requires.equals("java.compiler"))
				.anyMatch(e -> e.requires.equals("java.desktop"));
			assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.desktop"))
				.flatExtracting(e -> Arrays.asList(e.requires_flags))
				.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);

			assertThat(moduleAttribute.exports).hasSize(2)
				.anyMatch(e -> e.exports.equals("toexport"))
				.anyMatch(e -> e.exports.equals("toexporttosomeone"));
			assertThat(moduleAttribute.exports).filteredOn(e -> e.exports.equals("toexporttosomeone"))
				.flatExtracting(e -> Arrays.asList(e.exports_to))
				.containsExactlyInAnyOrder("java.logging", "java.naming");

			assertThat(moduleAttribute.opens).hasSize(2)
				.anyMatch(e -> e.opens.equals("toopen"))
				.anyMatch(e -> e.opens.equals("toopentosomeone"));
			assertThat(moduleAttribute.opens).filteredOn(e -> e.opens.equals("toopentosomeone"))
				.flatExtracting(e -> Arrays.asList(e.opens_to))
				.containsExactlyInAnyOrder("java.logging", "java.naming");

			assertThat(moduleAttribute.uses).hasSize(1)
				.containsExactlyInAnyOrder("foo/Foo");

			assertThat(moduleAttribute.provides).hasSize(1)
				.allMatch(e -> e.provides.equals("foo/Foo"));
			assertThat(moduleAttribute.provides).flatExtracting(e -> Arrays.asList(e.provides_with))
				.containsExactlyInAnyOrder("foo/FooImpl");

			ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleMainClassAttribute.class::isInstance)
				.map(ModuleMainClassAttribute.class::cast)
				.findFirst()
				.orElse(null);
			assertThat(moduleMainClassAttribute).isNull();
		}
	}

	@Test
	public void testModuleInfoWriter() throws Exception {
		ModuleInfoWriter writer = new ModuleInfoWriter(Clazz.JAVA.OpenJDK9.getMajor(), "biz.aQute.bnd.test", null,
			false);
		writer.requires("java.logging", 0, null);
		writer.requires("java.compiler", 0, null);
		writer.requires("java.desktop", ModuleAttribute.Require.ACC_TRANSITIVE, null);
		writer.exports("toexport", 0);
		writer.exports("toexporttosomeone", 0, "java.logging", "java.naming");
		writer.opens("toopen", 0);
		writer.opens("toopentosomeone", 0, "java.logging", "java.naming");
		writer.uses("foo/Foo");
		writer.provides("foo/Foo", "foo/FooImpl");
		writer.mainClass("foo/FooMain");
		ByteBufferDataOutput bbout = new ByteBufferDataOutput();
		writer.write(bbout);
		DataInput in = ByteBufferDataInput.wrap(bbout.toByteBuffer());

		ClassFile module_info = ClassFile.parseClassFile(in);
		assertThat(module_info.this_class).isEqualTo("module-info");
		ModuleAttribute moduleAttribute = Arrays.stream(module_info.attributes)
			.filter(ModuleAttribute.class::isInstance)
			.map(ModuleAttribute.class::cast)
			.findFirst()
			.orElse(null);
		assertThat(moduleAttribute).isNotNull();

		assertThat(moduleAttribute.module_name).isEqualTo("biz.aQute.bnd.test");
		assertThat(moduleAttribute.module_version).isNull();

		assertThat(moduleAttribute.requires).hasSize(4)
			.anyMatch(e -> e.requires.equals("java.base"))
			.anyMatch(e -> e.requires.equals("java.logging"))
			.anyMatch(e -> e.requires.equals("java.compiler"))
			.anyMatch(e -> e.requires.equals("java.desktop"));
		assertThat(moduleAttribute.requires).filteredOn(e -> e.requires.equals("java.desktop"))
			.flatExtracting(e -> Arrays.asList(e.requires_flags))
			.containsExactlyInAnyOrder(ModuleAttribute.Require.ACC_TRANSITIVE);

		assertThat(moduleAttribute.exports).hasSize(2)
			.anyMatch(e -> e.exports.equals("toexport"))
			.anyMatch(e -> e.exports.equals("toexporttosomeone"));
		assertThat(moduleAttribute.exports).filteredOn(e -> e.exports.equals("toexporttosomeone"))
			.flatExtracting(e -> Arrays.asList(e.exports_to))
			.containsExactlyInAnyOrder("java.logging", "java.naming");

		assertThat(moduleAttribute.opens).hasSize(2)
			.anyMatch(e -> e.opens.equals("toopen"))
			.anyMatch(e -> e.opens.equals("toopentosomeone"));
		assertThat(moduleAttribute.opens).filteredOn(e -> e.opens.equals("toopentosomeone"))
			.flatExtracting(e -> Arrays.asList(e.opens_to))
			.containsExactlyInAnyOrder("java.logging", "java.naming");

		assertThat(moduleAttribute.uses).hasSize(1)
			.containsExactlyInAnyOrder("foo/Foo");

		assertThat(moduleAttribute.provides).hasSize(1)
			.allMatch(e -> e.provides.equals("foo/Foo"));
		assertThat(moduleAttribute.provides).flatExtracting(e -> Arrays.asList(e.provides_with))
			.containsExactlyInAnyOrder("foo/FooImpl");

		ModuleMainClassAttribute moduleMainClassAttribute = Arrays.stream(module_info.attributes)
			.filter(ModuleMainClassAttribute.class::isInstance)
			.map(ModuleMainClassAttribute.class::cast)
			.findFirst()
			.orElse(null);
		assertThat(moduleMainClassAttribute).isNotNull();
		assertThat(moduleMainClassAttribute.main_class).isEqualTo("foo/FooMain");

	}

}
