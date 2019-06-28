package aQute.libg.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.Test;
import org.osgi.framework.Version;

public class ParameterMapTest {

	@Test
	public void testDuplicateKeys() {
		ParameterMap p = new ParameterMap(" foo , foo;abc=1;def=2.1, foo;def=2, foo");

		assertThat(p.keySet()).containsExactly("foo", "foo~", "foo~~", "foo~~~");

		assertThat(p.get("foo")).hasSize(0);
		assertThat(p.get("foo~")).hasSize(2);
		assertThat(p.get("foo~~")).hasSize(1);
		assertThat(p.get("foo~~~")).hasSize(0);

		assertThat(p.get("foo~")).containsKeys("abc", "def");
		assertThat(p.get("foo~")).containsKeys("def");

		assertThat(p.toString()).isEqualTo("foo,foo;abc=1;def=\"2.1\",foo;def=2,foo");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTypes() {
		ParameterMap p = new ParameterMap(
			"values;long:Long=1;string=s; version:Version=\"1.2.3\"; double:Double=\"1.2\"; longs:List<Long>=\"1,2,3\"; strings:List<String>=\"s1,s2,s3\"; doubles:List<Double>=\"1,2,3\"; versions:List<Version>=\"1.0,1.2,1.3\"");

		Attributes attributes = p.get("values");
		assertThat(attributes).isNotNull();
		System.out.println(p);
		assertThat(attributes.getTyped("long")).isEqualTo(1L);
		assertThat(attributes.getTyped("string")).isEqualTo("s");
		assertThat((double) attributes.getTyped("double")).isEqualTo(1.2D);
		assertThat((Version) attributes.getTyped("version")).isEqualTo(Version.valueOf("1.2.3"));

		assertThat((Collection<Long>) attributes.getTyped("longs")).containsExactlyInAnyOrder(1L, 2L, 3L);
		assertThat((Collection<String>) attributes.getTyped("strings")).containsExactlyInAnyOrder("s1", "s2", "s3");
		assertThat((Collection<Double>) attributes.getTyped("doubles")).containsExactlyInAnyOrder(1D, 2D, 3D);
		assertThat((Collection<Version>) attributes.getTyped("versions")).containsExactlyInAnyOrder(new Version("1.0"),
			new Version("1.3"), new Version("1.2"));
	}

	public void testKeySet() {
		ParameterMap p = new ParameterMap(" a, b, c, d, e");

		assertThat(p.keySet()).containsExactly("a", "b", "c", "d", "e");
		assertThat(p.get("a")).isNotNull();
	}
}
