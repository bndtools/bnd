package biz.aQute.bnd.proxy.generator;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.io.IO;
import biz.aQute.bnd.javagen.util.JavaSourceBuilder;

class JavaSourceBuilderTest {

	static class Subject<T> {
		void foo(T t) {

		}
	}
	@Test
	void test() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.addClasspath(IO.getFile("bin_test"));
			TypeRef subject = a.getTypeRefFrom(Subject.class);
			Clazz clazz = a.findClass(subject);

			JavaSourceBuilder sb = new JavaSourceBuilder();

			sb.public_()
				.class_("Test")
				.extends_("OtherTest")
				.implements_("A", "B")
				.body(() -> {
					clazz.methods()
						.filter(m -> !m.isConstructor())
						.forEach(m -> {
							sb.public_()
								.method(m)
								.body(null);
						});
				});

			System.out.println(sb);

			/**
			 * should test the output TODO
			 */
		}
	}

}
