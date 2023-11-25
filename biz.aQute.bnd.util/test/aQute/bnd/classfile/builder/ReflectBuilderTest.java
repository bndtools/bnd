package aQute.bnd.classfile.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import aQute.bnd.classfile.ClassFile;

class ReflectBuilderTest {

	@Test
	void testSupplier() {
		ClassFileBuilder cfb = ReflectBuilder.of(Supplier.class);
		ClassFile build = cfb.build();
		assertThat(build).isNotNull();
		assertThat(build.methods).hasSize(1);
		assertThat(build.methods[0].name).isEqualTo("get");
		assertThat(build.methods[0].descriptor).isEqualTo("()Ljava.lang.Object;");
	}

	@Test
	void testFunction() throws IOException {
		ClassFileBuilder cfb = ReflectBuilder.of(Function.class);
		ClassFile build = cfb.build();
		assertThat(build).isNotNull();
		assertThat(build.methods).hasSize(7);
		byte[] write = build.write();
		assertThat(write).isNotNull();
	}
}
