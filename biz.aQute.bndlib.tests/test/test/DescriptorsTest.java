package test;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;

@ExtendWith(SoftAssertionsExtension.class)
public class DescriptorsTest {


	@Test
	public void testReferences(SoftAssertions softly) {
		Descriptors d = new Descriptors();
		TypeRef r = d.getTypeRef("[B");
		assertThat(r).isNotNull();
		softly.assertThat(r.getFQN())
			.isEqualTo("byte[]");
		assertThat(r.getPackageRef()).isNotNull();
		softly.assertThat(r.getPackageRef()
			.getFQN())
			.isEqualTo(".");

		PackageRef a = d.getPackageRef("a.b.c");
		PackageRef b = d.getPackageRef("a/b/c");
		softly.assertThat(a)
			.isSameAs(b);

	}

	@Test
	public void testDetermine(SoftAssertions softly) {
		softly.assertThat(Descriptors.determine("simple")
			.unwrap()).isEqualTo(new String[] {
				"simple", null
		});
		softly.assertThat(Descriptors.determine("Simple")
			.unwrap()).isEqualTo(new String[] {
				null, "Simple"
		});
		softly.assertThat(Descriptors.determine("simple.Simple")
			.unwrap()).isEqualTo(new String[] {
				"simple", "Simple"
		});
		softly.assertThat(Descriptors.determine("simple.sample")
			.unwrap()).isEqualTo(new String[] {
				"simple.sample", null
		});
		softly.assertThat(Descriptors.determine("simple.sample.Simple")
			.unwrap()).isEqualTo(new String[] {
				"simple.sample", "Simple"
		});
		softly.assertThat(Descriptors.determine("Simple.Sample")
			.unwrap()).isEqualTo(new String[] {
				null, "Simple.Sample"
		});
		softly.assertThat(Descriptors.determine("foo.bar.Simple.Sample")
			.unwrap()).isEqualTo(new String[] {
				"foo.bar", "Simple.Sample"
		});
		softly.assertThat(Descriptors.determine("foo.bar.Simple$Sample")
			.unwrap()).isEqualTo(new String[] {
				"foo.bar", "Simple$Sample"
		});

		softly.assertThat(Descriptors.determine("")
			.isErr()).isTrue();
		softly.assertThat(Descriptors.determine("123")
			.isErr()).isTrue();
		softly.assertThat(Descriptors.determine(".123")
			.isErr()).isTrue();
		softly.assertThat(Descriptors.determine("@foo.bar.Soo")
			.isErr()).isTrue();
	}

	@Test
	void classToBinary(SoftAssertions softly) {
		softly.assertThat(Descriptors.classToPath("MyClass"))
			.isEqualTo("MyClass.class");
		softly.assertThat(Descriptors.classToPath("MyClass.MyInnerClass"))
			.isEqualTo("MyClass$MyInnerClass.class");
		softly.assertThat(Descriptors.classToPath("MyClass.MyInnerClass.MyDoubleNester"))
			.isEqualTo("MyClass$MyInnerClass$MyDoubleNester.class");
	}

	@ParameterizedTest(name = "[{index}] {0}=>{1}.class")
	@CsvSource({
		//@formatter:off
		"simple, simple",
		"simple.Simple, simple/Simple",
		"simple.sample, simple/sample",
		"simple.sample.Simple, simple/sample/Simple",
		"Simple.Sample, Simple$Sample",
		"foo.bar.Simple.Sample, foo/bar/Simple$Sample",
		"foo.bar.Simple$Sample, foo/bar/Simple$Sample"
		//@formatter:on
	})
	void fqnClassToBinary(String input, String expected) {
		assertThat(Descriptors.fqnClassToBinary(input)).isEqualTo(expected + ".class");
	}
}
