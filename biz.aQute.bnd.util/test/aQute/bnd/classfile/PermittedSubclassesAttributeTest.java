package aQute.bnd.classfile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import org.assertj.core.api.ObjectArrayAssert;
import org.junit.jupiter.api.Test;

import aQute.bnd.classfile.preview.PermittedSubclassesAttribute;
import aQute.lib.io.IO;

public class PermittedSubclassesAttributeTest {
	private static final int MAJOR_JAVA_16 = 60;

	@Test
	public void top_level_sealed() throws Exception {
		try (InputStream stream = IO.stream(new File("testresources/sealed/Expr.class"))) {
			ClassFile clazz = ClassFile.parseClassFile(new DataInputStream(stream));
			assertThat(clazz.this_class).isEqualTo("Expr");
			assertThat(clazz.super_class).isEqualTo("java/lang/Object");
			assertThat(clazz.major_version).isGreaterThanOrEqualTo(MAJOR_JAVA_16);

			assertPermittedSubclasses(clazz).containsExactlyInAnyOrder("ConstantExpr", "PlusExpr", "TimesExpr",
				"NegExpr", "OtherExpr", "SubExpr");
		}
	}

	@Test
	public void sub_level_sealed() throws Exception {
		try (InputStream stream = IO.stream(new File("testresources/sealed/SubExpr.class"))) {
			ClassFile clazz = ClassFile.parseClassFile(new DataInputStream(stream));
			assertThat(clazz.this_class).isEqualTo("SubExpr");
			assertThat(clazz.super_class).isEqualTo("java/lang/Object");
			assertThat(clazz.major_version).isGreaterThanOrEqualTo(MAJOR_JAVA_16);

			assertPermittedSubclasses(clazz).containsExactlyInAnyOrder("SubExpr1", "SubExpr2");
		}
	}

	@Test
	public void sub_level_nonsealed() throws Exception {
		try (InputStream stream = IO.stream(new File("testresources/sealed/OtherExpr.class"))) {
			ClassFile clazz = ClassFile.parseClassFile(new DataInputStream(stream));
			assertThat(clazz.this_class).isEqualTo("OtherExpr");
			assertThat(clazz.super_class).isEqualTo("java/lang/Object");
			assertThat(clazz.major_version).isGreaterThanOrEqualTo(MAJOR_JAVA_16);

			assertPermittedSubclasses(clazz).isNull();
		}
	}

	private ObjectArrayAssert<String> assertPermittedSubclasses(ClassFile clazz) {
		return assertThat(Arrays.stream(clazz.attributes)
			.filter(PermittedSubclassesAttribute.class::isInstance)
			.map(PermittedSubclassesAttribute.class::cast)
			.map(a -> a.classes)
			.findFirst()
			.orElse(null));
	}
}
