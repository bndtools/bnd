package test;

import static org.assertj.core.api.Assertions.assertThat;

import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import junit.framework.TestCase;

public class DescriptorsTest extends TestCase {

	public static void testReferences() {
		Descriptors d = new Descriptors();
		TypeRef r = d.getTypeRef("[B");
		assertNotNull(r);
		assertEquals("byte[]", r.getFQN());
		assertNotNull(r.getPackageRef());
		assertEquals(".", r.getPackageRef()
			.getFQN());

		PackageRef a = d.getPackageRef("a.b.c");
		PackageRef b = d.getPackageRef("a/b/c");
		assertTrue(a == b);

	}

	public void testDetermine() {
		assertThat(Descriptors.determine("simple")
			.unwrap()).isEqualTo(new String[] {
				"simple", null
		});
		assertThat(Descriptors.determine("Simple")
			.unwrap()).isEqualTo(new String[] {
				null, "Simple"
		});
		assertThat(Descriptors.determine("simple.Simple")
			.unwrap()).isEqualTo(new String[] {
				"simple", "Simple"
		});
		assertThat(Descriptors.determine("simple.sample")
			.unwrap()).isEqualTo(new String[] {
				"simple.sample", null
		});
		assertThat(Descriptors.determine("simple.sample.Simple")
			.unwrap()).isEqualTo(new String[] {
				"simple.sample", "Simple"
		});
		assertThat(Descriptors.determine("Simple.Sample")
			.unwrap()).isEqualTo(new String[] {
				null, "Simple.Sample"
		});
		assertThat(Descriptors.determine("foo.bar.Simple.Sample")
			.unwrap()).isEqualTo(new String[] {
				"foo.bar", "Simple.Sample"
		});
		assertThat(Descriptors.determine("foo.bar.Simple$Sample")
			.unwrap()).isEqualTo(new String[] {
				"foo.bar", "Simple$Sample"
		});

		assertThat(Descriptors.determine("")
			.isErr()).isTrue();
		assertThat(Descriptors.determine("123")
			.isErr()).isTrue();
		assertThat(Descriptors.determine(".123")
			.isErr()).isTrue();
		assertThat(Descriptors.determine("@foo.bar.Soo")
			.isErr()).isTrue();
	}
}
