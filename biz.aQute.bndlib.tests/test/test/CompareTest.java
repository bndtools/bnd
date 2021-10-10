package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import test.CompareTest.ComparingTest.A0;
import test.CompareTest.ComparingTest.A1;
import test.CompareTest.ComparingTest.A2;
import test.CompareTest.ComparingTest.A3;

public class CompareTest {

	public static class ComparingTest<O extends Serializable> {
		public interface I<T> {}

		public class B {
			class C<X extends O> {}
		}

		public class A0 {}

		// Declarations
		public class A1<T> { //
		}

		public class A2<T extends Serializable> { //
		}

		public class A3<T extends Exception & Serializable> { //
		}

		public static String foo0() {
			return null;
		}

		public static <T> T foo1() {
			return null;
		}

		public static <T extends Serializable> T foo2() {
			return null;
		}

		public static <T extends Exception & I<String>> T foo3() {
			return null;
		}

		@SuppressWarnings("static-method")
		public <T extends O> T foo4() {
			return null;
		}

		public A1<Collection<String>[]> a1;

		@SuppressWarnings("static-method")
		<Y, X extends A1<Y>> A1<? extends X> bar() {
			return null;
		}
	}

	@Test
	public void testGenericDeclaration() throws Exception {
		assertTrue(A0.class.getTypeParameters().length == 0);

		assertTrue(A1.class.getTypeParameters().length == 1);
		assertEquals("T", A1.class.getTypeParameters()[0].getName());
		assertEquals(1, A1.class.getTypeParameters()[0].getBounds().length);
		assertEquals(Object.class, A1.class.getTypeParameters()[0].getBounds()[0]);

		assertTrue(A2.class.getTypeParameters().length == 1);
		assertEquals("T", A2.class.getTypeParameters()[0].getName());
		assertEquals(1, A2.class.getTypeParameters()[0].getBounds().length);
		assertEquals(Serializable.class, A2.class.getTypeParameters()[0].getBounds()[0]);

		assertTrue(A3.class.getTypeParameters().length == 1);
		assertEquals("T", A3.class.getTypeParameters()[0].getName());
		assertEquals(2, A3.class.getTypeParameters()[0].getBounds().length);
		assertEquals(Exception.class, A3.class.getTypeParameters()[0].getBounds()[0]);
		assertEquals(Serializable.class, A3.class.getTypeParameters()[0].getBounds()[1]);

		Method m = ComparingTest.class.getMethod("foo0");
		assertEquals(0, m.getTypeParameters().length);

		m = ComparingTest.class.getMethod("foo1");
		assertEquals(1, m.getTypeParameters().length);
		assertEquals("T", m.getTypeParameters()[0].getName());
		assertEquals(1, m.getTypeParameters()[0].getBounds().length);
		assertEquals(Object.class, m.getTypeParameters()[0].getBounds()[0]);

		m = ComparingTest.class.getMethod("foo3");
		assertEquals(1, m.getTypeParameters().length);
		assertEquals("T", m.getTypeParameters()[0].getName());
		assertEquals(2, m.getTypeParameters()[0].getBounds().length);
		assertEquals(Exception.class, m.getTypeParameters()[0].getBounds()[0]);

		m = ComparingTest.class.getMethod("foo4");
		assertEquals(1, m.getTypeParameters().length);
		assertEquals("T", m.getTypeParameters()[0].getName());
		assertEquals(1, m.getTypeParameters()[0].getBounds().length);
		assertTrue(m.getTypeParameters()[0].getBounds()[0] instanceof TypeVariable);
		assertEquals(ComparingTest.class,
			((TypeVariable<?>) m.getTypeParameters()[0].getBounds()[0]).getGenericDeclaration());
	}
}
