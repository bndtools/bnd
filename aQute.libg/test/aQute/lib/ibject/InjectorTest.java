package aQute.lib.ibject;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import aQute.lib.inject.Injector;

public class InjectorTest {

	@Retention(RetentionPolicy.RUNTIME)
	@interface Foo {

	}

	static class BaseSimpleTest {
		@Foo
		public int n = 10;

	}

	static class SimpleTest extends BaseSimpleTest {

		@Foo
		double					d	= 10;

		@Foo
		protected String		ref	= "";

		@Foo
		private List<String>	strings;
	}

	@Test
	public void nullHandling() throws Exception {
		Injector<Foo> inj = new Injector<>(this::nullReturn, Foo.class);
		SimpleTest st = new SimpleTest();
		inj.inject(st);
		assertThat(st.n).isEqualTo(0);
		assertThat(st.d).isEqualTo(0D);
		assertThat(st.ref).isEqualTo(null);
		assertThat(st.strings).isEqualTo(null);
	}

	Object nullReturn(Injector.Target<Foo> param) {
		return null;
	}

	@Test
	public void valueHandling() throws Exception {
		Injector<Foo> inj = new Injector<>(this::fiveReturn, Foo.class);
		SimpleTest st = new SimpleTest();
		inj.inject(st);
		assertThat(st.n).isEqualTo(5);
		assertThat(st.d).isEqualTo(5D);
		assertThat(st.ref).isEqualTo("5");
		assertThat(st.strings).containsAll(Arrays.asList("5"));
	}

	Object fiveReturn(Injector.Target<Foo> param) {
		return "5";
	}

	@Test
	public void testNewInstance() throws Exception {
		Injector<Foo> inj = new Injector<>(this::fiveReturn, Foo.class);
		SimpleTest construct = inj.newInstance(SimpleTest.class);
		assertThat(construct).isNotNull();
		assertThat(construct.n).isEqualTo(5);
		assertThat(construct.d).isEqualTo(5D);
		assertThat(construct.ref).isEqualTo("5");
		assertThat(construct.strings).containsAll(Arrays.asList("5"));
	}

	static class ConstructorInj {
		final int			n;
		final double		d;
		final String		ref;
		final List<String>	strings;

		@Foo
		ConstructorInj(int n, double d, String ref, List<String> strings) {
			this.n = n;
			this.d = d;
			this.ref = ref;
			this.strings = strings;
		}
	}

	@Test
	public void testConstructorInjection() throws Exception {
		Injector<Foo> inj = new Injector<>(this::fiveReturn, Foo.class);
		ConstructorInj construct = inj.newInstance(ConstructorInj.class);
		assertThat(construct).isNotNull();
		assertThat(construct.n).isEqualTo(5);
		assertThat(construct.d).isEqualTo(5D);
		assertThat(construct.ref).isEqualTo("5");
		assertThat(construct.strings).containsAll(Arrays.asList("5"));
	}

}
