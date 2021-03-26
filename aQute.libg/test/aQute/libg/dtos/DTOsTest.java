package aQute.libg.dtos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.osgi.dto.DTO;

import aQute.lib.collections.ExtList;
import aQute.libg.dtos.DTOs.Difference;
import aQute.libg.map.BuilderMap;

public class DTOsTest {
	DTOs dtos = DTOs.INSTANCE;

	public static class A extends DTO {
		public int		a;
		public String	s;
		public List<B>	bs		= new ArrayList<>();
		public List<A>	parents	= new ArrayList<>();
		public A[]		as;
	}

	public static class B extends DTO {
		public short c;
	}

	@Test
	public void testSimple() throws Exception {
		A a = new A();
		a.a = 1;
		a.s = "string";

		B b = new B();
		b.c = 10;
		a.bs.add(b);
		A x = new A();
		x.a = 10;
		a.parents.add(x);

		A aa = dtos.deepCopy(a);

		assertThat(aa.a).isEqualTo(1);
		assertThat(aa.s).isEqualTo("string");
		assertThat(aa.bs).hasSize(1);
		assertThat(dtos.deepEquals(aa, a)).isTrue();

		assertThat(dtos.get(a, "s")
			.get()).isEqualTo("string");
		assertThat(dtos.get(a, "bs.0.c")
			.get()).isEqualTo((short) 10);

		A ax = new A();
		ax.as = new A[3];
		ax.as[1] = new A();
		ax.as[1].s = "A";
		assertThat(dtos.get(ax, "as.1.s")
			.get()).isEqualTo("A");

		Map<String, Object> map = BuilderMap.map("a", ax);
		assertThat(dtos.get(map, "a.as.1.s")).isPresent()
			.get()
			.isEqualTo("A");
	}

	/*
	 * Show Map -> Interface
	 */
	enum Option {
		bar,
		don,
		zun
	}

	interface FooMap {
		short port();

		String host();

		Set<Option> options();
	}

	/*
	 * Show DTO to map
	 */

	public static class MyData extends DTO {
		public short	port;
		public String	host;
		public Option[]	options;
	}

	@Test
	public void testDtoAsMap() throws Exception {
		MyData m = new MyData();
		m.port = 20;
		m.host = "example.com";
		m.options = new Option[] {
			Option.bar, Option.don, Option.zun
		};

		Map<String, Object> map = dtos.asMap(m);

		assertEquals(Arrays.asList("host", "options", "port"), new ArrayList<String>(map.keySet()));
		assertEquals((short) 20, map.get("port"));
		assertEquals("example.com", map.get("host"));
	}

	@Test
	public void testDiff() throws Exception {
		MyData source = new MyData();
		source.port = 20;
		source.host = "example.com";
		source.options = new Option[] {
			Option.bar, Option.don, Option.zun
		};

		MyData copy = dtos.deepCopy(source);

		assertFalse(source == copy);
		assertTrue(dtos.equals(source, copy));

		List<Difference> diff = dtos.diff(source, copy);
		assertEquals(0, diff.size());

		copy.port = 10;
		diff = dtos.diff(source, copy);
		assertEquals(1, diff.size());
		assertEquals("port", diff.get(0).path[0]);
	}

	@Test
	public void testCycleDetection() {
		try {
			A a = new A();
			a.parents.add(a);
			dtos.deepCopy(a);
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains("Cycle");
			return;
		}
		fail("Expected a cycle exception");
	}

	static public class X {
		public static String staticField;
	}
	@Test
	public void testValidDto() {
		assertThat(dtos.isDTO(new X())).isFalse();
	}

	@Test
	public void shallowCopyTest() {
		String s = "foo";
		assertThat(dtos.shallowCopy(s) == s).isTrue();

		byte[] bytes = new byte[] {
			1, 2, 3
		};
		assertThat(dtos.shallowCopy(bytes) == bytes).isFalse();
		assertThat(dtos.shallowCopy(bytes)).isEqualTo(bytes);

		Map<String, Object> map = BuilderMap.map("a", bytes);
		assertThat(dtos.shallowCopy(map) == map).isFalse();
		assertThat(dtos.shallowCopy(map)
			.get("a")).isEqualTo(bytes);

		List<Object> list = new ExtList<>(bytes);
		assertThat(dtos.shallowCopy(list) == list).isFalse();
		assertThat(dtos.shallowCopy(list)
			.get(0)).isEqualTo(bytes);

		A a = new A();
		a.parents.add(new A());
		a.bs.add(new B());
		A shallowCopy = dtos.shallowCopy(a);
		assertThat(a).isNotEqualTo(shallowCopy);
		assertThat(a.parents.get(0)).isEqualTo(shallowCopy.parents.get(0));
		assertThat(a.bs.get(0)).isEqualTo(shallowCopy.bs.get(0));
	}

	@Test
	public void deepCopyTest() {
		String s = "foo";
		assertThat(dtos.deepCopy(s) == s).isTrue();

		A[] as = new A[] {
			new A(), new A()
		};
		assertThat(dtos.deepCopy(as) == as).isFalse();
		assertThat(dtos.deepCopy(as)[0]).isNotEqualTo(as[0]);

		Map<String, Map<String, Object>> map = BuilderMap.map("a", new HashMap<>());
		assertThat(dtos.deepCopy(map) == map).isFalse();
		assertThat(dtos.deepCopy(map)
			.get("a") == map.get("a")).isFalse();

		List<A> list = new ExtList<>(as);
		assertThat(dtos.deepCopy(list) == list).isFalse();
		assertThat(dtos.deepCopy(list)
			.get(0)).isNotEqualTo(list.get(0));

		A a = new A();
		a.parents.add(new A());
		a.bs.add(new B());
		A copy = dtos.deepCopy(a);
		assertThat(a).isNotEqualTo(copy);
		assertThat(a.parents.get(0)).isNotEqualTo(copy.parents.get(0));
		assertThat(a.bs.get(0)).isNotEqualTo(copy.bs.get(0));
	}

	@Test
	public void fromSegmentsToPathsTest() {

		assertThat(dtos.fromPathToSegments("a.b\\.")).isEqualTo(new String[] {
			"a", "b."
		});
		assertThat(dtos.fromSegmentsToPath(new String[] {
			"a", "b."
		})).isEqualTo("a.b\\.");

	}

	@Test
	public void deepEqualsTest() {
		A[] as1 = new A[] {
			new A(), new A()
		};
		A[] as2 = new A[] {
			new A(), new A()
		};
		A[] as3 = new A[] {
			new A(), new A(), new A()
		};
		A[] as4 = new A[] {
			new A(), new A()
		};
		as4[1].parents.add(new A());

		assertThat(dtos.deepEquals(as1, as2)).isTrue();
		assertThat(dtos.deepEquals(as1, as3)).isFalse();
		assertThat(dtos.deepEquals(as1, as4)).isFalse();
	}

	@Test
	public void testToString() {

		assertThat(dtos.toString(null)).isEqualTo("null");
		X x = new X();
		assertThat(dtos.toString(x)).isEqualTo(x.toString());
		String s = dtos.toString(new A());
		assertThat(s).isNotNull();

	}

}
