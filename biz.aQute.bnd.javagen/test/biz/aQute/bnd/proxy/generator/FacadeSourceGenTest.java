package biz.aQute.bnd.proxy.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.io.IO;
import biz.aQute.bnd.proxy.generator.TestFacade.Facade;

class FacadeSourceGenTest {

	/*
	 * Helper to generate a test generated class
	 */
	public static void main(String[] args) throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.addClasspath(IO.getFile("bin_test"));

			TypeRef base = a.getTypeRefFrom(TestBase.class);

			TypeRef domain_fc = a.getTypeRefFrom(FacadeClass.class);
			TypeRef domain_fi = a.getTypeRefFrom(FacadeInterface.class);

			TypeRef facade = a.getTypeRefFromFQN("biz.aQute.bnd.proxy.generator.TestFacade");

			Source fsg = new Source(a, facade, base, /* domain_fc, */ domain_fi);
			String source = fsg.source();
			System.out.println(source);
		}
	}

	@Test
	public void test() {
		List<String> events = new ArrayList<>();
		AtomicReference<Object> facade = new AtomicReference<>();

		class R implements TestFacade.Delegate {

			@Override
			public void foo() {
				events.add("foo");
			}

			@Override
			public int fooint() {
				events.add("fooint");
				return 42;
			}

			@Override
			public String fooString() {
				events.add("foostring");
				return "42";
			}

			@Override
			public void foo(String a1, NetworkInterface ni) {
				events.add("foo(String,NetworkInterface)");
			}

			@Override
			public void bar() {
				events.add("bar");
			}

			@Override
			public <T, E extends T> List<T> generics(Class<?> arg0, Set<? super E> arg1) throws Exception {
				events.add("generics");
				return Collections.emptyList();
			}

			@Override
			public void clean(Map<String, String> args) throws Exception {
				// TODO Auto-generated method stub

			}

		}
		TestFacade f = new TestFacade();
		Facade d = f.createFacade((b) -> {
			R r = new R();
			facade.set(b);
			return () -> r;
		});
		d.foo();
		assertThat(events.remove(0)).isEqualTo("foo");
		d.foo("abc", null);
		assertThat(events.remove(0)).isEqualTo("foo(String,NetworkInterface)");

		d.bar();
		assertThat(events.remove(0)).isEqualTo("bar");
		assertThat(events.isEmpty());
	}

}
