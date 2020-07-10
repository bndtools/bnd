package aQute.bnd.classfile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import aQute.bnd.classfile.preview.PermittedSubclassesAttribute;
import aQute.bnd.osgi.Clazz;
import aQute.lib.io.IO;

public class PermittedSubclassesAttributeTest {

	@Test
	public void testPermittedSubclasses() throws Exception {
		try (InputStream stream = IO.stream(new File("testresources/sealed/Expr.class"))) {
			ClassFile clazz = ClassFile.parseClassFile(new DataInputStream(stream));
			assertThat(clazz.this_class).isEqualTo("Expr");
			assertThat(clazz.super_class).isEqualTo("java/lang/Object");
			assertThat(clazz.major_version).isGreaterThanOrEqualTo(Clazz.JAVA.OpenJDK15.getMajor());

			PermittedSubclassesAttribute permittedSubclassesAttribute = Arrays.stream(clazz.attributes)
				.filter(PermittedSubclassesAttribute.class::isInstance)
				.map(PermittedSubclassesAttribute.class::cast)
				.findFirst()
				.orElse(null);
			assertThat(permittedSubclassesAttribute).isNotNull();
			assertThat(permittedSubclassesAttribute.classes).containsExactlyInAnyOrder("ConstantExpr", "PlusExpr",
				"TimesExpr", "NegExpr");
		}
	}

}
