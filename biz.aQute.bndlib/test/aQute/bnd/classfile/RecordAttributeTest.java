package aQute.bnd.classfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import aQute.bnd.classfile.preview.RecordAttribute;
import aQute.bnd.osgi.Clazz;
import aQute.lib.io.IO;

public class RecordAttributeTest {

	@Test
	public void testRecord() throws Exception {
		try (InputStream stream = IO.stream(new File("testresources/record/MinMax.class"))) {
			ClassFile clazz = ClassFile.parseClassFile(new DataInputStream(stream));
			assertThat(clazz.this_class).isEqualTo("MinMax");
			assertThat(clazz.super_class).isEqualTo("java/lang/Record");
			assertThat(clazz.major_version).isGreaterThanOrEqualTo(Clazz.JAVA.OpenJDK15.getMajor());

			RecordAttribute recordAttribute = Arrays.stream(clazz.attributes)
				.filter(RecordAttribute.class::isInstance)
				.map(RecordAttribute.class::cast)
				.findFirst()
				.orElse(null);
			assertThat(recordAttribute).isNotNull();
			assertSoftly(softly -> {
				softly.assertThat(recordAttribute.components)
					.extracting(e -> e.name)
					.containsExactly("min", "max");

				AbstractListAssert<?, List<? extends Attribute>, Attribute, ObjectAssert<Attribute>> attributesAssert;

				attributesAssert = softly.assertThat(recordAttribute.components)
					.filteredOn(e -> e.name.equals("min"))
					.allMatch(e -> e.descriptor.equals("Ljava/lang/Object;"))
					.flatExtracting(e -> Arrays.asList(e.attributes))
					.hasSize(2);
				attributesAssert.filteredOn(a -> a.name()
					.equals(SignatureAttribute.NAME))
					.usingFieldByFieldElementComparator()
					.containsExactly(new SignatureAttribute("TT;"));
				attributesAssert.filteredOn(a -> a.name()
					.equals(RuntimeVisibleAnnotationsAttribute.NAME))
					.flatExtracting(a -> Arrays.asList(((RuntimeVisibleAnnotationsAttribute) a).annotations))
					.usingFieldByFieldElementComparator()
					.containsExactly(new AnnotationInfo("LFoo;", new ElementValueInfo[0]));

				attributesAssert = softly.assertThat(recordAttribute.components)
					.filteredOn(e -> e.name.equals("max"))
					.allMatch(e -> e.descriptor.equals("Ljava/lang/Object;"))
					.flatExtracting(e -> Arrays.asList(e.attributes))
					.hasSize(1);
				attributesAssert.filteredOn(a -> a.name()
					.equals(SignatureAttribute.NAME))
					.usingFieldByFieldElementComparator()
					.containsExactly(new SignatureAttribute("TT;"));
			});
		}
	}

}
